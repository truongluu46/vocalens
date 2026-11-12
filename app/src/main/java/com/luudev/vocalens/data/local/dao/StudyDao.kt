package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StudyDao {
    @Query("SELECT * FROM study_sessions WHERE userId = :uid ORDER BY startedAt DESC")
    abstract fun observeSessions(uid: String): Flow<List<StudySessionEntity>>
    @Query("SELECT * FROM vocabulary_reviews WHERE userId = :uid ORDER BY reviewedAt DESC")
    abstract fun observeReviews(uid: String): Flow<List<VocabularyReviewEntity>>
    @Query("SELECT * FROM study_sessions WHERE userId = :uid AND sessionId = :id")
    abstract suspend fun session(uid: String, id: String): StudySessionEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertSession(value: StudySessionEntity): Long
    @Query("SELECT COUNT(*) FROM study_sessions WHERE userId = :uid AND status = 'ACTIVE'")
    protected abstract suspend fun activeCount(uid: String): Int
    @Query("SELECT MAX(endedAt) FROM study_sessions WHERE userId = :uid")
    protected abstract suspend fun lastEnd(uid: String): Long?
    @Transaction
    open suspend fun start(value: StudySessionEntity) {
        require(value.userId.isNotBlank() && value.sessionId.isNotBlank())
        require(value.sessionType in setOf("LESSON", "VOCAB_REVIEW"))
        require((value.sessionType == "LESSON") == (value.lessonId != null))
        require(value.status == "ACTIVE" && value.endedAt == null && value.activeDurationMs == 0L && value.earnedXp == 0)
        val old = session(value.userId, value.sessionId)
        if (old != null) { require(old.sessionType == value.sessionType && old.lessonId == value.lessonId); return }
        check(activeCount(value.userId) == 0) { "Finish or abandon the active session first" }
        require(value.startedAt >= (lastEnd(value.userId) ?: Long.MIN_VALUE)) { "Study sessions cannot overlap" }
        insertSession(value)
    }
    @Query("""UPDATE study_sessions SET status = :status, endedAt = :endedAt, activeDurationMs = :activeMs, earnedXp = :xp
        WHERE userId = :uid AND sessionId = :id AND status = 'ACTIVE'""")
    protected abstract suspend fun close(uid: String, id: String, status: String, endedAt: Long, activeMs: Long, xp: Int)
    @Transaction
    open suspend fun finish(uid: String, id: String, status: String, endedAt: Long, activeMs: Long, xp: Int) {
        val old = requireNotNull(session(uid, id))
        require(status in setOf("COMPLETED", "ABANDONED") && endedAt >= old.startedAt)
        require(activeMs in 0..(endedAt - old.startedAt) && xp >= 0 && (status == "COMPLETED" || xp == 0))
        if (old.status != "ACTIVE") return // Retry cannot award XP twice.
        require((latestReview(uid, id) ?: old.startedAt) <= endedAt)
        close(uid, id, status, endedAt, activeMs, xp)
    }
    @Query("SELECT MAX(reviewedAt) FROM vocabulary_reviews WHERE userId = :uid AND sessionId = :id")
    protected abstract suspend fun latestReview(uid: String, id: String): Long?
    @Query("SELECT COUNT(*) FROM user_vocabulary WHERE userId = :uid AND vocabularyId = :id")
    protected abstract suspend fun isSaved(uid: String, id: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertReview(value: VocabularyReviewEntity): Long
    @Query("SELECT * FROM vocabulary_reviews WHERE userId = :uid AND reviewId = :id")
    protected abstract suspend fun review(uid: String, id: String): VocabularyReviewEntity?
    @Query("""UPDATE user_vocabulary SET reviewCount = reviewCount + 1,
        correctCount = correctCount + :correct, learningStatus = CASE WHEN learningStatus = 'NEW' THEN 'LEARNING' ELSE learningStatus END,
        lastReviewedAt = MAX(COALESCE(lastReviewedAt,0),:at), updatedAt = MAX(updatedAt,:at)
        WHERE userId = :uid AND vocabularyId = :wordId""")
    protected abstract suspend fun countReview(uid: String, wordId: String, correct: Int, at: Long)
    @Transaction
    open suspend fun recordReview(value: VocabularyReviewEntity) {
        val old = review(value.userId, value.reviewId)
        if (old != null) { require(old == value) { "Review ID already used for a different event" }; return }
        val owner = requireNotNull(session(value.userId, value.sessionId))
        require(owner.status == "ACTIVE" && owner.sessionType == "VOCAB_REVIEW")
        require(value.reviewedAt >= owner.startedAt && (value.responseDurationMs == null || value.responseDurationMs >= 0))
        require(isSaved(value.userId, value.vocabularyId) == 1)
        require(when (value.reviewMode) {
            "SELF_RATED" -> value.rating in setOf("AGAIN", "HARD", "GOOD", "EASY") && value.isCorrect == null
            "QUIZ" -> value.rating == null && value.isCorrect != null
            else -> false
        })
        require(value.reviewId.isNotBlank())
        if (insertReview(value) != -1L) countReview(value.userId, value.vocabularyId, if (value.isCorrect == true) 1 else 0, value.reviewedAt)
    }
}

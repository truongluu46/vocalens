package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VocabularyDao {
    @Query("""SELECT v.* FROM vocabulary v JOIN user_vocabulary uv ON uv.vocabularyId = v.vocabularyId
        WHERE uv.userId = :userId ORDER BY uv.savedAt DESC, v.vocabularyId ASC""")
    abstract fun observeWords(userId: String): Flow<List<VocabularyEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertContent(word: VocabularyEntity)
    @Query("SELECT * FROM vocabulary WHERE vocabularyId = :id")
    protected abstract suspend fun content(id: String): VocabularyEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertSaved(word: UserVocabularyEntity)
    @Query("SELECT COUNT(*) FROM vocabulary_reviews WHERE userId = :uid AND vocabularyId = :id")
    protected abstract suspend fun reviewCount(uid: String, id: String): Int
    @Query("SELECT COUNT(*) FROM vocabulary_reviews WHERE userId = :uid AND vocabularyId = :id AND isCorrect = 1")
    protected abstract suspend fun correctCount(uid: String, id: String): Int
    @Query("SELECT MAX(reviewedAt) FROM vocabulary_reviews WHERE userId = :uid AND vocabularyId = :id")
    protected abstract suspend fun lastReviewed(uid: String, id: String): Long?
    @Transaction
    open suspend fun saveWord(userId: String, word: VocabularyEntity, now: Long) {
        require(userId.isNotBlank() && word.vocabularyId.isNotBlank() && word.english.isNotBlank() && word.meaningVi.isNotBlank())
        val existing = content(word.vocabularyId)
        require(existing == null || (existing.english == word.english && existing.phonetic == word.phonetic &&
            existing.meaningVi == word.meaningVi && existing.categoryCode == word.categoryCode)) { "Use a new ID for different vocabulary content" }
        insertContent(word)
        val last = lastReviewed(userId, word.vocabularyId)
        insertSaved(UserVocabularyEntity(userId, word.vocabularyId,
            learningStatus = if (last == null) "NEW" else "LEARNING", savedAt = now,
            lastReviewedAt = last, reviewCount = reviewCount(userId, word.vocabularyId),
            correctCount = correctCount(userId, word.vocabularyId), updatedAt = now))
    }
    @Query("""SELECT COUNT(*) FROM vocabulary_reviews r JOIN study_sessions s
        ON s.userId = r.userId AND s.sessionId = r.sessionId
        WHERE r.userId = :uid AND r.vocabularyId = :id AND s.status = 'ACTIVE'""")
    protected abstract suspend fun activeReviews(uid: String, id: String): Int
    @Query("DELETE FROM user_vocabulary WHERE userId = :userId AND vocabularyId = :wordId")
    protected abstract suspend fun removeSaved(userId: String, wordId: String)
    @Transaction
    open suspend fun deleteWord(userId: String, wordId: String) {
        check(activeReviews(userId, wordId) == 0) { "Finish the active review before removing this word" }
        removeSaved(userId, wordId)
    }
}

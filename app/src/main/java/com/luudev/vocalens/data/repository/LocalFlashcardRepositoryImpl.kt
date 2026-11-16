package com.luudev.vocalens.data.repository

import androidx.room.withTransaction
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.StudySessionEntity
import com.luudev.vocalens.data.local.entity.VocabularyReviewEntity
import com.luudev.vocalens.domain.model.FlashcardReview
import com.luudev.vocalens.domain.repository.FlashcardRepository

class LocalFlashcardRepositoryImpl(private val database: VocabLensDatabase) : FlashcardRepository {
    private val study = database.studyDao()

    override suspend fun start(userId: String, sessionId: String, startedAt: Long) = database.withTransaction {
        // Recover sessions interrupted by process death before opening a new deck.
        study.activeVocabularySessions(userId).filter { it.sessionId != sessionId }.forEach {
            study.finish(userId, it.sessionId, "ABANDONED", startedAt.coerceAtLeast(it.startedAt), 0, 0)
        }
        study.start(StudySessionEntity(userId, sessionId, sessionType = "VOCAB_REVIEW", startedAt = startedAt))
    }

    override suspend fun review(userId: String, review: FlashcardReview) = database.withTransaction {
        study.recordReview(VocabularyReviewEntity(
            userId = userId, reviewId = "${review.sessionId}:${review.wordId}",
            sessionId = review.sessionId, vocabularyId = review.wordId,
            reviewMode = "SELF_RATED", rating = review.rating.name,
            responseDurationMs = review.responseDurationMs, reviewedAt = review.reviewedAt
        ))
        if (review.completed) {
            val session = requireNotNull(study.session(userId, review.sessionId))
            study.finish(userId, review.sessionId, "COMPLETED", review.reviewedAt,
                review.activeDurationMs.coerceIn(0, review.reviewedAt - session.startedAt), review.earnedXp)
        }
    }

    override suspend fun abandon(userId: String, sessionId: String, endedAt: Long, activeDurationMs: Long) {
        val session = requireNotNull(study.session(userId, sessionId))
        val end = endedAt.coerceAtLeast(session.startedAt)
        study.finish(userId, sessionId, "ABANDONED", end, activeDurationMs.coerceIn(0, end - session.startedAt), 0)
    }
}

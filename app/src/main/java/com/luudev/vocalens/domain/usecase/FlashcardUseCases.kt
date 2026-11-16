package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.FlashcardReview
import com.luudev.vocalens.domain.repository.FlashcardRepository

class StartFlashcards(private val repository: FlashcardRepository) {
    suspend operator fun invoke(userId: String, sessionId: String, at: Long) = repository.start(userId, sessionId, at)
}

class RateFlashcard(private val repository: FlashcardRepository) {
    suspend operator fun invoke(userId: String, review: FlashcardReview) {
        require(review.responseDurationMs >= 0 && review.activeDurationMs >= 0 && review.earnedXp >= 0)
        repository.review(userId, review)
    }
}

class AbandonFlashcards(private val repository: FlashcardRepository) {
    suspend operator fun invoke(userId: String, sessionId: String, at: Long, activeMs: Long) =
        repository.abandon(userId, sessionId, at, activeMs)
}

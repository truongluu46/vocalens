package com.luudev.vocalens.domain.model

enum class FlashcardRating { AGAIN, HARD, GOOD, EASY }

data class FlashcardReview(
    val sessionId: String,
    val wordId: String,
    val rating: FlashcardRating,
    val reviewedAt: Long,
    val responseDurationMs: Long,
    val activeDurationMs: Long,
    val completed: Boolean,
    val earnedXp: Int
)

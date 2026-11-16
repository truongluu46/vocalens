package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.FlashcardReview

interface FlashcardRepository {
    suspend fun start(userId: String, sessionId: String, startedAt: Long)
    suspend fun review(userId: String, review: FlashcardReview)
    suspend fun abandon(userId: String, sessionId: String, endedAt: Long, activeDurationMs: Long)
}

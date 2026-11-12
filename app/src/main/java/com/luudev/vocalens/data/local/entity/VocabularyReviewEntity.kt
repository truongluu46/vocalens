package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "vocabulary_reviews", primaryKeys = ["userId", "reviewId"], foreignKeys = [
    ForeignKey(entity = StudySessionEntity::class, parentColumns = ["userId", "sessionId"], childColumns = ["userId", "sessionId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = VocabularyEntity::class, parentColumns = ["vocabularyId"], childColumns = ["vocabularyId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index(value = ["userId", "sessionId"]), Index("vocabularyId"), Index(value = ["userId", "vocabularyId", "reviewedAt"])])
data class VocabularyReviewEntity(
    val userId: String,
    val reviewId: String,
    val sessionId: String,
    val vocabularyId: String,
    val reviewMode: String,
    val rating: String? = null,
    val isCorrect: Boolean? = null,
    val responseDurationMs: Long? = null,
    val reviewedAt: Long
)

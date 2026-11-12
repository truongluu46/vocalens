package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "user_vocabulary", primaryKeys = ["userId", "vocabularyId"], foreignKeys = [
    ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = VocabularyEntity::class, parentColumns = ["vocabularyId"], childColumns = ["vocabularyId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("vocabularyId"), Index(value = ["userId", "savedAt"]), Index(value = ["userId", "nextReviewAt"])])
data class UserVocabularyEntity(
    val userId: String,
    val vocabularyId: String,
    val learningStatus: String = "NEW",
    val personalNote: String? = null,
    val savedAt: Long,
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val updatedAt: Long
)

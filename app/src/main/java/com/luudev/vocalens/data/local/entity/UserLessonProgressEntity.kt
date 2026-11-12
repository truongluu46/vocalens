package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "user_lesson_progress", primaryKeys = ["userId", "lessonId"], foreignKeys = [
    ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LessonEntity::class, parentColumns = ["lessonId"], childColumns = ["lessonId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("lessonId")])
data class UserLessonProgressEntity(
    val userId: String,
    val lessonId: String,
    val status: String = "NOT_STARTED",
    val lastPositionSeconds: Int = 0,
    val completedAt: Long? = null,
    val updatedAt: Long
)

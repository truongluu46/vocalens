package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "study_sessions", primaryKeys = ["userId", "sessionId"], foreignKeys = [
    ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LessonEntity::class, parentColumns = ["lessonId"], childColumns = ["lessonId"], onDelete = ForeignKey.RESTRICT)
], indices = [Index("lessonId"), Index(value = ["userId", "startedAt"])])
data class StudySessionEntity(
    val userId: String,
    val sessionId: String,
    val lessonId: String? = null,
    val sessionType: String,
    val status: String = "ACTIVE",
    val startedAt: Long,
    val endedAt: Long? = null,
    val activeDurationMs: Long = 0,
    val earnedXp: Int = 0
)

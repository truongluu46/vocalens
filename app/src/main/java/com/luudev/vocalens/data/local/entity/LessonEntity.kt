package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val lessonId: String,
    val title: String,
    val categoryCode: String? = null,
    val mediaType: String,
    val mediaUrl: String,
    val durationSeconds: Int,
    val difficulty: String,
    val artworkUri: String? = null,
    val isToeic: Boolean = false,
    val isFeatured: Boolean = false,
    val publishedAt: Long,
    val updatedAt: Long
)

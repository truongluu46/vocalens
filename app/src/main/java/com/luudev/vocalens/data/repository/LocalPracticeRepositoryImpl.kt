package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.dao.LessonDao
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.map

class LocalPracticeRepositoryImpl(private val dao: LessonDao) : PracticeRepository {
    override fun observeLessons(userId: String) = dao.observeLessons(userId).map { rows -> rows.map { row ->
        val lesson = row.lesson
        PracticeLesson(lesson.lessonId, lesson.title, lesson.categoryCode.orEmpty(),
            if (lesson.mediaType == "PODCAST") PracticeMediaType.Podcast else PracticeMediaType.Video,
            when (lesson.difficulty) { "HARD" -> LessonDifficulty.Hard; "INTERMEDIATE" -> LessonDifficulty.Intermediate; else -> LessonDifficulty.Easy },
            lesson.durationSeconds, if (row.status == "COMPLETED") 1f else
                ((row.lastPositionSeconds ?: 0).toFloat() / lesson.durationSeconds.coerceAtLeast(1)).coerceIn(0f,1f),
            row.popularity, lesson.publishedAt, lesson.artworkUri.orEmpty(), lesson.isToeic, lesson.isFeatured)
    } }
}

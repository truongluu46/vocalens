package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class LessonRecord(@Embedded val lesson: LessonEntity, val status: String?, val lastPositionSeconds: Int?, val popularity: Int)

@Dao
abstract class LessonDao {
    @Query("""SELECT l.*, p.status, p.lastPositionSeconds,
        (SELECT COUNT(*) FROM study_sessions s WHERE s.lessonId = l.lessonId AND s.status = 'COMPLETED') AS popularity
        FROM lessons l LEFT JOIN user_lesson_progress p ON p.lessonId = l.lessonId AND p.userId = :uid
        ORDER BY l.publishedAt DESC, l.lessonId""")
    abstract fun observeLessons(uid: String): Flow<List<LessonRecord>>
    @Query("SELECT * FROM lessons WHERE lessonId = :id") abstract suspend fun getLesson(id: String): LessonEntity?
    @Upsert protected abstract suspend fun upsertLesson(lesson: LessonEntity)
    open suspend fun saveLesson(lesson: LessonEntity) {
        require(lesson.lessonId.isNotBlank() && lesson.title.isNotBlank() && lesson.mediaUrl.isNotBlank() && lesson.durationSeconds > 0)
        require(lesson.mediaType in setOf("VIDEO", "PODCAST") && lesson.difficulty in setOf("EASY", "INTERMEDIATE", "HARD"))
        upsertLesson(lesson)
    }
    @Query("SELECT * FROM user_lesson_progress WHERE userId = :uid AND lessonId = :id")
    protected abstract suspend fun progress(uid: String, id: String): UserLessonProgressEntity?
    @Upsert protected abstract suspend fun upsertProgress(progress: UserLessonProgressEntity)
    @Transaction
    open suspend fun saveProgress(value: UserLessonProgressEntity) {
        val lesson = requireNotNull(getLesson(value.lessonId))
        require(value.lastPositionSeconds in 0..lesson.durationSeconds)
        require(value.status in setOf("NOT_STARTED", "IN_PROGRESS", "COMPLETED"))
        require((value.status == "COMPLETED") == (value.completedAt != null))
        val old = progress(value.userId, value.lessonId)
        upsertProgress(if (old?.status == "COMPLETED") value.copy(status = "COMPLETED", completedAt = old.completedAt) else value)
    }
}

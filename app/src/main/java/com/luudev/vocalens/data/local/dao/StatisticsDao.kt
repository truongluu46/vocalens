package com.luudev.vocalens.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LibraryStatistics(val saved: Int, val mastered: Int, val pending: Int)
data class CourseStatistics(val progress: Float, val continuedLessonTitle: String?)

@Dao
interface StatisticsDao {
    @Query("""SELECT COUNT(*) AS saved, COALESCE(SUM(learningStatus = 'MASTERED'),0) AS mastered,
        COALESCE(SUM(learningStatus != 'MASTERED'),0) AS pending FROM user_vocabulary WHERE userId = :uid""")
    fun observeLibrary(uid: String): Flow<LibraryStatistics>
    @Query("""SELECT COALESCE(AVG(CASE WHEN p.status = 'COMPLETED' THEN 1.0
        ELSE COALESCE(p.lastPositionSeconds,0) * 1.0 / MAX(l.durationSeconds,1) END),0.0) AS progress,
        (SELECT ll.title FROM user_lesson_progress pp JOIN lessons ll ON ll.lessonId = pp.lessonId
        WHERE pp.userId = :uid AND pp.status = 'IN_PROGRESS' ORDER BY pp.updatedAt DESC LIMIT 1) AS continuedLessonTitle
        FROM lessons l LEFT JOIN user_lesson_progress p ON p.lessonId = l.lessonId AND p.userId = :uid""")
    fun observeCourses(uid: String): Flow<CourseStatistics>
}

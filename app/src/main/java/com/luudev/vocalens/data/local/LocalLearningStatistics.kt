package com.luudev.vocalens.data.local

import com.luudev.vocalens.data.local.dao.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

data class LearningStatistics(
    val mastered: Int, val pending: Int, val reviewedToday: Int, val streak: Int,
    val totalXp: Int, val todayXp: Int, val todayMinutes: Int,
    val recentMinutes: Int, val hasRecentActivity: Boolean,
    val courseProgress: Float, val continuedLessonTitle: String?
)

class LocalLearningStatistics(private val statistics: StatisticsDao, private val study: StudyDao) {
    private fun clock() = flow {
        while (true) { emit(Instant.now()); delay(60_000) }
    }

    fun observe(uid: String) = combine(statistics.observeLibrary(uid), statistics.observeCourses(uid),
        study.observeSessions(uid), study.observeReviews(uid), clock()) { library, courses, sessions, reviews, now ->
        val zone = ZoneId.systemDefault()
        val today = now.atZone(zone).toLocalDate()
        fun date(at: Long) = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
        val closed = sessions.filter { it.status != "ACTIVE" && it.endedAt != null }
        val complete = closed.filter { it.status == "COMPLETED" }
        val todaySessions = closed.filter { date(it.endedAt!!) == today }
        val activeDates = complete.filter { it.activeDurationMs > 0 }.map { date(it.endedAt!!) }.toSet()
        var day: LocalDate = if (today in activeDates) today else today.minusDays(1)
        var streak = 0
        while (day in activeDates) { streak++; day = day.minusDays(1) }
        val recent = closed.maxByOrNull { it.endedAt!! }
        LearningStatistics(library.mastered, library.pending,
            reviews.filter { date(it.reviewedAt) == today }.map { it.vocabularyId }.distinct().size,
            streak, complete.sumOf { it.earnedXp }, todaySessions.filter { it.status == "COMPLETED" }.sumOf { it.earnedXp },
            (todaySessions.sumOf { it.activeDurationMs } / 60_000).toInt(),
            ((recent?.activeDurationMs ?: 0) / 60_000).toInt(), recent != null,
            courses.progress.coerceIn(0f, 1f), courses.continuedLessonTitle)
    }
}

package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.LocalLearningStatistics
import com.luudev.vocalens.data.local.dao.UserDao
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.combine

class LocalDashboardRepositoryImpl(private val userDao: UserDao, private val statistics: LocalLearningStatistics) : DashboardRepository {
    override fun observeDashboard(userId: String) = combine(userDao.observeProfile(userId), statistics.observe(userId)) { user, stats ->
        val profile = UserProfile(fullName = user?.fullName.orEmpty(), email = user?.email.orEmpty())
        Dashboard(profile.shortName, profile.initials, stats.streak, stats.reviewedToday,
            user?.dailyGoal ?: 25, stats.todayMinutes, stats.pending, stats.courseProgress,
            stats.recentMinutes, stats.todayXp, stats.continuedLessonTitle, stats.hasRecentActivity)
    }
}

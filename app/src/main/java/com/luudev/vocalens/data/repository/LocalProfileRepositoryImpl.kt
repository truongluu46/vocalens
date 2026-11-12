package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.LocalLearningStatistics
import com.luudev.vocalens.data.local.dao.UserDao
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.combine

class LocalProfileRepositoryImpl(private val dao: UserDao, private val statistics: LocalLearningStatistics) : ProfileRepository {
    override fun observeProfile(userId: String) = combine(dao.observeProfile(userId), statistics.observe(userId)) { record, stats ->
        UserProfile(record?.fullName.orEmpty(), record?.email.orEmpty(), record?.dailyGoal ?: 25,
            record?.reminderEnabled ?: true, record?.soundEnabled ?: true,
            stats.mastered, stats.streak, stats.totalXp, stats.reviewedToday)
    }
    override suspend fun saveProfile(userId: String, profile: UserProfile) {
        // Email is an Auth-owned cache; editing a local profile must not change the sign-in address.
        dao.save(userId, profile.fullName, profile.dailyGoal, profile.reminderEnabled,
            profile.soundEnabled, System.currentTimeMillis())
    }
}

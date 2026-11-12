package com.luudev.vocalens.domain.model

import java.util.Locale

data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val dailyGoal: Int = 25,
    val reminderEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val wordsLearned: Int = 0,
    val streakDays: Int = 0,
    val totalXp: Int = 0,
    val wordsToday: Int = 0
) {
    val initials: String get() = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .takeLast(2).joinToString("") { it.take(1) }.uppercase(Locale.ROOT).ifEmpty { "VL" }
    val shortName: String get() = fullName.trim().split(Regex("\\s+")).takeLast(2).joinToString(" ")
    val dailyProgress: Float get() = if (dailyGoal > 0) (wordsToday.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f
}

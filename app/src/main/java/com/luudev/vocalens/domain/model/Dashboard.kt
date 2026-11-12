package com.luudev.vocalens.domain.model

data class Dashboard(
    val learnerName: String = "",
    val initials: String = "VL",
    val streakDays: Int = 0,
    val completedWords: Int = 0,
    val targetWords: Int = 25,
    val studyMinutes: Int = 0,
    val pendingWords: Int = 0,
    val courseProgress: Float = 0f,
    val recentActivityMinutes: Int = 0,
    val earnedXp: Int = 0,
    val continuedLessonTitle: String? = null,
    val hasRecentActivity: Boolean = false
) {
    val dailyProgress: Float
        get() = if (targetWords > 0) (completedWords.toFloat() / targetWords).coerceIn(0f, 1f) else 0f
    val remainingWords: Int get() = (targetWords - completedWords).coerceAtLeast(0)
}

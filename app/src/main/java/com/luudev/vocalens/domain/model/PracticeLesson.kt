package com.luudev.vocalens.domain.model

enum class PracticeMediaType { Video, Podcast }
enum class LessonDifficulty { Easy, Intermediate, Hard }
enum class PracticeFilter { All, Easy, Intermediate, Toeic }
enum class PracticeDuration { All, Short, Long }
enum class PracticeSort { Newest, Popular }

data class PracticeLesson(
    val id: String,
    val title: String,
    val category: String,
    val mediaType: PracticeMediaType,
    val difficulty: LessonDifficulty,
    val durationSeconds: Int,
    val progress: Float,
    val popularity: Int,
    val publishedOrder: Long,
    val artworkKey: String,
    val isToeic: Boolean = false,
    val isFeatured: Boolean = false
)

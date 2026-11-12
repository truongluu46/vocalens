package com.luudev.vocalens.domain.model

enum class VocabularyCategory { Objects, Technology, Travel }
enum class VocabularySort { RecentlySaved, Alphabetical }

data class VocabularyWord(
    val id: String,
    val english: String,
    val phonetic: String,
    val meaning: String,
    val category: VocabularyCategory,
    val partOfSpeech: String? = null,
    val exampleSentence: String? = null
)

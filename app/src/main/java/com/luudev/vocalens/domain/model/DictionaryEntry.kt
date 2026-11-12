package com.luudev.vocalens.domain.model

data class DictionaryEntry(
    val word: VocabularyWord,
    val definition: String,
    val sourceUrl: String,
    val licenseName: String,
    val licenseUrl: String,
    val accent: DictionaryAccent? = null
)

enum class DictionaryAccent { British, American }

enum class DictionaryError { NotFound, Network, InvalidResponse }
class DictionaryException(val reason: DictionaryError) : Exception()

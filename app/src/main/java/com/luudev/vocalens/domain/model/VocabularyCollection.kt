package com.luudev.vocalens.domain.model

data class VocabularyCollection(
    val id: String,
    val name: String,
    val description: String = "",
    val wordCount: Int = 0
)

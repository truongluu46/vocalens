package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey val vocabularyId: String,
    val english: String,
    val phonetic: String,
    val meaningVi: String,
    val partOfSpeech: String? = null,
    val exampleSentence: String? = null,
    val categoryCode: String? = null,
    val createdAt: Long
)

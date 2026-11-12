package com.luudev.vocalens.data.mapper

import com.luudev.vocalens.data.local.entity.VocabularyEntity
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularyWord

fun VocabularyEntity.toDomain() = VocabularyWord(
    id = vocabularyId, english = english, phonetic = phonetic, meaning = meaningVi,
    category = VocabularyCategory.entries.find { it.name == categoryCode } ?: VocabularyCategory.Objects,
    partOfSpeech = partOfSpeech, exampleSentence = exampleSentence
)

fun VocabularyWord.toEntity(createdAt: Long) = VocabularyEntity(
    vocabularyId = id, english = english, phonetic = phonetic,
    meaningVi = meaning, categoryCode = category.name, createdAt = createdAt,
    partOfSpeech = partOfSpeech, exampleSentence = exampleSentence
)

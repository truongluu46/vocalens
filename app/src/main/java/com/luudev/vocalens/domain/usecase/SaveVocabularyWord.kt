package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository

class SaveVocabularyWord(private val repository: VocabularyRepository) {
    suspend operator fun invoke(userId: String, word: VocabularyWord) = repository.saveWord(userId, word)
}

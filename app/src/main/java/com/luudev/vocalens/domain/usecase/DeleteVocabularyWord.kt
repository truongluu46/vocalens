package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.VocabularyRepository

class DeleteVocabularyWord(private val repository: VocabularyRepository) {
    suspend operator fun invoke(userId: String, wordId: String) = repository.deleteWord(userId, wordId)
}

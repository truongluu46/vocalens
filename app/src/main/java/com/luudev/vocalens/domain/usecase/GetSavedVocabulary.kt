package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.VocabularyRepository

class GetSavedVocabulary(private val repository: VocabularyRepository) {
    operator fun invoke(userId: String) = repository.observeSavedWords(userId)
}

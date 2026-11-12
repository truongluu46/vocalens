package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.DictionaryRepository

class LookupVocabulary(private val repository: DictionaryRepository) {
    suspend operator fun invoke(query: String) = repository.lookup(query.trim())
}

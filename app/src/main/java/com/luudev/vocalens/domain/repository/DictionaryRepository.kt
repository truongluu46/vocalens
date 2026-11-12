package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.DictionaryEntry

interface DictionaryRepository {
    suspend fun lookup(query: String): List<DictionaryEntry>
}

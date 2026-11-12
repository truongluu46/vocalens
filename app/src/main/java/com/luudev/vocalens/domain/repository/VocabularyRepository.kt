package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.VocabularyWord
import kotlinx.coroutines.flow.Flow

interface VocabularyRepository {
    fun observeSavedWords(userId: String): Flow<List<VocabularyWord>>
    suspend fun saveWord(userId: String, word: VocabularyWord)
    suspend fun deleteWord(userId: String, wordId: String)
}

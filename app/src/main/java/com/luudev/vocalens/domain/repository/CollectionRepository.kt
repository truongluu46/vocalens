package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.VocabularyCollection
import com.luudev.vocalens.domain.model.VocabularyWord
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeCollections(userId: String): Flow<List<VocabularyCollection>>
    fun observeWords(userId: String, collectionId: String): Flow<List<VocabularyWord>>
    suspend fun create(userId: String, name: String, description: String)
    suspend fun update(userId: String, collectionId: String, name: String, description: String)
    suspend fun delete(userId: String, collectionId: String)
    suspend fun setWords(userId: String, collectionId: String, wordIds: Set<String>)
    suspend fun removeWord(userId: String, collectionId: String, wordId: String)
}

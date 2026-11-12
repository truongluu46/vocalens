package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.dao.VocabularyDao
import com.luudev.vocalens.data.mapper.toDomain
import com.luudev.vocalens.data.mapper.toEntity
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository
import kotlinx.coroutines.flow.map

class LocalVocabularyRepositoryImpl(private val dao: VocabularyDao) : VocabularyRepository {
    override fun observeSavedWords(userId: String) = dao.observeWords(requireUser(userId)).map { words ->
        words.map { it.toDomain() }
    }

    override suspend fun saveWord(userId: String, word: VocabularyWord) {
        val now = System.currentTimeMillis()
        dao.saveWord(requireUser(userId), word.toEntity(now), now)
    }

    override suspend fun deleteWord(userId: String, wordId: String) {
        dao.deleteWord(requireUser(userId), wordId)
    }

    private fun requireUser(userId: String): String {
        require(userId.isNotBlank()) { "A signed-in user is required" }
        return userId
    }
}

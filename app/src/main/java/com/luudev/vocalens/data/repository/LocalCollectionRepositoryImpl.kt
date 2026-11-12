package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.dao.CollectionDao
import com.luudev.vocalens.data.local.entity.CollectionEntity
import com.luudev.vocalens.data.mapper.toDomain
import com.luudev.vocalens.domain.model.VocabularyCollection
import com.luudev.vocalens.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalCollectionRepositoryImpl(private val dao: CollectionDao) : CollectionRepository {
    private fun owner(uid: String) = uid.also { require(it.isNotBlank()) }
    override fun observeCollections(userId: String) = dao.observeSummaries(owner(userId)).map { rows ->
        rows.map { VocabularyCollection(it.collection.collectionId, it.collection.name,
            it.collection.description.orEmpty(), it.wordCount) }
    }
    override fun observeWords(userId: String, collectionId: String) =
        dao.observeWords(owner(userId), collectionId).map { words -> words.map { it.toDomain() } }

    override suspend fun create(userId: String, name: String, description: String) {
        val now = System.currentTimeMillis()
        dao.create(CollectionEntity(owner(userId), UUID.randomUUID().toString(), name,
            description.takeIf { it.isNotBlank() }, now, now))
    }
    override suspend fun update(userId: String, collectionId: String, name: String, description: String) {
        check(dao.update(owner(userId), collectionId, name, description, System.currentTimeMillis()) == 1)
    }
    override suspend fun delete(userId: String, collectionId: String) = dao.delete(owner(userId), collectionId)
    override suspend fun setWords(userId: String, collectionId: String, wordIds: Set<String>) =
        dao.replaceItems(owner(userId), collectionId, wordIds, System.currentTimeMillis())
    override suspend fun removeWord(userId: String, collectionId: String, wordId: String) =
        dao.removeItem(owner(userId), collectionId, wordId)
}

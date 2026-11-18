package com.luudev.vocalens.data.repository

import androidx.room.withTransaction
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.CollectionEntity
import com.luudev.vocalens.data.local.entity.CollectionItemEntity
import com.luudev.vocalens.data.mapper.toEntity
import com.luudev.vocalens.domain.model.CollectionDestination
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.WordCollectionRepository
import java.util.UUID

class LocalWordCollectionRepository(private val database: VocabLensDatabase) : WordCollectionRepository {
    override suspend fun save(userId: String, word: VocabularyWord, destination: CollectionDestination) {
        require(userId.isNotBlank())
        database.withTransaction {
            val now = System.currentTimeMillis()
            val collections = database.collectionDao()
            val id = when (destination) {
                is CollectionDestination.Existing -> destination.id.also {
                    check(collections.touch(userId, it, now) == 1) { "Collection no longer exists for this user" }
                }
                is CollectionDestination.New -> UUID.randomUUID().toString().also {
                    collections.create(CollectionEntity(userId, it, destination.name, null, now, now))
                }
            }
            database.vocabularyDao().saveWord(userId, word.toEntity(now), now)
            collections.addItem(CollectionItemEntity(userId, id, word.id, now))
        }
    }
}

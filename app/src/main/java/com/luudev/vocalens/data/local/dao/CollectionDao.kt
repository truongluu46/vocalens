package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class CollectionSummary(
    @Embedded val collection: CollectionEntity,
    val wordCount: Int
)

@Dao
abstract class CollectionDao {
    @Query("""SELECT c.*, (SELECT COUNT(*) FROM collection_items i
        WHERE i.userId = c.userId AND i.collectionId = c.collectionId) AS wordCount
        FROM collections c WHERE c.userId = :uid ORDER BY c.updatedAt DESC, c.collectionId""")
    abstract fun observeSummaries(uid: String): Flow<List<CollectionSummary>>
    @Insert abstract suspend fun create(collection: CollectionEntity)
    @Query("""UPDATE collections SET name = :name, description = :description, updatedAt = :now
        WHERE userId = :uid AND collectionId = :id""")
    abstract suspend fun update(uid: String, id: String, name: String, description: String, now: Long): Int
    @Query("SELECT EXISTS(SELECT 1 FROM collections WHERE userId = :uid AND collectionId = :id)")
    protected abstract suspend fun exists(uid: String, id: String): Boolean
    @Query("DELETE FROM collection_items WHERE userId = :uid AND collectionId = :id")
    protected abstract suspend fun clearItems(uid: String, id: String)
    @Transaction
    open suspend fun replaceItems(uid: String, id: String, wordIds: Set<String>, now: Long) {
        check(exists(uid, id)) { "Collection no longer exists" }
        // Foreign keys validate that every selected word is saved by this account.
        // A failed insertion rolls back the deletion as well.
        clearItems(uid, id)
        wordIds.forEach { addItem(CollectionItemEntity(uid, id, it, now)) }
    }
    @Query("SELECT * FROM collections WHERE userId = :uid ORDER BY updatedAt DESC, collectionId")
    abstract fun observeCollections(uid: String): Flow<List<CollectionEntity>>
    @Query("""SELECT v.* FROM vocabulary v JOIN collection_items i ON i.vocabularyId = v.vocabularyId
        WHERE i.userId = :uid AND i.collectionId = :id ORDER BY i.addedAt DESC, v.vocabularyId""")
    abstract fun observeWords(uid: String, id: String): Flow<List<VocabularyEntity>>
    @Upsert protected abstract suspend fun upsertCollection(collection: CollectionEntity)
    open suspend fun save(collection: CollectionEntity) {
        require(collection.userId.isNotBlank() && collection.collectionId.isNotBlank() && collection.name.isNotBlank())
        upsertCollection(collection.copy(name = collection.name.trim()))
    }
    // Composite FKs require the saved word and collection to belong to the same user.
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun addItem(item: CollectionItemEntity)
    @Query("DELETE FROM collection_items WHERE userId = :uid AND collectionId = :id AND vocabularyId = :wordId")
    abstract suspend fun removeItem(uid: String, id: String, wordId: String)
    @Query("DELETE FROM collections WHERE userId = :uid AND collectionId = :id")
    abstract suspend fun delete(uid: String, id: String)
}

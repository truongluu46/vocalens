package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "collection_items", primaryKeys = ["userId", "collectionId", "vocabularyId"], foreignKeys = [
    ForeignKey(entity = CollectionEntity::class, parentColumns = ["userId", "collectionId"], childColumns = ["userId", "collectionId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = UserVocabularyEntity::class, parentColumns = ["userId", "vocabularyId"], childColumns = ["userId", "vocabularyId"], onDelete = ForeignKey.CASCADE)
], indices = [Index(value = ["userId", "vocabularyId"])])
data class CollectionItemEntity(val userId: String, val collectionId: String, val vocabularyId: String, val addedAt: Long)

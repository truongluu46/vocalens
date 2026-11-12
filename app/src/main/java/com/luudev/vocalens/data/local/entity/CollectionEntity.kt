package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "collections", primaryKeys = ["userId", "collectionId"], foreignKeys = [ForeignKey(
    entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE
)])
data class CollectionEntity(
    val userId: String,
    val collectionId: String,
    val name: String,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

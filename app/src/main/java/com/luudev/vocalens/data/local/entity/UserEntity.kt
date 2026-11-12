package com.luudev.vocalens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String,
    val avatarUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

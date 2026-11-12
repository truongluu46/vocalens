package com.luudev.vocalens.data.local.entity

import androidx.room.*

@Entity(tableName = "user_settings", foreignKeys = [ForeignKey(
    entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE
)])
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val dailyWordGoal: Int = 25,
    val reminderEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val updatedAt: Long
)

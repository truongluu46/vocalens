package com.luudev.vocalens.data.local.dao

import androidx.room.*
import com.luudev.vocalens.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class ProfileRecord(val fullName: String, val email: String, val dailyGoal: Int,
    val reminderEnabled: Boolean, val soundEnabled: Boolean)

@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertUser(user: UserEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) protected abstract suspend fun insertSettings(settings: UserSettingsEntity)
    @Query("UPDATE users SET email = :email, updatedAt = :now WHERE userId = :uid")
    protected abstract suspend fun syncEmail(uid: String, email: String, now: Long)
    @Transaction
    open suspend fun synchronize(user: UserEntity) {
        require(user.userId.isNotBlank())
        insertUser(user)
        syncEmail(user.userId, user.email, user.updatedAt)
        insertSettings(UserSettingsEntity(user.userId, updatedAt = user.updatedAt))
    }
    @Query("""SELECT u.displayName AS fullName, u.email,
        COALESCE(s.dailyWordGoal,25) AS dailyGoal, COALESCE(s.reminderEnabled,1) AS reminderEnabled,
        COALESCE(s.soundEnabled,1) AS soundEnabled FROM users u
        LEFT JOIN user_settings s ON s.userId = u.userId WHERE u.userId = :uid""")
    abstract fun observeProfile(uid: String): Flow<ProfileRecord?>
    @Query("UPDATE users SET displayName = :name, updatedAt = :now WHERE userId = :uid")
    protected abstract suspend fun updateName(uid: String, name: String, now: Long): Int
    @Upsert protected abstract suspend fun updateSettings(settings: UserSettingsEntity)
    @Transaction
    open suspend fun save(uid: String, name: String, goal: Int, reminder: Boolean, sound: Boolean, now: Long) {
        require(name.isNotBlank() && goal in 1..500)
        check(updateName(uid, name.trim(), now) == 1) { "Unknown user" }
        updateSettings(UserSettingsEntity(uid, goal, reminder, sound, now))
    }
}

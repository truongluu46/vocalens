package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(userId: String): Flow<UserProfile>
    suspend fun saveProfile(userId: String, profile: UserProfile)
}

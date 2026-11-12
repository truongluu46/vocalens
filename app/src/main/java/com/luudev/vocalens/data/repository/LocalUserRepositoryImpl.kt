package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.local.dao.UserDao
import com.luudev.vocalens.data.local.entity.UserEntity
import com.luudev.vocalens.domain.model.AuthUser
import com.luudev.vocalens.domain.repository.UserRepository

class LocalUserRepositoryImpl(private val dao: UserDao) : UserRepository {
    override suspend fun synchronize(user: AuthUser) {
        val now = System.currentTimeMillis()
        dao.synchronize(UserEntity(user.uid, user.email,
            user.displayName.ifBlank { user.email.substringBefore('@') }, createdAt = now, updatedAt = now))
    }
}

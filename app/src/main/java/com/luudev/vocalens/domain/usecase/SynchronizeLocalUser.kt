package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.AuthUser
import com.luudev.vocalens.domain.repository.UserRepository

class SynchronizeLocalUser(private val repository: UserRepository) {
    suspend operator fun invoke(user: AuthUser) = repository.synchronize(user)
}

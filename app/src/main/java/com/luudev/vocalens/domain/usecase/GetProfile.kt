package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.ProfileRepository

class GetProfile(private val repository: ProfileRepository) {
    operator fun invoke(userId: String) = repository.observeProfile(userId)
}

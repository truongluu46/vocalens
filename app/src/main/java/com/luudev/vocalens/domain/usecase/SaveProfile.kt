package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository

data class ProfileValidation(val invalidName: Boolean, val invalidEmail: Boolean, val invalidGoal: Boolean) {
    val isValid: Boolean get() = !invalidName && !invalidEmail && !invalidGoal
}

class SaveProfile(private val repository: ProfileRepository) {
    suspend operator fun invoke(userId: String, profile: UserProfile): ProfileValidation {
        val result = ProfileValidation(profile.fullName.isBlank(),
            !EMAIL.matches(profile.email.trim()), profile.dailyGoal !in 1..500)
        if (result.isValid) repository.saveProfile(userId, profile.copy(fullName = profile.fullName.trim(), email = profile.email.trim()))
        return result
    }

    private companion object { val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") }
}

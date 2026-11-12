package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class SaveProfileTest {
    private val initial = UserProfile("Nguyễn Minh Anh", "minhanh@email.com")
    private class FakeRepository(var storedProfile: UserProfile) : ProfileRepository {
        var writes = 0
        override fun observeProfile(userId: String) = flowOf(storedProfile)
        override suspend fun saveProfile(userId: String, profile: UserProfile) { storedProfile = profile; writes++ }
    }

    @Test
    fun trimsNameAndEmailBeforeSaving() = runTest {
        val repository = FakeRepository(initial)
        val result = SaveProfile(repository)("A", initial.copy(fullName = "  Trần Bảo An  ", email = "  an@example.com  "))
        assertTrue(result.isValid)
        assertEquals("Trần Bảo An", repository.storedProfile.fullName)
        assertEquals("an@example.com", repository.storedProfile.email)
        assertEquals("BA", repository.storedProfile.initials)
    }

    @Test
    fun invalidNameAndEmailDoNotOverwriteProfile() = runTest {
        val repository = FakeRepository(initial)
        val result = SaveProfile(repository)("A", initial.copy(fullName = "   ", email = "invalid"))
        assertTrue(result.invalidName)
        assertTrue(result.invalidEmail)
        assertEquals(0, repository.writes)
        assertEquals(initial, repository.storedProfile)
    }

    @Test
    fun rejectsOutOfRangeGoalsAndAcceptsBoundaries() = runTest {
        val repository = FakeRepository(initial)
        val save = SaveProfile(repository)
        assertTrue(save("A", initial.copy(dailyGoal = 0)).invalidGoal)
        assertTrue(save("A", initial.copy(dailyGoal = 501)).invalidGoal)
        assertEquals(0, repository.writes)
        assertTrue(save("A", initial.copy(dailyGoal = 1)).isValid)
        assertTrue(save("A", initial.copy(dailyGoal = 500)).isValid)
    }

    @Test
    fun reducingGoalBelowCompletedWordsKeepsProgressAtOneHundredPercent() = runTest {
        assertEquals(1f, initial.copy(dailyGoal = 10, wordsToday = 17).dailyProgress, 0f)
        assertEquals(0f, initial.copy(dailyGoal = 0).dailyProgress, 0f)
    }
}

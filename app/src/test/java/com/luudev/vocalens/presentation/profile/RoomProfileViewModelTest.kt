package com.luudev.vocalens.presentation.profile

import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.repository.ProfileRepository
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomProfileViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private class FakeRepository : ProfileRepository {
        val profile = MutableStateFlow(UserProfile("Learner", "learner@example.com", wordsToday = 17))
        var failSave = false
        override fun observeProfile(userId: String) = profile
        override suspend fun saveProfile(userId: String, profile: UserProfile) {
            if (failSave) error("Disk failure")
            this.profile.value = profile
        }
    }
    private fun viewModel(repo: FakeRepository) = ProfileViewModel(GetProfile(repo), SaveProfile(repo)).apply { onUserChanged("A") }

    @Test fun cancelDiscardsDraft() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onNameChange("Unsaved")
        vm.onDismissDialog()
        vm.onOpenDialog(ProfileDialog.EditInfo)
        assertEquals("Learner", vm.uiState.draftName)
    }
    @Test fun invalidNameKeepsEditorOpen() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onNameChange("")
        vm.onSaveInfo()
        runCurrent()
        assertTrue(vm.uiState.validation.invalidName)
        assertEquals(ProfileDialog.EditInfo, vm.uiState.dialog)
        assertEquals("Learner", repo.profile.value.fullName)
    }
    @Test fun goalAndTogglesPersistThroughRepository() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onOpenDialog(ProfileDialog.EditGoal)
        vm.onGoalSelected(50)
        vm.onSaveGoal()
        runCurrent()
        assertNull(vm.uiState.dialog)
        vm.onSoundChanged(false)
        runCurrent()
        vm.onReminderChanged(false)
        runCurrent()
        val restored = viewModel(repo)
        runCurrent()
        assertEquals(50, restored.uiState.profile.dailyGoal)
        assertEquals(0.34f, restored.uiState.profile.dailyProgress, 0.001f)
        assertFalse(restored.uiState.profile.soundEnabled)
        assertFalse(restored.uiState.profile.reminderEnabled)
    }
    @Test fun logoutClearsProfileAndUnsubscribes() = runTest {
        val repo = FakeRepository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onUserChanged(null)
        repo.profile.value = UserProfile("Other", "other@example.com")
        runCurrent()
        assertEquals(UserProfile(), vm.uiState.profile)
    }
    @Test fun failedWriteShowsErrorAndKeepsEditor() = runTest {
        val repo = FakeRepository().apply { failSave = true }
        val vm = viewModel(repo)
        runCurrent()
        vm.onOpenDialog(ProfileDialog.EditInfo)
        vm.onNameChange("New name")
        vm.onSaveInfo()
        runCurrent()
        assertTrue(vm.uiState.storageError)
        assertFalse(vm.uiState.isSaving)
        assertEquals(ProfileDialog.EditInfo, vm.uiState.dialog)
    }
}

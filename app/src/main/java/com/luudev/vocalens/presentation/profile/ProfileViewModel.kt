package com.luudev.vocalens.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.usecase.GetProfile
import com.luudev.vocalens.domain.usecase.ProfileValidation
import com.luudev.vocalens.domain.usecase.SaveProfile

enum class ProfileDialog { EditInfo, EditGoal, Statistics, Settings, ReminderInfo, Logout }

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val dialog: ProfileDialog? = null,
    val draftName: String = "",
    val draftEmail: String = "",
    val draftGoal: Int = 25,
    val validation: ProfileValidation = ProfileValidation(false, false, false),
    val isSaving: Boolean = false,
    val storageError: Boolean = false
)

class ProfileViewModel(private val getProfile: GetProfile, private val saveProfile: SaveProfile) : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set
    private var owner: String? = null
    private var observation: Job? = null
    private var write: Job? = null

    fun onUserChanged(uid: String?) {
        if (uid == owner) return
        observation?.cancel()
        write?.cancel()
        owner = uid
        uiState = ProfileUiState()
        if (uid == null) return
        observation = viewModelScope.launch {
            try {
                getProfile(uid).collect { if (owner == uid) uiState = uiState.copy(profile = it, storageError = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == uid) uiState = uiState.copy(storageError = true) }
        }
    }

    fun onOpenDialog(dialog: ProfileDialog) {
        uiState = uiState.copy(dialog = dialog, draftName = uiState.profile.fullName,
            draftEmail = uiState.profile.email, draftGoal = uiState.profile.dailyGoal,
            validation = ProfileValidation(false, false, false))
    }

    fun onDismissDialog() { uiState = uiState.copy(dialog = null) }
    fun onNameChange(name: String) {
        uiState = uiState.copy(draftName = name, validation = uiState.validation.copy(invalidName = false))
    }
    fun onEmailChange(email: String) {
        uiState = uiState.copy(draftEmail = email, validation = uiState.validation.copy(invalidEmail = false))
    }
    fun onGoalSelected(goal: Int) {
        uiState = uiState.copy(draftGoal = goal, validation = uiState.validation.copy(invalidGoal = false))
    }

    fun onSaveInfo() {
        persist(uiState.profile.copy(fullName = uiState.draftName), closeDialog = true)
    }

    fun onSaveGoal() {
        persist(uiState.profile.copy(dailyGoal = uiState.draftGoal), closeDialog = true)
    }

    fun onReminderChanged(enabled: Boolean) {
        persist(uiState.profile.copy(reminderEnabled = enabled))
    }

    fun onSoundChanged(enabled: Boolean) {
        persist(uiState.profile.copy(soundEnabled = enabled))
    }

    fun onStorageErrorDismissed() { uiState = uiState.copy(storageError = false) }

    private fun persist(profile: UserProfile, closeDialog: Boolean = false) {
        val uid = owner ?: return
        if (uiState.isSaving) return
        uiState = uiState.copy(isSaving = true, storageError = false)
        write = viewModelScope.launch {
            try {
                val result = saveProfile(uid, profile)
                if (owner == uid) uiState = uiState.copy(validation = result,
                    dialog = if (closeDialog && result.isValid) null else uiState.dialog)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == uid) uiState = uiState.copy(storageError = true) }
            finally { if (owner == uid) uiState = uiState.copy(isSaving = false) }
        }
    }
}

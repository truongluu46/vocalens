package com.luudev.vocalens.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.luudev.vocalens.domain.model.Dashboard
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.domain.usecase.GetDashboard

enum class HomeAction { Notifications, Profile, Progress, Flashcards, Camera, Listening, Pronunciation, Courses, Vocabulary, Practice }

data class HomeUiState(val dashboard: Dashboard = Dashboard(), val selectedAction: HomeAction? = null, val storageError: Boolean = false)

class HomeViewModel(private val getDashboard: GetDashboard) : ViewModel() {
    var uiState by mutableStateOf(HomeUiState())
        private set
    private var owner: String? = null
    private var observation: Job? = null

    fun onUserChanged(uid: String?) {
        if (uid == owner) return
        observation?.cancel()
        owner = uid
        uiState = HomeUiState()
        if (uid == null) return
        observation = viewModelScope.launch {
            try {
                getDashboard(uid).collect { if (owner == uid) uiState = uiState.copy(dashboard = it, storageError = false) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == uid) uiState = uiState.copy(storageError = true) }
        }
    }

    fun onAction(action: HomeAction) {
        uiState = uiState.copy(selectedAction = action)
    }

    fun onDismissAction() {
        uiState = uiState.copy(selectedAction = null)
    }

}

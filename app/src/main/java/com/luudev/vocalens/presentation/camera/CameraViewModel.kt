package com.luudev.vocalens.presentation.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class CameraUiState(
    val useFrontCamera: Boolean = false,
    val canSwitchCamera: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val retryCount: Int = 0
)

/** Camera objects and permission launchers belong to the view's lifecycle. */
class CameraViewModel : ViewModel() {
    var uiState by mutableStateOf(CameraUiState())
        private set

    fun onCameraAvailability(hasBack: Boolean, hasFront: Boolean) {
        uiState = uiState.copy(
            canSwitchCamera = hasBack && hasFront,
            useFrontCamera = if (uiState.useFrontCamera) hasFront else !hasBack && hasFront
        )
    }

    fun onOpening() {
        uiState = uiState.copy(isLoading = true, hasError = false)
    }

    fun onReady() {
        uiState = uiState.copy(isLoading = false, hasError = false)
    }

    fun onError() {
        uiState = uiState.copy(isLoading = false, hasError = true)
    }

    fun onSwitchCamera() {
        if (uiState.canSwitchCamera) {
            uiState = uiState.copy(useFrontCamera = !uiState.useFrontCamera, isLoading = true, hasError = false)
        }
    }

    fun onRetry() {
        uiState = uiState.copy(retryCount = uiState.retryCount + 1, isLoading = true, hasError = false)
    }
}

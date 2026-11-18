package com.luudev.vocalens.presentation.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.domain.usecase.RecognizeObjectsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class CameraStage { Preview, Capturing, Analyzing, Results }
enum class CaptureError { Capture, Recognition }

data class CameraUiState(
    val useFrontCamera: Boolean = false,
    val canSwitchCamera: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val retryCount: Int = 0,
    val stage: CameraStage = CameraStage.Preview,
    val photo: ByteArray? = null,
    val objects: List<RecognizedObject> = emptyList(),
    val captureError: CaptureError? = null
)

/** Camera objects and permission launchers belong to the view's lifecycle. */
class CameraViewModel(private val recognizeObjects: RecognizeObjectsUseCase) : ViewModel() {
    private var generation = 0L
    private var recognitionJob: Job? = null
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
        if (uiState.stage == CameraStage.Capturing) {
            generation++
            uiState = uiState.copy(stage = CameraStage.Preview)
        }
        uiState = uiState.copy(isLoading = false, hasError = true)
    }

    fun onSwitchCamera() {
        if (uiState.canSwitchCamera && uiState.stage == CameraStage.Preview) {
            uiState = uiState.copy(useFrontCamera = !uiState.useFrontCamera, isLoading = true, hasError = false)
        }
    }

    fun onRetry() {
        onRetake()
        uiState = uiState.copy(retryCount = uiState.retryCount + 1, isLoading = true, hasError = false)
    }

    fun onCaptureStarted(): Long? {
        if (uiState.isLoading || uiState.hasError || uiState.stage != CameraStage.Preview) return null
        uiState = uiState.copy(stage = CameraStage.Capturing, captureError = null)
        return ++generation
    }

    fun onCaptured(ticket: Long, photo: ByteArray) {
        if (ticket != generation || uiState.stage != CameraStage.Capturing) return
        uiState = uiState.copy(photo = photo)
        analyze(photo, ticket)
    }

    fun onCaptureFailed(ticket: Long) {
        if (ticket != generation || uiState.stage != CameraStage.Capturing) return
        uiState = uiState.copy(stage = CameraStage.Preview, captureError = CaptureError.Capture)
    }

    fun retryRecognition() {
        if (uiState.stage != CameraStage.Results || uiState.captureError != CaptureError.Recognition) return
        uiState.photo?.let { analyze(it, ++generation) }
    }

    private fun analyze(photo: ByteArray, ticket: Long) {
        uiState = uiState.copy(stage = CameraStage.Analyzing, captureError = null, objects = emptyList())
        recognitionJob = viewModelScope.launch {
            try {
                val objects = recognizeObjects(photo)
                if (ticket == generation) uiState = uiState.copy(stage = CameraStage.Results, objects = objects)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (ticket == generation) uiState = uiState.copy(stage = CameraStage.Results,
                    captureError = CaptureError.Recognition)
            }
        }
    }

    /** Discard late capture/inference callbacks after retaking, navigation, or permission loss. */
    fun onRetake() {
        generation++
        recognitionJob?.cancel()
        recognitionJob = null
        uiState = uiState.copy(stage = CameraStage.Preview, photo = null, objects = emptyList(), captureError = null)
    }
}

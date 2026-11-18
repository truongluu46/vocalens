package com.luudev.vocalens.presentation.camera

import com.luudev.vocalens.domain.model.ObjectBounds
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.domain.repository.ObjectRecognitionRepository
import com.luudev.vocalens.domain.usecase.RecognizeObjectsUseCase
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val word = RecognizedObject(0, "Book", .9f, ObjectBounds(0f, 0f, 1f, 1f))
    private fun vm(block: suspend (ByteArray) -> List<RecognizedObject>) = CameraViewModel(
        RecognizeObjectsUseCase(object : ObjectRecognitionRepository {
            override suspend fun recognize(image: ByteArray) = block(image)
        })).apply { onReady() }

    @Test fun preventsDuplicateCaptureAndDisplaysResults() = runTest {
        val result = CompletableDeferred<List<RecognizedObject>>()
        val vm = vm { result.await() }
        val ticket = vm.onCaptureStarted()!!
        assertNull(vm.onCaptureStarted())
        vm.onCaptured(ticket, byteArrayOf(1))
        runCurrent()
        assertEquals(CameraStage.Analyzing, vm.uiState.stage)
        result.complete(listOf(word))
        runCurrent()
        assertEquals(listOf(word), vm.uiState.objects)
        assertEquals(CameraStage.Results, vm.uiState.stage)
    }

    @Test fun retakeDiscardsLateCaptureAndInferenceResults() = runTest {
        val result = CompletableDeferred<List<RecognizedObject>>()
        val vm = vm { result.await() }
        val oldTicket = vm.onCaptureStarted()!!
        vm.onRetake()
        val newTicket = vm.onCaptureStarted()!!
        vm.onCaptured(oldTicket, byteArrayOf(1))
        assertNull(vm.uiState.photo)
        vm.onCaptured(newTicket, byteArrayOf(2))
        runCurrent()
        vm.onRetake()
        result.complete(listOf(word))
        runCurrent()
        assertEquals(CameraStage.Preview, vm.uiState.stage)
        assertTrue(vm.uiState.objects.isEmpty())
        assertNull(vm.uiState.photo)
    }

    @Test fun recognitionFailureCanRetrySamePhotoAndEmptyResultIsSuccessful() = runTest {
        var calls = 0
        val vm = vm { if (++calls == 1) error("Model unavailable") else emptyList() }
        val photo = byteArrayOf(1)
        vm.onCaptured(vm.onCaptureStarted()!!, photo)
        runCurrent()
        assertEquals(CaptureError.Recognition, vm.uiState.captureError)
        vm.retryRecognition()
        runCurrent()
        assertEquals(2, calls)
        assertSame(photo, vm.uiState.photo)
        assertNull(vm.uiState.captureError)
        assertEquals(CameraStage.Results, vm.uiState.stage)
        assertTrue(vm.uiState.objects.isEmpty())
    }

    @Test fun captureFailureReleasesShutterAndSwitchIsBlockedWhileBusy() {
        val vm = vm { emptyList() }
        vm.onCameraAvailability(true, true)
        val ticket = vm.onCaptureStarted()!!
        vm.onSwitchCamera()
        assertFalse(vm.uiState.useFrontCamera)
        vm.onCaptureFailed(ticket)
        assertEquals(CaptureError.Capture, vm.uiState.captureError)
        assertNotNull(vm.onCaptureStarted())
    }

    @Test fun cameraErrorDuringCaptureCanReopenWithoutAcceptingOldCallback() {
        val vm = vm { emptyList() }
        val oldTicket = vm.onCaptureStarted()!!
        vm.onError()
        assertEquals(CameraStage.Preview, vm.uiState.stage)
        assertNull(vm.onCaptureStarted())
        vm.onRetry()
        vm.onReady()
        assertNotNull(vm.onCaptureStarted())
        vm.onCaptured(oldTicket, byteArrayOf(1))
        assertNull(vm.uiState.photo)
    }
}

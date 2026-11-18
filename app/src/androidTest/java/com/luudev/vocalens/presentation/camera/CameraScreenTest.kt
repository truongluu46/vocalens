package com.luudev.vocalens.presentation.camera

import android.Manifest
import android.graphics.BitmapFactory
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.repository.LocalObjectRecognitionRepository
import com.luudev.vocalens.domain.usecase.RecognizeObjectsUseCase
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cameraCaptureAnalysisAndRetakeWorkOnDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName,
            Manifest.permission.CAMERA)
        lateinit var vm: CameraViewModel
        compose.runOnUiThread {
            vm = CameraViewModel(RecognizeObjectsUseCase(LocalObjectRecognitionRepository(instrumentation.targetContext)))
        }
        compose.setContent { VocaLensTheme { CameraRoute(vm, onBack = {}) } }
        repeat(2) {
            compose.waitUntil(20_000) {
                compose.onAllNodesWithTag("camera_capture").fetchSemanticsNodes().isNotEmpty() &&
                    !vm.uiState.isLoading && !vm.uiState.hasError
            }
            compose.onNodeWithTag("camera_capture").assertIsEnabled().performClick()
            compose.waitUntil(30_000) { vm.uiState.stage == CameraStage.Results }
            compose.runOnIdle {
                assertNull("Capture or inference failed", vm.uiState.captureError)
                val photo = vm.uiState.photo!!
                val bitmap = BitmapFactory.decodeByteArray(photo, 0, photo.size)
                assertNotNull(bitmap)
                assertTrue(maxOf(bitmap.width, bitmap.height) <= 1280)
                bitmap.recycle()
            }
            compose.onNodeWithText("Chụp lại").performClick()
        }
    }
}

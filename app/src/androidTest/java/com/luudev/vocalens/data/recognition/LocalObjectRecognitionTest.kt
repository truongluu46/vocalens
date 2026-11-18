package com.luudev.vocalens.data.recognition

import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.repository.LocalObjectRecognitionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LocalObjectRecognitionTest {
    @Test fun productionRepositoryRecognizesPedestrianWithValidBounds() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val photo = instrumentation.context.assets.open("bus.jpg").use { it.readBytes() }
        val objects = LocalObjectRecognitionRepository(instrumentation.targetContext).recognize(photo)
        assertTrue(objects.any { it.label == "Man" && it.confidence > .5f })
        objects.forEach {
            assertTrue(it.bounds.left >= 0 && it.bounds.right <= 1)
            assertTrue(it.bounds.top >= 0 && it.bounds.bottom <= 1)
            assertTrue(it.bounds.left < it.bounds.right && it.bounds.top < it.bounds.bottom)
        }
    }
}

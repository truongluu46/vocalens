package com.luudev.vocalens.data.recognition

import com.luudev.vocalens.domain.model.ObjectBounds
import com.luudev.vocalens.domain.model.RecognizedObject
import org.junit.Assert.*
import org.junit.Test

class YoloPostprocessorTest {
    @Test fun mapsNormalizedModelCoordinatesBackThroughLetterbox() {
        // Wide source fitted into 640x320 with vertical padding; output is channel-major.
        val result = YoloPostprocessor.decode(floatArrayOf(.5f, .5f, .5f, .25f, .9f, .1f),
            listOf("Book", "Cup"), 1, 640, 320, 0, 160).single()
        assertEquals("Book", result.label)
        assertEquals(ObjectBounds(.25f, .25f, .75f, .75f), result.bounds)
    }

    @Test fun suppressesDuplicatesOnlyWithinTheSameClass() {
        val box = ObjectBounds(.1f, .1f, .8f, .8f)
        val a = RecognizedObject(0, "Book", .9f, box)
        val b = a.copy(confidence = .8f)
        val c = RecognizedObject(1, "Cup", .85f, box)
        assertEquals(listOf(a, c), YoloPostprocessor.nms(listOf(b, c, a)))
    }

    @Test fun ignoresWeakAndPaddingOnlyDetections() {
        assertTrue(YoloPostprocessor.decode(floatArrayOf(.5f, .05f, .2f, .05f, .9f),
            listOf("Book"), 1, 640, 320, 0, 160).isEmpty())
        assertTrue(YoloPostprocessor.decode(floatArrayOf(.5f, .5f, .2f, .2f, .1f),
            listOf("Book"), 1, 640, 640, 0, 0).isEmpty())
    }
}

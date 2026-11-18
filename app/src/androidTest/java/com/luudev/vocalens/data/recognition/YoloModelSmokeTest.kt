package com.luudev.vocalens.data.recognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Diagnostic only: validates this model before integrating inference into the camera's data layer. */
@RunWith(AndroidJUnit4::class)
class YoloModelSmokeTest {
    private data class Detection(val classId: Int, val score: Float, val box: RectF)

    @Test
    fun recognizesObjectsInSamplePhoto() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        val asset = args.getString("imageAsset") ?: "bus.jpg"
        // The sample also contains pedestrians. This export reliably detects Man at confidence .25;
        // Bus is not detected at that threshold, so this is a pipeline smoke test, not a bus benchmark.
        val expectedLabel = args.getString("expectedLabel") ?: "Man"
        // This exported model returns coordinates relative to the 640x640 letterboxed image.
        val coordinateSpace = args.getString("coordinateSpace") ?: "normalized"
        require(coordinateSpace in setOf("normalized", "pixels")) { "Invalid coordinateSpace: $coordinateSpace" }
        val normalizedCoordinates = coordinateSpace == "normalized"
        val labels = context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
        assertEquals(601, labels.size)
        assertTrue("Blank label would shift class IDs", labels.none { it.isBlank() })
        val photo = instrumentation.context.assets.open(asset).use {
            requireNotNull(BitmapFactory.decodeStream(it)) { "Cannot decode $asset" }
        }
        val options = Interpreter.Options().setNumThreads(4)
        try {
            Interpreter(FileUtil.loadMappedFile(context, "yolov8n-oiv7.tflite"), options).use { model ->
                val input = model.getInputTensor(0)
                val output = model.getOutputTensor(0)
                Log.i(TAG, "Input=${input.shape().contentToString()} ${input.dataType()}")
                Log.i(TAG, "Output=${output.shape().contentToString()} ${output.dataType()}")
                assertArrayEquals(intArrayOf(1, 3, SIDE, SIDE), input.shape())
                assertArrayEquals(intArrayOf(1, labels.size + 4, ANCHORS), output.shape())
                assertEquals(DataType.FLOAT32, input.dataType())
                assertEquals(DataType.FLOAT32, output.dataType())

                // Letterbox preserves aspect ratio; record exact rounded resize for inverse mapping.
                val ratio = min(SIDE.toFloat() / photo.width, SIDE.toFloat() / photo.height)
                val resizedWidth = (photo.width * ratio).roundToInt().coerceIn(1, SIDE)
                val resizedHeight = (photo.height * ratio).roundToInt().coerceIn(1, SIDE)
                val padX = (SIDE - resizedWidth) / 2
                val padY = (SIDE - resizedHeight) / 2
                val letterbox = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
                Canvas(letterbox).apply {
                    drawColor(Color.rgb(114, 114, 114))
                    drawBitmap(photo, null,
                        RectF(padX.toFloat(), padY.toFloat(), (padX + resizedWidth).toFloat(),
                            (padY + resizedHeight).toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                }
                val pixels = IntArray(SIDE * SIDE)
                letterbox.getPixels(pixels, 0, SIDE, 0, 0, SIDE, SIDE)
                letterbox.recycle()
                val inputBuffer = ByteBuffer.allocateDirect(input.numBytes()).order(ByteOrder.nativeOrder())
                // NCHW: all red values, then all green, then all blue. RGB normalized to [0, 1].
                for (shift in intArrayOf(16, 8, 0)) {
                    for (pixel in pixels) inputBuffer.putFloat(((pixel shr shift) and 255) / 255f)
                }
                inputBuffer.rewind()
                val outputBuffer = ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder())
                val start = System.nanoTime()
                model.run(inputBuffer, outputBuffer)
                val elapsedMs = (System.nanoTime() - start) / 1_000_000
                outputBuffer.rewind()
                val values = FloatArray(output.numElements())
                outputBuffer.asFloatBuffer().get(values)
                assertTrue("Output contains NaN/Infinity", values.all { it.isFinite() })
                fun value(channel: Int, anchor: Int) = values[channel * ANCHORS + anchor]
                val candidates = mutableListOf<Detection>()
                var maxScore = 0f
                var maxCoordinate = 0f
                for (anchor in 0 until ANCHORS) {
                    var bestClass = 0
                    var score = value(4, anchor)
                    for (classId in 1 until labels.size) {
                        val candidate = value(4 + classId, anchor)
                        if (candidate > score) { score = candidate; bestClass = classId }
                    }
                    maxScore = max(maxScore, score)
                    for (channel in 0..3) maxCoordinate = max(maxCoordinate, value(channel, anchor))
                    if (score < CONFIDENCE) continue
                    val coordinateScale = if (normalizedCoordinates) SIDE.toFloat() else 1f
                    val cx = value(0, anchor) * coordinateScale
                    val cy = value(1, anchor) * coordinateScale
                    val width = value(2, anchor) * coordinateScale
                    val height = value(3, anchor) * coordinateScale
                    if (width <= 0 || height <= 0) continue
                    val scaleX = resizedWidth.toFloat() / photo.width
                    val scaleY = resizedHeight.toFloat() / photo.height
                    val box = RectF(
                        ((cx - width / 2 - padX) / scaleX).coerceIn(0f, photo.width.toFloat()),
                        ((cy - height / 2 - padY) / scaleY).coerceIn(0f, photo.height.toFloat()),
                        ((cx + width / 2 - padX) / scaleX).coerceIn(0f, photo.width.toFloat()),
                        ((cy + height / 2 - padY) / scaleY).coerceIn(0f, photo.height.toFloat())
                    )
                    if (box.width() > 0 && box.height() > 0) candidates += Detection(bestClass, score, box)
                }
                val detections = nms(candidates)
                val report = buildString {
                    appendLine("Image=$asset (${photo.width}x${photo.height})")
                    appendLine("Input=${input.shape().contentToString()} ${input.dataType()}")
                    appendLine("Output=${output.shape().contentToString()} ${output.dataType()}")
                    appendLine("RGB / 255, letterbox=114, coordinateSpace=${if (normalizedCoordinates) "normalized" else "pixels"}")
                    appendLine("Inference=${elapsedMs}ms, maxScore=$maxScore, maxCoordinate=$maxCoordinate")
                    appendLine("Candidates=${candidates.size}, after class-aware NMS=${detections.size}")
                    detections.forEach { appendLine("${labels[it.classId]} score=${it.score} box=${it.box}") }
                }
                val directory = File(context.filesDir, "model-smoke-test").apply { mkdirs() }
                File(directory, "detections.txt").writeText(report)
                saveAnnotated(photo, detections, labels, File(directory, "detections.jpg"))
                Log.i(TAG, report)
                Log.i(TAG, "Artifacts: ${directory.absolutePath}")
                assertTrue("No detections. Check preprocessing/runtime. $report", detections.isNotEmpty())
                assertTrue("Expected '$expectedLabel'; inspect report/annotated image. $report",
                    detections.any { labels[it.classId].equals(expectedLabel, ignoreCase = true) })
            }
        } finally {
            photo.recycle()
        }
    }

    @Test
    fun nmsRemovesSameClassDuplicatesAndKeepsOtherClasses() {
        val best = Detection(0, .95f, RectF(0f, 0f, 100f, 100f))
        val duplicate = Detection(0, .8f, RectF(2f, 2f, 102f, 102f))
        val otherClass = Detection(1, .9f, RectF(0f, 0f, 100f, 100f))
        val separate = Detection(0, .7f, RectF(200f, 200f, 250f, 250f))
        assertEquals(listOf(best, otherClass, separate), nms(listOf(duplicate, separate, otherClass, best)))
    }

    private fun nms(candidates: List<Detection>): List<Detection> {
        val selected = mutableListOf<Detection>()
        for (candidate in candidates.sortedByDescending { it.score }) {
            if (selected.none { it.classId == candidate.classId && iou(it.box, candidate.box) > .45f }) {
                selected += candidate
                if (selected.size == 100) break
            }
        }
        return selected
    }

    private fun iou(a: RectF, b: RectF): Float {
        val intersection = max(0f, min(a.right, b.right) - max(a.left, b.left)) *
            max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
        val union = a.width() * a.height() + b.width() * b.height() - intersection
        return if (union > 0) intersection / union else 0f
    }

    private fun saveAnnotated(photo: Bitmap, detections: List<Detection>, labels: List<String>, file: File) {
        val result = photo.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeWidth = max(2f, photo.width / 250f)
            textSize = max(16f, photo.width / 35f)
        }
        detections.forEach {
            paint.style = Paint.Style.STROKE
            canvas.drawRect(it.box, paint)
            paint.style = Paint.Style.FILL
            canvas.drawText("${labels[it.classId]} ${"%.2f".format(java.util.Locale.US, it.score)}",
                it.box.left, max(paint.textSize, it.box.top - 5f), paint)
        }
        file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        result.recycle()
    }

    private companion object {
        const val TAG = "VocabLensModelTest"
        const val SIDE = 640
        const val ANCHORS = 8400
        const val CONFIDENCE = .25f
    }
}

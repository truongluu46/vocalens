package com.luudev.vocalens.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.luudev.vocalens.data.recognition.YoloPostprocessor
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.domain.repository.ObjectRecognitionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToInt

class LocalObjectRecognitionRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ObjectRecognitionRepository {
    private val context = context.applicationContext

    override suspend fun recognize(image: ByteArray): List<RecognizedObject> = withContext(dispatcher) {
        ensureActive()
        val labels = context.assets.open("labels.txt").bufferedReader().use { it.readLines() }
        require(labels.size == 601 && labels.none { it.isBlank() }) { "Invalid model labels" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot decode photo" }
        val options = BitmapFactory.Options().apply { inSampleSize = 1 }
        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600) {
            options.inSampleSize *= 2
        }
        val photo = requireNotNull(BitmapFactory.decodeByteArray(image, 0, image.size, options))
        try {
            val ratio = min(640f / photo.width, 640f / photo.height)
            val width = (photo.width * ratio).roundToInt().coerceIn(1, 640)
            val height = (photo.height * ratio).roundToInt().coerceIn(1, 640)
            val padX = (640 - width) / 2
            val padY = (640 - height) / 2
            val letterbox = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(640 * 640)
            try {
                Canvas(letterbox).apply {
                    drawColor(Color.rgb(114, 114, 114))
                    drawBitmap(photo, null, RectF(padX.toFloat(), padY.toFloat(),
                        (padX + width).toFloat(), (padY + height).toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                }
                letterbox.getPixels(pixels, 0, 640, 0, 0, 640, 640)
            } finally { letterbox.recycle() }
            ensureActive()
            // Each call owns its interpreter; cancellation and leaving the camera cannot leak it.
            Interpreter(FileUtil.loadMappedFile(context, "yolov8n-oiv7.tflite"),
                Interpreter.Options().setNumThreads(4)).use { model ->
                require(model.getInputTensor(0).shape().contentEquals(intArrayOf(1, 3, 640, 640)))
                require(model.getOutputTensor(0).shape().contentEquals(intArrayOf(1, labels.size + 4, 8400)))
                require(model.getInputTensor(0).dataType() == DataType.FLOAT32)
                require(model.getOutputTensor(0).dataType() == DataType.FLOAT32)
                val input = ByteBuffer.allocateDirect(model.getInputTensor(0).numBytes()).order(ByteOrder.nativeOrder())
                for (shift in intArrayOf(16, 8, 0)) {
                    for (pixel in pixels) input.putFloat(((pixel shr shift) and 255) / 255f)
                }
                input.rewind()
                val output = ByteBuffer.allocateDirect(model.getOutputTensor(0).numBytes()).order(ByteOrder.nativeOrder())
                model.run(input, output)
                ensureActive()
                output.rewind()
                val values = FloatArray(model.getOutputTensor(0).numElements())
                output.asFloatBuffer().get(values)
                YoloPostprocessor.decode(values, labels, 8400, width, height, padX, padY)
            }
        } finally { photo.recycle() }
    }
}

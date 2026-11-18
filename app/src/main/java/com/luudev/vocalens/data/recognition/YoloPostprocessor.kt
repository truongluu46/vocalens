package com.luudev.vocalens.data.recognition

import com.luudev.vocalens.domain.model.ObjectBounds
import com.luudev.vocalens.domain.model.RecognizedObject
import kotlin.math.max
import kotlin.math.min

/** Decoder for the bundled export: NCHW input and normalized cx/cy/w/h output, no objectness. */
internal object YoloPostprocessor {
    fun decode(
        values: FloatArray, labels: List<String>, anchors: Int,
        resizedWidth: Int, resizedHeight: Int, padX: Int, padY: Int,
        side: Int = 640, threshold: Float = .25f
    ): List<RecognizedObject> {
        require(values.size == (labels.size + 4) * anchors)
        require(values.all { it.isFinite() }) { "Model returned invalid values" }
        fun value(channel: Int, anchor: Int) = values[channel * anchors + anchor]
        val candidates = mutableListOf<RecognizedObject>()
        for (anchor in 0 until anchors) {
            var best = 0
            for (id in 1 until labels.size) {
                if (value(id + 4, anchor) > value(best + 4, anchor)) best = id
            }
            val score = value(best + 4, anchor)
            if (score < threshold) continue
            val cx = value(0, anchor) * side
            val cy = value(1, anchor) * side
            val width = value(2, anchor) * side
            val height = value(3, anchor) * side
            if (width <= 0 || height <= 0) continue
            val bounds = ObjectBounds(
                ((cx - width / 2 - padX) / resizedWidth).coerceIn(0f, 1f),
                ((cy - height / 2 - padY) / resizedHeight).coerceIn(0f, 1f),
                ((cx + width / 2 - padX) / resizedWidth).coerceIn(0f, 1f),
                ((cy + height / 2 - padY) / resizedHeight).coerceIn(0f, 1f)
            )
            if (bounds.right > bounds.left && bounds.bottom > bounds.top) {
                candidates += RecognizedObject(best, labels[best], score, bounds)
            }
        }
        return nms(candidates)
    }

    fun nms(candidates: List<RecognizedObject>): List<RecognizedObject> {
        val selected = mutableListOf<RecognizedObject>()
        for (candidate in candidates.sortedByDescending { it.confidence }) {
            if (selected.none { it.classId == candidate.classId && iou(it.bounds, candidate.bounds) > .45f }) {
                selected += candidate
                if (selected.size == 100) break
            }
        }
        return selected
    }

    private fun iou(a: ObjectBounds, b: ObjectBounds): Float {
        val intersection = max(0f, min(a.right, b.right) - max(a.left, b.left)) *
            max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
        val union = (a.right - a.left) * (a.bottom - a.top) +
            (b.right - b.left) * (b.bottom - b.top) - intersection
        return if (union > 0) intersection / union else 0f
    }
}

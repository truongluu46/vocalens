package com.luudev.vocalens.domain.model

/** Coordinates are normalized to the upright source image, independently of the model input. */
data class ObjectBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class RecognizedObject(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val bounds: ObjectBounds
)

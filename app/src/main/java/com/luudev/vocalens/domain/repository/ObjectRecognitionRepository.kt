package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.RecognizedObject

interface ObjectRecognitionRepository {
    /** Encoded JPEG/PNG pixels must already be upright; EXIF rotation is not applied here. */
    suspend fun recognize(image: ByteArray): List<RecognizedObject>
}

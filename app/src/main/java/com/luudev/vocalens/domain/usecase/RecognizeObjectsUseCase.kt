package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.ObjectRecognitionRepository

class RecognizeObjectsUseCase(private val repository: ObjectRecognitionRepository) {
    suspend operator fun invoke(image: ByteArray) = repository.recognize(image)
}

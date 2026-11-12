package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.PracticeLesson
import kotlinx.coroutines.flow.Flow

interface PracticeRepository {
    fun observeLessons(userId: String): Flow<List<PracticeLesson>>
}

package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.CollectionDestination
import com.luudev.vocalens.domain.model.VocabularyWord

/** Saves the word and its collection membership atomically, without replacing existing members. */
interface WordCollectionRepository {
    suspend fun save(userId: String, word: VocabularyWord, destination: CollectionDestination)
}

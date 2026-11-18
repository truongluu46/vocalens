package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.WordCollectionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SaveWordToCollectionTest {
    private val saved = mutableListOf<VocabularyWord>()
    private val useCase = SaveWordToCollection(object : WordCollectionRepository {
        override suspend fun save(userId: String, word: VocabularyWord, destination: CollectionDestination) {
            saved += word
        }
    })
    private val word = VocabularyWord("", " Book ", "", " Sách ", VocabularyCategory.Objects)

    @Test fun normalizesContentAndProducesStableIdsWithoutOverwritingEditedMeaning() = runTest {
        useCase("A", word, CollectionDestination.Existing("collection"))
        useCase("A", word.copy(id = "untrusted-id"), CollectionDestination.Existing("collection"))
        useCase("A", word.copy(meaning = "Quyển sách"), CollectionDestination.Existing("collection"))
        assertEquals("Book", saved[0].english)
        assertEquals("Sách", saved[0].meaning)
        assertEquals(saved[0].id, saved[1].id)
        assertNotEquals(saved[0].id, saved[2].id)
    }

    @Test fun rejectsBlankMeaningAndCollectionNameBeforeWriting() = runTest {
        for ((draft, target) in listOf(
            word.copy(meaning = " ") to CollectionDestination.Existing("collection"),
            word to CollectionDestination.New(" ")
        )) {
            try { useCase("A", draft, target); fail("Invalid draft was accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertTrue(saved.isEmpty())
    }
}

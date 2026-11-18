package com.luudev.vocalens.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.UserEntity
import com.luudev.vocalens.data.repository.*
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.SaveWordToCollection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WordCollectionRepositoryTest {
    private lateinit var db: VocabLensDatabase
    private lateinit var writer: LocalWordCollectionRepository
    private lateinit var collections: LocalCollectionRepositoryImpl
    private lateinit var words: LocalVocabularyRepositoryImpl
    private val book = VocabularyWord("", "Book", "", "Sách", VocabularyCategory.Objects)

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            VocabLensDatabase::class.java).build()
        for (owner in listOf("A", "B")) db.userDao().synchronize(
            UserEntity(owner, "$owner@example.com", owner, createdAt = 1, updatedAt = 1))
        writer = LocalWordCollectionRepository(db)
        collections = LocalCollectionRepositoryImpl(db.collectionDao())
        words = LocalVocabularyRepositoryImpl(db.vocabularyDao())
    }
    @After fun close() { db.close() }

    @Test fun savesToNewCollectionThenAppendsIdempotentlyWithoutRemovingOtherWords() = runBlocking {
        val save = SaveWordToCollection(writer)
        save("A", book, CollectionDestination.New("Đồ vật"))
        val id = collections.observeCollections("A").first().single().id
        save("A", book.copy(english = "Cup", meaning = "Cốc"), CollectionDestination.Existing(id))
        save("A", book, CollectionDestination.Existing(id))
        assertEquals(setOf("Book", "Cup"), collections.observeWords("A", id).first().map { it.english }.toSet())
        assertEquals(2, words.observeSavedWords("A").first().size)
        assertEquals(2, collections.observeCollections("A").first().single().wordCount)
        assertTrue(collections.observeCollections("B").first().isEmpty())
    }

    @Test fun rejectsOtherOwnersCollectionWithoutSavingOrphanWord() = runBlocking {
        collections.create("B", "Riêng tư", "")
        val id = collections.observeCollections("B").first().single().id
        try { SaveWordToCollection(writer)("A", book, CollectionDestination.Existing(id)); fail("Wrong owner accepted") }
        catch (_: IllegalStateException) { }
        assertTrue(words.observeSavedWords("A").first().isEmpty())
        assertEquals(0, collections.observeCollections("B").first().single().wordCount)
    }

    @Test fun rollsBackNewCollectionWhenVocabularyWriteFails() = runBlocking {
        words.saveWord("A", book.copy(id = "collision"))
        try {
            writer.save("A", book.copy(id = "collision", meaning = "Nội dung khác"), CollectionDestination.New("Rollback"))
            fail("Conflicting content accepted")
        } catch (_: IllegalArgumentException) { }
        assertTrue(collections.observeCollections("A").first().isEmpty())
        assertEquals("Sách", words.observeSavedWords("A").first().single().meaning)
    }
}

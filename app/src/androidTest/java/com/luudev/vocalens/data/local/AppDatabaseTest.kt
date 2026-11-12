package com.luudev.vocalens.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.*
import com.luudev.vocalens.data.repository.*
import com.luudev.vocalens.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: VocabLensDatabase
    private val word = VocabularyWord("book", "Book", "/bʊk/", "Quyển sách", VocabularyCategory.Objects)
    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).build()
        db.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
        db.userDao().synchronize(UserEntity("B", "b@example.com", "Binh", createdAt = 1, updatedAt = 1))
    }
    @After fun tearDown() { db.close() }

    private suspend fun save(uid: String = "A") = LocalVocabularyRepositoryImpl(db.vocabularyDao()).saveWord(uid, word)
    private suspend fun mustFail(block: suspend () -> Unit) {
        var failed = false
        try { block() } catch (expected: Exception) { failed = true }
        assertTrue("Expected constraint or validation failure", failed)
    }
    private suspend fun start(uid: String = "A", id: String = "session", at: Long = 1000) =
        db.studyDao().start(StudySessionEntity(uid, id, sessionType = "VOCAB_REVIEW", startedAt = at))
    private fun review(uid: String = "A", id: String = "review") = VocabularyReviewEntity(
        uid, id, "session", "book", "QUIZ", isCorrect = true, reviewedAt = 1500)

    @Test fun schemaContainsTenTablesAndRealForeignKeys() = runBlocking {
        val tables = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use {
            while (it.moveToNext()) tables.add(it.getString(0))
        }
        assertTrue(tables.containsAll(setOf("users", "user_settings", "vocabulary", "user_vocabulary", "collections",
            "collection_items", "lessons", "user_lesson_progress", "study_sessions", "vocabulary_reviews")))
        mustFail { save("unknown") }
    }

    @Test fun savedWordsAndCollectionsAreIsolatedAndDeletionCascadesOnlyLinks() = runBlocking {
        save("A"); save("B")
        val collections = db.collectionDao()
        collections.save(CollectionEntity("A", "toeic", "TOEIC", createdAt = 1, updatedAt = 1))
        collections.addItem(CollectionItemEntity("A", "toeic", "book", 2))
        collections.addItem(CollectionItemEntity("A", "toeic", "book", 3))
        assertEquals(1, collections.observeWords("A", "toeic").first().size)
        mustFail { collections.addItem(CollectionItemEntity("B", "toeic", "book", 4)) }
        collections.delete("A", "toeic")
        assertEquals(1, db.vocabularyDao().observeWords("A").first().size)
        collections.save(CollectionEntity("A", "new", "New", createdAt = 1, updatedAt = 1))
        collections.addItem(CollectionItemEntity("A", "new", "book", 2))
        db.vocabularyDao().deleteWord("A", "book")
        assertTrue(collections.observeWords("A", "new").first().isEmpty())
        assertEquals(1, db.vocabularyDao().observeWords("B").first().size)
    }

    @Test fun collectionCrudCountsAndMembershipRollbackAreAccountScoped() = runBlocking {
        save("A"); save("B")
        val repository = LocalCollectionRepositoryImpl(db.collectionDao())
        repository.create("A", "Travel", "Words for trips")
        val id = repository.observeCollections("A").first().single().id
        assertTrue(repository.observeCollections("B").first().isEmpty())
        repository.update("A", id, "TOEIC", "Updated description")
        assertEquals("TOEIC", repository.observeCollections("A").first().single().name)
        mustFail { repository.update("B", id, "Wrong owner", "") }
        repository.setWords("A", id, setOf("book"))
        assertEquals(1, repository.observeCollections("A").first().single().wordCount)
        mustFail { repository.setWords("A", id, setOf("book", "missing")) }
        assertEquals(1, repository.observeWords("A", id).first().size)
        mustFail { repository.setWords("B", id, setOf("book")) }
        repository.removeWord("A", id, "book")
        assertEquals(0, repository.observeCollections("A").first().single().wordCount)
        repository.setWords("A", id, setOf("book"))
        repository.setWords("A", id, emptySet())
        assertTrue(repository.observeWords("A", id).first().isEmpty())
        repository.setWords("A", id, setOf("book"))
        repository.delete("A", id)
        mustFail { repository.update("A", id, "Cannot recreate", "") }
        assertTrue(repository.observeCollections("A").first().isEmpty())
        assertEquals(1, db.vocabularyDao().observeWords("A").first().size)
        assertEquals(1, db.vocabularyDao().observeWords("B").first().size)
    }

    @Test fun reviewAndXpRetriesAreIdempotentAndHistorySurvivesUnsave() = runBlocking {
        save(); start()
        db.studyDao().recordReview(review())
        db.studyDao().recordReview(review())
        assertEquals(1, db.studyDao().observeReviews("A").first().size)
        mustFail { db.vocabularyDao().deleteWord("A", "book") }
        db.studyDao().finish("A", "session", "COMPLETED", 2000, 1000, 5)
        db.studyDao().finish("A", "session", "COMPLETED", 2000, 1000, 5)
        db.studyDao().recordReview(review())
        mustFail { db.studyDao().recordReview(review().copy(isCorrect = false)) }
        assertEquals(5, db.studyDao().session("A", "session")!!.earnedXp)
        db.vocabularyDao().deleteWord("A", "book")
        assertEquals(1, db.studyDao().observeReviews("A").first().size)
        save()
        db.openHelper.readableDatabase.query("SELECT reviewCount, correctCount FROM user_vocabulary WHERE userId='A'").use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)); assertEquals(1, it.getInt(1))
        }
    }

    @Test fun invalidOrCrossAccountReviewDoesNotWriteOrIncrementCounters() = runBlocking {
        save(); start()
        mustFail { db.studyDao().recordReview(review("B")) }
        mustFail { db.studyDao().recordReview(review().copy(reviewMode = "SELF_RATED", rating = "GOOD")) }
        mustFail { db.studyDao().recordReview(review().copy(reviewedAt = 999)) }
        db.studyDao().recordReview(review().copy(reviewMode = "SELF_RATED", rating = "GOOD", isCorrect = null))
        assertNull(db.studyDao().observeReviews("A").first().single().isCorrect)
        assertTrue(db.studyDao().observeReviews("B").first().isEmpty())
    }

    @Test fun sessionsRejectOverlapAndInvalidAccounting() = runBlocking {
        start()
        mustFail { start(id = "second", at = 2000) }
        mustFail { db.studyDao().finish("A", "session", "COMPLETED", 2000, 1001, 5) }
        mustFail { db.studyDao().finish("A", "session", "ABANDONED", 2000, 1000, 5) }
        db.studyDao().finish("A", "session", "ABANDONED", 2000, 1000, 0)
        mustFail { start(id = "backdated", at = 1500) }
        start(id = "next", at = 2000)
        assertEquals("ACTIVE", db.studyDao().session("A", "next")!!.status)
    }

    @Test fun lessonsHaveIndependentProgressAndCompletionSurvivesRewatch() = runBlocking {
        val lesson = LessonEntity("lesson", "Actual lesson", mediaType = "VIDEO", mediaUrl = "https://example.com/lesson.mp4",
            durationSeconds = 100, difficulty = "EASY", publishedAt = 1, updatedAt = 1)
        db.lessonDao().saveLesson(lesson)
        db.lessonDao().saveProgress(UserLessonProgressEntity("A", "lesson", "IN_PROGRESS", 80, updatedAt = 2))
        assertEquals(80, db.lessonDao().observeLessons("A").first().single().lastPositionSeconds)
        assertNull(db.lessonDao().observeLessons("B").first().single().lastPositionSeconds)
        mustFail { db.lessonDao().saveProgress(UserLessonProgressEntity("A", "lesson", "IN_PROGRESS", 101, updatedAt = 3)) }
        db.lessonDao().saveProgress(UserLessonProgressEntity("A", "lesson", "COMPLETED", 100, completedAt = 4, updatedAt = 4))
        db.lessonDao().saveProgress(UserLessonProgressEntity("A", "lesson", "IN_PROGRESS", 5, updatedAt = 5))
        assertEquals("COMPLETED", db.lessonDao().observeLessons("A").first().single().status)
        val repo = LocalPracticeRepositoryImpl(db.lessonDao())
        assertEquals(1f, repo.observeLessons("A").first().single().progress, 0f)
        assertEquals(0f, repo.observeLessons("B").first().single().progress, 0f)
    }

    @Test fun profileAndDashboardStartAtZeroAndReflectOnlyOwnersActualData() = runBlocking {
        val stats = LocalLearningStatistics(db.statisticsDao(), db.studyDao())
        val profiles = LocalProfileRepositoryImpl(db.userDao(), stats)
        val dashboard = LocalDashboardRepositoryImpl(db.userDao(), stats)
        assertEquals(0, profiles.observeProfile("A").first().totalXp)
        assertEquals(0, dashboard.observeDashboard("A").first().studyMinutes)
        val now = System.currentTimeMillis()
        save(); start(at = now - 60_000)
        db.studyDao().recordReview(review().copy(reviewedAt = now - 1000))
        db.studyDao().finish("A", "session", "COMPLETED", now, 60_000, 5)
        assertEquals(5, profiles.observeProfile("A").first().totalXp)
        assertEquals(1, dashboard.observeDashboard("A").first().completedWords)
        assertEquals(1, dashboard.observeDashboard("A").first().studyMinutes)
        assertEquals(0, profiles.observeProfile("B").first().totalXp)
        val profile = profiles.observeProfile("A").first()
        profiles.saveProfile("A", profile.copy(fullName = "New name", dailyGoal = 50, soundEnabled = false, email = "fake@example.com"))
        db.userDao().synchronize(UserEntity("A", "updated@example.com", "Auth name", createdAt = now, updatedAt = now))
        val updated = profiles.observeProfile("A").first()
        assertEquals("New name", updated.fullName)
        assertEquals("updated@example.com", updated.email)
        assertEquals(50, updated.dailyGoal)
        assertFalse(updated.soundEnabled)
    }

    @Test fun dataPersistsAfterClosingAndReopeningFreshDatabase() = runBlocking {
        val name = "new-erd-persistence-test.db"
        context.deleteDatabase(name)
        var disk = Room.databaseBuilder(context, VocabLensDatabase::class.java, name).build()
        try {
            disk.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
            LocalVocabularyRepositoryImpl(disk.vocabularyDao()).saveWord("A", word)
            disk.close()
            disk = Room.databaseBuilder(context, VocabLensDatabase::class.java, name).build()
            assertEquals(listOf(word), LocalVocabularyRepositoryImpl(disk.vocabularyDao()).observeSavedWords("A").first())
            assertTrue(disk.vocabularyDao().observeWords("B").first().isEmpty())
        } finally { disk.close(); context.deleteDatabase(name) }
    }
}

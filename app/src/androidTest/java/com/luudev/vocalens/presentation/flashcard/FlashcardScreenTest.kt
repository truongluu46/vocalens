package com.luudev.vocalens.presentation.flashcard

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.UserEntity
import com.luudev.vocalens.data.repository.LocalFlashcardRepositoryImpl
import com.luudev.vocalens.data.repository.LocalVocabularyRepositoryImpl
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: VocabLensDatabase
    private lateinit var vm: FlashcardViewModel
    private val store = ViewModelStore()

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).build()
        db.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
        val vocabulary = LocalVocabularyRepositoryImpl(db.vocabularyDao())
        vocabulary.saveWord("A", VocabularyWord("book", "Book", "", "Quyển sách", VocabularyCategory.Objects))
        val repository = LocalFlashcardRepositoryImpl(db)
        compose.runOnUiThread {
            vm = FlashcardViewModel(GetSavedVocabulary(vocabulary), StartFlashcards(repository),
                RateFlashcard(repository), AbandonFlashcards(repository))
            store.put("flashcard", vm)
            vm.onUserChanged("A")
            vm.setForeground(true)
        }
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                FlashcardScreen(vm.uiState, onBack = {}, onCategory = vm::selectCategory,
                    onShuffle = vm::setShuffled, onReverse = vm::setMeaningFirst,
                    onStart = { vm.start() }, onReveal = vm::reveal, onRate = vm::rate,
                    onRetry = vm::retry, onDifficult = { vm.start(true) },
                    onSetup = { vm.prepare() }, onOpenVocabulary = {}, onSpeak = {})
            }
        }
        compose.waitUntil(5_000) { !vm.uiState.loading }
    }

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
        db.close()
    }

    @Test fun flipRateAndCompletePersistResult() {
        compose.onNodeWithText("Bắt đầu ôn tập").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Studying }
        compose.onNodeWithText("Quyển sách").assertDoesNotExist()
        compose.onNodeWithText("Lật thẻ").performScrollTo().performClick()
        compose.onNodeWithText("Quyển sách").assertExists()
        compose.onNodeWithText("Đã nhớ").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Completed }
        compose.onNodeWithText("Hoàn thành lượt ôn!").assertExists()
        runBlocking {
            assertEquals("GOOD", db.studyDao().observeReviews("A").first().single().rating)
            assertEquals(5, db.studyDao().observeSessions("A").first().single().earnedXp)
        }
    }

    @Test fun reverseCardHidesEnglishUntilRevealed() {
        compose.runOnUiThread { vm.setMeaningFirst(true) }
        compose.onNodeWithText("Bắt đầu ôn tập").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Studying }
        compose.onNodeWithText("Book").assertDoesNotExist()
        compose.onNodeWithText("Quyển sách").assertExists()
        compose.onNodeWithText("Lật thẻ").performScrollTo().performClick()
        compose.onNodeWithText("Book").assertExists()
    }
}

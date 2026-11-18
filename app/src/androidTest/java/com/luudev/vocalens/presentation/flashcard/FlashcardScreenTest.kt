package com.luudev.vocalens.presentation.flashcard

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import java.io.File

@RunWith(AndroidJUnit4::class)
class FlashcardScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: VocabLensDatabase
    private lateinit var vm: FlashcardViewModel
    private val store = ViewModelStore()
    private var viewport by mutableStateOf(DpSize(360.dp, 640.dp))
    private var fontScale by mutableFloatStateOf(1f)

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VocabLensDatabase::class.java).build()
        db.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
        val vocabulary = LocalVocabularyRepositoryImpl(db.vocabularyDao())
        vocabulary.saveWord("A", VocabularyWord("book", "Book", "/bʊk/", "Quyển sách", VocabularyCategory.Objects,
            partOfSpeech = "noun", exampleSentence = "I read a book every evening before going to bed."))
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
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(viewport).testTag("flashcard-viewport")) {
                            FlashcardScreen(vm.uiState, onBack = {}, onCategory = vm::selectCategory,
                                onShuffle = vm::setShuffled, onReverse = vm::setMeaningFirst,
                                onStart = { vm.start() }, onReveal = vm::reveal, onRate = vm::rate,
                                onRetry = vm::retry, onDifficult = { vm.start(true) },
                                onSetup = { vm.prepare() }, onOpenVocabulary = {}, onSpeak = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(5_000) { !vm.uiState.loading }
    }

    @After fun tearDown() {
        compose.runOnUiThread { store.clear() }
        db.close()
    }

    @Test fun flipRateAndCompletePersistResult() {
        capture("setup")
        compose.onNodeWithText("Bắt đầu ôn tập").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Studying }
        compose.onNodeWithText("Quyển sách").assertDoesNotExist()
        capture("question")
        compose.onNodeWithText("Lật thẻ").assertIsDisplayed().performClick()
        compose.onNodeWithText("Quyển sách").assertExists()
        listOf("Chưa nhớ", "Khó nhớ", "Đã nhớ", "Dễ dàng").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        capture("answer")
        compose.onNodeWithText("Đã nhớ").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Completed }
        compose.onNodeWithText("Hoàn thành lượt ôn!").assertExists()
        capture("result")
        runBlocking {
            assertEquals("GOOD", db.studyDao().observeReviews("A").first().single().rating)
            assertEquals(5, db.studyDao().observeSessions("A").first().single().earnedXp)
        }
    }

    @Test fun reverseCardHidesEnglishUntilRevealed() {
        compose.runOnUiThread { vm.setMeaningFirst(true) }
        compose.onNodeWithText("Bắt đầu ôn tập").assertIsDisplayed().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Studying }
        compose.onNodeWithText("Book").assertDoesNotExist()
        compose.onNodeWithText("Quyển sách").assertExists()
        compose.onNodeWithText("Lật thẻ").assertIsDisplayed().performClick()
        compose.onNodeWithText("Book").assertExists()
    }

    @Test fun optionRowCanBeTappedAndEmptyCategoryOffersVocabulary() {
        compose.onNodeWithText("Đoán từ tiếng Anh").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(vm.uiState.meaningFirst) }
        compose.onNodeWithText("Du lịch").performScrollTo().performClick()
        compose.onNodeWithText("Mở sổ từ vựng").assertIsDisplayed()
        compose.onNodeWithText("Bắt đầu ôn tập").assertDoesNotExist()
    }

    @Test fun largeTextAndShortViewportKeepActionsReachable() {
        compose.runOnIdle { viewport = DpSize(320.dp, 420.dp); fontScale = 1.6f }
        compose.onNodeWithText("Bắt đầu ôn tập").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Studying }
        compose.onNodeWithText("Lật thẻ").performScrollTo().performClick()
        compose.onNodeWithText("Đã nhớ").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.stage == FlashcardStage.Completed }
        compose.onNodeWithText("Hoàn tất").performScrollTo().assertIsDisplayed()
        capture("large-text")
    }

    private fun capture(name: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it).apply { mkdirs() } } ?: context.getExternalFilesDir(null)
        File(output, "flashcard-$name.png").outputStream().use {
            compose.onNodeWithTag("flashcard-viewport").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

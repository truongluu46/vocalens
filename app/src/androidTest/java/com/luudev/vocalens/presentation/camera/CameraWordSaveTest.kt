package com.luudev.vocalens.presentation.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.local.database.VocabLensDatabase
import com.luudev.vocalens.data.local.entity.UserEntity
import com.luudev.vocalens.data.repository.*
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.DictionaryRepository
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CameraWordSaveTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var database: VocabLensDatabase

    @After fun close() {
        compose.runOnUiThread { store.clear() }
        if (::database.isInitialized) database.close()
    }

    @Test fun recognizedWordCanBeSavedOfflineToNewAndExistingCollectionWithoutDuplicates() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        database = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, VocabLensDatabase::class.java).build()
        database.userDao().synchronize(UserEntity("A", "a@example.com", "An", createdAt = 1, updatedAt = 1))
        val photo = instrumentation.context.assets.open("bus.jpg").use { it.readBytes() }
        val found = LocalObjectRecognitionRepository(instrumentation.targetContext).recognize(photo).first { it.label == "Man" }
        val collections = LocalCollectionRepositoryImpl(database.collectionDao())
        lateinit var vm: CameraWordViewModel
        compose.runOnUiThread {
            vm = CameraWordViewModel(ObserveCollections(collections), LookupVocabulary(object : DictionaryRepository {
                override suspend fun lookup(query: String): List<DictionaryEntry> = throw DictionaryException(DictionaryError.Network)
            }), SaveWordToCollection(LocalWordCollectionRepository(database)))
            store.put("cameraWords", vm)
            vm.onUserChanged("A")
        }
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.fillMaxSize()) {
                    RecognitionResults(CameraUiState(stage = CameraStage.Results, photo = photo, objects = listOf(found)),
                        onRetake = {}, onRetry = {}, onWordSelected = vm::selectWord)
                    CameraWordDialog(vm)
                }
            }
        }
        compose.waitUntil(5_000) { !vm.uiState.collectionsLoading }
        compose.onNodeWithText("Man").performClick()
        compose.onNodeWithText("Lưu từ").assertIsNotEnabled()
        compose.onNodeWithTag("camera_word_meaning").performScrollTo().performTextInput("Người đàn ông")
        compose.onNodeWithTag("camera_word_collection_name").performScrollTo().performTextInput("Đồ vật quanh tôi")
        compose.onNodeWithText("Lưu từ").assertIsEnabled().performClick()
        compose.waitUntil(5_000) { vm.uiState.editor == null && vm.uiState.savedLabel != null }
        val collection = collections.observeCollections("A").first().single()
        assertEquals("Đồ vật quanh tôi", collection.name)
        assertEquals("Người đàn ông", collections.observeWords("A", collection.id).first().single().meaning)

        // A second save to the existing collection must not replace its contents or add duplicates.
        compose.waitUntil(5_000) { vm.uiState.collections.any { it.id == collection.id } }
        compose.onNodeWithText("Man").performClick()
        compose.onNodeWithTag("camera_word_meaning").performScrollTo().performTextInput("Người đàn ông")
        compose.onNodeWithText("Đồ vật quanh tôi").performScrollTo().performClick()
        compose.onNodeWithText("Lưu từ").performClick()
        compose.waitUntil(5_000) { vm.uiState.editor == null }
        assertEquals(1, collections.observeWords("A", collection.id).first().size)
        assertEquals(1, LocalVocabularyRepositoryImpl(database.vocabularyDao()).observeSavedWords("A").first().size)
    }
}

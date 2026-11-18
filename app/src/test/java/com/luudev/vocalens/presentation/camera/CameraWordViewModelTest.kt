package com.luudev.vocalens.presentation.camera

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.*
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraWordViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val groups = MutableStateFlow(listOf(VocabularyCollection("one", "Đồ vật")))
    private val collections = object : CollectionRepository {
        override fun observeCollections(userId: String) = groups
        override fun observeWords(userId: String, collectionId: String) = flowOf(emptyList<VocabularyWord>())
        override suspend fun create(userId: String, name: String, description: String) = Unit
        override suspend fun update(userId: String, collectionId: String, name: String, description: String) = Unit
        override suspend fun delete(userId: String, collectionId: String) = Unit
        override suspend fun setWords(userId: String, collectionId: String, wordIds: Set<String>) = Unit
        override suspend fun removeWord(userId: String, collectionId: String, wordId: String) = Unit
    }
    private val objectFound = RecognizedObject(0, "Book", .9f, ObjectBounds(0f, 0f, 1f, 1f))
    private val entry = DictionaryEntry(VocabularyWord("dictionary", "Book", "/bʊk/", "Quyển sách",
        VocabularyCategory.Objects), "A book", "https://example.com/book", "CC", "https://example.com/license")
    private fun vm(
        lookup: suspend () -> List<DictionaryEntry> = { throw DictionaryException(DictionaryError.Network) },
        write: suspend (String, VocabularyWord, CollectionDestination) -> Unit
    ) = CameraWordViewModel(ObserveCollections(collections), LookupVocabulary(object : DictionaryRepository {
        override suspend fun lookup(query: String) = lookup()
    }), SaveWordToCollection(object : WordCollectionRepository {
        override suspend fun save(userId: String, word: VocabularyWord, destination: CollectionDestination) =
            write(userId, word, destination)
    }))

    @Test fun canSaveManualMeaningOfflineAndBlocksDuplicateSave() = runTest {
        var calls = 0
        val completed = CompletableDeferred<Unit>()
        val vm = vm { owner, word, target ->
            assertEquals("A", owner); assertEquals("Sách", word.meaning)
            assertEquals(CollectionDestination.Existing("one"), target)
            calls++; completed.await()
        }
        vm.onUserChanged("A"); runCurrent()
        vm.selectWord(objectFound); runCurrent()
        assertTrue(vm.uiState.editor!!.lookupFailed)
        vm.changeMeaning(" Sách "); vm.selectCollection("one")
        vm.save(); vm.save(); runCurrent()
        assertEquals(1, calls)
        assertTrue(vm.uiState.editor!!.saving)
        completed.complete(Unit); runCurrent()
        assertNull(vm.uiState.editor)
        assertEquals("Book", vm.uiState.savedLabel)
    }

    @Test fun dictionaryResponseDoesNotOverwriteTypedMeaningUntilUserChoosesIt() = runTest {
        val result = CompletableDeferred<List<DictionaryEntry>>()
        val vm = vm(lookup = { result.await() }) { _, _, _ -> }
        vm.onUserChanged("A"); runCurrent()
        vm.selectWord(objectFound); runCurrent()
        vm.changeMeaning("Sách của tôi")
        result.complete(listOf(entry)); runCurrent()
        assertEquals("Sách của tôi", vm.uiState.editor!!.word.meaning)
        vm.useEntry(entry)
        assertEquals("Quyển sách", vm.uiState.editor!!.word.meaning)
    }

    @Test fun deletedCollectionClearsSelectionAndSaveFailurePreservesDraftForRetry() = runTest {
        var attempts = 0
        val vm = vm { _, _, _ -> if (++attempts == 1) error("Storage error") }
        vm.onUserChanged("A"); runCurrent()
        vm.selectWord(objectFound); vm.changeMeaning("Sách"); vm.selectCollection("one")
        groups.value = emptyList(); runCurrent()
        assertNull(vm.uiState.editor!!.collectionId)
        vm.save(); runCurrent(); assertEquals(0, attempts)
        vm.createNewCollection(); vm.changeCollectionName("Bộ mới")
        vm.save(); runCurrent()
        assertTrue(vm.uiState.editor!!.saveFailed)
        assertEquals("Sách", vm.uiState.editor!!.word.meaning)
        vm.save(); runCurrent()
        assertEquals(2, attempts)
        assertNull(vm.uiState.editor)
    }

    @Test fun accountChangeClearsDraftAndOldLookupCannotPopulateNewAccount() = runTest {
        val result = CompletableDeferred<List<DictionaryEntry>>()
        val vm = vm(lookup = { result.await() }) { _, _, _ -> fail("Unexpected save") }
        vm.onUserChanged("A"); runCurrent()
        vm.selectWord(objectFound); runCurrent()
        vm.onUserChanged("B")
        result.complete(listOf(entry)); runCurrent()
        assertEquals("B", vm.uiState.ownerId)
        assertNull(vm.uiState.editor)
        assertNull(vm.uiState.savedLabel)
    }
}

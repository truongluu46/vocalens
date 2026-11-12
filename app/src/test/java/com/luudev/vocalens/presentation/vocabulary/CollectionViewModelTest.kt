package com.luudev.vocalens.presentation.vocabulary

import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.CollectionRepository
import com.luudev.vocalens.domain.repository.VocabularyRepository
import com.luudev.vocalens.domain.usecase.*
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val book = VocabularyWord("book", "Book", "", "Sách", VocabularyCategory.Objects)
    private val pen = VocabularyWord("pen", "Pen", "", "Bút", VocabularyCategory.Objects)
    private val saved = object : VocabularyRepository {
        val words = MutableStateFlow(listOf(book, pen))
        override fun observeSavedWords(userId: String) = words
        override suspend fun saveWord(userId: String, word: VocabularyWord) = Unit
        override suspend fun deleteWord(userId: String, wordId: String) = Unit
    }
    private inner class FakeCollections : CollectionRepository {
        val groups = mutableMapOf<String, MutableStateFlow<List<VocabularyCollection>>>()
        val members = mutableMapOf<Pair<String, String>, MutableStateFlow<List<VocabularyWord>>>()
        var failWrite = false
        var writeDelay = 0L
        override fun observeCollections(userId: String) = groups.getOrPut(userId) { MutableStateFlow(emptyList()) }
        override fun observeWords(userId: String, collectionId: String) =
            members.getOrPut(userId to collectionId) { MutableStateFlow(emptyList()) }
        private suspend fun beforeWrite() { delay(writeDelay); check(!failWrite) }
        override suspend fun create(userId: String, name: String, description: String) {
            beforeWrite()
            observeCollections(userId).value += VocabularyCollection("group", name, description)
        }
        override suspend fun update(userId: String, collectionId: String, name: String, description: String) {
            beforeWrite()
            observeCollections(userId).value = observeCollections(userId).value.map {
                if (it.id == collectionId) it.copy(name = name, description = description) else it
            }
        }
        override suspend fun delete(userId: String, collectionId: String) {
            beforeWrite()
            observeCollections(userId).value = observeCollections(userId).value.filterNot { it.id == collectionId }
            observeWords(userId, collectionId).value = emptyList()
        }
        override suspend fun setWords(userId: String, collectionId: String, wordIds: Set<String>) {
            beforeWrite()
            observeWords(userId, collectionId).value = saved.words.value.filter { it.id in wordIds }
            observeCollections(userId).value = observeCollections(userId).value.map {
                if (it.id == collectionId) it.copy(wordCount = wordIds.size) else it
            }
        }
        override suspend fun removeWord(userId: String, collectionId: String, wordId: String) {
            setWords(userId, collectionId, observeWords(userId, collectionId).value.map { it.id }.toSet() - wordId)
        }
    }
    private fun vm(repo: FakeCollections) = CollectionViewModel(ObserveCollections(repo), ObserveCollectionWords(repo),
        SaveCollection(repo), DeleteCollection(repo), SetCollectionWords(repo), RemoveCollectionWord(repo),
        GetSavedVocabulary(saved)).apply { onUserChanged("A") }
    private fun create(vm: CollectionViewModel, name: String = "  Du lịch  ") {
        vm.onEvent(CollectionEvent.Create)
        vm.onEvent(CollectionEvent.Name(name))
        vm.onEvent(CollectionEvent.Description("  Các từ cần học  "))
        vm.onEvent(CollectionEvent.Save)
    }

    @Test fun createEditDeleteAndCancelWorkWithoutDeletingSavedWords() = runTest {
        val repo = FakeCollections(); val vm = vm(repo); runCurrent()
        create(vm); runCurrent()
        assertEquals("Du lịch", vm.uiState.collections.single().name)
        assertEquals("Các từ cần học", vm.uiState.collections.single().description)
        assertNull(vm.uiState.editor)
        vm.onEvent(CollectionEvent.Edit(vm.uiState.collections.single()))
        vm.onEvent(CollectionEvent.Name("TOEIC")); vm.onEvent(CollectionEvent.Save); runCurrent()
        assertEquals("TOEIC", vm.uiState.collections.single().name)
        vm.onEvent(CollectionEvent.RequestDelete(vm.uiState.collections.single()))
        vm.onEvent(CollectionEvent.DismissDelete); runCurrent()
        assertEquals(1, vm.uiState.collections.size)
        vm.onEvent(CollectionEvent.Open("group")); runCurrent()
        vm.onEvent(CollectionEvent.RequestDelete(vm.uiState.collections.single()))
        vm.onEvent(CollectionEvent.ConfirmDelete); runCurrent()
        assertTrue(vm.uiState.collections.isEmpty())
        assertNull(vm.uiState.selectedId)
        assertEquals(2, saved.words.value.size)
        vm.onUserChanged(null)
    }

    @Test fun selectingAndRemovingMembersDoesNotDeleteWordsAndCancelDiscardsDraft() = runTest {
        val vm = vm(FakeCollections()); runCurrent(); create(vm); runCurrent()
        vm.onEvent(CollectionEvent.Open("group")); runCurrent()
        vm.onEvent(CollectionEvent.PickWords)
        vm.onEvent(CollectionEvent.ToggleWord("book")); vm.onEvent(CollectionEvent.ToggleWord("pen"))
        vm.onEvent(CollectionEvent.SaveWords); runCurrent()
        assertEquals(2, vm.uiState.collections.single().wordCount)
        assertEquals(2, vm.uiState.words.size)
        vm.onEvent(CollectionEvent.PickWords); vm.onEvent(CollectionEvent.ToggleWord("book"))
        vm.onEvent(CollectionEvent.DismissPicker)
        assertEquals(2, vm.uiState.words.size)
        vm.onEvent(CollectionEvent.RemoveWord("book")); runCurrent()
        assertEquals(listOf(pen), vm.uiState.words)
        vm.onEvent(CollectionEvent.PickWords); vm.onEvent(CollectionEvent.ToggleWord("pen"))
        vm.onEvent(CollectionEvent.SaveWords); runCurrent()
        assertTrue(vm.uiState.words.isEmpty())
        assertEquals(2, saved.words.value.size)
        vm.onUserChanged(null)
    }

    @Test fun invalidNameAndFailedWriteKeepEditorForRetry() = runTest {
        val repo = FakeCollections(); val vm = vm(repo); runCurrent()
        create(vm, "   "); runCurrent()
        assertTrue(vm.uiState.editor!!.invalid)
        assertTrue(vm.uiState.collections.isEmpty())
        vm.onEvent(CollectionEvent.Name("TOEIC")); repo.failWrite = true
        vm.onEvent(CollectionEvent.Save); runCurrent()
        assertTrue(vm.uiState.error); assertFalse(vm.uiState.busy)
        assertEquals("TOEIC", vm.uiState.editor!!.name)
        repo.failWrite = false; vm.onEvent(CollectionEvent.DismissError)
        vm.onEvent(CollectionEvent.Save); runCurrent()
        assertEquals(1, vm.uiState.collections.size)
        assertFalse(vm.uiState.error)
        vm.onUserChanged(null)
    }

    @Test fun accountSwitchClearsSelectionAndCancelsPendingWrites() = runTest {
        val repo = FakeCollections(); val vm = vm(repo); runCurrent(); create(vm); runCurrent()
        vm.onEvent(CollectionEvent.Open("group")); runCurrent()
        vm.onEvent(CollectionEvent.Edit(vm.uiState.collections.single()))
        vm.onEvent(CollectionEvent.Name("Should not be saved")); repo.writeDelay = 1000
        vm.onEvent(CollectionEvent.Save); runCurrent()
        assertTrue(vm.uiState.busy)
        vm.onUserChanged("B"); runCurrent()
        assertTrue(vm.uiState.collections.isEmpty()); assertNull(vm.uiState.selectedId)
        assertNull(vm.uiState.editor); assertFalse(vm.uiState.busy)
        vm.onUserChanged("A"); runCurrent()
        assertEquals("Du lịch", vm.uiState.collections.single().name)
        vm.onUserChanged(null)
    }
}

package com.luudev.vocalens.presentation.vocabulary

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.VocabularyRepository
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import com.luudev.vocalens.domain.usecase.LookupVocabulary
import com.luudev.vocalens.domain.usecase.SaveVocabularyWord
import com.luudev.vocalens.domain.repository.DictionaryRepository
import com.luudev.vocalens.domain.model.DictionaryEntry
import com.luudev.vocalens.domain.model.DictionaryError
import com.luudev.vocalens.domain.model.DictionaryException
import kotlinx.coroutines.delay
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomVocabularyViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val headphones = VocabularyWord("headphones", "Headphones", "", "Tai nghe", VocabularyCategory.Technology)
    private val notebook = VocabularyWord("notebook", "Notebook", "", "Sổ ghi chép", VocabularyCategory.Objects)

    private class FakeRepository : VocabularyRepository {
        val users = mutableMapOf<String, MutableStateFlow<List<VocabularyWord>>>()
        var failDeletion = false
        fun words(uid: String) = users.getOrPut(uid) { MutableStateFlow(emptyList()) }
        override fun observeSavedWords(userId: String): Flow<List<VocabularyWord>> = words(userId)
        override suspend fun saveWord(userId: String, word: VocabularyWord) {
            words(userId).value = words(userId).value.filterNot { it.id == word.id } + word
        }
        override suspend fun deleteWord(userId: String, wordId: String) {
            if (failDeletion) error("Disk unavailable")
            words(userId).value = words(userId).value.filterNot { it.id == wordId }
        }
    }

    private fun repository() = FakeRepository().apply { words("A").value = listOf(headphones, notebook) }
    private fun viewModel(repo: FakeRepository, handle: SavedStateHandle = SavedStateHandle()) =
        VocabularyViewModel(handle, GetSavedVocabulary(repo), DeleteVocabularyWord(repo),
            LookupVocabulary(object : DictionaryRepository {
                override suspend fun lookup(query: String) = emptyList<DictionaryEntry>()
            }), SaveVocabularyWord(repo)).apply { onUserChanged("A") }

    @Test fun lookupDoesNotFilterNotebookAndCanSaveTranslation() = runTest {
        val repo = repository()
        val word = VocabularyWord("hello", "hello", "/hello/", "xin chào", VocabularyCategory.Objects,
            "interjection", "Hello, everyone.")
        var received = ""
        val vm = VocabularyViewModel(SavedStateHandle(), GetSavedVocabulary(repo), DeleteVocabularyWord(repo),
            LookupVocabulary(object : DictionaryRepository {
                override suspend fun lookup(query: String): List<DictionaryEntry> {
                    received = query
                    return listOf(DictionaryEntry(word, "A greeting", "", "", ""))
                }
            }), SaveVocabularyWord(repo))
        vm.onUserChanged("A")
        runCurrent()
        vm.onQueryChange(" hello ")
        vm.onLookup()
        runCurrent()
        assertEquals("hello", received)
        assertEquals(2, vm.uiState.visibleWords.size)
        assertEquals(word, vm.uiState.lookupResults.single().word)
        vm.onSaveLookupWord(word)
        runCurrent()
        assertTrue(repo.words("A").value.contains(word))
        assertEquals("interjection", repo.words("A").value.last().partOfSpeech)
    }

    @Test fun queryChangeCancelsOldLookupAndErrorsAllowRetry() = runTest {
        val repo = repository()
        val vm = VocabularyViewModel(SavedStateHandle(), GetSavedVocabulary(repo), DeleteVocabularyWord(repo),
            LookupVocabulary(object : DictionaryRepository {
                override suspend fun lookup(query: String): List<DictionaryEntry> {
                    if (query == "old") delay(1000)
                    throw DictionaryException(DictionaryError.NotFound)
                }
            }), SaveVocabularyWord(repo))
        vm.onUserChanged("A")
        vm.onQueryChange("old")
        vm.onLookup()
        runCurrent()
        assertTrue(vm.uiState.lookupLoading)
        vm.onQueryChange("new")
        assertFalse(vm.uiState.lookupLoading)
        assertTrue(vm.uiState.lookupResults.isEmpty())
        vm.onLookup()
        runCurrent()
        assertEquals(DictionaryError.NotFound, vm.uiState.lookupError)
        assertFalse(vm.uiState.lookupLoading)
    }

    @Test fun cancelingDeletionKeepsWords() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteDismissed()
        assertEquals(2, repo.words("A").value.size)
        assertNull(vm.uiState.pendingDeletion)
    }

    @Test fun deletionRefreshesFilteredListAndSurvivesViewModelRecreation() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        runCurrent()
        vm.onCategorySelected(VocabularyCategory.Technology)
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.visibleWords.isEmpty())
        vm.onCategorySelected(null)
        assertEquals(listOf(notebook), vm.uiState.visibleWords)
        val restored = viewModel(repo)
        runCurrent()
        assertEquals(listOf(notebook), restored.uiState.savedWords)
    }

    @Test fun restoresFiltersForSameOwner() = runTest {
        val repo = repository()
        val handle = SavedStateHandle()
        val vm = viewModel(repo, handle)
        runCurrent()
        vm.onQueryChange("so ghi")
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onSortSelected(VocabularySort.Alphabetical)
        val restored = viewModel(repo, handle)
        runCurrent()
        assertEquals("so ghi", restored.uiState.query)
        assertEquals(VocabularyCategory.Objects, restored.uiState.category)
        assertEquals(VocabularySort.Alphabetical, restored.uiState.sort)
        assertEquals(listOf(notebook), restored.uiState.visibleWords)
    }

    @Test fun reviewUsesFilteredWordsAndFinishesAfterLastCard() = runTest {
        val vm = viewModel(repository())
        runCurrent()
        vm.onCategorySelected(VocabularyCategory.Objects)
        vm.onStartReview()
        assertEquals(listOf(notebook), vm.uiState.reviewWords)
        vm.onRevealMeaning()
        assertTrue(vm.uiState.meaningRevealed)
        vm.onNextReviewWord()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test fun advancingReviewHidesMeaningAndCancelClosesSession() = runTest {
        val vm = viewModel(repository())
        runCurrent()
        vm.onStartReview()
        vm.onRevealMeaning()
        vm.onNextReviewWord()
        assertEquals(1, vm.uiState.reviewIndex)
        assertFalse(vm.uiState.meaningRevealed)
        vm.onDismissReview()
        assertTrue(vm.uiState.reviewWords.isEmpty())
    }

    @Test fun deletingLastWordLeavesEmptyBank() = runTest {
        val repo = repository().apply { words("A").value = listOf(headphones) }
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.savedWords.isEmpty())
    }

    @Test fun switchingAccountsImmediatelyClearsOldWordsReviewAndFilters() = runTest {
        val repo = repository().apply { words("B").value = listOf(notebook) }
        val vm = viewModel(repo)
        runCurrent()
        vm.onQueryChange("head")
        vm.onStartReview()
        vm.onUserChanged("B")
        assertTrue(vm.uiState.savedWords.isEmpty())
        assertTrue(vm.uiState.reviewWords.isEmpty())
        assertEquals("", vm.uiState.query)
        runCurrent()
        assertEquals(listOf(notebook), vm.uiState.savedWords)
        repo.words("A").value = emptyList()
        runCurrent()
        assertEquals(listOf(notebook), vm.uiState.savedWords)
        vm.onUserChanged(null)
        assertTrue(vm.uiState.savedWords.isEmpty())
        repo.words("B").value = listOf(headphones)
        runCurrent()
        assertTrue(vm.uiState.savedWords.isEmpty())
    }

    @Test fun failedDeletionKeepsWordAndShowsStorageError() = runTest {
        val repo = repository().apply { failDeletion = true }
        val vm = viewModel(repo)
        runCurrent()
        vm.onDeleteRequested(headphones)
        vm.onDeleteConfirmed()
        runCurrent()
        assertTrue(vm.uiState.storageError)
        assertEquals(2, vm.uiState.savedWords.size)
    }
}

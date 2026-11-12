package com.luudev.vocalens.presentation.vocabulary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.domain.usecase.FilterVocabulary
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import com.luudev.vocalens.domain.usecase.LookupVocabulary
import com.luudev.vocalens.domain.usecase.SaveVocabularyWord
import com.luudev.vocalens.domain.model.DictionaryEntry
import com.luudev.vocalens.domain.model.DictionaryError
import com.luudev.vocalens.domain.model.DictionaryException

data class VocabularyUiState(
    val savedWords: List<VocabularyWord> = emptyList(),
    val visibleWords: List<VocabularyWord> = emptyList(),
    val query: String = "",
    val category: VocabularyCategory? = null,
    val sort: VocabularySort = VocabularySort.RecentlySaved,
    val pendingDeletion: VocabularyWord? = null,
    val reviewWords: List<VocabularyWord> = emptyList(),
    val reviewIndex: Int = 0,
    val meaningRevealed: Boolean = false,
    val storageError: Boolean = false,
    val lookupResults: List<DictionaryEntry> = emptyList(),
    val lookupLoading: Boolean = false,
    val lookupError: DictionaryError? = null,
    val savingWordId: String? = null
)

class VocabularyViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getSavedVocabulary: GetSavedVocabulary,
    private val deleteVocabularyWord: DeleteVocabularyWord,
    private val lookupVocabulary: LookupVocabulary,
    private val saveVocabularyWord: SaveVocabularyWord,
    private val filterVocabulary: FilterVocabulary = FilterVocabulary()
) : ViewModel() {
    var uiState by mutableStateOf(VocabularyUiState(
        query = savedStateHandle[QUERY] ?: "",
        category = savedStateHandle.get<String>(CATEGORY)?.let { name -> VocabularyCategory.entries.find { it.name == name } },
        sort = savedStateHandle.get<String>(SORT)?.let { name -> VocabularySort.entries.find { it.name == name } }
            ?: VocabularySort.RecentlySaved
    ))
        private set

    private var userId: String? = null
    private var wordsJob: Job? = null
    private var deleteJob: Job? = null
    private var lookupJob: Job? = null
    private var saveJob: Job? = null

    fun onUserChanged(newUserId: String?) {
        val next = newUserId?.takeIf { it.isNotBlank() }
        if (next == userId) return
        wordsJob?.cancel()
        deleteJob?.cancel()
        lookupJob?.cancel()
        saveJob?.cancel()
        userId = next
        val sameOwner = savedStateHandle.get<String>(OWNER) == next
        if (!sameOwner) {
            savedStateHandle[QUERY] = ""
            savedStateHandle[CATEGORY] = null
            savedStateHandle[SORT] = VocabularySort.RecentlySaved.name
        }
        savedStateHandle[OWNER] = next
        uiState = if (sameOwner) uiState.copy(savedWords = emptyList(), visibleWords = emptyList(),
            pendingDeletion = null, reviewWords = emptyList(), reviewIndex = 0,
            meaningRevealed = false, storageError = false, lookupResults = emptyList(),
            lookupLoading = false, lookupError = null, savingWordId = null) else VocabularyUiState()
        if (next == null) return
        wordsJob = viewModelScope.launch {
            try {
                getSavedVocabulary(next).collect { words ->
                    if (userId == next) {
                        uiState = uiState.copy(savedWords = words, storageError = false)
                        updateVisibleWords()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (userId == next) uiState = uiState.copy(storageError = true)
            }
        }
    }

    fun onStorageErrorDismissed() { uiState = uiState.copy(storageError = false) }

    fun onQueryChange(query: String) {
        lookupJob?.cancel()
        savedStateHandle[QUERY] = query
        uiState = uiState.copy(query = query, lookupResults = emptyList(), lookupLoading = false, lookupError = null)
    }

    fun onLookup() {
        val query = uiState.query.trim()
        if (query.isBlank()) return
        lookupJob?.cancel()
        uiState = uiState.copy(lookupLoading = true, lookupResults = emptyList(), lookupError = null)
        lookupJob = viewModelScope.launch {
            try {
                val results = lookupVocabulary(query)
                uiState = uiState.copy(lookupResults = results, lookupLoading = false)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                uiState = uiState.copy(lookupLoading = false,
                    lookupError = (error as? DictionaryException)?.reason ?: DictionaryError.Network)
            }
        }
    }

    fun onSaveLookupWord(word: VocabularyWord) {
        val owner = userId ?: return
        if (word.meaning.isBlank() || saveJob?.isActive == true ||
            uiState.lookupResults.none { it.word == word } || uiState.savedWords.any { it.id == word.id }) return
        uiState = uiState.copy(savingWordId = word.id)
        saveJob = viewModelScope.launch {
            try { saveVocabularyWord(owner, word) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (userId == owner) uiState = uiState.copy(storageError = true)
            } finally {
                if (userId == owner) uiState = uiState.copy(savingWordId = null)
            }
        }
    }

    fun onCategorySelected(category: VocabularyCategory?) {
        savedStateHandle[CATEGORY] = category?.name
        uiState = uiState.copy(category = category)
        updateVisibleWords()
    }

    fun onSortSelected(sort: VocabularySort) {
        savedStateHandle[SORT] = sort.name
        uiState = uiState.copy(sort = sort)
        updateVisibleWords()
    }

    fun onDeleteRequested(word: VocabularyWord) { uiState = uiState.copy(pendingDeletion = word) }
    fun onDeleteDismissed() { uiState = uiState.copy(pendingDeletion = null) }

    fun onDeleteConfirmed() {
        val word = uiState.pendingDeletion ?: return
        val owner = userId ?: return
        if (deleteJob?.isActive == true) return
        uiState = uiState.copy(pendingDeletion = null)
        deleteJob = viewModelScope.launch {
            try {
                deleteVocabularyWord(owner, word.id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (userId == owner) uiState = uiState.copy(storageError = true)
            }
        }
    }

    fun onStartReview() {
        uiState = uiState.copy(reviewWords = uiState.visibleWords.toList(), reviewIndex = 0, meaningRevealed = false)
    }

    fun onRevealMeaning() { uiState = uiState.copy(meaningRevealed = true) }

    fun onNextReviewWord() {
        if (uiState.reviewIndex + 1 >= uiState.reviewWords.size) onDismissReview()
        else uiState = uiState.copy(reviewIndex = uiState.reviewIndex + 1, meaningRevealed = false)
    }

    fun onDismissReview() { uiState = uiState.copy(reviewWords = emptyList(), reviewIndex = 0, meaningRevealed = false) }

    private fun updateVisibleWords() {
        uiState = uiState.copy(visibleWords = filterVocabulary(uiState.savedWords, "", uiState.category, uiState.sort))
    }

    private companion object {
        const val OWNER = "vocabulary_owner"
        const val QUERY = "vocabulary_query"
        const val CATEGORY = "vocabulary_category"
        const val SORT = "vocabulary_sort"
    }
}

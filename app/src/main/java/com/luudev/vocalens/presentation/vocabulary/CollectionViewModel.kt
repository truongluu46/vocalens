package com.luudev.vocalens.presentation.vocabulary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.VocabularyCollection
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CollectionEditor(val id: String? = null, val name: String = "",
    val description: String = "", val invalid: Boolean = false)

data class CollectionUiState(
    val ownerId: String? = null,
    val collections: List<VocabularyCollection> = emptyList(),
    val availableWords: List<VocabularyWord> = emptyList(),
    val selectedId: String? = null,
    val words: List<VocabularyWord> = emptyList(),
    val loading: Boolean = false,
    val wordsLoading: Boolean = false,
    val editor: CollectionEditor? = null,
    val pendingDelete: VocabularyCollection? = null,
    val pickedWordIds: Set<String>? = null,
    val busy: Boolean = false,
    val error: Boolean = false
) {
    val selected get() = collections.find { it.id == selectedId }
}

sealed interface CollectionEvent {
    data object Create : CollectionEvent
    data class Edit(val collection: VocabularyCollection) : CollectionEvent
    data class Name(val value: String) : CollectionEvent
    data class Description(val value: String) : CollectionEvent
    data object Save : CollectionEvent
    data object DismissEditor : CollectionEvent
    data class RequestDelete(val collection: VocabularyCollection) : CollectionEvent
    data object ConfirmDelete : CollectionEvent
    data object DismissDelete : CollectionEvent
    data class Open(val id: String) : CollectionEvent
    data object Back : CollectionEvent
    data object PickWords : CollectionEvent
    data class ToggleWord(val id: String) : CollectionEvent
    data object SaveWords : CollectionEvent
    data object DismissPicker : CollectionEvent
    data class RemoveWord(val id: String) : CollectionEvent
    data object DismissError : CollectionEvent
    data object Retry : CollectionEvent
}

class CollectionViewModel(
    private val observeCollections: ObserveCollections,
    private val observeWords: ObserveCollectionWords,
    private val saveCollection: SaveCollection,
    private val deleteCollection: DeleteCollection,
    private val setWords: SetCollectionWords,
    private val removeWord: RemoveCollectionWord,
    private val getSavedVocabulary: GetSavedVocabulary
) : ViewModel() {
    var uiState by mutableStateOf(CollectionUiState())
        private set
    private var observeJob: Job? = null
    private var wordsJob: Job? = null
    private var operationJob: Job? = null

    fun onUserChanged(uid: String?) {
        val owner = uid?.takeIf { it.isNotBlank() }
        if (owner == uiState.ownerId) return
        observeJob?.cancel(); wordsJob?.cancel(); operationJob?.cancel()
        uiState = CollectionUiState(ownerId = owner, loading = owner != null)
        if (owner != null) startObserving(owner)
    }

    private fun startObserving(owner: String) {
        observeJob?.cancel()
        uiState = uiState.copy(loading = true, error = false)
        observeJob = viewModelScope.launch {
            try {
                combine(observeCollections(owner), getSavedVocabulary(owner)) { groups, words -> groups to words }
                    .collect { (groups, words) ->
                        if (uiState.ownerId != owner) return@collect
                        uiState = uiState.copy(collections = groups, availableWords = words, loading = false,
                            pickedWordIds = uiState.pickedWordIds?.intersect(words.map { it.id }.toSet()))
                        if (uiState.selectedId != null && groups.none { it.id == uiState.selectedId }) select(null)
                    }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (uiState.ownerId == owner) uiState = uiState.copy(error = true, loading = false)
            }
        }
    }

    private fun select(id: String?) {
        wordsJob?.cancel()
        uiState = uiState.copy(selectedId = id, words = emptyList(), wordsLoading = id != null,
            pickedWordIds = null)
        val owner = uiState.ownerId ?: return
        if (id == null) return
        wordsJob = viewModelScope.launch {
            try {
                observeWords(owner, id).collect { words ->
                    if (uiState.ownerId == owner && uiState.selectedId == id)
                        uiState = uiState.copy(words = words, wordsLoading = false)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (uiState.ownerId == owner && uiState.selectedId == id)
                    uiState = uiState.copy(error = true, wordsLoading = false)
            }
        }
    }

    private fun mutate(action: suspend (String) -> Unit, onSuccess: () -> Unit = {}) {
        val owner = uiState.ownerId ?: return
        if (uiState.busy) return
        uiState = uiState.copy(busy = true, error = false)
        operationJob = viewModelScope.launch {
            try {
                action(owner)
                if (uiState.ownerId == owner) {
                    onSuccess()
                    if (observeJob?.isActive != true) startObserving(owner)
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (uiState.ownerId == owner) uiState = uiState.copy(error = true)
            } finally {
                if (uiState.ownerId == owner) uiState = uiState.copy(busy = false)
            }
        }
    }

    fun onEvent(event: CollectionEvent) {
        if (event == CollectionEvent.DismissError) { uiState = uiState.copy(error = false); return }
        if (uiState.busy || uiState.ownerId == null) return
        when (event) {
            CollectionEvent.Create -> uiState = uiState.copy(editor = CollectionEditor())
            is CollectionEvent.Edit -> uiState.collections.find { it.id == event.collection.id }?.let {
                uiState = uiState.copy(editor = CollectionEditor(it.id, it.name, it.description))
            }
            is CollectionEvent.Name -> uiState = uiState.copy(editor = uiState.editor?.copy(name = event.value, invalid = false))
            is CollectionEvent.Description -> uiState = uiState.copy(editor = uiState.editor?.copy(description = event.value, invalid = false))
            CollectionEvent.Save -> {
                val draft = uiState.editor ?: return
                if (draft.name.trim().length !in 1..120 || draft.description.trim().length > 1000) {
                    uiState = uiState.copy(editor = draft.copy(invalid = true)); return
                }
                mutate({ saveCollection(it, draft.id, draft.name, draft.description) }) {
                    uiState = uiState.copy(editor = null)
                }
            }
            CollectionEvent.DismissEditor -> uiState = uiState.copy(editor = null)
            is CollectionEvent.RequestDelete -> uiState = uiState.copy(pendingDelete =
                uiState.collections.find { it.id == event.collection.id })
            CollectionEvent.DismissDelete -> uiState = uiState.copy(pendingDelete = null)
            CollectionEvent.ConfirmDelete -> {
                val id = uiState.pendingDelete?.id ?: return
                mutate({ deleteCollection(it, id) }) {
                    uiState = uiState.copy(pendingDelete = null)
                    if (uiState.selectedId == id) select(null)
                }
            }
            is CollectionEvent.Open -> if (uiState.collections.any { it.id == event.id }) select(event.id)
            CollectionEvent.Back -> select(null)
            CollectionEvent.PickWords -> if (!uiState.wordsLoading && uiState.selected != null)
                uiState = uiState.copy(pickedWordIds = uiState.words.map { it.id }.toSet())
            is CollectionEvent.ToggleWord -> {
                val picked = uiState.pickedWordIds ?: return
                if (uiState.availableWords.none { it.id == event.id }) return
                uiState = uiState.copy(pickedWordIds = if (event.id in picked) picked - event.id else picked + event.id)
            }
            CollectionEvent.SaveWords -> {
                val id = uiState.selectedId ?: return
                val picked = uiState.pickedWordIds ?: return
                mutate({ setWords(it, id, picked) }) { uiState = uiState.copy(pickedWordIds = null) }
            }
            CollectionEvent.DismissPicker -> uiState = uiState.copy(pickedWordIds = null)
            is CollectionEvent.RemoveWord -> {
                val id = uiState.selectedId ?: return
                if (uiState.words.any { it.id == event.id }) mutate({ removeWord(it, id, event.id) })
            }
            CollectionEvent.Retry -> {
                uiState.ownerId?.let { startObserving(it) }
                uiState.selectedId?.let { select(it) }
            }
            CollectionEvent.DismissError -> Unit
        }
    }
}

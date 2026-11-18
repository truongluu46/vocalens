package com.luudev.vocalens.presentation.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.LookupVocabulary
import com.luudev.vocalens.domain.usecase.ObserveCollections
import com.luudev.vocalens.domain.usecase.SaveWordToCollection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class CameraWordEditor(
    val word: VocabularyWord,
    val collectionId: String? = null,
    val createNew: Boolean = false,
    val newCollectionName: String = "",
    val entries: List<DictionaryEntry> = emptyList(),
    val lookupLoading: Boolean = false,
    val lookupFailed: Boolean = false,
    val selectedEntry: DictionaryEntry? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false
) {
    val valid get() = word.meaning.trim().length in 1..2000 &&
        if (createNew) newCollectionName.trim().length in 1..120 else collectionId != null
}

data class CameraWordUiState(
    val ownerId: String? = null,
    val collections: List<VocabularyCollection> = emptyList(),
    val collectionsLoading: Boolean = false,
    val collectionsFailed: Boolean = false,
    val editor: CameraWordEditor? = null,
    val savedLabel: String? = null
)

class CameraWordViewModel(
    private val observeCollections: ObserveCollections,
    private val lookupVocabulary: LookupVocabulary,
    private val saveWord: SaveWordToCollection
) : ViewModel() {
    var uiState by mutableStateOf(CameraWordUiState())
        private set
    private var collectionsJob: Job? = null
    private var lookupJob: Job? = null
    private var saveJob: Job? = null
    private var generation = 0L

    fun onUserChanged(userId: String?) {
        val owner = userId?.takeIf { it.isNotBlank() }
        if (owner == uiState.ownerId) return
        generation++
        collectionsJob?.cancel(); lookupJob?.cancel(); saveJob?.cancel()
        uiState = CameraWordUiState(ownerId = owner)
        if (owner != null) reloadCollections()
    }

    fun reloadCollections() {
        val owner = uiState.ownerId ?: return
        collectionsJob?.cancel()
        uiState = uiState.copy(collectionsLoading = true, collectionsFailed = false)
        collectionsJob = viewModelScope.launch {
            try {
                observeCollections(owner).collect { groups ->
                    if (uiState.ownerId == owner) {
                        val editor = uiState.editor?.let { draft ->
                            if (draft.collectionId != null && groups.none { it.id == draft.collectionId })
                                draft.copy(collectionId = null) else draft
                        }
                        uiState = uiState.copy(collections = groups, collectionsLoading = false,
                            collectionsFailed = false, editor = editor)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (uiState.ownerId == owner) uiState = uiState.copy(collectionsLoading = false, collectionsFailed = true)
            }
        }
    }

    fun selectWord(objectFound: RecognizedObject) {
        if (uiState.ownerId == null || uiState.editor?.saving == true) return
        generation++
        lookupJob?.cancel()
        uiState = uiState.copy(savedLabel = null, editor = CameraWordEditor(
            VocabularyWord("", objectFound.label, "", "", VocabularyCategory.Objects),
            createNew = uiState.collections.isEmpty() && !uiState.collectionsLoading))
        lookup()
    }

    private fun update(change: (CameraWordEditor) -> CameraWordEditor) {
        uiState.editor?.takeUnless { it.saving }?.let { uiState = uiState.copy(editor = change(it)) }
    }
    fun changeMeaning(value: String) = update { it.copy(word = it.word.copy(meaning = value.take(2000)), saveFailed = false) }
    fun selectCollection(id: String) {
        if (uiState.collections.any { it.id == id }) update { it.copy(collectionId = id, createNew = false, saveFailed = false) }
    }
    fun createNewCollection() = update { it.copy(collectionId = null, createNew = true, saveFailed = false) }
    fun changeCollectionName(value: String) = update { it.copy(newCollectionName = value.take(120), saveFailed = false) }

    fun lookup() {
        val draft = uiState.editor?.takeUnless { it.saving } ?: return
        lookupJob?.cancel()
        val ticket = ++generation
        uiState = uiState.copy(editor = draft.copy(lookupLoading = true, lookupFailed = false, entries = emptyList()))
        lookupJob = viewModelScope.launch {
            try {
                val entries = lookupVocabulary(draft.word.english)
                if (ticket == generation) uiState = uiState.copy(editor = uiState.editor?.copy(
                    lookupLoading = false, entries = entries))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (ticket == generation) uiState = uiState.copy(editor = uiState.editor?.copy(
                    lookupLoading = false, lookupFailed = true))
            }
        }
    }

    fun useEntry(entry: DictionaryEntry) {
        if (uiState.editor?.entries?.contains(entry) != true || entry.word.meaning.isBlank()) return
        update { it.copy(word = entry.word.copy(id = ""), selectedEntry = entry, saveFailed = false) }
    }

    fun save() {
        val owner = uiState.ownerId ?: return
        val draft = uiState.editor?.takeIf { it.valid && !it.saving } ?: return
        val target = if (draft.createNew) CollectionDestination.New(draft.newCollectionName)
            else CollectionDestination.Existing(draft.collectionId ?: return)
        if (!draft.createNew && uiState.collections.none { it.id == draft.collectionId }) return
        lookupJob?.cancel()
        val ticket = ++generation
        uiState = uiState.copy(editor = draft.copy(saving = true, lookupLoading = false, saveFailed = false))
        saveJob = viewModelScope.launch {
            try {
                saveWord(owner, draft.word, target)
                if (ticket == generation && uiState.ownerId == owner) {
                    uiState = uiState.copy(editor = null, savedLabel = draft.word.english)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (ticket == generation && uiState.ownerId == owner) uiState = uiState.copy(
                    editor = uiState.editor?.copy(saving = false, saveFailed = true))
            }
        }
    }

    fun dismiss() {
        if (uiState.editor?.saving == true) return
        generation++
        lookupJob?.cancel()
        uiState = uiState.copy(editor = null)
    }

    fun dismissSavedMessage() { uiState = uiState.copy(savedLabel = null) }
}

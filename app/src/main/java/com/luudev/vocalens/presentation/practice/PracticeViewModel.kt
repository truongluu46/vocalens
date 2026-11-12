package com.luudev.vocalens.presentation.practice

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.FilterPracticeLessons
import com.luudev.vocalens.domain.usecase.GetPracticeLessons

data class PracticeUiState(
    val mediaType: PracticeMediaType = PracticeMediaType.Video,
    val query: String = "",
    val filter: PracticeFilter = PracticeFilter.All,
    val duration: PracticeDuration = PracticeDuration.All,
    val sort: PracticeSort = PracticeSort.Newest,
    val featuredLesson: PracticeLesson? = null,
    val lessons: List<PracticeLesson> = emptyList(),
    val selectedLesson: PracticeLesson? = null,
    val showDownloads: Boolean = false,
    val storageError: Boolean = false
)

class PracticeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getPracticeLessons: GetPracticeLessons,
    private val filterPracticeLessons: FilterPracticeLessons = FilterPracticeLessons()
) : ViewModel() {
    private var allLessons = emptyList<PracticeLesson>()
    private var owner: String? = null
    private var observation: Job? = null
    var uiState by mutableStateOf(PracticeUiState(
        mediaType = restoredEnum("practice_media", PracticeMediaType.Video),
        query = savedStateHandle["practice_query"] ?: "",
        filter = restoredEnum("practice_filter", PracticeFilter.All),
        duration = restoredEnum("practice_duration", PracticeDuration.All),
        sort = restoredEnum("practice_sort", PracticeSort.Newest)
    ))
        private set

    fun onUserChanged(uid: String?) {
        if (uid == owner) return
        observation?.cancel()
        owner = uid
        allLessons = emptyList()
        val sameOwner = savedStateHandle.get<String>("practice_owner") == uid
        if (!sameOwner) {
            listOf("practice_media", "practice_query", "practice_filter", "practice_duration", "practice_sort").forEach { savedStateHandle.remove<Any>(it) }
        }
        savedStateHandle["practice_owner"] = uid
        uiState = if (sameOwner) uiState.copy(featuredLesson = null, lessons = emptyList(), selectedLesson = null,
            showDownloads = false, storageError = false) else PracticeUiState()
        if (uid == null) return
        observation = viewModelScope.launch {
            try {
                getPracticeLessons(uid).collect {
                    if (owner == uid) { allLessons = it; uiState = uiState.copy(storageError = false); refreshLessons() }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (owner == uid) uiState = uiState.copy(storageError = true) }
        }
    }

    fun onMediaTypeSelected(mediaType: PracticeMediaType) {
        savedStateHandle["practice_media"] = mediaType.name
        uiState = uiState.copy(mediaType = mediaType)
        refreshLessons()
    }

    fun onQueryChange(query: String) {
        savedStateHandle["practice_query"] = query
        uiState = uiState.copy(query = query)
        refreshLessons()
    }

    fun onFilterSelected(filter: PracticeFilter) {
        savedStateHandle["practice_filter"] = filter.name
        uiState = uiState.copy(filter = filter)
        refreshLessons()
    }

    fun onDurationSelected(duration: PracticeDuration) {
        savedStateHandle["practice_duration"] = duration.name
        uiState = uiState.copy(duration = duration)
        refreshLessons()
    }

    fun onToggleSort() {
        val sort = if (uiState.sort == PracticeSort.Newest) PracticeSort.Popular else PracticeSort.Newest
        savedStateHandle["practice_sort"] = sort.name
        uiState = uiState.copy(sort = sort)
        refreshLessons()
    }

    fun onLessonClick(lesson: PracticeLesson) { uiState = uiState.copy(selectedLesson = lesson) }
    fun onDismissLesson() { uiState = uiState.copy(selectedLesson = null) }
    fun onDownloadsClick() { uiState = uiState.copy(showDownloads = true) }
    fun onDismissDownloads() { uiState = uiState.copy(showDownloads = false) }

    private fun refreshLessons() {
        val filtered = filterPracticeLessons(allLessons, uiState.mediaType, uiState.query,
            uiState.filter, uiState.duration, uiState.sort)
        uiState = uiState.copy(featuredLesson = filtered.firstOrNull { it.isFeatured },
            lessons = filtered.filterNot { it.isFeatured })
    }

    private inline fun <reified T : Enum<T>> restoredEnum(key: String, default: T): T =
        enumValues<T>().find { it.name == savedStateHandle.get<String>(key) } ?: default
}

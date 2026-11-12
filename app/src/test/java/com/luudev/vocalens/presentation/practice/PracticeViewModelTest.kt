package com.luudev.vocalens.presentation.practice

import androidx.lifecycle.SavedStateHandle
import com.luudev.vocalens.domain.repository.PracticeRepository
import com.luudev.vocalens.testing.MainDispatcherRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.usecase.GetPracticeLessons
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private class FakeRepository : PracticeRepository {
        override fun observeLessons(userId: String) = flowOf(listOf(
            PracticeLesson("small-talk", "Small Talk", "Daily", PracticeMediaType.Video, LessonDifficulty.Easy, 600, 0f, 98, 4, "", isFeatured = true),
            PracticeLesson("cafe", "Cafe", "Daily", PracticeMediaType.Video, LessonDifficulty.Easy, 500, 0f, 90, 3, ""),
            PracticeLesson("workday", "Work", "Work", PracticeMediaType.Video, LessonDifficulty.Intermediate, 700, 0f, 85, 2, ""),
            PracticeLesson("toeic-conversations", "TOEIC", "TOEIC", PracticeMediaType.Video, LessonDifficulty.Hard, 900, 0f, 99, 1, "", isToeic = true),
            PracticeLesson("daily-podcast", "Daily", "Daily", PracticeMediaType.Podcast, LessonDifficulty.Easy, 480, 0f, 90, 4, "", isFeatured = true),
            PracticeLesson("toeic-podcast", "TOEIC", "TOEIC", PracticeMediaType.Podcast, LessonDifficulty.Hard, 900, 0f, 99, 1, "", isToeic = true)
        ))
    }
    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        PracticeViewModel(handle, GetPracticeLessons(FakeRepository())).apply { onUserChanged("A"); mainDispatcher.dispatcher.scheduler.runCurrent() }

    @Test
    fun featuredLessonIsShownOnceAboveNewestVideos() = runTest {
        val state = viewModel().uiState
        assertEquals("small-talk", state.featuredLesson?.id)
        assertEquals(listOf("cafe", "workday", "toeic-conversations"), state.lessons.map { it.id })
        assertTrue(state.lessons.all { !it.isFeatured && it.mediaType == PracticeMediaType.Video })
    }

    @Test
    fun switchingMediaUpdatesFeaturedAndList() = runTest {
        val vm = viewModel()
        vm.onMediaTypeSelected(PracticeMediaType.Podcast)
        assertEquals("daily-podcast", vm.uiState.featuredLesson?.id)
        assertTrue(vm.uiState.lessons.all { it.mediaType == PracticeMediaType.Podcast })
    }

    @Test
    fun restoresAllSearchControlsAfterRecreation() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.onMediaTypeSelected(PracticeMediaType.Podcast)
        vm.onQueryChange("TOEIC")
        vm.onFilterSelected(PracticeFilter.Toeic)
        vm.onDurationSelected(PracticeDuration.Long)
        vm.onToggleSort()
        val restored = viewModel(handle).uiState
        assertEquals(vm.uiState, restored)
        assertEquals(listOf("toeic-podcast"), restored.lessons.map { it.id })
        assertNull(restored.featuredLesson)
    }

    @Test
    fun emptySearchDoesNotLeaveUnrelatedFeaturedLesson() = runTest {
        val vm = viewModel()
        vm.onQueryChange("does not exist")
        assertNull(vm.uiState.featuredLesson)
        assertTrue(vm.uiState.lessons.isEmpty())
    }

    @Test
    fun detailsAndDownloadsCanBeDismissedWithoutChangingProgress() = runTest {
        val vm = viewModel()
        val original = vm.uiState.lessons
        vm.onLessonClick(original.first())
        assertEquals(original.first(), vm.uiState.selectedLesson)
        vm.onDismissLesson()
        vm.onDownloadsClick()
        assertTrue(vm.uiState.showDownloads)
        vm.onDismissDownloads()
        assertNull(vm.uiState.selectedLesson)
        assertFalse(vm.uiState.showDownloads)
        assertEquals(original, vm.uiState.lessons)
    }
}

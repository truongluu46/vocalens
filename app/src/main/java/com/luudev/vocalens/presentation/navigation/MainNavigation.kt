package com.luudev.vocalens.presentation.navigation

import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.activity.compose.BackHandler
import com.luudev.vocalens.presentation.camera.CameraRoute
import com.luudev.vocalens.presentation.camera.CameraViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.luudev.vocalens.presentation.components.BottomNavigationBar
import com.luudev.vocalens.presentation.home.HomeAction
import com.luudev.vocalens.presentation.home.HomeScreen
import com.luudev.vocalens.presentation.home.HomeUiState
import com.luudev.vocalens.presentation.vocabulary.CollectionViewModel
import com.luudev.vocalens.presentation.vocabulary.VocabularyRoute
import com.luudev.vocalens.presentation.vocabulary.VocabularyViewModel
import com.luudev.vocalens.presentation.practice.PracticeScreen
import com.luudev.vocalens.presentation.practice.PracticeViewModel
import com.luudev.vocalens.presentation.profile.ProfileScreen
import com.luudev.vocalens.presentation.profile.ProfileViewModel

/** Hosts main destinations. Screens and the bottom bar only emit navigation events. */
@Composable
fun MainNavigation(
    homeUiState: HomeUiState,
    onHomeAction: (HomeAction) -> Unit,
    onDismissHomeAction: () -> Unit,
    vocabularyViewModel: VocabularyViewModel,
    collectionViewModel: CollectionViewModel,
    cameraViewModel: CameraViewModel,
    practiceViewModel: PracticeViewModel,
    profileViewModel: ProfileViewModel,
    onLogoutConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by rememberSaveable { mutableStateOf(MainDestination.Home) }
    val stateHolder = rememberSaveableStateHolder()

    BackHandler(enabled = destination != MainDestination.Home) {
        destination = MainDestination.Home
    }

    Scaffold(
        modifier = modifier,
        containerColor = if (destination == MainDestination.Profile) KoiColors.DeepWater else KoiColors.Mint,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavigationBar(destination.tab, onTabSelected = { tab ->
                destination = MainDestination.fromTab(tab)
            })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            // Retain each destination's local state, including the home scroll position.
            stateHolder.SaveableStateProvider(destination.name) {
                when (destination) {
                    MainDestination.Home -> HomeScreen(homeUiState, onAction = { action ->
                        when (action) {
                            HomeAction.Camera -> destination = MainDestination.Camera
                            HomeAction.Profile -> destination = MainDestination.Profile
                            HomeAction.Vocabulary, HomeAction.Flashcards -> destination = MainDestination.Vocabulary
                            HomeAction.Practice, HomeAction.Listening -> destination = MainDestination.Practice
                            else -> onHomeAction(action)
                        }
                    }, onDismissAction = onDismissHomeAction)
                    MainDestination.Vocabulary -> VocabularyRoute(vocabularyViewModel, collectionViewModel,
                        soundEnabled = profileViewModel.uiState.profile.soundEnabled) {
                        destination = MainDestination.Home
                    }
                    MainDestination.Practice -> PracticeScreen(
                        uiState = practiceViewModel.uiState,
                        onBackClick = { destination = MainDestination.Home },
                        onMediaTypeSelected = practiceViewModel::onMediaTypeSelected,
                        onQueryChange = practiceViewModel::onQueryChange,
                        onFilterSelected = practiceViewModel::onFilterSelected,
                        onDurationSelected = practiceViewModel::onDurationSelected,
                        onToggleSort = practiceViewModel::onToggleSort,
                        onLessonClick = practiceViewModel::onLessonClick,
                        onDismissLesson = practiceViewModel::onDismissLesson,
                        onDownloadsClick = practiceViewModel::onDownloadsClick,
                        onDismissDownloads = practiceViewModel::onDismissDownloads
                    )
                    MainDestination.Profile -> ProfileScreen(
                        uiState = profileViewModel.uiState,
                        onOpenDialog = profileViewModel::onOpenDialog,
                        onDismissDialog = profileViewModel::onDismissDialog,
                        onNameChange = profileViewModel::onNameChange,
                        onEmailChange = profileViewModel::onEmailChange,
                        onGoalSelected = profileViewModel::onGoalSelected,
                        onSaveInfo = profileViewModel::onSaveInfo,
                        onSaveGoal = profileViewModel::onSaveGoal,
                        onReminderChanged = profileViewModel::onReminderChanged,
                        onSoundChanged = profileViewModel::onSoundChanged,
                        onLogoutConfirmed = onLogoutConfirmed
                    )
                    MainDestination.Camera -> CameraRoute(cameraViewModel) {
                        destination = MainDestination.Home
                    }
                }
            }
        }
    }
}

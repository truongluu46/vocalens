package com.luudev.vocalens.presentation.navigation

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
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
                    else -> MainDestinationScreen(destination, homeUiState.dashboard.learnerName) {
                        destination = MainDestination.Home
                    }
                }
            }
        }
    }
}

// Temporary destinations until the corresponding feature screens are implemented.
@Composable
private fun MainDestinationScreen(destination: MainDestination, learnerName: String, onHomeClick: () -> Unit) {
    val title = stringResource(when (destination) {
        MainDestination.Home -> R.string.home_nav_home
        MainDestination.Vocabulary -> R.string.home_nav_vocabulary
        MainDestination.Camera -> R.string.home_camera
        MainDestination.Practice -> R.string.home_nav_practice
        MainDestination.Profile -> R.string.home_nav_profile
    })
    val icon = when (destination) {
        MainDestination.Home -> AppGlyph.Home
        MainDestination.Vocabulary -> AppGlyph.Book
        MainDestination.Camera -> AppGlyph.Camera
        MainDestination.Practice -> AppGlyph.Practice
        MainDestination.Profile -> AppGlyph.Person
    }
    Column(Modifier.fillMaxSize().pondBackground().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = KoiColors.SoftWater, shape = RoundedCornerShape(20.dp)) {
            AppIcon(icon, KoiColors.Primary, Modifier.padding(18.dp).size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KoiColors.Ink)
        Spacer(Modifier.height(12.dp))
        Text(if (destination == MainDestination.Profile) stringResource(R.string.home_profile_body, learnerName)
            else stringResource(R.string.navigation_feature_pending, title),
            textAlign = TextAlign.Center, color = KoiColors.Muted)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onHomeClick) { Text(stringResource(R.string.navigation_back_home), color = KoiColors.Primary) }
    }
}

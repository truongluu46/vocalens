package com.luudev.vocalens.presentation.flashcard

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luudev.vocalens.R
import java.util.Locale

@Composable
fun FlashcardRoute(viewModel: FlashcardViewModel, soundEnabled: Boolean, onBack: () -> Unit,
    onOpenVocabulary: () -> Unit) {
    val state = viewModel.uiState
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val unavailable = stringResource(R.string.vocabulary_voice_unavailable)
    val disabled = stringResource(R.string.profile_sound_disabled)
    DisposableEffect(context, lifecycleOwner, viewModel) {
        var disposed = false
        val tts = TextToSpeech(context) { status ->
            if (!disposed) ready = status == TextToSpeech.SUCCESS
        }
        engine = tts
        viewModel.setForeground(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.setForeground(true)
            if (event == Lifecycle.Event.ON_PAUSE) { viewModel.setForeground(false); tts.stop() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            viewModel.setForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
            ready = false
            tts.stop()
            tts.shutdown()
            engine = null
        }
    }
    LaunchedEffect(state.index, state.stage, soundEnabled) { engine?.stop() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); message = null } }
    BackHandler { viewModel.requestExit(onBack) }
    Box(Modifier.fillMaxSize()) {
        FlashcardScreen(state, onBack = { viewModel.requestExit(onBack) },
            onCategory = viewModel::selectCategory, onShuffle = viewModel::setShuffled,
            onReverse = viewModel::setMeaningFirst, onStart = { viewModel.start() },
            onReveal = viewModel::reveal, onRate = viewModel::rate, onRetry = viewModel::retry,
            onDifficult = { viewModel.start(difficultOnly = true) },
            onSetup = { viewModel.prepare(state.category) }, onOpenVocabulary = onOpenVocabulary,
            onSpeak = { word ->
                val tts = engine
                when {
                    !soundEnabled -> message = disabled
                    !ready || tts == null -> message = unavailable
                    tts.setLanguage(Locale.UK) < TextToSpeech.LANG_AVAILABLE -> message = unavailable
                    tts.speak(word.english, TextToSpeech.QUEUE_FLUSH, null, word.id) != TextToSpeech.SUCCESS -> message = unavailable
                }
            })
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (state.showExit) AlertDialog(
        onDismissRequest = viewModel::dismissExit,
        title = { Text(stringResource(R.string.flashcard_exit_title)) },
        text = { Text(stringResource(R.string.flashcard_exit_body)) },
        confirmButton = { TextButton(onClick = { viewModel.confirmExit(onBack) }, enabled = !state.busy) {
            Text(stringResource(R.string.flashcard_exit))
        } },
        dismissButton = { TextButton(onClick = viewModel::dismissExit, enabled = !state.busy) {
            Text(stringResource(R.string.flashcard_continue))
        } })
}


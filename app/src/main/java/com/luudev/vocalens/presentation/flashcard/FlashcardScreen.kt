package com.luudev.vocalens.presentation.flashcard

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
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
    Box(Modifier.fillMaxSize().pondBackground()) {
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

@Composable
fun FlashcardScreen(
    state: FlashcardUiState, onBack: () -> Unit,
    onCategory: (VocabularyCategory?) -> Unit, onShuffle: (Boolean) -> Unit, onReverse: (Boolean) -> Unit,
    onStart: () -> Unit, onReveal: () -> Unit, onRate: (FlashcardRating) -> Unit,
    onRetry: () -> Unit, onDifficult: () -> Unit, onSetup: () -> Unit,
    onOpenVocabulary: () -> Unit, onSpeak: (VocabularyWord) -> Unit
) {
    val scroll = rememberScrollState()
    LaunchedEffect(state.stage, state.index) { scroll.scrollTo(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !state.busy) {
                AppIcon(AppGlyph.Back, KoiColors.Ink, description = stringResource(R.string.vocabulary_back))
            }
            Text(stringResource(R.string.flashcard_title), fontWeight = FontWeight.Bold, fontSize = 21.sp, color = KoiColors.Ink)
        }
        Column(Modifier.weight(1f).widthIn(max = 600.dp).fillMaxWidth().align(Alignment.CenterHorizontally)
            .verticalScroll(scroll).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.busy || state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) {
                Text(stringResource(R.string.flashcard_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, enabled = !state.busy) { Text(stringResource(R.string.local_storage_retry)) }
            }
            when (state.stage) {
                FlashcardStage.Setup -> {
                    AppIcon(AppGlyph.Flashcard, KoiColors.Primary, Modifier.size(56.dp))
                    Text(stringResource(R.string.flashcard_intro), color = KoiColors.Muted)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.category == null, onClick = { onCategory(null) }, enabled = !state.busy,
                            label = { Text(stringResource(R.string.vocabulary_all)) })
                        VocabularyCategory.entries.forEach { category ->
                            FilterChip(selected = state.category == category, onClick = { onCategory(category) }, enabled = !state.busy,
                                label = { Text(stringResource(when (category) {
                                    VocabularyCategory.Objects -> R.string.vocabulary_objects
                                    VocabularyCategory.Technology -> R.string.vocabulary_technology
                                    VocabularyCategory.Travel -> R.string.vocabulary_travel
                                })) })
                        }
                    }
                    Text(stringResource(R.string.flashcard_count, state.deck.size), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KoiColors.Ink)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.flashcard_shuffle), Modifier.weight(1f))
                        Switch(checked = state.shuffled, onCheckedChange = onShuffle, enabled = !state.busy)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.flashcard_reverse), Modifier.weight(1f))
                        Switch(checked = state.meaningFirst, onCheckedChange = onReverse, enabled = !state.busy)
                    }
                    Button(onClick = onStart, enabled = state.deck.isNotEmpty() && !state.loading && !state.busy && !state.error,
                        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.flashcard_start)) }
                    if (state.deck.isEmpty() && !state.loading) {
                        Text(stringResource(R.string.flashcard_empty), color = KoiColors.Muted)
                        TextButton(onClick = onOpenVocabulary, enabled = !state.busy) { Text(stringResource(R.string.flashcard_add_words)) }
                    }
                }
                FlashcardStage.Studying -> state.current?.let { word ->
                    Text(stringResource(R.string.vocabulary_review_index, state.index + 1, state.cards.size), color = KoiColors.Muted)
                    LinearProgressIndicator(progress = { state.index.toFloat() / state.cards.size }, modifier = Modifier.fillMaxWidth())
                    Surface(onClick = onReveal, enabled = !state.busy, shape = RoundedCornerShape(24.dp),
                        color = KoiColors.Surface, border = BorderStroke(1.dp, KoiColors.Outline), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp).heightIn(min = 220.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                            Text(if (state.meaningFirst) word.meaning else word.english, fontSize = 30.sp,
                                fontWeight = FontWeight.Bold, color = KoiColors.Ink)
                            if (!state.meaningFirst || state.revealed) {
                                if (word.phonetic.isNotBlank()) Text(word.phonetic, color = KoiColors.Muted)
                                IconButton(onClick = { onSpeak(word) }) {
                                    AppIcon(AppGlyph.Sound, KoiColors.Primary, description = stringResource(R.string.vocabulary_pronounce, word.english))
                                }
                            }
                            if (state.revealed) {
                                HorizontalDivider()
                                Text(if (state.meaningFirst) word.english else word.meaning, fontSize = 23.sp, color = KoiColors.Primary)
                                word.partOfSpeech?.let { Text(it, color = KoiColors.Muted) }
                                word.exampleSentence?.let { Text(it, color = KoiColors.Muted) }
                            } else Text(stringResource(R.string.flashcard_tap), color = KoiColors.Muted)
                        }
                    }
                    if (!state.revealed) Button(onClick = onReveal, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.flashcard_reveal))
                    } else {
                        Text(stringResource(R.string.flashcard_rate), color = KoiColors.Muted)
                        FlashcardRating.entries.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { rating ->
                                    OutlinedButton(onClick = { onRate(rating) }, enabled = !state.busy && !state.error, modifier = Modifier.weight(1f)) {
                                        Text(stringResource(ratingLabel(rating)))
                                    }
                                }
                            }
                        }
                    }
                }
                FlashcardStage.Completed -> {
                    AppIcon(AppGlyph.Flashcard, KoiColors.Primary, Modifier.size(64.dp))
                    Text(stringResource(R.string.flashcard_completed), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = KoiColors.Ink)
                    Text(stringResource(R.string.flashcard_result, state.remembered, state.cards.size), color = KoiColors.Primary, fontSize = 20.sp)
                    Text(stringResource(R.string.flashcard_xp, state.earnedXp), color = KoiColors.OrangeInk)
                    FlashcardRating.entries.forEach { rating ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(ratingLabel(rating)))
                            Text(state.ratings.count { it == rating }.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.remembered < state.cards.size) Button(onClick = onDifficult, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.flashcard_repeat_difficult))
                    }
                    OutlinedButton(onClick = onSetup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.flashcard_new_round))
                    }
                    TextButton(onClick = onBack, enabled = !state.busy) { Text(stringResource(R.string.vocabulary_finish)) }
                }
            }
        }
    }
}

private fun ratingLabel(rating: FlashcardRating) = when (rating) {
    FlashcardRating.AGAIN -> R.string.flashcard_again
    FlashcardRating.HARD -> R.string.flashcard_hard
    FlashcardRating.GOOD -> R.string.flashcard_good
    FlashcardRating.EASY -> R.string.flashcard_easy
}

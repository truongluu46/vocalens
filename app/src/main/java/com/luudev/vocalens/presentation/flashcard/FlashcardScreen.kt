package com.luudev.vocalens.presentation.flashcard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.theme.KoiColors

@Composable
fun FlashcardScreen(
    state: FlashcardUiState, onBack: () -> Unit,
    onCategory: (VocabularyCategory?) -> Unit, onShuffle: (Boolean) -> Unit, onReverse: (Boolean) -> Unit,
    onStart: () -> Unit, onReveal: () -> Unit, onRate: (FlashcardRating) -> Unit,
    onRetry: () -> Unit, onDifficult: () -> Unit, onSetup: () -> Unit,
    onOpenVocabulary: () -> Unit, onSpeak: (VocabularyWord) -> Unit
) {
    val scroll = rememberScrollState()
    LaunchedEffect(state.stage, state.index, state.error) { scroll.scrollTo(0) }
    CompositionLocalProvider(LocalContentColor provides KoiColors.Ink) {
        BoxWithConstraints(Modifier.fillMaxSize().background(KoiColors.Mint)) {
            // Keep actions reachable with large fonts and in landscape, where a fixed footer would crowd out the card.
            val scrollActions = maxHeight < 520.dp || LocalDensity.current.fontScale > 1.3f
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize().align(Alignment.TopCenter)) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !state.busy) {
                        AppIcon(AppGlyph.Back, KoiColors.Ink, description = stringResource(R.string.flashcard_back))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.flashcard_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(when (state.stage) {
                            FlashcardStage.Setup -> R.string.flashcard_setup_subtitle
                            FlashcardStage.Studying -> R.string.flashcard_study_subtitle
                            FlashcardStage.Completed -> R.string.flashcard_result_subtitle
                        }), style = MaterialTheme.typography.bodySmall, color = KoiColors.Muted)
                    }
                }
                if (state.stage == FlashcardStage.Studying) StudyProgress(state)
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.error) ErrorNotice(state.busy, onRetry)
                    when (state.stage) {
                        FlashcardStage.Setup -> SetupContent(state, onCategory, onShuffle, onReverse)
                        FlashcardStage.Studying -> state.current?.let {
                            StudyCard(it, state.meaningFirst, state.revealed, state.busy, onReveal, onSpeak)
                        }
                        FlashcardStage.Completed -> ResultContent(state)
                    }
                    if (scrollActions) FlashcardActions(state, onStart, onReveal, onRate, onDifficult, onSetup, onBack, onOpenVocabulary)
                }
                if (!scrollActions) Surface(color = KoiColors.Surface, shadowElevation = 6.dp) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        FlashcardActions(state, onStart, onReveal, onRate, onDifficult, onSetup, onBack, onOpenVocabulary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyProgress(state: FlashcardUiState) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.flashcard_position, state.index + 1, state.cards.size), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.flashcard_reviewed, state.ratings.size), color = KoiColors.Muted,
                style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(progress = { state.ratings.size.toFloat() / state.cards.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), trackColor = KoiColors.Outline)
    }
}

@Composable
private fun SetupContent(state: FlashcardUiState, onCategory: (VocabularyCategory?) -> Unit,
    onShuffle: (Boolean) -> Unit, onReverse: (Boolean) -> Unit) {
    Surface(color = KoiColors.DeepWater, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(AppGlyph.Flashcard, KoiColors.SoftWater, Modifier.size(32.dp))
            Text(stringResource(R.string.flashcard_setup_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(stringResource(R.string.flashcard_intro), color = KoiColors.Mint, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.flashcard_choose_group))
        val categories = listOf<VocabularyCategory?>(null) + VocabularyCategory.entries
        categories.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { category ->
                    val selected = category == state.category
                    val count = state.availableWords.count { category == null || it.category == category }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                        color = if (selected) KoiColors.SoftWater else KoiColors.Surface,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) KoiColors.Primary else KoiColors.Outline)) {
                        Column(Modifier.selectable(selected, enabled = !state.busy && !state.loading, role = Role.RadioButton,
                            onClick = { onCategory(category) }).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(categoryLabel(category)), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (selected) AppIcon(AppGlyph.Check, KoiColors.Primary, Modifier.size(18.dp))
                            }
                            Text(stringResource(R.string.flashcard_group_count, count), color = KoiColors.Muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.flashcard_options))
        Surface(color = KoiColors.Surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, KoiColors.Outline)) {
            Column {
                OptionRow(stringResource(R.string.flashcard_shuffle), stringResource(R.string.flashcard_shuffle_hint),
                    state.shuffled, !state.busy, onShuffle)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = KoiColors.Outline)
                OptionRow(stringResource(R.string.flashcard_reverse), stringResource(R.string.flashcard_reverse_hint),
                    state.meaningFirst, !state.busy, onReverse)
            }
        }
    }
    if (state.deck.isEmpty() && !state.loading && !state.error) Surface(color = KoiColors.SoftOrange, shape = RoundedCornerShape(16.dp)) {
        Text(stringResource(R.string.flashcard_empty), Modifier.padding(16.dp), color = KoiColors.OrangeInk)
    }
}

@Composable
private fun OptionRow(title: String, hint: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(hint, color = KoiColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun StudyCard(word: VocabularyWord, meaningFirst: Boolean, revealed: Boolean, busy: Boolean,
    onReveal: () -> Unit, onSpeak: (VocabularyWord) -> Unit) {
    val revealLabel = stringResource(R.string.flashcard_reveal)
    val tapModifier = if (revealed) Modifier else Modifier.clickable(enabled = !busy, onClickLabel = revealLabel, onClick = onReveal)
    Surface(shape = RoundedCornerShape(28.dp), color = KoiColors.Surface,
        border = BorderStroke(1.dp, KoiColors.Outline), shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(tapModifier.padding(24.dp).heightIn(min = 280.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            Text(stringResource(if (revealed) R.string.flashcard_answer
                else if (meaningFirst) R.string.flashcard_meaning_face else R.string.flashcard_english_face),
                style = MaterialTheme.typography.labelMedium, color = KoiColors.Muted)
            val front = if (meaningFirst) word.meaning else word.english
            val answer = if (meaningFirst) word.english else word.meaning
            Text(if (revealed) answer else front, fontSize = 32.sp, lineHeight = 40.sp,
                fontWeight = FontWeight.Bold, color = if (revealed) KoiColors.Primary else KoiColors.Ink,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (revealed) Text(front, fontSize = 18.sp, color = KoiColors.Ink, textAlign = TextAlign.Center)
            if ((!meaningFirst || revealed) && word.phonetic.isNotBlank()) Text(word.phonetic, color = KoiColors.Muted)
            if (revealed) word.partOfSpeech?.takeIf { it.isNotBlank() }?.let { Text(it, color = KoiColors.Muted) }
            if (!revealed) Text(stringResource(R.string.flashcard_think), style = MaterialTheme.typography.bodyMedium,
                color = KoiColors.Muted, textAlign = TextAlign.Center)
            if (!meaningFirst || revealed) OutlinedButton(onClick = { onSpeak(word) }, enabled = !busy,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                AppIcon(AppGlyph.Sound, KoiColors.Primary, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.flashcard_listen))
            }
            if (revealed) word.exampleSentence?.takeIf { it.isNotBlank() }?.let {
                Surface(color = KoiColors.Mint, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.flashcard_example), style = MaterialTheme.typography.labelMedium, color = KoiColors.Muted)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardActions(state: FlashcardUiState, onStart: () -> Unit, onReveal: () -> Unit,
    onRate: (FlashcardRating) -> Unit, onDifficult: () -> Unit, onSetup: () -> Unit, onBack: () -> Unit, onOpenVocabulary: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.stage) {
            FlashcardStage.Setup -> {
                if (state.deck.isEmpty() && !state.loading && !state.error) {
                    PrimaryAction(stringResource(R.string.flashcard_add_words), !state.busy, false, onOpenVocabulary)
                } else {
                    Text(stringResource(R.string.flashcard_count, state.deck.size), style = MaterialTheme.typography.bodySmall,
                        color = KoiColors.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
                    PrimaryAction(stringResource(R.string.flashcard_start), state.deck.isNotEmpty() && !state.loading && !state.busy && !state.error,
                        state.busy, onStart)
                }
            }
            FlashcardStage.Studying -> {
                if (!state.revealed) {
                    Text(stringResource(R.string.flashcard_reveal_hint), color = KoiColors.Muted, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
                    PrimaryAction(stringResource(R.string.flashcard_reveal), !state.busy, state.busy, onReveal)
                } else {
                    Text(stringResource(if (state.busy) R.string.flashcard_saving else R.string.flashcard_rate),
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        color = KoiColors.Muted, fontWeight = FontWeight.Medium)
                    FlashcardRating.entries.chunked(2).forEach { row ->
                        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { rating ->
                                Surface(onClick = { onRate(rating) }, enabled = !state.busy && !state.error,
                                    color = ratingBackground(rating), contentColor = ratingColor(rating),
                                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(stringResource(ratingLabel(rating)), fontWeight = FontWeight.Bold)
                                        Text(stringResource(ratingHint(rating)), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FlashcardStage.Completed -> {
                val difficult = state.cards.size - state.remembered
                if (difficult > 0) {
                    PrimaryAction(stringResource(R.string.flashcard_repeat_count, difficult), !state.busy, state.busy, onDifficult)
                    OutlinedButton(onClick = onBack, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.vocabulary_finish))
                    }
                    TextButton(onClick = onSetup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.flashcard_new_round))
                    }
                } else {
                    PrimaryAction(stringResource(R.string.vocabulary_finish), !state.busy, false, onBack)
                    TextButton(onClick = onSetup, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.flashcard_new_round))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean, busy: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), contentPadding = PaddingValues(16.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ResultContent(state: FlashcardUiState) {
    Surface(color = KoiColors.Surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(72.dp).background(KoiColors.SoftWater, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                AppIcon(AppGlyph.Check, KoiColors.Primary, Modifier.size(36.dp))
            }
            Text(stringResource(R.string.flashcard_completed), fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(if (state.remembered == state.cards.size) R.string.flashcard_all_remembered else R.string.flashcard_encouragement),
                color = KoiColors.Muted, textAlign = TextAlign.Center)
            Text(stringResource(R.string.flashcard_result, state.remembered, state.cards.size), color = KoiColors.Primary,
                fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Surface(color = KoiColors.SoftOrange, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppGlyph.Sparkles, KoiColors.OrangeInk, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.flashcard_xp, state.earnedXp), color = KoiColors.OrangeInk, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    SectionTitle(stringResource(R.string.flashcard_breakdown))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FlashcardRating.entries.forEach { rating ->
            val count = state.ratings.count { it == rating }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(ratingLabel(rating)), color = KoiColors.Ink)
                    Text(stringResource(R.string.flashcard_group_count, count), fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(progress = { count.toFloat() / state.cards.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ratingColor(rating), trackColor = KoiColors.Outline)
            }
        }
    }
}

@Composable
private fun ErrorNotice(busy: Boolean, onRetry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.flashcard_error), color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.local_storage_retry)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

private fun categoryLabel(category: VocabularyCategory?) = when (category) {
    null -> R.string.vocabulary_all
    VocabularyCategory.Objects -> R.string.vocabulary_objects
    VocabularyCategory.Technology -> R.string.vocabulary_technology
    VocabularyCategory.Travel -> R.string.vocabulary_travel
}
private fun ratingLabel(rating: FlashcardRating) = when (rating) {
    FlashcardRating.AGAIN -> R.string.flashcard_again
    FlashcardRating.HARD -> R.string.flashcard_hard
    FlashcardRating.GOOD -> R.string.flashcard_good
    FlashcardRating.EASY -> R.string.flashcard_easy
}
private fun ratingHint(rating: FlashcardRating) = when (rating) {
    FlashcardRating.AGAIN -> R.string.flashcard_again_hint
    FlashcardRating.HARD -> R.string.flashcard_hard_hint
    FlashcardRating.GOOD -> R.string.flashcard_good_hint
    FlashcardRating.EASY -> R.string.flashcard_easy_hint
}
private fun ratingColor(rating: FlashcardRating) = when (rating) {
    FlashcardRating.AGAIN -> Color(0xFF993F39)
    FlashcardRating.HARD -> KoiColors.OrangeInk
    FlashcardRating.GOOD -> KoiColors.DeepWater
    FlashcardRating.EASY -> Color(0xFF35643D)
}
private fun ratingBackground(rating: FlashcardRating) = when (rating) {
    FlashcardRating.AGAIN -> Color(0xFFFBE8E5)
    FlashcardRating.HARD -> KoiColors.SoftOrange
    FlashcardRating.GOOD -> KoiColors.SoftWater
    FlashcardRating.EASY -> Color(0xFFE3EFDD)
}

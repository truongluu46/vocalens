package com.luudev.vocalens.presentation.vocabulary

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalUriHandler
import com.luudev.vocalens.domain.model.DictionaryError
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.VocabularyCategory
import com.luudev.vocalens.domain.model.VocabularySort
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.theme.VocaLensTheme

private val Blue = KoiColors.Primary
private val Ink = KoiColors.Ink
private val Muted = KoiColors.Muted
private val Outline = KoiColors.Outline

@Composable
fun VocabularyScreen(
    uiState: VocabularyUiState,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (VocabularyCategory?) -> Unit,
    onSortSelected: (VocabularySort) -> Unit,
    onSpeakWord: (VocabularyWord) -> Unit,
    onDeleteRequested: (VocabularyWord) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onStartReview: () -> Unit,
    onRevealMeaning: () -> Unit,
    onNextReviewWord: () -> Unit,
    onDismissReview: () -> Unit,
    modifier: Modifier = Modifier,
    onLookup: () -> Unit = {},
    onSaveLookupWord: (VocabularyWord) -> Unit = {}
) {
    Column(modifier.fillMaxSize().pondBackground().imePadding()) {
        VocabularyTopBar(uiState.sort, onBackClick, onSortSelected)
        LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxWidth().weight(1f).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                OutlinedTextField(
                    value = uiState.query, onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 13.sp),
                    placeholder = { Text(stringResource(R.string.vocabulary_search), color = Muted, fontSize = 12.sp) },
                    leadingIcon = { AppIcon(AppGlyph.Search, Muted, Modifier.size(19.dp)) },
                    trailingIcon = if (uiState.query.isNotEmpty()) { {
                        IconButton(onClick = { onQueryChange("") }) {
                            AppIcon(AppGlyph.Close, Muted, description = stringResource(R.string.vocabulary_clear_search))
                        }
                    } } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onLookup() }),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = KoiColors.Surface,
                        unfocusedContainerColor = KoiColors.Surface, focusedBorderColor = Blue,
                        unfocusedBorderColor = Outline, cursorColor = Blue)
                )
                Button(onClick = onLookup, enabled = uiState.query.isNotBlank() && !uiState.lookupLoading,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.vocabulary_lookup))
                }
                if (uiState.lookupLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                uiState.lookupError?.let { error ->
                    Text(stringResource(when (error) {
                        DictionaryError.NotFound -> R.string.vocabulary_lookup_not_found
                        DictionaryError.Network -> R.string.vocabulary_lookup_network
                        DictionaryError.InvalidResponse -> R.string.vocabulary_lookup_invalid
                    }), color = MaterialTheme.colorScheme.error)
                }
            }
            items(uiState.lookupResults, key = { "lookup:${it.word.id}" }) { result ->
                val word = result.word
                val uriHandler = LocalUriHandler.current
                Surface(color = KoiColors.Surface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, Outline)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(word.english, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSpeakWord(word) }) {
                                AppIcon(AppGlyph.Sound, Blue, description = stringResource(R.string.vocabulary_pronounce, word.english))
                            }
                        }
                        result.accent?.let { accent ->
                            Text(stringResource(if (accent == com.luudev.vocalens.domain.model.DictionaryAccent.British)
                                R.string.vocabulary_accent_uk else R.string.vocabulary_accent_us), color = Blue)
                        }
                        Text(listOfNotNull(word.phonetic.takeIf { it.isNotBlank() }, word.partOfSpeech).joinToString(" · "), color = Muted)
                        if (word.meaning.isNotBlank()) Text(word.meaning, color = Blue)
                        else Text(stringResource(R.string.vocabulary_lookup_no_vi), color = Muted)
                        Text(result.definition, color = Ink)
                        word.exampleSentence?.let { Text(it, color = Muted) }
                        val saved = uiState.savedWords.any { it.id == word.id }
                        TextButton(onClick = { onSaveLookupWord(word) },
                            enabled = !saved && word.meaning.isNotBlank() && uiState.savingWordId == null) {
                            Text(stringResource(if (saved) R.string.vocabulary_lookup_saved else R.string.vocabulary_lookup_save))
                        }
                        Row {
                            if (result.sourceUrl.startsWith("https://")) TextButton(onClick = { uriHandler.openUri(result.sourceUrl) }) {
                                Text(stringResource(R.string.vocabulary_lookup_source))
                            }
                            if (result.licenseUrl.startsWith("https://")) TextButton(onClick = { uriHandler.openUri(result.licenseUrl) }) {
                                Text(result.licenseName)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CategoryChip(stringResource(R.string.vocabulary_all), uiState.category == null) { onCategorySelected(null) }
                    VocabularyCategory.entries.forEach { category ->
                        CategoryChip(categoryLabel(category), uiState.category == category) { onCategorySelected(category) }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.vocabulary_saved_count, uiState.visibleWords.size),
                        color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    FilledTonalButton(onClick = onStartReview, enabled = uiState.visibleWords.isNotEmpty(),
                        shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = KoiColors.SoftWater, contentColor = Blue)) {
                        AppIcon(AppGlyph.Flashcard, Blue, Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.vocabulary_review), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(uiState.visibleWords, key = { it.id }) { word -> VocabularyWordCard(word,
                onSpeak = { onSpeakWord(word) }, onDelete = { onDeleteRequested(word) }) }
            if (uiState.visibleWords.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(AppGlyph.Book, Muted, Modifier.size(36.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(if (uiState.savedWords.isEmpty()) R.string.vocabulary_empty_title
                        else R.string.vocabulary_no_results), color = Ink, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(if (uiState.savedWords.isEmpty()) R.string.vocabulary_empty_body
                        else R.string.vocabulary_no_results_body), color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
    uiState.pendingDeletion?.let { word ->
        AlertDialog(onDismissRequest = onDeleteDismissed,
            title = { Text(stringResource(R.string.vocabulary_delete_title)) },
            text = { Text(stringResource(R.string.vocabulary_delete_body, word.english)) },
            confirmButton = { TextButton(onClick = onDeleteConfirmed) { Text(stringResource(R.string.vocabulary_delete_confirm)) } },
            dismissButton = { TextButton(onClick = onDeleteDismissed) { Text(stringResource(R.string.vocabulary_cancel)) } },
            containerColor = KoiColors.Surface)
    }
    uiState.reviewWords.getOrNull(uiState.reviewIndex)?.let { word ->
        AlertDialog(onDismissRequest = onDismissReview, containerColor = KoiColors.Surface,
            title = { Text(stringResource(R.string.vocabulary_review_index, uiState.reviewIndex + 1, uiState.reviewWords.size), fontSize = 17.sp) },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(word.english, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text(word.phonetic, color = Muted)
                    IconButton(onClick = { onSpeakWord(word) }) {
                        AppIcon(AppGlyph.Sound, Blue, description = stringResource(R.string.vocabulary_pronounce, word.english))
                    }
                    if (uiState.meaningRevealed) Text(word.meaning, color = Blue, fontSize = 20.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = if (uiState.meaningRevealed) onNextReviewWord else onRevealMeaning) {
                    Text(stringResource(if (!uiState.meaningRevealed) R.string.vocabulary_reveal
                        else if (uiState.reviewIndex == uiState.reviewWords.lastIndex) R.string.vocabulary_finish
                        else R.string.vocabulary_next), color = Blue)
                }
            }, dismissButton = { TextButton(onClick = onDismissReview) { Text(stringResource(R.string.vocabulary_close_review)) } })
    }
}

@Composable
private fun VocabularyTopBar(sort: VocabularySort, onBackClick: () -> Unit, onSortSelected: (VocabularySort) -> Unit) {
    var showSortMenu by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBackClick, modifier = Modifier.size(42.dp), color = Color.White,
                shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Outline)) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(AppGlyph.Back, Ink, Modifier.size(19.dp), stringResource(R.string.vocabulary_back))
                }
            }
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    AppIcon(AppGlyph.Sort, Ink, description = stringResource(R.string.vocabulary_sort))
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    VocabularySort.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(stringResource(if (option == VocabularySort.RecentlySaved)
                            R.string.vocabulary_sort_recent else R.string.vocabulary_sort_alphabetical)) },
                            onClick = { onSortSelected(option); showSortMenu = false },
                            trailingIcon = if (sort == option) { { AppIcon(AppGlyph.Check, Blue, Modifier.size(16.dp)) } } else null)
                    }
                }
            }
        }
        HorizontalDivider(color = Outline.copy(alpha = 0.6f))
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected, onClick, label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) Blue else Outline),
        colors = FilterChipDefaults.filterChipColors(containerColor = KoiColors.Surface,
            selectedContainerColor = Blue, selectedLabelColor = Color.White, labelColor = KoiColors.Muted))
}

@Composable
private fun categoryLabel(category: VocabularyCategory) = stringResource(when (category) {
    VocabularyCategory.Objects -> R.string.vocabulary_objects
    VocabularyCategory.Technology -> R.string.vocabulary_technology
    VocabularyCategory.Travel -> R.string.vocabulary_travel
})

@Composable
private fun VocabularyWordCard(word: VocabularyWord, onSpeak: () -> Unit, onDelete: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = KoiColors.Surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Outline)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(if (word.id == "headphones") R.drawable.vocabulary_headphones else R.drawable.vocabulary_notebook),
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(word.english, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(word.phonetic, color = Muted, fontSize = 9.sp)
                Spacer(Modifier.height(4.dp))
                Text(word.meaning, color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onSpeak) {
                Box(Modifier.size(32.dp).background(KoiColors.Mint, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    AppIcon(AppGlyph.Sound, Blue, Modifier.size(17.dp), stringResource(R.string.vocabulary_pronounce, word.english))
                }
            }
            IconButton(onClick = onDelete) {
                AppIcon(AppGlyph.Trash, Muted, Modifier.size(18.dp), stringResource(R.string.vocabulary_delete, word.english))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 740)
@Preview(showBackground = true, widthDp = 320, heightDp = 560)
@Composable
private fun VocabularyScreenPreview() {
    val words = listOf(VocabularyWord("headphones", "Headphones", "/ˈhed.fəʊnz/", "Tai nghe", VocabularyCategory.Technology),
        VocabularyWord("notebook", "Notebook", "/ˈnəʊt.bʊk/", "Sổ ghi chép", VocabularyCategory.Objects))
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        VocabularyScreen(VocabularyUiState(savedWords = words, visibleWords = words), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

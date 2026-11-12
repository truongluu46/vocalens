package com.luudev.vocalens.presentation.vocabulary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors

@Composable
fun CollectionsScreen(state: CollectionUiState, onEvent: (CollectionEvent) -> Unit,
    onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = state.selectedId != null && !state.busy) { onEvent(CollectionEvent.Back) }
    Column(modifier.fillMaxSize().pondBackground().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (state.selectedId != null) onEvent(CollectionEvent.Back) else onBack() },
                enabled = !state.busy) {
                AppIcon(AppGlyph.Back, KoiColors.Ink, description = stringResource(R.string.collection_back))
            }
            Text(state.selected?.name ?: stringResource(R.string.collection_tab),
                color = KoiColors.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        if (state.loading || state.wordsLoading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxWidth().weight(1f).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.error) item {
                Text(stringResource(R.string.collection_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onEvent(CollectionEvent.Retry) }, enabled = !state.busy) {
                    Text(stringResource(R.string.collection_retry))
                }
            }
            if (state.selectedId == null) {
                item {
                    Button(onClick = { onEvent(CollectionEvent.Create) }, modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading && !state.busy) { Text(stringResource(R.string.collection_create)) }
                }
                if (!state.loading && state.collections.isEmpty()) item {
                    Text(stringResource(R.string.collection_empty), color = KoiColors.Ink, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.collection_empty_body), color = KoiColors.Muted)
                }
                items(state.collections, key = { it.id }) { group ->
                    Surface(onClick = { onEvent(CollectionEvent.Open(group.id)) }, enabled = !state.busy,
                        color = KoiColors.Surface, shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, KoiColors.Outline)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(group.name, Modifier.weight(1f), color = KoiColors.Ink,
                                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                AppIcon(AppGlyph.Chevron, KoiColors.Primary)
                            }
                            if (group.description.isNotBlank()) Text(group.description, color = KoiColors.Muted,
                                modifier = Modifier.padding(top = 6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.collection_word_count, group.wordCount),
                                    Modifier.weight(1f), color = KoiColors.Primary, fontSize = 12.sp)
                                IconButton(onClick = { onEvent(CollectionEvent.Edit(group)) }, enabled = !state.busy) {
                                    AppIcon(AppGlyph.Edit, KoiColors.Primary, description = stringResource(R.string.collection_edit))
                                }
                                IconButton(onClick = { onEvent(CollectionEvent.RequestDelete(group)) }, enabled = !state.busy) {
                                    AppIcon(AppGlyph.Trash, KoiColors.Muted, description = stringResource(R.string.collection_delete))
                                }
                            }
                        }
                    }
                }
            } else {
                state.selected?.let { group ->
                    item {
                        if (group.description.isNotBlank()) Text(group.description, color = KoiColors.Muted)
                        Row {
                            TextButton(onClick = { onEvent(CollectionEvent.Edit(group)) }, enabled = !state.busy) {
                                Text(stringResource(R.string.collection_edit))
                            }
                            TextButton(onClick = { onEvent(CollectionEvent.RequestDelete(group)) }, enabled = !state.busy) {
                                Text(stringResource(R.string.collection_delete))
                            }
                        }
                        Button(onClick = { onEvent(CollectionEvent.PickWords) },
                            enabled = !state.busy && !state.wordsLoading, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.collection_manage_words))
                        }
                        Text(stringResource(R.string.collection_word_count, state.words.size),
                            color = KoiColors.Primary, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (!state.wordsLoading && state.words.isEmpty()) item {
                    Text(stringResource(R.string.collection_no_words), color = KoiColors.Muted)
                }
                items(state.words, key = { it.id }) { word ->
                    Surface(color = KoiColors.Surface, shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, KoiColors.Outline)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(word.english, color = KoiColors.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (word.phonetic.isNotBlank()) Text(word.phonetic, color = KoiColors.Muted)
                            Text(word.meaning, color = KoiColors.Primary)
                            TextButton(onClick = { onEvent(CollectionEvent.RemoveWord(word.id)) }, enabled = !state.busy) {
                                AppIcon(AppGlyph.Close, KoiColors.Muted, Modifier.size(16.dp),
                                    stringResource(R.string.collection_remove_word, word.english))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.collection_remove))
                            }
                        }
                    }
                }
            }
        }
    }
    CollectionDialogs(state, onEvent)
}

@Composable
private fun CollectionDialogs(state: CollectionUiState, onEvent: (CollectionEvent) -> Unit) {
    state.editor?.let { editor ->
        AlertDialog(onDismissRequest = { onEvent(CollectionEvent.DismissEditor) },
            containerColor = KoiColors.Surface,
            title = { Text(stringResource(if (editor.id == null) R.string.collection_create else R.string.collection_edit)) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(editor.name, { onEvent(CollectionEvent.Name(it)) }, enabled = !state.busy,
                        label = { Text(stringResource(R.string.collection_name)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, isError = editor.invalid)
                    OutlinedTextField(editor.description, { onEvent(CollectionEvent.Description(it)) }, enabled = !state.busy,
                        label = { Text(stringResource(R.string.collection_description)) },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                    if (editor.invalid) Text(stringResource(R.string.collection_validation), color = MaterialTheme.colorScheme.error)
                    if (state.error) Text(stringResource(R.string.collection_error), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(onClick = { onEvent(CollectionEvent.Save) }, enabled = !state.busy) {
                Text(stringResource(if (state.busy) R.string.collection_working else R.string.collection_save))
            } },
            dismissButton = { TextButton(onClick = { onEvent(CollectionEvent.DismissEditor) }, enabled = !state.busy) {
                Text(stringResource(R.string.collection_cancel))
            } })
    }
    state.pendingDelete?.let { group ->
        AlertDialog(onDismissRequest = { onEvent(CollectionEvent.DismissDelete) }, containerColor = KoiColors.Surface,
            title = { Text(stringResource(R.string.collection_delete)) },
            text = { Column {
                Text(stringResource(R.string.collection_delete_body, group.name))
                if (state.error) Text(stringResource(R.string.collection_error), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = { onEvent(CollectionEvent.ConfirmDelete) }, enabled = !state.busy) {
                Text(stringResource(R.string.collection_delete))
            } }, dismissButton = { TextButton(onClick = { onEvent(CollectionEvent.DismissDelete) }, enabled = !state.busy) {
                Text(stringResource(R.string.collection_cancel))
            } })
    }
    state.pickedWordIds?.let { picked ->
        AlertDialog(onDismissRequest = { onEvent(CollectionEvent.DismissPicker) }, containerColor = KoiColors.Surface,
            title = { Text(stringResource(R.string.collection_manage_words)) },
            text = { Column {
                Text(stringResource(R.string.collection_picker_hint), color = KoiColors.Muted)
                if (state.availableWords.isEmpty()) Text(stringResource(R.string.collection_no_saved_words),
                    modifier = Modifier.padding(top = 12.dp))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(state.availableWords, key = { it.id }) { word ->
                        Row(Modifier.fillMaxWidth().toggleable(word.id in picked, enabled = !state.busy,
                            role = Role.Checkbox, onValueChange = { onEvent(CollectionEvent.ToggleWord(word.id)) })
                            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = word.id in picked, onCheckedChange = null, enabled = !state.busy)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(word.english, fontWeight = FontWeight.SemiBold)
                                Text(word.meaning, color = KoiColors.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (state.error) Text(stringResource(R.string.collection_error), color = MaterialTheme.colorScheme.error)
            } },
            confirmButton = { TextButton(onClick = { onEvent(CollectionEvent.SaveWords) }, enabled = !state.busy) {
                Text(stringResource(if (state.busy) R.string.collection_working else R.string.collection_save))
            } }, dismissButton = { TextButton(onClick = { onEvent(CollectionEvent.DismissPicker) }, enabled = !state.busy) {
                Text(stringResource(R.string.collection_cancel))
            } })
    }
}

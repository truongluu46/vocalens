package com.luudev.vocalens.presentation.camera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.KoiDialog
import com.luudev.vocalens.presentation.theme.KoiColors

@Composable
internal fun CameraWordDialog(viewModel: CameraWordViewModel) {
    val state = viewModel.uiState
    val snackbar = remember { SnackbarHostState() }
    val saved = state.savedLabel
    val message = saved?.let { stringResource(R.string.camera_word_saved, it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.dismissSavedMessage()
        }
    }
    DisposableEffect(viewModel) { onDispose { viewModel.dismiss() } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(snackbar, modifier = Modifier.padding(16.dp))
    }
    val editor = state.editor ?: return
    val uriHandler = LocalUriHandler.current
    KoiDialog(
        title = editor.word.english,
        subtitle = stringResource(R.string.camera_word_subtitle), glyph = AppGlyph.Book,
        confirmLabel = stringResource(if (editor.saving) R.string.camera_word_saving else R.string.camera_word_save),
        cancelLabel = stringResource(R.string.camera_word_cancel),
        onConfirm = viewModel::save, onDismiss = viewModel::dismiss,
        busy = editor.saving, confirmEnabled = editor.valid
    ) {
        OutlinedTextField(
            value = editor.word.meaning, onValueChange = viewModel::changeMeaning,
            label = { Text(stringResource(R.string.camera_word_meaning)) },
            supportingText = { Text(stringResource(R.string.camera_word_manual_hint)) },
            modifier = Modifier.fillMaxWidth().testTag("camera_word_meaning"),
            enabled = !editor.saving, minLines = 2, maxLines = 4
        )
        if (editor.word.phonetic.isNotBlank()) Text(editor.word.phonetic, color = KoiColors.Muted)
        editor.word.exampleSentence?.takeIf { it.isNotBlank() }?.let { Text(it, color = KoiColors.Muted) }
        if (editor.lookupLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.camera_word_looking_up))
            }
        } else {
            if (editor.lookupFailed || editor.entries.none { it.word.meaning.isNotBlank() }) {
                Text(stringResource(R.string.camera_word_lookup_unavailable), color = KoiColors.Muted)
            }
            TextButton(onClick = viewModel::lookup, enabled = !editor.saving) {
                Text(stringResource(R.string.camera_word_lookup))
            }
        }
        editor.entries.filter { it.word.meaning.isNotBlank() }.forEach { entry ->
            Surface(color = KoiColors.SoftWater, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(entry.word.meaning, color = KoiColors.Ink)
                    if (entry.word.phonetic.isNotBlank()) Text(entry.word.phonetic, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.useEntry(entry) }, enabled = !editor.saving) {
                        Text(stringResource(R.string.camera_word_use_meaning))
                    }
                    Text(stringResource(R.string.camera_word_source, entry.licenseName.ifBlank { "Free Dictionary API" }),
                        style = MaterialTheme.typography.bodySmall, color = KoiColors.Muted)
                    Row {
                        if (entry.sourceUrl.startsWith("https://")) TextButton(onClick = {
                            runCatching { uriHandler.openUri(entry.sourceUrl) }
                        }) { Text(stringResource(R.string.camera_word_source_link)) }
                        if (entry.licenseUrl.startsWith("https://")) TextButton(onClick = {
                            runCatching { uriHandler.openUri(entry.licenseUrl) }
                        }) { Text(stringResource(R.string.camera_word_license)) }
                    }
                }
            }
        }
        Text(stringResource(R.string.camera_word_collection), style = MaterialTheme.typography.titleSmall)
        if (state.collectionsLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.collectionsFailed) {
            Text(stringResource(R.string.camera_word_collections_error), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = viewModel::reloadCollections, enabled = !editor.saving) {
                Text(stringResource(R.string.camera_retry))
            }
        }
        if (state.collections.isEmpty() && !state.collectionsLoading) {
            Text(stringResource(R.string.camera_word_no_collections), color = KoiColors.Muted)
        }
        state.collections.forEach { collection ->
            val selected = !editor.createNew && editor.collectionId == collection.id
            Row(Modifier.fillMaxWidth().selectable(selected, enabled = !editor.saving,
                role = Role.RadioButton, onClick = { viewModel.selectCollection(collection.id) }).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null, enabled = !editor.saving)
                Spacer(Modifier.width(8.dp))
                Text(collection.name, Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth().selectable(editor.createNew, enabled = !editor.saving,
            role = Role.RadioButton, onClick = viewModel::createNewCollection), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = editor.createNew, onClick = null, enabled = !editor.saving)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.camera_word_new_collection))
        }
        if (editor.createNew) OutlinedTextField(
            value = editor.newCollectionName, onValueChange = viewModel::changeCollectionName,
            label = { Text(stringResource(R.string.camera_word_collection_name)) },
            singleLine = true, enabled = !editor.saving,
            modifier = Modifier.fillMaxWidth().testTag("camera_word_collection_name")
        )
        if (editor.saveFailed) Text(stringResource(R.string.camera_word_save_error),
            color = MaterialTheme.colorScheme.error)
    }
}

package com.luudev.vocalens.presentation.vocabulary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.components.KoiDialog
import com.luudev.vocalens.presentation.theme.KoiColors

@Composable
internal fun CollectionDialogs(state: CollectionUiState, onEvent: (CollectionEvent) -> Unit) {
    val cancelLabel = stringResource(R.string.collection_cancel)
    val saveLabel = stringResource(if (state.busy) R.string.collection_working else R.string.collection_save)
    val focus = LocalFocusManager.current
    state.editor?.let { editor ->
        val valid = editor.name.isNotBlank() && editor.name.length <= 120 && editor.description.length <= 1000
        val submit = { if (valid && !state.busy) { focus.clearFocus(); onEvent(CollectionEvent.Save) } }
        KoiDialog(
            title = stringResource(if (editor.id == null) R.string.collection_create else R.string.collection_edit_title),
            subtitle = stringResource(R.string.collection_editor_subtitle),
            glyph = if (editor.id == null) AppGlyph.Book else AppGlyph.Edit,
            confirmLabel = saveLabel, cancelLabel = cancelLabel,
            onConfirm = submit, onDismiss = { onEvent(CollectionEvent.DismissEditor) },
            busy = state.busy, confirmEnabled = valid
        ) {
            CollectionField(label = stringResource(R.string.collection_name), value = editor.name,
                onChange = { onEvent(CollectionEvent.Name(it)) },
                placeholder = stringResource(R.string.collection_name_placeholder),
                glyph = AppGlyph.Book, enabled = !state.busy,
                error = editor.invalid || editor.name.length > 120,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            CollectionField(label = stringResource(R.string.collection_description), value = editor.description,
                onChange = { onEvent(CollectionEvent.Description(it)) },
                placeholder = stringResource(R.string.collection_description_placeholder),
                glyph = AppGlyph.Edit, enabled = !state.busy, multiline = true,
                error = editor.description.length > 1000,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }))
            if (editor.invalid || editor.name.length > 120 || editor.description.length > 1000)
                CollectionNotice(stringResource(R.string.collection_validation), error = true)
            if (state.error) CollectionNotice(stringResource(R.string.collection_error), error = true)
        }
    }
    state.pendingDelete?.let { group ->
        KoiDialog(
            title = stringResource(R.string.collection_delete),
            subtitle = stringResource(R.string.collection_delete_subtitle), glyph = AppGlyph.Trash,
            confirmLabel = stringResource(if (state.busy) R.string.collection_working else R.string.collection_delete_confirm),
            cancelLabel = cancelLabel, busy = state.busy, destructive = true,
            onConfirm = { onEvent(CollectionEvent.ConfirmDelete) },
            onDismiss = { onEvent(CollectionEvent.DismissDelete) }
        ) {
            Surface(color = KoiColors.SoftOrange.copy(alpha = .55f), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, KoiColors.Orange.copy(alpha = .25f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppGlyph.Book, KoiColors.OrangeInk)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(group.name, color = KoiColors.Ink, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.collection_word_count, group.wordCount),
                            color = KoiColors.Muted, fontSize = 12.sp)
                    }
                }
            }
            Text(stringResource(R.string.collection_delete_body, group.name), color = KoiColors.Muted,
                fontSize = 13.sp, lineHeight = 20.sp)
            if (state.error) CollectionNotice(stringResource(R.string.collection_error), error = true)
        }
    }
    state.pickedWordIds?.let { picked ->
        KoiDialog(
            title = stringResource(R.string.collection_manage_words),
            subtitle = stringResource(R.string.collection_picker_hint), glyph = AppGlyph.Flashcard,
            confirmLabel = saveLabel, cancelLabel = cancelLabel, busy = state.busy,
            onConfirm = { onEvent(CollectionEvent.SaveWords) },
            onDismiss = { onEvent(CollectionEvent.DismissPicker) }
        ) {
            CollectionNotice(stringResource(R.string.collection_selected_count, picked.size))
            if (state.availableWords.isEmpty()) Text(stringResource(R.string.collection_no_saved_words),
                color = KoiColors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
            else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.availableWords, key = { it.id }) { word ->
                    val selected = word.id in picked
                    Surface(shape = RoundedCornerShape(15.dp),
                        color = if (selected) KoiColors.Mint else KoiColors.Surface,
                        border = BorderStroke(1.dp, if (selected) KoiColors.Water else KoiColors.Outline)) {
                        Row(Modifier.fillMaxWidth().toggleable(selected, enabled = !state.busy,
                            role = Role.Checkbox, onValueChange = { onEvent(CollectionEvent.ToggleWord(word.id)) })
                            .padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = null, enabled = !state.busy,
                                colors = CheckboxDefaults.colors(checkedColor = KoiColors.Primary))
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(word.english, color = KoiColors.Ink, fontWeight = FontWeight.SemiBold)
                                Text(word.meaning, color = KoiColors.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (state.error) CollectionNotice(stringResource(R.string.collection_error), error = true)
        }
    }
}

@Composable
private fun CollectionField(
    label: String, value: String, onChange: (String) -> Unit, placeholder: String,
    glyph: AppGlyph, enabled: Boolean, error: Boolean,
    multiline: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = KoiColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value, onChange, enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            placeholder = { Text(placeholder, color = KoiColors.Muted.copy(alpha = .8f), fontSize = 13.sp) },
            leadingIcon = { AppIcon(glyph, KoiColors.Water, Modifier.size(19.dp)) },
            shape = RoundedCornerShape(15.dp), singleLine = !multiline,
            minLines = if (multiline) 3 else 1, maxLines = if (multiline) 4 else 1,
            isError = error, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KoiColors.Primary, unfocusedBorderColor = KoiColors.Outline,
                focusedContainerColor = KoiColors.Mint.copy(alpha = .45f),
                unfocusedContainerColor = KoiColors.Mint.copy(alpha = .3f),
                disabledContainerColor = KoiColors.Mint.copy(alpha = .2f),
                cursorColor = KoiColors.Primary, focusedTextColor = KoiColors.Ink, unfocusedTextColor = KoiColors.Ink))
    }
}

@Composable
private fun CollectionNotice(text: String, error: Boolean = false) {
    Row(Modifier.fillMaxWidth().background(if (error) KoiColors.SoftOrange else KoiColors.Mint,
        RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(if (error) AppGlyph.Bell else AppGlyph.Book,
            if (error) KoiColors.OrangeInk else KoiColors.Primary, Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (error) KoiColors.OrangeInk else KoiColors.Primary,
            fontSize = 12.sp, lineHeight = 18.sp)
    }
}

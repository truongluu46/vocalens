package com.luudev.vocalens.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luudev.vocalens.presentation.theme.KoiColors

/** Shared pond-themed shell. Form state and actions remain with the screen's ViewModel. */
@Composable
internal fun KoiDialog(
    title: String,
    subtitle: String,
    glyph: AppGlyph,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean = false,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val dismiss = { if (!busy) onDismiss() }
    val accent = if (destructive) KoiColors.OrangeInk else KoiColors.Primary
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
        dismissOnBackPress = !busy,
        dismissOnClickOutside = !busy
    )) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().safeDrawingPadding().imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().heightIn(max = maxHeight),
                shape = RoundedCornerShape(28.dp), color = KoiColors.Surface,
                border = BorderStroke(1.dp, KoiColors.Outline), shadowElevation = 20.dp
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().pondBackground()) {
                        Column(Modifier.padding(22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).background(
                                    if (destructive) KoiColors.SoftOrange else KoiColors.SoftWater,
                                    RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                                    AppIcon(glyph, accent, Modifier.size(23.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(title, color = KoiColors.Ink, fontSize = 20.sp,
                                    lineHeight = 25.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(subtitle, color = KoiColors.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp).padding(top = 18.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
                    Row(Modifier.fillMaxWidth().padding(22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = dismiss, enabled = !busy,
                            modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                            shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, KoiColors.Outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KoiColors.Muted)) {
                            Text(cancelLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Button(onClick = onConfirm, enabled = !busy && confirmEnabled,
                            modifier = Modifier.weight(1.45f).heightIn(min = 50.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                    color = KoiColors.Primary)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                AppIcon(if (destructive) AppGlyph.Trash else AppGlyph.Check,
                                    Color.White, Modifier.size(16.dp))
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(confirmLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

package com.luudev.vocalens.presentation.profile

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.UserProfile
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private val Blue = KoiColors.Primary
private val Ink = KoiColors.Ink
private val Muted = KoiColors.Muted
private val Outline = KoiColors.Outline

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onOpenDialog: (ProfileDialog) -> Unit,
    onDismissDialog: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onGoalSelected: (Int) -> Unit,
    onSaveInfo: () -> Unit,
    onSaveGoal: () -> Unit,
    onReminderChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onLogoutConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    LazyColumn(modifier.fillMaxSize().pondBackground(),
        contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ProfileHero(profile, onOpenDialog) }
        if (uiState.storageError) item { Text(stringResource(R.string.local_storage_error), color = MaterialTheme.colorScheme.error) }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.profile_goal_title), color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpenDialog(ProfileDialog.EditGoal) }, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.profile_edit), color = Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                GoalCard(profile)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionLabel(stringResource(R.string.profile_account_section))
                Spacer(Modifier.height(9.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = KoiColors.Surface.copy(alpha = .96f)) {
                    Column {
                        ProfileRow(AppGlyph.Edit, Blue, KoiColors.SoftWater,
                            stringResource(R.string.profile_edit_info), stringResource(R.string.profile_edit_info_caption),
                            onClick = { onOpenDialog(ProfileDialog.EditInfo) })
                        HorizontalDivider(color = Outline.copy(alpha = 0.65f))
                        ProfileRow(AppGlyph.Chart, Color(0xFF00AB9D), Color(0xFFCFFAF1),
                            stringResource(R.string.profile_stats), stringResource(R.string.profile_stats_caption),
                            onClick = { onOpenDialog(ProfileDialog.Statistics) })
                        HorizontalDivider(color = Outline.copy(alpha = 0.65f))
                        ProfileRow(AppGlyph.Bell, Color(0xFFEA9200), Color(0xFFFFF3C5),
                            stringResource(R.string.profile_reminder),
                            stringResource(if (profile.reminderEnabled) R.string.profile_reminder_time else R.string.profile_reminder_off),
                            onClick = { onOpenDialog(ProfileDialog.ReminderInfo) },
                            trailing = {
                                Switch(profile.reminderEnabled, onCheckedChange = onReminderChanged,
                                    modifier = Modifier.scale(0.78f),
                                    colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = Color.White,
                                        checkedBorderColor = Color.Transparent))
                            })
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionLabel(stringResource(R.string.profile_app_section))
                Spacer(Modifier.height(9.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = KoiColors.Surface.copy(alpha = .96f)) {
                    Column {
                        ProfileRow(AppGlyph.Settings, KoiColors.OrangeInk, KoiColors.SoftOrange,
                            stringResource(R.string.profile_settings), stringResource(R.string.profile_settings_caption),
                            onClick = { onOpenDialog(ProfileDialog.Settings) })
                        HorizontalDivider(color = Outline.copy(alpha = 0.65f))
                        ProfileRow(AppGlyph.Logout, Color(0xFFFF424C), Color(0xFFFFE1E5),
                            stringResource(R.string.profile_logout), stringResource(R.string.profile_logout_caption),
                            titleColor = Color(0xFFFF424C), onClick = { onOpenDialog(ProfileDialog.Logout) })
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.profile_version), color = Muted, fontSize = 9.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 12.dp))
        }
    }
    uiState.dialog?.let { dialog ->
        ProfileDialogContent(dialog, uiState, onDismissDialog, onNameChange, onEmailChange, onGoalSelected,
            onSaveInfo, onSaveGoal, onSoundChanged, onLogoutConfirmed)
    }
}

@Composable
private fun ProfileHero(profile: UserProfile, onOpenDialog: (ProfileDialog) -> Unit) {
    Column {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .pondBackground(prominent = true)
            .padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(Brush.linearGradient(listOf(KoiColors.Primary, KoiColors.Water)),
                    RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    AppIcon(AppGlyph.Sparkles, Color.White, Modifier.size(23.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.welcome_brand), color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(onClick = { onOpenDialog(ProfileDialog.Settings) }, shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = 0.14f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(AppGlyph.Settings, Color.White, description = stringResource(R.string.profile_settings))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(onClick = { onOpenDialog(ProfileDialog.EditInfo) }, shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(80.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    Text(profile.initials, color = Blue, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(22.dp)
                        .background(Color(0xFF11B5A8), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                        AppIcon(AppGlyph.Edit, Color.White, Modifier.size(13.dp), stringResource(R.string.profile_edit_avatar))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(profile.fullName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Text(profile.email, color = KoiColors.SoftWater, fontSize = 10.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppGlyph.Flame, Color(0xFFBFFAF3), Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.profile_active_learner), color = Color.White, fontSize = 8.sp)
            }
        }
        Surface(Modifier.padding(horizontal = 18.dp).offset(y = (-20).dp).fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFFB8C8D6).copy(alpha = 0.25f)),
            color = KoiColors.Surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Outline)) {
            Row(Modifier.height(IntrinsicSize.Min).padding(vertical = 15.dp)) {
                StatColumn(profile.wordsLearned.toString(), stringResource(R.string.profile_words_learned), Modifier.weight(1f))
                VerticalDivider(color = Outline)
                StatColumn(profile.streakDays.toString(), stringResource(R.string.profile_streak), Modifier.weight(1f))
                VerticalDivider(color = Outline)
                StatColumn(formatNumber(profile.totalXp), stringResource(R.string.profile_total_xp), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Blue, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Muted, fontSize = 8.sp)
    }
}

@Composable
private fun GoalCard(profile: UserProfile) {
    Surface(shape = RoundedCornerShape(18.dp), color = KoiColors.Surface, border = BorderStroke(1.dp, Outline)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileIcon(AppGlyph.Target, Blue, KoiColors.SoftWater)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.profile_daily_goal, profile.dailyGoal), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.profile_today_words, profile.wordsToday), color = Muted, fontSize = 8.sp)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { profile.dailyProgress }, modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = KoiColors.Primary, trackColor = KoiColors.SoftWater, strokeCap = StrokeCap.Round,
                    gapSize = 0.dp, drawStopIndicator = {})
            }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.home_percent, (profile.dailyProgress * 100).roundToInt()), color = Blue,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
}

@Composable
private fun ProfileIcon(glyph: AppGlyph, tint: Color, background: Color) {
    Box(Modifier.size(39.dp).background(background, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        AppIcon(glyph, tint, Modifier.size(22.dp))
    }
}

@Composable
private fun ProfileRow(glyph: AppGlyph, tint: Color, background: Color, title: String, subtitle: String,
    titleColor: Color = Ink, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileIcon(glyph, tint, background)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = titleColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontSize = 8.sp)
            }
            if (trailing != null) trailing() else AppIcon(AppGlyph.Chevron, Muted, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProfileDialogContent(dialog: ProfileDialog, state: ProfileUiState, onDismiss: () -> Unit,
    onNameChange: (String) -> Unit, onEmailChange: (String) -> Unit, onGoalSelected: (Int) -> Unit,
    onSaveInfo: () -> Unit, onSaveGoal: () -> Unit, onSoundChanged: (Boolean) -> Unit, onLogout: () -> Unit) {
    val title = stringResource(when (dialog) {
        ProfileDialog.EditInfo -> R.string.profile_edit_info
        ProfileDialog.EditGoal -> R.string.profile_goal_title
        ProfileDialog.Statistics -> R.string.profile_stats
        ProfileDialog.Settings -> R.string.profile_settings
        ProfileDialog.ReminderInfo -> R.string.profile_reminder
        ProfileDialog.Logout -> R.string.profile_logout
    })
    val editing = dialog == ProfileDialog.EditInfo || dialog == ProfileDialog.EditGoal
    AlertDialog(onDismissRequest = onDismiss, containerColor = KoiColors.Surface, title = { Text(title, color = Ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (dialog) {
                    ProfileDialog.EditInfo -> {
                        Text(stringResource(R.string.profile_local_edit_note), fontSize = 12.sp)
                        OutlinedTextField(state.draftName, onNameChange, singleLine = true,
                            label = { Text(stringResource(R.string.profile_name_label)) },
                            isError = state.validation.invalidName,
                            supportingText = if (state.validation.invalidName) { { Text(stringResource(R.string.register_name_error)) } } else null)
                        OutlinedTextField(state.draftEmail, onEmailChange, singleLine = true,
                            readOnly = true,
                            label = { Text(stringResource(R.string.profile_email_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = state.validation.invalidEmail,
                            supportingText = if (state.validation.invalidEmail) { { Text(stringResource(R.string.login_email_error)) } } else null)
                    }
                    ProfileDialog.EditGoal -> {
                        Text(stringResource(R.string.profile_goal_note))
                        listOf(10, 15, 25, 50, 100).forEach { goal ->
                            FilterChip(state.draftGoal == goal, onClick = { onGoalSelected(goal) },
                                label = { Text(stringResource(R.string.profile_daily_goal, goal)) })
                        }
                        if (state.validation.invalidGoal) Text(stringResource(R.string.profile_goal_error), color = MaterialTheme.colorScheme.error)
                    }
                    ProfileDialog.Statistics -> Text(stringResource(R.string.profile_stats_body,
                        state.profile.wordsLearned, state.profile.streakDays, formatNumber(state.profile.totalXp),
                        state.profile.wordsToday, state.profile.dailyGoal))
                    ProfileDialog.Settings -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.profile_sound), modifier = Modifier.weight(1f))
                            Switch(state.profile.soundEnabled, onSoundChanged)
                        }
                        Text(stringResource(R.string.profile_language))
                        Text(stringResource(R.string.profile_privacy), color = Muted, fontSize = 12.sp)
                    }
                    ProfileDialog.ReminderInfo -> Text(stringResource(R.string.profile_reminder_note))
                    ProfileDialog.Logout -> Text(stringResource(R.string.profile_logout_question))
                }
            }
        }, confirmButton = {
            TextButton(onClick = when (dialog) {
                ProfileDialog.EditInfo -> onSaveInfo
                ProfileDialog.EditGoal -> onSaveGoal
                ProfileDialog.Logout -> onLogout
                else -> onDismiss
            }) {
                Text(stringResource(if (editing) R.string.profile_save else if (dialog == ProfileDialog.Logout)
                    R.string.profile_logout else R.string.profile_close), color = if (dialog == ProfileDialog.Logout) Color(0xFFFF424C) else Blue)
            }
        }, dismissButton = if (editing || dialog == ProfileDialog.Logout) { {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.profile_cancel)) }
        } } else null)
}

private fun formatNumber(value: Int) = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value)

@Preview(showBackground = true, widthDp = 393, heightDp = 770)
@Preview(showBackground = true, widthDp = 320, heightDp = 560)
@Composable
private fun ProfileScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        ProfileScreen(ProfileUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

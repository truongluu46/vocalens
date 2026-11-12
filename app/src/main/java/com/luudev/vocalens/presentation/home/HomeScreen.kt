package com.luudev.vocalens.presentation.home

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.components.KoiPond
import com.luudev.vocalens.presentation.components.KoiFeatureArtwork
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.Dashboard
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

private val Blue = KoiColors.Primary
private val Ink = KoiColors.Ink
private val Muted = KoiColors.Muted
private val Teal = KoiColors.Water
private val Outline = KoiColors.Outline

@Composable
fun HomeScreen(uiState: HomeUiState, onAction: (HomeAction) -> Unit,
    onDismissAction: () -> Unit, modifier: Modifier = Modifier) {
    val dashboard = uiState.dashboard
    Box(modifier.fillMaxSize().pondBackground(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 600.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { HomeHeader(dashboard, onAction) }
            if (uiState.storageError) item { Text(stringResource(R.string.local_storage_error), color = MaterialTheme.colorScheme.error) }
            item { PondBanner() }
            item { StreakCard(dashboard.streakDays) }
            item {
                SectionTitle(stringResource(R.string.home_today), stringResource(R.string.home_view_progress)) {
                    onAction(HomeAction.Progress)
                }
                Spacer(Modifier.height(10.dp))
                DailyGoalCard(dashboard)
            }
            item {
                SectionTitle(stringResource(R.string.home_study_prompt))
                Spacer(Modifier.height(12.dp))
                LearningGrid(dashboard.pendingWords, onAction)
            }
            if (dashboard.continuedLessonTitle != null) item {
                SectionTitle(stringResource(R.string.home_continue), stringResource(R.string.home_view_all)) {
                    onAction(HomeAction.Courses)
                }
                Spacer(Modifier.height(10.dp))
                ContinueLearningCard(dashboard) { onAction(HomeAction.Courses) }
            }
            if (dashboard.hasRecentActivity) item {
                SectionTitle(stringResource(R.string.home_recent))
                Spacer(Modifier.height(12.dp))
                RecentActivityCard(dashboard) { onAction(HomeAction.Listening) }
            }
        }
    }
    uiState.selectedAction?.let { action -> HomeActionDialog(action, dashboard, onDismissAction) }
}

@Composable
private fun PondBanner() {
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))) {
        KoiPond(Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
            KoiColors.DeepWater.copy(alpha = 0.98f),
            KoiColors.DeepWater.copy(alpha = 0.85f), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.65f).padding(18.dp)) {
            Text(stringResource(R.string.home_pond_title), color = KoiColors.Surface,
                fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_pond_caption), color = KoiColors.SoftWater,
                fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun HomeHeader(dashboard: Dashboard, onAction: (HomeAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.home_greeting), color = Muted, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(5.dp))
            Text(stringResource(R.string.home_name, dashboard.learnerName), color = Ink,
                fontSize = 24.sp, letterSpacing = (-0.6).sp)
        }
        Surface(onClick = { onAction(HomeAction.Notifications) }, shape = RoundedCornerShape(14.dp),
            color = KoiColors.Surface, border = BorderStroke(1.dp, Outline), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) {
                AppIcon(AppGlyph.Bell, Ink, description = stringResource(R.string.home_notifications))
            }
        }
        Spacer(Modifier.width(9.dp))
        Surface(onClick = { onAction(HomeAction.Profile) }, shape = RoundedCornerShape(15.dp),
            modifier = Modifier.size(42.dp), color = Color.Transparent) {
            Box(Modifier.background(Brush.linearGradient(listOf(Blue, Teal))), contentAlignment = Alignment.Center) {
                Text(dashboard.initials, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StreakCard(days: Int) {
    val description = stringResource(R.string.home_streak_accessibility, days)
    Column(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(22.dp),
        spotColor = Blue.copy(alpha = 0.2f)).clip(RoundedCornerShape(22.dp))
        .pondBackground(prominent = true)
        .padding(15.dp).semantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) {
                AppIcon(AppGlyph.Flame, KoiColors.Orange, Modifier.size(27.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.home_streak, days), color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(stringResource(R.string.home_streak_caption), color = KoiColors.SoftWater,
                    fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(7) { index ->
                StreakDayLeaf(completed = index < days.coerceIn(0, 7))
            }
        }
    }
}

@Composable
private fun StreakDayLeaf(completed: Boolean) {
    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 2.dp.toPx()
            val bounds = Rect(inset, inset, size.width - inset, size.height - inset)
            val leafCenter = Offset(size.width * .5f, size.height * .5f)
            // A radial notch and fine veins give the badge a recognizable lily-pad silhouette.
            val leaf = Path().apply {
                moveTo(leafCenter.x, leafCenter.y * .78f)
                arcTo(bounds, -65f, 330f, forceMoveTo = false)
                close()
            }
            drawPath(leaf, Brush.linearGradient(
                if (completed) listOf(Color(0xFF90BA7F), KoiColors.Lily)
                else listOf(Color(0xFF588E7B), Color(0xFF3F786D))
            ))
            drawPath(leaf, KoiColors.SoftWater.copy(alpha = if (completed) .5f else .2f),
                style = Stroke(.7.dp.toPx()))
            repeat(6) { vein ->
                val angle = (vein * 45f) * Math.PI / 180.0
                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                drawLine(KoiColors.Surface.copy(alpha = if (completed) .18f else .08f),
                    leafCenter + direction * (bounds.width * .24f),
                    leafCenter + direction * (bounds.width * .45f), .6.dp.toPx())
            }
        }
        if (completed) AppIcon(AppGlyph.Check, KoiColors.Surface,
            Modifier.offset(y = 1.dp).size(14.dp))
        else Text(stringResource(R.string.home_sunday), color = KoiColors.SoftWater,
            fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 1.dp))
    }
}

@Composable
private fun SectionTitle(title: String, actionLabel: String? = null, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            TextButton(onClick, modifier = Modifier.heightIn(min = 32.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                Text(actionLabel, color = Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DailyGoalCard(dashboard: Dashboard) {
    DashboardCard {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { dashboard.dailyProgress },
                        modifier = Modifier.fillMaxSize(), color = Blue, trackColor = KoiColors.SoftWater,
                        strokeWidth = 5.dp)
                    Text(stringResource(R.string.home_percent, (dashboard.dailyProgress * 100).roundToInt()),
                        color = Blue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_word_goal, dashboard.completedWords, dashboard.targetWords),
                        color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(stringResource(R.string.home_remaining, dashboard.remainingWords), color = Muted, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(AppGlyph.Clock, Teal, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(dashboard.studyMinutes.toString(), color = Teal, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.home_minutes), color = Muted, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            ProgressLine(dashboard.dailyProgress)
        }
    }
}

@Composable
private fun LearningGrid(pendingWords: Int, onAction: (HomeAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LearningCard(stringResource(R.string.home_flashcards), stringResource(R.string.home_pending, pendingWords),
                AppGlyph.Flashcard, 0, Modifier.weight(1f)) { onAction(HomeAction.Flashcards) }
            LearningCard(stringResource(R.string.home_camera), stringResource(R.string.home_camera_caption),
                AppGlyph.Camera, 1, Modifier.weight(1f)) { onAction(HomeAction.Camera) }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LearningCard(stringResource(R.string.home_listening), stringResource(R.string.home_listening_caption),
                AppGlyph.Headphones, 2, Modifier.weight(1f)) { onAction(HomeAction.Listening) }
            LearningCard(stringResource(R.string.home_pronunciation), stringResource(R.string.home_pronunciation_caption),
                AppGlyph.Mic, 3, Modifier.weight(1f)) { onAction(HomeAction.Pronunciation) }
        }
    }
}

@Composable
private fun LearningCard(title: String, subtitle: String, glyph: AppGlyph, artwork: Int,
    modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(24.dp), color = KoiColors.DeepWater,
        border = BorderStroke(1.dp, KoiColors.Surface.copy(alpha = .85f))) {
        Box(Modifier.fillMaxWidth().heightIn(min = 158.dp)) {
            KoiFeatureArtwork(artwork, Modifier.matchParentSize())
            Column(Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 14.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xFF9BE8D7).copy(alpha = .70f))) {
                    Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(
                        Color(0xFF72D4BE).copy(alpha = .50f), Color(0xFF3CA99C).copy(alpha = .24f)))),
                        contentAlignment = Alignment.Center) {
                        AppIcon(glyph, KoiColors.Surface, Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = KoiColors.Surface, fontSize = 17.sp, lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(subtitle, color = Color(0xFFC7E6DF), fontSize = 11.sp, lineHeight = 16.sp,
                        modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    AppIcon(AppGlyph.Chevron, KoiColors.Surface, Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningCard(dashboard: Dashboard, onClick: () -> Unit) {
    DashboardCard(onClick = onClick) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            HouseholdThumbnail(Modifier.size(72.dp).clip(RoundedCornerShape(13.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_course_category), color = Blue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(dashboard.continuedLessonTitle.orEmpty(), color = Ink,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(9.dp))
                ProgressLine(dashboard.courseProgress)
                Spacer(Modifier.height(7.dp))
                Text(stringResource(R.string.home_course_progress, (dashboard.courseProgress * 100).roundToInt()),
                    color = Muted, fontSize = 8.sp)
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(28.dp).background(KoiColors.SoftWater, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(AppGlyph.Chevron, Blue, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RecentActivityCard(dashboard: Dashboard, onClick: () -> Unit) {
    DashboardCard(onClick = onClick) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(AppGlyph.Headphones, Teal, Color(0xFFCFF9F1))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_activity_title), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.home_activity_caption, dashboard.recentActivityMinutes), color = Muted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun DashboardCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit) {
    val cardModifier = modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(19.dp),
        ambientColor = Color(0xFFCBD5E1).copy(alpha = 0.15f), spotColor = Color(0xFFCBD5E1).copy(alpha = 0.15f))
    if (onClick != null) Surface(onClick = onClick, modifier = cardModifier, color = KoiColors.Surface,
        shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, Outline), content = content)
    else Surface(modifier = cardModifier, color = KoiColors.Surface, shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, Outline), content = content)
}

@Composable
private fun IconTile(glyph: AppGlyph, tint: Color, background: Color) {
    Box(Modifier.size(39.dp).background(background, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        AppIcon(glyph, tint, Modifier.size(21.dp))
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp),
        color = KoiColors.Primary, trackColor = KoiColors.SoftWater, strokeCap = StrokeCap.Round,
        gapSize = 0.dp, drawStopIndicator = {})
}

@Composable
private fun HouseholdThumbnail(modifier: Modifier) {
    Canvas(modifier.background(KoiColors.Orange)) {
        val w = size.width
        val h = size.height
        // A small original illustration of household objects in the reference's warm palette.
        rotate(-22f, Offset(w * 0.3f, h * 0.5f)) {
            drawRoundRect(Color(0xFFF1F6FA), Offset(w * 0.19f, h * 0.15f), Size(w * 0.18f, h * 0.66f), CornerRadius(w * 0.05f))
            drawRoundRect(Color(0xFFCAD3DD), Offset(w * 0.21f, h * 0.1f), Size(w * 0.14f, h * 0.15f), CornerRadius(w * 0.03f))
        }
        drawCircle(Color(0xFF263747), w * 0.13f, Offset(w * 0.74f, h * 0.22f))
        drawCircle(Color(0xFF263747), w * 0.13f, Offset(w * 0.53f, h * 0.19f))
        drawLine(Color(0xFF263747), Offset(w * 0.55f, h * 0.19f), Offset(w * 0.76f, h * 0.22f), w * 0.06f)
        rotate(32f) {
            drawRoundRect(Color(0xFF29444B), Offset(w * 0.62f, h * 0.49f), Size(w * 0.09f, h * 0.36f), CornerRadius(w * 0.03f))
        }
        drawCircle(Color(0xFFE9F0F0), w * 0.12f, Offset(w * 0.81f, h * 0.76f))
        drawCircle(Color(0xFF9CB7BC), w * 0.12f, Offset(w * 0.81f, h * 0.76f), style = Stroke(w * 0.025f))
        drawLine(Color(0xFF614126), Offset(w * 0.13f, h * 0.8f), Offset(w * 0.34f, h * 0.94f), w * 0.035f, StrokeCap.Round)
    }
}

@Composable
private fun HomeActionDialog(action: HomeAction, dashboard: Dashboard, onDismiss: () -> Unit) {
    val title = stringResource(when (action) {
        HomeAction.Notifications -> R.string.home_notifications
        HomeAction.Profile -> R.string.home_nav_profile
        HomeAction.Progress -> R.string.home_view_progress
        HomeAction.Flashcards -> R.string.home_flashcards
        HomeAction.Camera -> R.string.home_camera
        HomeAction.Listening -> R.string.home_listening
        HomeAction.Pronunciation -> R.string.home_pronunciation
        HomeAction.Courses -> R.string.home_continue
        HomeAction.Vocabulary -> R.string.home_nav_vocabulary
        HomeAction.Practice -> R.string.home_nav_practice
    })
    val body = when (action) {
        HomeAction.Notifications -> stringResource(R.string.home_no_notifications)
        HomeAction.Profile -> stringResource(R.string.home_profile_body, dashboard.learnerName)
        HomeAction.Progress -> stringResource(R.string.home_progress_body, dashboard.completedWords,
            dashboard.targetWords, dashboard.studyMinutes, dashboard.remainingWords)
        else -> stringResource(R.string.home_feature_pending, title)
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, color = Ink) },
        text = { Text(body) }, containerColor = KoiColors.Surface,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_close), color = Blue) } })
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun HomeScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        HomeScreen(HomeUiState(), {}, {})
    }
}

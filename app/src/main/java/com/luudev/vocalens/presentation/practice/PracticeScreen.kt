package com.luudev.vocalens.presentation.practice

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.presentation.components.AppGlyph
import com.luudev.vocalens.presentation.components.AppIcon
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import java.util.Locale
import kotlin.math.roundToInt

private val Blue = KoiColors.Primary
private val Ink = KoiColors.Ink
private val Muted = KoiColors.Muted
private val Outline = KoiColors.Outline

@Composable
fun PracticeScreen(
    uiState: PracticeUiState,
    onBackClick: () -> Unit,
    onMediaTypeSelected: (PracticeMediaType) -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (PracticeFilter) -> Unit,
    onDurationSelected: (PracticeDuration) -> Unit,
    onToggleSort: () -> Unit,
    onLessonClick: (PracticeLesson) -> Unit,
    onDismissLesson: () -> Unit,
    onDownloadsClick: () -> Unit,
    onDismissDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().pondBackground().imePadding()) {
        PracticeTopBar(onBackClick, onDownloadsClick)
        LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxWidth().weight(1f).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            item {
                MediaTypeSelector(uiState.mediaType, onMediaTypeSelected)
                Spacer(Modifier.height(13.dp))
                PracticeSearch(uiState.query, uiState.duration, onQueryChange, onDurationSelected)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PracticeFilter.entries.forEach { filter ->
                        val selected = uiState.filter == filter
                        FilterChip(selected, onClick = { onFilterSelected(filter) },
                            label = { Text(filterLabel(filter), fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (selected) Blue else Outline),
                            colors = FilterChipDefaults.filterChipColors(containerColor = KoiColors.Surface,
                                selectedContainerColor = Blue, selectedLabelColor = Color.White, labelColor = KoiColors.Muted))
                    }
                }
            }
            uiState.featuredLesson?.let { lesson -> item(key = "featured-${lesson.id}") {
                FeaturedLesson(lesson) { onLessonClick(lesson) }
            } }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sectionTitle(uiState.mediaType, uiState.sort), color = Ink,
                        fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onToggleSort, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text(stringResource(if (uiState.sort == PracticeSort.Newest) R.string.practice_popular
                            else R.string.practice_newest), color = Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(uiState.lessons, key = { it.id }) { lesson -> LessonCard(lesson) { onLessonClick(lesson) } }
            if (uiState.lessons.isEmpty() && uiState.featuredLesson == null) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(AppGlyph.Headphones, Muted, Modifier.size(35.dp))
                    Spacer(Modifier.height(15.dp))
                    Text(stringResource(if (uiState.storageError) R.string.local_storage_error else R.string.practice_no_results), color = Ink, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.practice_no_results_body), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
    uiState.selectedLesson?.let { lesson ->
        AlertDialog(onDismissRequest = onDismissLesson, containerColor = KoiColors.Surface,
            title = { Text(lesson.title, color = Ink) },
            text = {
                Column {
                    Image(painterResource(artwork(lesson.artworkKey)), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)))
                    Spacer(Modifier.height(14.dp))
                    Text(lesson.category, color = Blue, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.practice_featured_meta, lesson.durationSeconds / 60,
                        difficultyLabel(lesson.difficulty)), color = Muted)
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.practice_media_unavailable))
                }
            }, confirmButton = { TextButton(onClick = onDismissLesson) { Text(stringResource(R.string.practice_close), color = Blue) } })
    }
    if (uiState.showDownloads) AlertDialog(onDismissRequest = onDismissDownloads,
        title = { Text(stringResource(R.string.practice_downloads)) },
        text = { Text(stringResource(R.string.practice_downloads_empty)) }, containerColor = KoiColors.Surface,
        confirmButton = { TextButton(onClick = onDismissDownloads) { Text(stringResource(R.string.practice_close), color = Blue) } })
}

@Composable
private fun PracticeTopBar(onBackClick: () -> Unit, onDownloadsClick: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onBackClick, modifier = Modifier.size(42.dp), color = Color.White,
                shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Outline)) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(AppGlyph.Back, Ink, Modifier.size(19.dp), stringResource(R.string.navigation_back_home))
                }
            }
            Text(stringResource(R.string.practice_title), color = Ink, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = onDownloadsClick) {
                AppIcon(AppGlyph.Download, Ink, description = stringResource(R.string.practice_downloads))
            }
        }
        HorizontalDivider(color = Outline.copy(alpha = 0.65f))
    }
}

@Composable
private fun MediaTypeSelector(selectedType: PracticeMediaType, onSelected: (PracticeMediaType) -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).background(KoiColors.SoftWater, RoundedCornerShape(14.dp))
        .padding(4.dp).selectableGroup()) {
        PracticeMediaType.entries.forEach { type ->
            val selected = selectedType == type
            val color = if (selected) Blue else KoiColors.Muted
            Row(Modifier.weight(1f).fillMaxHeight()
                .then(if (selected) Modifier.shadow(4.dp, RoundedCornerShape(11.dp)) else Modifier)
                .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(11.dp))
                .selectable(selected, role = Role.Tab, onClick = { onSelected(type) }),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                AppIcon(if (type == PracticeMediaType.Video) AppGlyph.Play else AppGlyph.Headphones, color, Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(if (type == PracticeMediaType.Video) R.string.practice_video else R.string.practice_podcast),
                    color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PracticeSearch(query: String, duration: PracticeDuration, onQueryChange: (String) -> Unit,
    onDurationSelected: (PracticeDuration) -> Unit) {
    var showDuration by remember { mutableStateOf(false) }
    OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.practice_search), color = Muted, fontSize = 12.sp) },
        singleLine = true, textStyle = TextStyle(color = Ink, fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), shape = RoundedCornerShape(15.dp),
        leadingIcon = { AppIcon(AppGlyph.Search, Muted, Modifier.size(19.dp)) },
        trailingIcon = {
            Box {
                IconButton(onClick = { showDuration = true }) {
                    Box(Modifier.size(34.dp).background(if (duration == PracticeDuration.All) KoiColors.Mint
                        else KoiColors.SoftWater, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                        AppIcon(AppGlyph.Sort, Blue, Modifier.size(18.dp), stringResource(R.string.practice_duration_filter))
                    }
                }
                DropdownMenu(showDuration, onDismissRequest = { showDuration = false }) {
                    PracticeDuration.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(stringResource(when (option) {
                            PracticeDuration.All -> R.string.practice_duration_all
                            PracticeDuration.Short -> R.string.practice_duration_short
                            PracticeDuration.Long -> R.string.practice_duration_long
                        })) }, onClick = { onDurationSelected(option); showDuration = false },
                            trailingIcon = if (duration == option) { { AppIcon(AppGlyph.Check, Blue, Modifier.size(16.dp)) } } else null)
                    }
                }
            }
        }, colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = KoiColors.Surface,
            unfocusedContainerColor = KoiColors.Surface, focusedBorderColor = Blue, unfocusedBorderColor = Outline, cursorColor = Blue))
}

@Composable
private fun FeaturedLesson(lesson: PracticeLesson, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 185.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 185.dp)) {
            Image(painterResource(artwork(lesson.artworkKey)), null, contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xFF12203B).copy(alpha = 0.10f),
                Color(0xFF11182C).copy(alpha = 0.75f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(15.dp)) {
                Text(stringResource(R.string.practice_featured), color = Color(0xFF5EF5D5), fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text(lesson.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppGlyph.Clock, Color.White, Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.practice_featured_meta, lesson.durationSeconds / 60,
                        if (lesson.difficulty == LessonDifficulty.Easy) stringResource(R.string.practice_basic)
                        else difficultyLabel(lesson.difficulty)), color = Color.White, fontSize = 9.sp)
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onClick, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KoiColors.Surface, contentColor = Blue)) {
                    AppIcon(AppGlyph.Play, Blue, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.practice_start), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: PracticeLesson, onClick: () -> Unit) {
    val description = stringResource(R.string.practice_open_lesson, lesson.title)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = RoundedCornerShape(18.dp), color = KoiColors.Surface, border = BorderStroke(1.dp, Outline)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(105.dp, 82.dp).clip(RoundedCornerShape(12.dp))) {
                Image(painterResource(artwork(lesson.artworkKey)), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.padding(7.dp).size(27.dp).background(Color.White.copy(alpha = 0.94f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    AppIcon(if (lesson.mediaType == PracticeMediaType.Video) AppGlyph.Play else AppGlyph.Headphones,
                        Blue, Modifier.size(15.dp))
                }
                Text(durationLabel(lesson.durationSeconds), color = Color.White, fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
                        .background(Color(0xFF14283B).copy(alpha = 0.85f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(vertical = 2.dp)) {
                Text(lesson.category, color = Blue, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(7.dp))
                Text(lesson.title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyBadge(lesson.difficulty)
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.practice_progress, (lesson.progress * 100).roundToInt()), color = Muted, fontSize = 8.sp)
                }
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(progress = { lesson.progress }, modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = KoiColors.Primary, trackColor = KoiColors.SoftWater, strokeCap = StrokeCap.Round,
                    gapSize = 0.dp, drawStopIndicator = {})
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: LessonDifficulty) {
    val (background, foreground) = when (difficulty) {
        LessonDifficulty.Easy -> Color(0xFFD2FAF4) to Color(0xFF00A99C)
        LessonDifficulty.Intermediate -> KoiColors.SoftOrange to KoiColors.OrangeInk
        LessonDifficulty.Hard -> Color(0xFFFFF1C2) to Color(0xFFCA8B11)
    }
    Text(difficultyLabel(difficulty), color = foreground, fontSize = 8.sp,
        modifier = Modifier.background(background, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 3.dp))
}

@Composable
private fun difficultyLabel(difficulty: LessonDifficulty) = stringResource(when (difficulty) {
    LessonDifficulty.Easy -> R.string.practice_easy
    LessonDifficulty.Intermediate -> R.string.practice_intermediate
    LessonDifficulty.Hard -> R.string.practice_hard
})

@Composable
private fun filterLabel(filter: PracticeFilter) = stringResource(when (filter) {
    PracticeFilter.All -> R.string.practice_all
    PracticeFilter.Easy -> R.string.practice_easy
    PracticeFilter.Intermediate -> R.string.practice_intermediate
    PracticeFilter.Toeic -> R.string.practice_toeic
})

@Composable
private fun sectionTitle(mediaType: PracticeMediaType, sort: PracticeSort) = stringResource(when {
    mediaType == PracticeMediaType.Video && sort == PracticeSort.Newest -> R.string.practice_latest_videos
    mediaType == PracticeMediaType.Video -> R.string.practice_popular_videos
    sort == PracticeSort.Newest -> R.string.practice_latest_podcasts
    else -> R.string.practice_popular_podcasts
})

private fun artwork(key: String) = when (key) {
    "cafe" -> R.drawable.practice_cafe
    "workspace" -> R.drawable.practice_workspace
    else -> R.drawable.practice_student
}

private fun durationLabel(seconds: Int) = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

@Preview(showBackground = true, widthDp = 393, heightDp = 770)
@Preview(showBackground = true, widthDp = 320, heightDp = 560)
@Composable
private fun PracticeScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        PracticeScreen(PracticeUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

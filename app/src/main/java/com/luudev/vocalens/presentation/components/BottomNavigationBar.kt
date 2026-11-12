package com.luudev.vocalens.presentation.components

import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R

enum class BottomTab { Home, Vocabulary, Camera, Practice, Profile }

private val BarBlue = KoiColors.Primary
private val BarMuted = KoiColors.Muted

/** Draws tabs and emits clicks. The parent owns selection and navigation. */
@Composable
fun BottomNavigationBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = KoiColors.Surface, shadowElevation = 8.dp) {
        Column {
            HorizontalDivider(color = KoiColors.Outline)
            Row(Modifier.fillMaxWidth().navigationBarsPadding().height(80.dp).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically) {
                BottomTab.entries.forEach { tab ->
                    val label = stringResource(when (tab) {
                        BottomTab.Home -> R.string.home_nav_home
                        BottomTab.Vocabulary -> R.string.home_nav_vocabulary
                        BottomTab.Camera -> R.string.home_camera
                        BottomTab.Practice -> R.string.home_nav_practice
                        BottomTab.Profile -> R.string.home_nav_profile
                    })
                    val glyph = when (tab) {
                        BottomTab.Home -> AppGlyph.Home
                        BottomTab.Vocabulary -> AppGlyph.Book
                        BottomTab.Camera -> AppGlyph.Camera
                        BottomTab.Practice -> AppGlyph.Practice
                        BottomTab.Profile -> AppGlyph.Person
                    }
                    BottomItem(label, glyph, tab == selectedTab, tab == BottomTab.Camera,
                        Modifier.weight(1f)) { onTabSelected(tab) }
                }
            }
        }
    }
}

@Composable
private fun BottomItem(label: String, glyph: AppGlyph, selected: Boolean, camera: Boolean,
    modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().selectable(selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (camera) {
            Surface(shape = CircleShape, color = BarBlue, border = BorderStroke(4.dp, Color.White),
                modifier = Modifier.size(49.dp).shadow(6.dp, CircleShape,
                    spotColor = BarBlue.copy(alpha = 0.35f))) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(glyph, Color.White, Modifier.size(23.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        } else {
            Box(Modifier.size(38.dp, 31.dp).background(if (selected) KoiColors.SoftWater else Color.Transparent,
                RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                AppIcon(glyph, if (selected) BarBlue else KoiColors.Muted, Modifier.size(22.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(label, color = if (selected) BarBlue else BarMuted, fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun BottomNavigationBarPreview() {
    BottomNavigationBar(BottomTab.Home, {})
}

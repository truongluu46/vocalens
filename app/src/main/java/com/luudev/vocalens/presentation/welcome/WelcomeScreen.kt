package com.luudev.vocalens.presentation.welcome

import com.luudev.vocalens.presentation.components.pondBackground
import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import com.luudev.vocalens.presentation.components.KoiPond
import com.luudev.vocalens.presentation.components.BrandHeader

private val Blue = KoiColors.Primary
private val Ink = KoiColors.Ink
private val Muted = KoiColors.Muted

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().pondBackground()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))
            PondIllustration(Modifier.widthIn(max = 400.dp).fillMaxWidth().height(292.dp))
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.welcome_eyebrow), color = Blue,
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_title), color = Ink, fontSize = 30.sp,
                lineHeight = 36.sp, letterSpacing = (-1.2).sp, textAlign = TextAlign.Center)
            Text(stringResource(R.string.welcome_title_accent), color = Blue, fontSize = 30.sp,
                lineHeight = 36.sp, letterSpacing = (-1.2).sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_description), color = Muted,
                fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp))
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStartClick,
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
                    .height(54.dp).shadow(14.dp, RoundedCornerShape(16.dp),
                        ambientColor = Blue.copy(alpha = 0.25f), spotColor = Blue.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KoiColors.Primary)
            ) {
                Text(stringResource(R.string.welcome_start), fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(12.dp))
                LineIcon(Glyph.Chevron, Color.White, Modifier.size(18.dp))
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon(Glyph.Shield, KoiColors.Muted, Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.welcome_safety), color = KoiColors.Muted,
                    fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PondIllustration(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        KoiPond(Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)))
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            shape = RoundedCornerShape(20.dp), color = KoiColors.Surface,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, KoiColors.Outline)
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(KoiColors.SoftWater, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center) {
                    LineIcon(Glyph.Camera, KoiColors.Primary, Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.welcome_pond_title), color = KoiColors.Ink,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.welcome_pond_caption), color = KoiColors.Muted,
                        fontSize = 11.sp)
                }
            }
        }
    }
}

private enum class Glyph { Camera, Sound, Shield, Chevron }

// Decorative vector strokes keep the illustration crisp at every screen density.
@Composable
private fun LineIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round)
            fun line(vararg points: Float) {
                val path = Path().apply {
                    moveTo(points[0], points[1])
                    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
                }
                drawPath(path, color, style = stroke)
            }
            when (glyph) {
                Glyph.Camera -> {
                    line(3f, 7f, 8f, 7f, 9.5f, 4f, 15f, 4f, 16.5f, 7f, 21f, 7f,
                        21f, 21f, 3f, 21f, 3f, 7f)
                    drawCircle(color, 4.4f, Offset(12f, 13.5f), style = stroke)
                }
                Glyph.Chevron -> line(9f, 5f, 16f, 12f, 9f, 19f)
                Glyph.Shield -> {
                    line(12f, 2f, 20f, 6f, 19f, 14f, 16f, 19f, 12f, 22f,
                        8f, 19f, 5f, 14f, 4f, 6f, 12f, 2f)
                    line(8f, 12f, 11f, 15f, 16f, 9f)
                }
                Glyph.Sound -> {
                    line(3f, 9f, 7f, 9f, 12f, 5f, 12f, 19f, 7f, 15f, 3f, 15f, 3f, 9f)
                    drawArc(color, -55f, 110f, false, Offset(10f, 6f), Size(10f, 12f), style = stroke)
                    drawArc(color, -55f, 110f, false, Offset(10f, 2f), Size(16f, 20f), style = stroke)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun WelcomeScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        WelcomeScreen(onStartClick = {})
    }
}

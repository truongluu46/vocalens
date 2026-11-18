package com.luudev.vocalens.presentation.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.components.BrandHeader
import com.luudev.vocalens.presentation.components.WelcomePond
import com.luudev.vocalens.presentation.theme.VocaLensTheme

private val WelcomeInk = Color(0xFF004C48)
private val WelcomeTeal = Color(0xFF008C80)

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFFE0F6EE), Color(0xFFE6F8F1)))
    )) {
        WelcomeDecoration(Modifier.fillMaxSize())
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Keep the reference proportions on short phones and allow scrolling at large font sizes.
            val unit = minOf(maxWidth.value / 393f, maxHeight.value / 850f).coerceIn(.75f, 1.2f)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = (28 * unit).dp, bottom = (40 * unit).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader(showAiBadge = false)
                Spacer(Modifier.height((26 * unit).dp))
                WelcomePond(Modifier.width((363 * unit).dp).aspectRatio(1.15f)
                    .clip(RoundedCornerShape((52 * unit).dp)))
                Spacer(Modifier.height((38 * unit).dp))
                Text(stringResource(R.string.welcome_title), color = WelcomeInk,
                    fontSize = (42 * unit).sp, lineHeight = (47 * unit).sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5 * unit).sp,
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.welcome_title_accent), color = WelcomeTeal,
                    fontSize = (42 * unit).sp, lineHeight = (47 * unit).sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5 * unit).sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height((18 * unit).dp))
                Text(stringResource(R.string.welcome_description), color = Color(0xFF788A86),
                    fontSize = (14 * unit).sp, lineHeight = (22 * unit).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width((365 * unit).dp))
                Spacer(Modifier.height((40 * unit).dp))
                Button(
                    onClick = onStartClick,
                    modifier = Modifier.width((350 * unit).dp).height((54 * unit).dp),
                    shape = RoundedCornerShape((19 * unit).dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B827B))
                ) {
                    Text(stringResource(R.string.welcome_start), fontSize = (20 * unit).sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width((14 * unit).dp))
                    StartArrow(Modifier.size((24 * unit).dp))
                }
                Spacer(Modifier.height((20 * unit).dp))
                Text(stringResource(R.string.welcome_safety), color = Color(0xFF81938C),
                    fontSize = (13 * unit).sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StartArrow(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val arrow = Path().apply {
                moveTo(4f, 12f); lineTo(20f, 12f)
                moveTo(13f, 5f); lineTo(20f, 12f); lineTo(13f, 19f)
            }
            drawPath(arrow, Color.White, style = Stroke(1.8f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun WelcomeDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.width / 393f
        drawCircle(Color(0xFFCCE8DC).copy(alpha = .55f), 54 * u, Offset(10 * u, 31 * u),
            style = Stroke(8 * u))
        drawCircle(Color(0xFFB8E1C7).copy(alpha = .75f), 11 * u, Offset(40 * u, 49 * u))
        val leaf = Offset(29 * u, size.height - 40 * u)
        drawCircle(Color(0xFFCAEADD).copy(alpha = .6f), 48 * u, leaf + Offset(13 * u, 19 * u))
        drawCircle(Color(0xFFADD9B8).copy(alpha = .7f), 32 * u, leaf)
        repeat(8) { index ->
            rotate(index * 45f, leaf) {
                drawOval(Color(0xFFFFFDF1), leaf - Offset(4 * u, 18 * u),
                    androidx.compose.ui.geometry.Size(8 * u, 20 * u))
            }
        }
        drawCircle(Color(0xFFFFEAB1), 5 * u, leaf)
        drawCircle(Color(0xFFE5EEE1), 48 * u, Offset(size.width, size.height - 7 * u))
        drawCircle(Color.White.copy(alpha = .5f), 44 * u,
            Offset(size.width, size.height - 7 * u), style = Stroke(1.5f * u))
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

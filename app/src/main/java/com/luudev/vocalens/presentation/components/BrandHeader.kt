package com.luudev.vocalens.presentation.components

import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R

@Composable
@Preview
fun BrandHeader(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).shadow(12.dp, RoundedCornerShape(18.dp),
            spotColor = KoiColors.Water).background(
            Brush.linearGradient(listOf(KoiColors.Primary, KoiColors.Water)),
            RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(30.dp)) {
                scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                    val path = Path().apply {
                        moveTo(10f, 2f); lineTo(12f, 8f); lineTo(18f, 10f)
                        lineTo(12f, 12f); lineTo(10f, 18f); lineTo(8f, 12f)
                        lineTo(2f, 10f); lineTo(8f, 8f); close()
                        moveTo(19f, 15f); lineTo(20f, 18f); lineTo(23f, 19f)
                        lineTo(20f, 20f); lineTo(19f, 23f); lineTo(18f, 20f)
                        lineTo(15f, 19f); lineTo(18f, 18f); close()
                    }
                    drawPath(path, Color.White, style = Stroke(1.9f, cap = StrokeCap.Round))
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.welcome_brand), color = KoiColors.Ink,
                fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.welcome_ai), color = KoiColors.Primary, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier
                    .background(KoiColors.SoftWater, RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp))
        }
    }
}

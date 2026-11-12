package com.luudev.vocalens.presentation.components

import com.luudev.vocalens.presentation.theme.KoiColors
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

private val FormMuted = KoiColors.Muted
enum class FormGlyph { Email, Lock, Eye, EyeOff, Person }

@Composable
fun FormIcon(glyph: FormGlyph, description: String? = null) {
    val vector = remember(glyph) {
        androidx.compose.ui.graphics.vector.ImageVector.Builder(
            defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            // Paths are outlined with rounded caps to match the reference's light icons.
            val nodes = androidx.compose.ui.graphics.vector.PathBuilder().apply {
                when (glyph) {
                    FormGlyph.Person -> {
                        moveTo(16f, 7f)
                        curveTo(16f, 12.3f, 8f, 12.3f, 8f, 7f)
                        curveTo(8f, 1.7f, 16f, 1.7f, 16f, 7f); close()
                        moveTo(4f, 21f); curveTo(4f, 10f, 20f, 10f, 20f, 21f)
                    }
                    FormGlyph.Email -> {
                        moveTo(3f, 5f); lineTo(21f, 5f); lineTo(21f, 19f)
                        lineTo(3f, 19f); close()
                        moveTo(3f, 6f); lineTo(12f, 13f); lineTo(21f, 6f)
                    }
                    FormGlyph.Lock -> {
                        moveTo(5f, 10f); lineTo(19f, 10f); lineTo(19f, 21f)
                        lineTo(5f, 21f); close()
                        moveTo(8f, 10f); lineTo(8f, 6f)
                        curveTo(8f, 0f, 16f, 0f, 16f, 6f); lineTo(16f, 10f)
                    }
                    FormGlyph.Eye, FormGlyph.EyeOff -> {
                        moveTo(2f, 12f); curveTo(7f, 4f, 17f, 4f, 22f, 12f)
                        curveTo(17f, 20f, 7f, 20f, 2f, 12f); close()
                        moveTo(15f, 12f)
                        curveTo(15f, 16f, 9f, 16f, 9f, 12f)
                        curveTo(9f, 8f, 15f, 8f, 15f, 12f); close()
                        if (glyph == FormGlyph.EyeOff) { moveTo(3f, 3f); lineTo(21f, 21f) }
                    }
                }
            }.nodes
            addPath(nodes, stroke = androidx.compose.ui.graphics.SolidColor(FormMuted),
                strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round)
        }.build()
    }
    Icon(vector, contentDescription = description, tint = FormMuted, modifier = Modifier.size(18.dp))
}


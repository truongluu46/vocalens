package com.luudev.vocalens.presentation.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.luudev.vocalens.presentation.components.PondMotion

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD5CB),
    secondary = Color(0xFFB0D3AD),
    tertiary = KoiColors.Orange,
    background = Color(0xFF102728),
    surface = Color(0xFF183233),
    onBackground = KoiColors.Mint,
    onSurface = KoiColors.Mint
)

private val LightColorScheme = lightColorScheme(
    primary = KoiColors.Primary,
    onPrimary = Color.White,
    primaryContainer = KoiColors.SoftWater,
    onPrimaryContainer = KoiColors.DeepWater,
    secondary = KoiColors.Lily,
    secondaryContainer = KoiColors.SoftWater,
    onSecondaryContainer = KoiColors.Ink,
    tertiary = KoiColors.OrangeInk,
    tertiaryContainer = KoiColors.SoftOrange,
    onTertiaryContainer = KoiColors.OrangeInk,
    background = KoiColors.Mint,
    onBackground = KoiColors.Ink,
    surface = KoiColors.Surface,
    onSurface = KoiColors.Ink,
    surfaceVariant = KoiColors.SoftWater,
    onSurfaceVariant = KoiColors.Muted,
    surfaceContainer = KoiColors.Surface,
    surfaceContainerHigh = KoiColors.Mint,
    surfaceContainerHighest = KoiColors.SoftWater,
    outline = KoiColors.Muted,
    outlineVariant = KoiColors.Outline
)

@Composable
fun VocaLensTheme(
    darkTheme: Boolean = false,
    // Keep the pond palette consistent; dynamic colors remain an explicit opt-in.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = { PondMotion(content) }
    )
}

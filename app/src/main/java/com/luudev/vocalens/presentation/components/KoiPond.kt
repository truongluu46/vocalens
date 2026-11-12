package com.luudev.vocalens.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.luudev.vocalens.presentation.theme.KoiColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val LocalPondPhase = staticCompositionLocalOf<State<Float>?> { null }

/** One animation clock shared across screens and decorative cards. */
@Composable
fun PondMotion(content: @Composable () -> Unit) {
    val phase = rememberPondPhase()
    CompositionLocalProvider(LocalPondPhase provides phase, content = content)
}

/** Decorative art; state is read during drawing so forms/lists do not recompose per frame. */
@Composable
fun KoiPond(modifier: Modifier = Modifier) {
    val phase = LocalPondPhase.current ?: rememberPondPhase()
    Canvas(modifier) { drawPond(phase.value, ambient = false) }
}

/** Compact pond composition with a quiet middle area reserved for the card's labels. */
@Composable
internal fun KoiFeatureArtwork(variant: Int, modifier: Modifier = Modifier) {
    val phase = LocalPondPhase.current ?: rememberPondPhase()
    Canvas(modifier) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF208A87), Color(0xFF096168), Color(0xFF278F85))))
        val unit = size.minDimension / 180f
        scale(unit, unit, pivot = center) {
            val w = size.width / unit
            val h = size.height / unit
            val left = center.x - w / 2
            val top = center.y - h / 2
            fun point(x: Float, y: Float) = Offset(left + w * x, top + h * y)
            // Soft ribbons of water follow the lower edge, away from the text.
            repeat(3) { index ->
                val wave = Path().apply {
                    val y = .86f + index * .035f
                    moveTo(left - 8f, top + h * y)
                    cubicTo(left + w * .20f, top + h * (y - .17f),
                        left + w * .35f, top + h * (y + .26f), left + w * .62f, top + h * y)
                    cubicTo(left + w * .80f, top + h * (y - .20f),
                        left + w * .95f, top + h * (y + .12f), left + w + 8f, top + h * (y - .05f))
                }
                drawPath(wave, Color(0xFF8AE1CA).copy(alpha = .12f - index * .025f),
                    style = Stroke(if (index == 0) 1.3f else .6f))
            }
            when (variant) {
                0 -> {
                    lily(point(.90f, .98f), 22f)
                    lily(point(.75f, 1.02f), 12f)
                    lily(point(.06f, .90f), 10f, flower = true)
                }
                1 -> {
                    lily(point(.83f, .02f), 20f)
                    lily(point(.96f, .14f), 10f)
                    lily(point(.68f, .04f), 9f, flower = true)
                    lily(point(.78f, .96f), 7f)
                }
                2 -> {
                    lily(point(.84f, .05f), 19f, flower = true)
                    lily(point(.68f, -.02f), 12f)
                    lily(point(.91f, .97f), 18f)
                    lily(point(.77f, 1.02f), 13f, flower = true)
                }
                else -> {
                    lily(point(.92f, -.01f), 24f)
                    lily(point(.72f, .02f), 10f)
                    lily(point(.91f, .23f), 11f)
                }
            }
            val t = phase.value
            val anchor = when (variant) {
                0 -> point(.83f, .28f)
                1 -> point(.84f, .38f)
                2 -> point(.17f, .87f)
                else -> point(.76f, .87f)
            }
            val fishPosition = anchor + Offset(sin(t + variant) * 3f, cos(t + variant) * 3f)
            repeat(3) { ring ->
                drawOval(Color(0xFF92DDC9).copy(alpha = .14f - ring * .035f),
                    fishPosition - Offset(14f + ring * 7f, 9f + ring * 5f),
                    Size(28f + ring * 14f, 18f + ring * 10f), style = Stroke(.6f))
            }
            val heading = when (variant) { 0 -> -35f; 1 -> 205f; 2 -> -42f; else -> 42f }
            koi(fishPosition, 34f, heading + sin(t * 3f) * 5f,
                KoiColors.Orange, swish = sin(t * 24f + variant))
        }
    }
}

/** Fixed to the screen viewport, behind all content, with no touch or accessibility nodes. */
@Composable
fun Modifier.pondBackground(prominent: Boolean = false): Modifier {
    val phase = LocalPondPhase.current ?: rememberPondPhase()
    return drawBehind {
        drawPond(phase.value, ambient = !prominent)
        if (prominent) drawRect(KoiColors.DeepWater.copy(alpha = .60f))
    }
}

@Composable
private fun rememberPondPhase(): State<Float> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Static previews, and no animation work while the activity is in the background.
    if (!resumed || LocalInspectionMode.current) return rememberUpdatedState(0f)
    val transition = rememberInfiniteTransition(label = "pond swimming")
    return transition.animateFloat(
        initialValue = 0f, targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing)),
        label = "pond orbit"
    )
}

private fun DrawScope.drawPond(phase: Float, ambient: Boolean) {
    if (size.minDimension <= 0f) return
    // Uniform scaling keeps fish and lily pads in proportion at any aspect ratio.
    drawRect(Brush.radialGradient(
        if (ambient) listOf(Color(0xFFBDDCD0), KoiColors.SoftWater, KoiColors.Mint)
        else listOf(KoiColors.DeepWater, KoiColors.Water, Color(0xFFB8E1CC)),
        center = Offset(size.width * .46f, size.height * .53f),
        radius = size.maxDimension * .75f
    ))
    val unit = size.minDimension / 300f
    scale(unit, unit, pivot = center) {
        val w = size.width / unit
        val h = size.height / unit
        val left = center.x - w / 2f
        val top = center.y - h / 2f
        fun point(x: Float, y: Float) = Offset(left + w * x, top + h * y)

        // Light caustics and ripples, drawn deterministically rather than per-frame noise.
        repeat(24) { index ->
            val p = point(((index * 37 % 101) / 100f), ((index * 23 % 97) / 96f))
            drawCircle(Color.White.copy(alpha = .045f), 4f + index % 5, p)
        }
        listOf(point(.38f, .28f), point(.66f, .68f), point(.12f, .60f)).forEach { p ->
            repeat(3) { ring ->
                drawCircle(Color(0xFFD5F6E7).copy(alpha = .13f - ring * .03f),
                    19f + ring * 10f, p, style = Stroke(.7f))
            }
        }
        fun stone(x: Float, y: Float, radius: Float) {
            val p = point(x, y)
            drawCircle(Color(0xFF235D53).copy(alpha = .15f), radius, p + Offset(3f, 5f))
            drawCircle(Color(0xFFDEDCD0), radius, p)
            drawArc(Color.White.copy(alpha = .55f), 195f, 120f, false,
                p - Offset(radius - 3f, radius - 3f), Size((radius - 3f) * 2, (radius - 3f) * 2),
                style = Stroke(1.3f))
            drawCircle(KoiColors.Lily.copy(alpha = .7f), radius * .22f, p + Offset(radius * .6f, radius * .4f))
        }
        stone(.02f, .03f, 38f)
        stone(.12f, -.02f, 26f)
        stone(-.02f, .19f, 27f)
        stone(.98f, .98f, 33f)
        stone(.90f, 1.02f, 22f)
        stone(1.02f, .82f, 21f)

        lily(point(.79f, .20f), 26f)
        lily(point(.89f, .28f), 16f)
        lily(point(.73f, .31f), 13f)
        lily(point(.16f, .79f), 24f, flower = true)
        lily(point(.27f, .88f), 16f)
        lily(point(.08f, .67f), 14f)

        // Closed ellipses make the loop seamless. Heading follows the path tangent.
        repeat(5) { index ->
            val t = phase + index * (Math.PI * 2f / 5f).toFloat()
            val direction = if (index % 2 == 0) 1f else -1f
            val theta = t * direction
            val rx = w * (.30f + (index % 2) * .08f)
            val ry = h * (.32f - (index % 3) * .035f)
            val p = Offset(center.x + rx * cos(theta), center.y + ry * sin(theta))
            val dx = -rx * sin(theta) * direction
            val dy = ry * cos(theta) * direction
            val heading = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
            val swish = sin(phase * 24f + index * 2f)
            koi(p, if (ambient) 39f + index * 2f else 27f + index * 2f,
                heading, if (index % 2 == 0) KoiColors.Orange else Color(0xFFE9B95A),
                patterned = index % 2 == 0, swish = swish)
        }
    }
    if (ambient) drawRect(KoiColors.Mint.copy(alpha = .62f))
}

private fun DrawScope.lily(p: Offset, radius: Float, flower: Boolean = false) {
    drawCircle(Color(0xFF164B40).copy(alpha = .18f), radius, p + Offset(3f, 5f))
    drawArc(Brush.linearGradient(listOf(Color(0xFF83BA79), KoiColors.Lily)),
        22f, 335f, true, p - Offset(radius, radius), Size(radius * 2, radius * 2))
    repeat(8) { i ->
        val angle = (i * 45 + 30) * Math.PI / 180
        drawLine(Color(0xFFC5E3AA).copy(alpha = .32f), p,
            p + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (radius * .91f), .6f)
    }
    drawCircle(Color(0xFFD0E6B7), 1.4f, p)
    if (flower) {
        repeat(8) { i ->
            rotate(i * 45f, p) {
                drawOval(Color(0xFFF9D8CD), p - Offset(3f, 12f), Size(6f, 14f))
            }
        }
        drawCircle(Color(0xFFEFC56C), 3.3f, p)
    }
}

private fun DrawScope.koi(p: Offset, length: Float, angle: Float, accent: Color, patterned: Boolean = true, swish: Float = 0f) {
    rotate(angle, p) {
        scale(length / 60f, length / 60f, pivot = p) {
            // Fish face upwards; curved tail and translucent fins suggest gentle movement.
            val body = Path().apply {
                moveTo(p.x, p.y - 30f)
                cubicTo(p.x - 16f, p.y - 27f, p.x - 12f, p.y + 11f, p.x + 3f, p.y + 30f)
                cubicTo(p.x + 17f, p.y + 9f, p.x + 13f, p.y - 27f, p.x, p.y - 30f)
                close()
            }
            val tail = Path().apply {
                moveTo(p.x + 3f, p.y + 25f)
                quadraticTo(p.x - 1f, p.y + 42f, p.x - 12f, p.y + 48f)
                quadraticTo(p.x + 3f, p.y + 49f, p.x + 7f, p.y + 38f)
                quadraticTo(p.x + 15f, p.y + 47f, p.x + 20f, p.y + 43f)
                quadraticTo(p.x + 10f, p.y + 33f, p.x + 3f, p.y + 25f)
                close()
            }
            rotate(swish * 18f, p + Offset(3f, 25f)) {
                drawPath(tail, Color(0xFFF6F3DA).copy(alpha = .85f))
            }
            val fins = Path().apply {
                moveTo(p.x - 8f, p.y - 7f)
                quadraticTo(p.x - 28f, p.y - 2f, p.x - 17f, p.y + 10f)
                lineTo(p.x - 6f, p.y + 5f)
                moveTo(p.x + 8f, p.y - 7f)
                quadraticTo(p.x + 28f, p.y - 2f, p.x + 17f, p.y + 10f)
                lineTo(p.x + 6f, p.y + 5f)
            }
            scale(1f + swish * .10f, 1f, pivot = p) {
                drawPath(fins, Color(0xFFF6F3DA).copy(alpha = .65f))
            }
            drawPath(body, if (patterned) KoiColors.Surface else accent)
            if (patterned) {
                drawOval(accent, p + Offset(-8f, -24f), Size(14f, 19f))
                drawOval(accent, p + Offset(-4f, 3f), Size(11f, 17f))
                drawOval(KoiColors.Ink, p + Offset(-9f, -3f), Size(8f, 10f))
            }
            drawLine(Color.White.copy(alpha = .55f), p + Offset(1f, -23f), p + Offset(4f, 21f), 1.5f)
            drawCircle(KoiColors.Ink, 1.3f, p + Offset(-5f, -22f))
            drawCircle(KoiColors.Ink, 1.3f, p + Offset(5f, -22f))
        }
    }
}

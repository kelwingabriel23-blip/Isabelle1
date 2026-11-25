package com.isabelle.assistant.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING }

@Composable
fun Orb(state: OrbState, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val fast = state != OrbState.IDLE
    val rot by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(if (fast) 5000 else 14000, easing = LinearEasing)), label = "rot"
    )
    val pulse by t.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(if (fast) 650 else 2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Canvas(modifier.size(260.dp)) {
        val c = center
        val r = size.minDimension * 0.26f * pulse
        // brilho
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .30f), Color.Transparent), c, r * 2.4f), r * 2.4f, c)
        // esfera
        drawCircle(Brush.radialGradient(listOf(Color(0xFF0B2E52), Color(0xFF020B18)), c, r), r, c)
        drawCircle(Cyan, r, c, style = Stroke(2.dp.toPx()))
        // anéis orbitais
        rotate(rot, c) {
            drawOval(Cyan.copy(alpha = .55f), Offset(c.x - r * 1.6f, c.y - r * .45f), Size(r * 3.2f, r * .9f), style = Stroke(1.dp.toPx()))
        }
        rotate(-rot * .6f + 60f, c) {
            drawOval(Cyan.copy(alpha = .30f), Offset(c.x - r * 1.35f, c.y - r * .35f), Size(r * 2.7f, r * .7f), style = Stroke(1.dp.toPx()))
        }
        drawCircle(Cyan.copy(alpha = .22f), r * 1.4f, c, style = Stroke(1.dp.toPx()))
        // faixa de luz dentro da esfera
        rotate(rot * .5f, c) {
            drawArc(Cyan.copy(alpha = .8f), 200f, 110f, false, Offset(c.x - r * .8f, c.y - r * .8f), Size(r * 1.6f, r * 1.6f), style = Stroke(2.dp.toPx()))
        }
    }
}

/** Barrinhas de áudio do cartão "Estado da IA". */
@Composable
fun Waveform(active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "ph")
    Canvas(modifier) {
        val n = 34
        val gap = size.width / n
        for (i in 0 until n) {
            val amp = if (active) abs(sin(phase + i * .45f)) else 0.12f + 0.1f * abs(sin(i * .6f))
            val h = (size.height * (0.15f + 0.85f * amp)).coerceAtLeast(3f)
            val x = i * gap + gap / 2
            drawLine(Cyan.copy(alpha = .85f), Offset(x, (size.height - h) / 2), Offset(x, (size.height + h) / 2), strokeWidth = 2.dp.toPx())
        }
    }
}

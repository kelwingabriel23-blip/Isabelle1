package com.isabelle.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Bg = Color(0xFF020712)
val Cyan = Color(0xFF18C7FF)
val CyanSoft = Color(0xFF7FA8C9)
val Green = Color(0xFF19E6B5)

@Composable
fun IsabelleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan, onPrimary = Bg, background = Bg, surface = Bg,
            onBackground = Color.White, onSurface = Color.White,
            secondary = Green, outline = Cyan.copy(alpha = .4f)
        ),
        content = content
    )
}

/** Cartão de "vidro" azul neon, igual ao da imagem de referência. */
fun Modifier.glass(radius: Dp = 20.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(Color(0x2218C7FF), Color(0x0A18C7FF))))
        .border(1.dp, Cyan.copy(alpha = .45f), shape)
}

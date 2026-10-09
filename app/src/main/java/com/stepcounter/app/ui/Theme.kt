package com.stepcounter.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Pal {
    val Coral = Color(0xFFFF6B4A)
    val Amber = Color(0xFFFFB347)
    val Mint = Color(0xFF2DBE8C)
    val Ink = Color(0xFF1E2A2F)
    val InkSoft = Color(0xFF6B7780)
    val BgTop = Color(0xFFFBF4EC)
    val BgBottom = Color(0xFFEAF3EE)
}

@Composable
fun StepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pal.Coral,
            onPrimary = Color.White,
            background = Pal.BgTop,
            surface = Color(0xFFFFFBF7),
            onSurface = Pal.Ink,
            onBackground = Pal.Ink
        ),
        content = content
    )
}

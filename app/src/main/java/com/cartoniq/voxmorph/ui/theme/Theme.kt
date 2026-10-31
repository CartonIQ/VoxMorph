package com.cartoniq.voxmorph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Neon {
    val Bg = Color(0xFF140B2E)
    val Surface = Color(0xFF24164A)
    val SurfaceHi = Color(0xFF35226B)
    val Cyan = Color(0xFF00E5FF)
    val Purple = Color(0xFFB388FF)
    val Muted = Color(0xFFAAB6D3)
    val Border = Color(0xFF2B5BFF)
    val Green = Color(0xFF00E676)
    val Danger = Color(0xFFFF5470)
    val Gradient = Brush.horizontalGradient(listOf(Cyan, Purple))
    val GradientSoft = Brush.linearGradient(listOf(Color(0xFF2D1B69), Color(0xFF110B29)))
}

@Composable
fun VoxMorphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Cyan, onPrimary = Color.Black,
            secondary = Neon.Purple, onSecondary = Color.Black,
            background = Neon.Bg, onBackground = Color.White,
            surface = Neon.Surface, onSurface = Color.White,
            surfaceVariant = Neon.SurfaceHi, onSurfaceVariant = Neon.Muted,
            error = Neon.Danger
        ),
        typography = Typography(),
        content = content
    )
}

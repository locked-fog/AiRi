package com.lockedfog.airi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- 顶部控制栏颜色设定 ---
val ConsoleBg = Color(0xFF1E1E1E)
val ConsoleText = Color(0xFF00FF00)
val ConsoleWarn = Color(0xFFFFD700)

// --- 主界面颜色设定 ---
val PrimaryFog = Color(0xFF006C4C)
val OnPrimary = Color(0xFFBEDED0)
val Background = Color(0xFFFBFDF9)
val SurfaceVariant = Color(0xFFB4E3B7)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryFog,
    onPrimary = OnPrimary,
    background = Background,
    surface = Background,
    surfaceVariant = SurfaceVariant,
    secondary = Color(0xFF4C6358),
    tertiary = Color(0xFF3E6373)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF55DDAF),
    onPrimary = Color(0xFF003826),
    background = Color(0xFF191C1A),
    surface = Color(0xFF191C1A)
)

@Composable
fun AiriTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}

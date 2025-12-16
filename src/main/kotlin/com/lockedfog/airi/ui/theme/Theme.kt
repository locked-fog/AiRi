package com.lockedfog.airi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lockedfog.airi.data.config.ThemePreset

// 定义简单的字体排版 (使用默认值)
val Typography = Typography()

/**
 * 动态生成配色方案
 */
@Suppress("MagicNumber")
private fun createScheme(preset: ThemePreset, isDark: Boolean) = if (isDark) {
    darkColorScheme(
        primary = Color(preset.colorHex),
        onPrimary = Color.Black, // 深色模式下，主色通常较亮，文字用黑色
        primaryContainer = Color(preset.colorHex).copy(alpha = 0.3f),
        onPrimaryContainer = Color(preset.colorHex),
        secondary = Color(preset.colorHex),
        // 深色背景配置
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        surfaceVariant = Color(0xFF2C2C2C), // 侧边栏颜色
        onSurface = Color(0xFFE6E1E5)
    )
} else {
    lightColorScheme(
        primary = Color(preset.colorHex),
        onPrimary = Color.White, // 浅色模式下，主色通常较深，文字用白色
        primaryContainer = Color(preset.colorHex).copy(alpha = 0.1f),
        onPrimaryContainer = Color(preset.colorHex),
        secondary = Color(preset.colorHex),
        // 浅色背景配置
        background = Color(0xFFFBFFEF), // 微微带点暖色或纯白
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF0F0F0), // 侧边栏颜色
        onSurface = Color(0xFF1C1B1F)
    )
}

@Composable
fun AiriTheme(
    // 接收来自 AppConfig 的配置
    themePreset: ThemePreset = ThemePreset.FogGreen,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = createScheme(themePreset, darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

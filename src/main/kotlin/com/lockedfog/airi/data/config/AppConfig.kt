package com.lockedfog.airi.data.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val llm: LlmConfig = LlmConfig(),
    val ui: UiConfig = UiConfig(),
    // 预留给未来插件的开关配置
    val plugins: Map<String, Boolean> = emptyMap()
)

@Serializable
data class LlmConfig(
    val baseUrl: String = "https://api.siliconflow.cn/v1", // 默认指向硅基流动
    val apiKey: String = "",
    val mainModel: String = "deepseek-ai/DeepSeek-V3.2", // 默认主模型
    val summaryModel: String = "Qwen/Qwen3-Next-80B-A3B-Instruct"
)

@Serializable
data class UiConfig(
    val isDarkMode: Boolean = false,
    val themeColor: ThemePreset = ThemePreset.FogGreen
)

@Serializable
@Suppress("MagicNumber")
enum class ThemePreset(val colorHex: Long) {
    FogGreen(0xFF006C4C),
    CyberBlue(0xFF007BFF),
    SakuraPink(0xFFFFC0CB)
}

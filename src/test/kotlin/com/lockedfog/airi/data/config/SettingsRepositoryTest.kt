package com.lockedfog.airi.data.config

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryTest {

    private val testDir = File("build/tmp/repo_test")
    private val configFile = File(testDir, "config.json")

    @Before
    fun setup() {
        testDir.deleteRecursively()
        testDir.mkdirs()
        SettingsRepository.configDir = testDir
        // 重置单例状态
        configFile.delete()
        SettingsRepository.loadConfig()
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun `should save and load valid config`() {
        val newConfig = AppConfig(
            llm = LlmConfig(apiKey = "sk-valid"),
            ui = UiConfig(isDarkMode = true)
        )

        SettingsRepository.saveConfig(newConfig)
        val loaded = SettingsRepository.loadConfig()

        assertEquals("sk-valid", loaded.llm.apiKey)
        assertTrue(loaded.ui.isDarkMode)
        assertFalse(SettingsRepository.isFirstRun())
    }

    @Test
    fun `should identify first run`() {
        SettingsRepository.saveConfig(AppConfig(llm = LlmConfig(apiKey = "")))
        assertTrue(SettingsRepository.isFirstRun())
    }

    @Test
    fun `should handle missing file by returning default`() {
        configFile.delete()
        val config = SettingsRepository.loadConfig()
        assertEquals("", config.llm.apiKey) // 默认值
    }

    @Test
    fun `should handle corrupted JSON`() {
        configFile.writeText("{ \"llm\": { broken... ")
        val config = SettingsRepository.loadConfig()
        assertEquals("", config.llm.apiKey) // 降级为默认
    }

    @Test
    fun `should handle invalid enum value`() {
        configFile.writeText("""{ "ui": { "themeColor": "INVALID_COLOR" } }""")
        val config = SettingsRepository.loadConfig()
        assertEquals(ThemePreset.FogGreen, config.ui.themeColor) // 降级为默认枚举
    }

    @Test
    fun `should throw exception on save failure`() {
        // 让 configDir 变成一个文件，导致 mkdirs 失败或无法写入
        testDir.deleteRecursively()
        testDir.createNewFile()

        assertFailsWith<IOException> {
            SettingsRepository.saveConfig(AppConfig())
        }
    }
}

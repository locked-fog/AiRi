// src/test/kotlin/com/lockedfog/airi/data/config/SettingsRepositoryTest.kt
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

    // [New] 现在我们需要持有 Repository 的实例
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        testDir.deleteRecursively()
        testDir.mkdirs()

        // [New] 实例化 Repository，注入测试目录
        repository = SettingsRepository(configDir = testDir)

        // 确保环境干净
        if (configFile.exists()) configFile.delete()
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

        // [Update] 使用实例方法
        repository.saveConfig(newConfig)

        // 重新加载以验证持久化
        // 为了模拟真实场景，我们可以新建一个指向相同目录的 Repository 实例
        val newRepoInstance = SettingsRepository(configDir = testDir)
        val loaded = newRepoInstance.loadConfig()

        assertEquals("sk-valid", loaded.llm.apiKey)
        assertTrue(loaded.ui.isDarkMode)
        assertFalse(newRepoInstance.isFirstRun())
    }

    @Test
    fun `should identify first run`() {
        repository.saveConfig(AppConfig(llm = LlmConfig(apiKey = "")))
        assertTrue(repository.isFirstRun())
    }

    @Test
    fun `should handle missing file by returning default`() {
        configFile.delete()
        // 重新加载
        val config = repository.loadConfig()
        assertEquals("", config.llm.apiKey) // 默认值
    }

    @Test
    fun `should handle corrupted JSON`() {
        configFile.writeText("{ \"llm\": { broken... ")
        // 强制重新读取
        val config = repository.loadConfig()
        assertEquals("", config.llm.apiKey) // 降级为默认
    }

    @Test
    fun `should handle invalid enum value`() {
        configFile.writeText("""{ "ui": { "themeColor": "INVALID_COLOR" } }""")
        val config = repository.loadConfig()
        assertEquals(ThemePreset.FogGreen, config.ui.themeColor) // 降级为默认枚举
    }

    @Test
    fun `should throw exception on save failure`() {
        // 让 configDir 变成一个文件，导致 mkdirs 失败或无法写入
        testDir.deleteRecursively()
        testDir.createNewFile()

        // 需要重新创建一个指向该“坏目录”的 repo 实例，或者确保现有实例操作时会触发错误
        // 由于 configDir 是构造参数传入的，我们用一个新的“坏”实例来测试
        val badRepo = SettingsRepository(configDir = testDir)

        assertFailsWith<IOException> {
            badRepo.saveConfig(AppConfig())
        }
    }
}

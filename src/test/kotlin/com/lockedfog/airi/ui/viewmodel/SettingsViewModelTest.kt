package com.lockedfog.airi.ui.viewmodel

import com.lockedfog.airi.data.config.AppConfig
import com.lockedfog.airi.data.config.LlmConfig
import com.lockedfog.airi.data.config.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var mockWebServer: MockWebServer

    // 使用 UnconfinedTestDispatcher 确保协程在测试线程立即执行，消除竞态条件
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // 1. 设置 Repository 环境 (使用临时目录)
        SettingsRepository.configDir = File("build/tmp/vm_test")
        SettingsRepository.configDir.deleteRecursively() // 确保干净
        SettingsRepository.configDir.mkdirs()
        SettingsRepository.saveConfig(AppConfig())

        // 2. 启动 MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // 3. 初始化 ViewModel (注入 testDispatcher)
        val scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        viewModel = SettingsViewModel(scope, SettingsRepository, ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        SettingsRepository.configDir.deleteRecursively()
    }

    // --- Happy Path ---

    @Test
    fun `testConnection should succeed and parse models when API returns 200`() = runTest {
        // Arrange: 模拟标准 OpenAI 响应
        val successJson = """
            {
              "object": "list",
              "data": [
                { "id": "deepseek-v3", "object": "model" },
                { "id": "qwen-2.5", "object": "model" }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(successJson).setResponseCode(200))

        val mockUrl = mockWebServer.url("/v1").toString()
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = mockUrl, apiKey = "sk-any")
            )
        )

        // Act
        viewModel.testConnection()

        // Assert
        val state = viewModel.state.value
        assertTrue(state.connectionSuccess, "Should be successful")
        assertEquals(null, state.connectionError)
        assertEquals(2, state.availableModels.size)
        assertEquals("deepseek-v3", state.availableModels[0])
    }

    // --- Edge Cases & Fault Injection ---

    @Test
    fun `testConnection should handle Empty Model List (Valid JSON but empty)`() = runTest {
        // Arrange: JSON 合法，但 data 为空
        val emptyJson = """{ "object": "list", "data": [] }"""
        mockWebServer.enqueue(MockResponse().setBody(emptyJson).setResponseCode(200))

        val mockUrl = mockWebServer.url("/v1").toString()
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = mockUrl, apiKey = "sk-any")
            )
        )

        // Act
        viewModel.testConnection()

        // Assert
        val state = viewModel.state.value
        assertFalse(state.connectionSuccess, "Should not be successful if no models found")
        assertNotNull(state.connectionError)
        assertTrue(state.connectionError!!.contains("found no models"))
    }

    @Test
    fun `testConnection should handle Bad JSON (SerializationException)`() = runTest {
        // Arrange: JSON 结构错误 (缺少闭合括号)
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{ \"data\": [ broken..."))

        val mockUrl = mockWebServer.url("/v1").toString()
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = mockUrl, apiKey = "sk-any")
            )
        )

        // Act
        viewModel.testConnection()

        // Assert
        val state = viewModel.state.value
        assertFalse(state.connectionSuccess, "Success should be false for bad JSON")
        assertNotNull(state.connectionError)
        // 验证这是刚才修复的 Bug：应该报解析错误，而不是 "found no models"
        assertTrue(state.connectionError!!.contains("Failed to parse"))
    }

    @Test
    fun `testConnection should handle HTTP 401 Unauthorized`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Invalid Key"))

        val mockUrl = mockWebServer.url("/v1").toString()
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = mockUrl, apiKey = "sk-wrong")
            )
        )

        viewModel.testConnection()

        val state = viewModel.state.value
        assertFalse(state.connectionSuccess)
        assertNotNull(state.connectionError)
        assertTrue(state.connectionError!!.contains("HTTP 401"))
    }

    @Test
    fun `testConnection should handle Network Error (IOException)`() = runTest {
        // Arrange: 关掉服务器制造连接拒绝
        mockWebServer.shutdown()
        val mockUrl = "http://localhost:${mockWebServer.port}/v1"

        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = mockUrl, apiKey = "sk-any")
            )
        )

        viewModel.testConnection()

        val state = viewModel.state.value
        assertFalse(state.connectionSuccess)
        assertNotNull(state.connectionError)
        assertTrue(state.connectionError!!.contains("Network Error"))
    }

    @Test
    fun `testConnection should handle Invalid URL (IllegalArgumentException)`() = runTest {
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(baseUrl = "htp://bad-scheme", apiKey = "sk-any")
            )
        )

        viewModel.testConnection()

        val state = viewModel.state.value
        assertFalse(state.connectionSuccess)
        assertTrue(state.connectionError!!.contains("Invalid URL"))
    }

    @Test
    fun `should block connection test if API key is empty`() = runTest {
        viewModel.updateConfig(
            viewModel.state.value.config.copy(
                llm = LlmConfig(apiKey = "")
            )
        )

        viewModel.testConnection()

        assertEquals(0, mockWebServer.requestCount)
        assertTrue(viewModel.state.value.connectionError!!.contains("API Key cannot be empty"))
    }
}

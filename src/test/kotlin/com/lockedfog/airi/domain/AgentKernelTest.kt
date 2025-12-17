package com.lockedfog.airi.domain

import com.lockedfog.airi.data.config.AppConfig
import com.lockedfog.airi.data.config.LlmConfig
import com.lockedfog.airi.data.config.SettingsRepository
import dev.lockedfog.streamllm.StreamLLM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AgentKernelTest {

    private lateinit var testDir: File
    private lateinit var repository: SettingsRepository

    // 使用 StandardTestDispatcher，让我们能通过 advanceUntilIdle() 控制时间
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // 1. 设置文件环境
        testDir = File("build/tmp/kernel_test")
        testDir.deleteRecursively()
        testDir.mkdirs()
        repository = SettingsRepository(configDir = testDir)

        // 2. [关键] 劫持 StreamLLM 单例
        mockkObject(StreamLLM)

        // 3. 定义行为：当调用 init 或 close 时，什么都不做 (just Runs)
        // 注意：这里使用 allAny() 是一种简便写法，但在 verify 中最好显式指定
        every {
            StreamLLM.init(
                baseUrl = any(),
                apiKey = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )
        } just Runs

        every { StreamLLM.close() } just Runs
    }

    @After
    fun tearDown() {
        // [关键] 测试结束后，释放单例，恢复原状，避免影响其他测试
        unmockkObject(StreamLLM)
        testDir.deleteRecursively()
    }

    @Test
    fun `should connect to StreamLLM when valid config is emitted`() = runTest(testDispatcher) {
        // Arrange
        // 注意：这里的 AgentKernel 必须是我们修改过、支持注入 dispatcher 的版本
        @Suppress("UnusedPrivateProperty")
        val kernel = AgentKernel(repository, testDispatcher)

        val validConfig = AppConfig(
            llm = LlmConfig(
                baseUrl = "https://api.deepseek.com",
                apiKey = "sk-test-123",
                mainModel = "deepseek-coder"
            )
        )

        // Act
        repository.saveConfig(validConfig)

        // 让协程把任务跑完
        advanceUntilIdle()

        // Assert
        // 验证 StreamLLM.init 是否被调用，且参数正确
        // 我们使用了 any() 来忽略那些 AgentKernel 没有显式传递（使用默认值）的参数
        verify(exactly = 1) {
            StreamLLM.init(
                baseUrl = "https://api.deepseek.com",
                apiKey = "sk-test-123",
                modelName = "deepseek-coder",
                timeoutSeconds = 60,
                storage = any(),
                maxMemoryCount = any()
            )
        }
    }

    @Test
    fun `should close previous connection before initializing new one`() = runTest(testDispatcher) {
        // Arrange
        @Suppress("UnusedPrivateProperty")
        val kernel = AgentKernel(repository, testDispatcher)

        // Act
        // 第一次配置
        repository.saveConfig(AppConfig(llm = LlmConfig(apiKey = "sk-1", baseUrl = "url", mainModel = "model")))
        advanceUntilIdle()

        // 第二次配置
        repository.saveConfig(AppConfig(llm = LlmConfig(apiKey = "sk-2", baseUrl = "url", mainModel = "model")))
        advanceUntilIdle()

        // Assert
        // 验证 verifyOrder：确保是先 Close 再 Init
        verifyOrder {
            // 第一次初始化
            StreamLLM.close()
            StreamLLM.init(
                apiKey = "sk-1",
                baseUrl = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )

            // 第二次初始化
            StreamLLM.close()
            StreamLLM.init(
                apiKey = "sk-2",
                baseUrl = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )
        }
    }

    @Test
    fun `should NOT connect if API key is missing`() = runTest(testDispatcher) {
        // Arrange
        @Suppress("UnusedPrivateProperty")
        val kernel = AgentKernel(repository, testDispatcher)

        val invalidConfig = AppConfig(
            llm = LlmConfig(
                baseUrl = "https://api.example.com",
                apiKey = "" // 空 Key
            )
        )

        // Act
        repository.saveConfig(invalidConfig)
        advanceUntilIdle()

        // Assert
        // 验证 StreamLLM.init 从未被调用
        verify(exactly = 0) {
            StreamLLM.init(
                apiKey = any(),
                baseUrl = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )
        }
    }

    @Test
    fun `should handle StreamLLM exceptions gracefully`() = runTest(testDispatcher) {
        // Arrange
        @Suppress("UnusedPrivateProperty")
        val kernel = AgentKernel(repository, testDispatcher)

        // 模拟 init 抛出异常 (比如参数错误)
        every {
            StreamLLM.init(
                apiKey = any(),
                baseUrl = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )
        } throws IllegalArgumentException("Bad URL")

        val config = AppConfig(llm = LlmConfig(apiKey = "sk-crash", baseUrl = "bad-url", mainModel = "model"))

        // Act
        repository.saveConfig(config)
        advanceUntilIdle()

        // Assert
        // 测试不应该崩溃 (红色)，而是通过日志记录错误。
        // 我们验证 init 确实被尝试调用了
        verify {
            StreamLLM.init(
                apiKey = "sk-crash",
                baseUrl = any(),
                modelName = any(),
                timeoutSeconds = any(),
                storage = any(),
                maxMemoryCount = any()
            )
        }
    }
}

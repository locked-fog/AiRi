package com.lockedfog.airi.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.status.ErrorStatus
import com.lockedfog.airi.data.log.LogBuffer
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import java.time.DateTimeException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeAppenderTest {

    private lateinit var appender: ComposeAppender
    private lateinit var loggerContext: LoggerContext

    @Before
    fun setup() {
        LogBuffer.logs.clear()

        // 1. 初始化 Logback 上下文 (用于捕获 addError 产生的内部状态)
        loggerContext = LoggerContext()

        appender = ComposeAppender()
        appender.context = loggerContext // 注入上下文，否则 reportError 无效
        appender.start()
    }

    @Test
    fun `should handle DateTimeException and use fallback`() {
        // Arrange
        val mockEvent = mockk<ILoggingEvent>()
        // 模拟 timeFormatter.format 内部可能调用的方法抛出异常
        // 注意：直接 mock Instant.ofEpochMilli 比较难，我们这里通过 mock event.timeStamp
        // 并配合一个总是抛出异常的场景，或者更简单的：
        // 我们可以 mock timeFormatter? 不行，它是私有的。
        // 策略：我们利用 MockK 强制让 timeStamp 获取时产生异常，
        // 或者如果你的代码是 Instant.ofEpochMilli(event.timeStamp)，
        // 我们可以传入一个会导致 Instant 溢出的值，或者直接 Mock 整个逻辑流程比较困难。

        // 更直接的测试策略：利用 MockK 对 event.timeStamp 的调用抛出异常
        // 虽然真实场景下 timeStamp 是个 long 很难抛异常，但为了测试 catch 块，我们强制抛出
        every { mockEvent.timeStamp } throws DateTimeException("Mock Time Error")
        every { mockEvent.level } returns Level.INFO
        every { mockEvent.formattedMessage } returns "Time Failed Message"

        // Act
        appender.doAppend(mockEvent)

        // Assert 1: 验证日志是否进入了 Buffer (Fallback 机制)
        assertEquals(1, LogBuffer.logs.size)
        val log = LogBuffer.logs.first()
        assertContains(log, "[TIME_ERR]") // 验证使用了 fallback 标签
        assertContains(log, "Time Failed Message") // 验证原始消息被保留

        // Assert 2: 验证 Logback 内部收到了错误报告
        val statusList = loggerContext.statusManager.copyOfStatusList
        assertTrue(statusList.any { it is ErrorStatus && it.message.contains("Time formatting failed") })
    }

    @Test
    fun `should handle IndexOutOfBoundsException and use fallback`() {
        // Arrange
        val mockEvent = mockk<ILoggingEvent>()
        every { mockEvent.timeStamp } returns System.currentTimeMillis()
        every { mockEvent.level } returns Level.INFO
        // 模拟 substringAfterLast 抛出异常 (虽然标准库很少抛，但为了测试 catch 块)
        every { mockEvent.loggerName } throws IndexOutOfBoundsException("Mock String Error")
        every { mockEvent.formattedMessage } returns "String Failed Message"

        // Act
        appender.doAppend(mockEvent)

        // Assert 1: 验证日志是否进入了 Buffer (Fallback 机制)
        assertEquals(1, LogBuffer.logs.size)
        val log = LogBuffer.logs.first()
        assertContains(log, "[FORMAT_ERR]") // 验证使用了 fallback 标签
        assertContains(log, "String Failed Message") // 验证原始消息被保留

        // Assert 2: 验证 Logback 内部收到了错误报告
        val statusList = loggerContext.statusManager.copyOfStatusList
        assertTrue(statusList.any { it is ErrorStatus && it.message.contains("String manipulation failed") })
    }

    @Test
    fun `should handle double fault in fallback mechanism`() {
        // 极端测试：当 Fallback 逻辑也失败时
        // Arrange
        val mockEvent = mockk<ILoggingEvent>()
        every { mockEvent.timeStamp } throws DateTimeException("Trigger Fallback")
        // 在 safelyAppendFallback 中会调用 formattedMessage，我们让它也抛异常
        every { mockEvent.formattedMessage } throws RuntimeException("Fallback Crash")
        every { mockEvent.level } returns Level.INFO

        // Act
        appender.doAppend(mockEvent)

        // Assert
        // Buffer 为空
        assertEquals(0, LogBuffer.logs.size)

        // Logback 应该收到两条错误：一条是最初的 Time Error，一条是 Fallback Error
        val statusList = loggerContext.statusManager.copyOfStatusList
        assertTrue(statusList.any { it.message.contains("Time formatting failed") })
        assertTrue(statusList.any { it.message.contains("Failed to append fallback log") })
    }
}

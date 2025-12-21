package com.lockedfog.airi.utils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.lockedfog.airi.data.log.LogBuffer
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 将 Logback 事件桥接到 Compose UI 的 Appender。
 * 实现了健壮的异常处理，确保日志系统自身不会导致应用崩溃。
 */
class ComposeAppender : AppenderBase<ILoggingEvent>() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    override fun append(eventObject: ILoggingEvent) {
        try {
            // 1. 尝试正常格式化时间
            val timestamp = timeFormatter.format(Instant.ofEpochMilli(eventObject.timeStamp))

            // 2. 尝试获取级别和Logger名
            val level = eventObject.level.toString()
            val loggerName = eventObject.loggerName.substringAfterLast('.')

            // 3. 获取消息主体
            val message = eventObject.formattedMessage

            // 4. 组装并推送到 UI 缓冲区
            val formattedLog = "[$timestamp][$level][$loggerName] $message"
            LogBuffer.add(formattedLog)
        } catch (e: DateTimeException) {
            // 处理时间格式化异常：使用 fallback 时间戳，不丢弃日志
            reportError("Time formatting failed for log event", e)
            safelyAppendFallback(eventObject, "TIME_ERR")
        } catch (e: IndexOutOfBoundsException) {
            reportError("String manipulation failed", e)
            safelyAppendFallback(eventObject, "FORMAT_ERR")
        }
    }

    /**
     * 当正常格式化失败时，尝试以最原始的方式记录日志，确保信息不丢失。
     */
    private fun safelyAppendFallback(eventObject: ILoggingEvent, errorTag: String) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val fallbackLog = "[$errorTag][${eventObject.level}] ${eventObject.formattedMessage}"
            LogBuffer.add(fallbackLog)
        } catch (e: Exception) {
            addError("Failed to append fallback log", e)
            throw e
        }
    }

    /**
     * 封装 Logback 的内部错误报告机制。
     */
    private fun reportError(msg: String, t: Throwable) {
        // addError 是 AppenderBase 提供的方法，会将错误记录到 Logback 的内部 StatusManager
        // 可以通过 StatusPrinter 打印，或者在 XML 配置中开启 debug="true" 查看
        addError(msg, t)
    }
}

package com.lockedfog.airi.data.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogBufferTest {

    @Before
    fun setup() {
        LogBuffer.logs.clear()
    }

    @Test
    fun `should add logs correctly`() {
        LogBuffer.add("Test log 1")
        LogBuffer.add("Test log 2")

        assertEquals(2, LogBuffer.logs.size)
        assertEquals("Test log 1", LogBuffer.logs[0])
        assertEquals("Test log 2", LogBuffer.logs[1])
    }

    @Test
    fun `should respect max capacity (Rolling Buffer)`() {
        // 假设 MAX_LOGS = 500。我们要验证第 501 条是否挤掉了第 1 条。
        val maxLogs = 500

        // 插入 505 条日志
        repeat(maxLogs + 5) { i ->
            LogBuffer.add("Log #$i")
        }

        // 验证：总数锁定在 500
        assertEquals(maxLogs, LogBuffer.logs.size)

        // 验证：第一条应该是 "Log #5" (因为 0-4 被挤出了)
        assertTrue(LogBuffer.logs.first().contains("Log #5"))
        // 验证：最后一条应该是 "Log #504"
        assertTrue(LogBuffer.logs.last().contains("Log #504"))
    }

    @Test
    fun `should handle concurrent writes robustly`() = runTest {
        // 模拟高并发场景：10个协程同时疯狂写入
        withContext(Dispatchers.Default) {
            val jobs = List(10) { id ->
                launch {
                    repeat(100) { i ->
                        LogBuffer.add("Thread-$id Log-$i")
                    }
                }
            }
            jobs.joinAll()
        }

        // 验证：没有抛出 ConcurrentModificationException，且总数被正确截断
        assertEquals(500, LogBuffer.logs.size)
    }
}

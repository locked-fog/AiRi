package com.lockedfog.airi.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class TopConsoleTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `should show summary only when collapsed`() {
        // Arrange
        val logs = listOf("[INFO] Log 1", "[INFO] Log 2", "[INFO] Log 3")

        // Act
        rule.setContent {
            TopConsole(
                logs = logs,
                isExpanded = false, // 收起状态
                onToggleExpand = {},
                windowHeight = 800
            )
        }

        // Assert
        // 应该能看到 status bar
        rule.onNodeWithText("AiRi CORE: ONLINE").assertIsDisplayed()

        // 收起时，应该只显示最后一条日志的摘要（部分内容）
        rule.onNodeWithText("[INFO] Log 3", substring = true).assertIsDisplayed()

        // 不应该看到完整列表中的第一条 (LazyColumn 应该不可见/高度极小)
        // 注意：Compose测试中，如果组件不在屏幕范围内或被遮挡，assertIsDisplayed可能会失败，
        // 但更好的验证方式是检查节点是否存在。在我们的实现中，收起时 LazyColumn 不会被渲染。
        rule.onNodeWithText("[INFO] Log 1").assertDoesNotExist()
    }

    @Test
    fun `should show full list when expanded`() {
        // Arrange
        val logs = listOf("[INFO] Log 1", "[INFO] Log 2")

        // Act
        rule.setContent {
            TopConsole(
                logs = logs,
                isExpanded = true, // 展开状态
                onToggleExpand = {},
                windowHeight = 800 // 模拟窗口高度
            )
        }

        // Assert
        // 所有日志都应该可见
        rule.onNodeWithText("[INFO] Log 1").assertIsDisplayed()
        rule.onNodeWithText("[INFO] Log 2").assertIsDisplayed()
    }

    @Test
    fun `should trigger toggle callback on click`() {
        var clicked = false

        rule.setContent {
            TopConsole(
                logs = emptyList(),
                isExpanded = false,
                onToggleExpand = { clicked = true },
                windowHeight = 800
            )
        }

        // 点击状态栏
        rule.onNodeWithText("AiRi CORE: ONLINE").performClick()

        // 验证回调是否触发
        assert(clicked)
    }

    @Test
    fun `console should handle empty logs when collapsed`() {
        // 目标：覆盖 if (!isExpanded && logs.isNotEmpty()) 中 logs.isNotEmpty() 为 false 的分支
        rule.setContent {
            TopConsole(
                logs = emptyList(), // 空日志
                isExpanded = false, // 收起状态
                onToggleExpand = {},
                windowHeight = 800
            )
        }

        // 验证 1: 状态栏依然显示
        rule.onNodeWithText("AiRi CORE: ONLINE").assertIsDisplayed()

        // 验证 2: 绝对不应显示任何日志摘要 (因为是空的)
        // 我们可以通过查找任何常见的日志字符来反向验证
        rule.onNodeWithText("[", substring = true).assertDoesNotExist()
    }

    @Test
    fun `console should handle empty logs when expanded`() {
        // 目标：覆盖 LaunchedEffect(logs.size) 中 if (logs.isNotEmpty() && isExpanded) 的 false 分支
        // 以及覆盖 LazyColumn 渲染空列表的情况
        rule.setContent {
            TopConsole(
                logs = emptyList(), // 空日志
                isExpanded = true, // 展开状态
                onToggleExpand = {},
                windowHeight = 800
            )
        }

        // 验证: 列表容器应该存在（因为高度展开了），但里面没有内容
        // 这里主要为了跑通代码路径，确保不会因为空列表崩溃，且逻辑判断正确
        rule.onNodeWithText("AiRi CORE: ONLINE").assertIsDisplayed()
        rule.onNodeWithText("[", substring = true).assertDoesNotExist()
    }
}

package com.lockedfog.airi.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val VISIBLE_WHEN_NOT_EXPANDED = 60
private val ConsoleBg = Color(0xFF1E1E1E)
private val ConsoleText = Color(0xFF00FF41)

/**
 * 潜意识控制台 (The Subconscious Console)
 * @param logs 日志列表
 * @param isExpanded 是否展开
 * @param onToggleExpand 切换展开状态的回调
 * @param windowHeight 当前窗口总高度，用于计算半屏高度
 */
@Composable
fun TopConsole(
    logs: List<String>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    windowHeight: Int
) {
    // 动画高度：展开时为窗口一半，收起时仅保留状态栏高度 (40dp)
    val targetHeight = if (isExpanded) (windowHeight / 2).dp else 40.dp
    val animatedHeight by animateDpAsState(targetHeight)

    val listState = rememberLazyListState()

    // 自动滚动到最新日志
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .background(ConsoleBg)
    ) {
        // --- 1. Status Bar (Always Visible) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { onToggleExpand() } // 点击整条均可切换
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧状态指示
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 呼吸灯模拟 (简单圆点)
                Box(
                    modifier = Modifier.size(
                        8.dp
                    ).background(Color.Green, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AiRi CORE: ONLINE",
                    color = ConsoleText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                // 收起状态下显示最新一条日志摘要
                if (!isExpanded && logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = logs.last().take(VISIBLE_WHEN_NOT_EXPANDED), // 截取前60字符
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // 右侧箭头
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        // --- 2. Log Content (Visible when expanded) ---
        // 只有高度足够时才渲染列表，节省性能
        if (animatedHeight > 40.dp) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = ConsoleText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

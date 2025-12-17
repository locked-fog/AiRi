package com.lockedfog.airi.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lockedfog.airi.data.config.SettingsRepository
import com.lockedfog.airi.data.log.LogBuffer
import com.lockedfog.airi.ui.components.TopConsole
import com.lockedfog.airi.ui.screen.SettingsDialog
import com.lockedfog.airi.ui.theme.AiriTheme
import com.lockedfog.airi.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
@Preview
fun App() {
    val koin = GlobalContext.get()

    val settingsRepository = remember { koin.get<SettingsRepository>() }
    val appConfig by settingsRepository.configFlow.collectAsState()

    val scope = rememberCoroutineScope()
    val settingsViewModel = remember {
        koin.get<SettingsViewModel> { parametersOf(scope) }
    }

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (settingsRepository.isFirstRun()) {
            @Suppress("MagicNumber")
            delay(300) // 稍作延迟等待界面渲染平稳
            showSettings = true
        }
    }

    // [核心] 将配置传给 Theme
    AiriTheme(
        themePreset = appConfig.ui.themeColor,
        darkTheme = appConfig.ui.isDarkMode
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- 主布局 (左右分栏) ---
            Row(modifier = Modifier.fillMaxSize()) {
                // [区域 A] 侧边栏 (ChatGPT Style)
                NavigationRail(
                    // 使用 surfaceVariant 作为背景，在深/浅色模式下都会有区分度
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.width(72.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    // 顶部：聊天入口
                    NavigationRailItem(
                        selected = true,
                        onClick = { /* TODO: Switch to Chat View */ },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") }
                    )

                    Spacer(Modifier.weight(1f)) // 撑开空间

                    // 底部：设置入口
                    NavigationRailItem(
                        selected = showSettings, // 如果弹窗打开，高亮此项
                        onClick = { showSettings = true },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Config") }
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // [区域 B] 内容区 (潜意识终端 + 表意识聊天)
                Column(modifier = Modifier.weight(1f)) {
                    // 1. 顶部潜意识终端 (TopConsole)
                    var logs by remember { mutableStateOf(LogBuffer.logs.toList()) }
                    var isConsoleExpanded by remember { mutableStateOf(false) }

                    // 定时刷新日志
                    LaunchedEffect(Unit) {
                        while (true) {
                            @Suppress("MagicNumber")
                            delay(500)
                            if (LogBuffer.logs.size != logs.size) {
                                logs = LogBuffer.logs.toList()
                            }
                        }
                    }

                    TopConsole(
                        logs = logs,
                        isExpanded = isConsoleExpanded,
                        onToggleExpand = { isConsoleExpanded = !isConsoleExpanded },
                        windowHeight = 800
                    )

                    // 2. 底部聊天占位符
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "AiRi Conscious System",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Waiting for Input...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // --- 弹窗层 ---
            if (showSettings) {
                SettingsDialog(
                    viewModel = settingsViewModel,
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

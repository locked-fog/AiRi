package com.lockedfog.airi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lockedfog.airi.data.log.LogBuffer
import com.lockedfog.airi.ui.components.TopConsole
import com.lockedfog.airi.ui.theme.AiriTheme
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AiRi.System")

@Composable
fun App() {
    val logs = LogBuffer.logs

    var isConsoleExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logger.info("Neural Link Established.")
        logger.info("Core Systems Online")
        logger.info("Hello, world!")
        logger.debug("Debug channel active")
    }

    AiriTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowHeight = maxHeight.value.toInt()

            Column(modifier = Modifier.fillMaxSize()) {
                // 1. 潜意识 (Top Console)
                TopConsole(
                    logs = logs,
                    isExpanded = isConsoleExpanded,
                    onToggleExpand = { isConsoleExpanded = !isConsoleExpanded },
                    windowHeight = windowHeight
                )

                // 2. 表意识 (Main Chat Area)
                // 剩余空间自动填充
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    // TODO: Phase 1.5 - 实现 ChatList
                    Text(
                        text = "AiRi Conscious Interface\n(Waiting for User Input...)",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

package com.lockedfog.airi.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lockedfog.airi.data.config.AppConfig
import com.lockedfog.airi.data.config.ThemePreset
import com.lockedfog.airi.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    // 使用 Dialog 组件创建一个模态窗口
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.size(width = 800.dp, height = 600.dp) // 固定大小的大弹窗
        ) {
            SettingsScreen(viewModel, onDismiss)
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("LLM Configuration", "Appearance", "Plugins")

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }

        // --- Tabs ---
        SecondaryTabRow(selectedTabIndex = selectedTab, tabs = {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        })

        // --- Content ---
        Box(modifier = Modifier.weight(1f).padding(24.dp)) {
            when (selectedTab) {
                0 -> LlmSettings(
                    config = state.config,
                    isTesting = state.isTestingConnection,
                    connectionError = state.connectionError,
                    connectionSuccess = state.connectionSuccess,
                    availableModels = state.availableModels,
                    onUpdate = viewModel::updateConfig,
                    onTestConnection = viewModel::testConnection
                )
                1 -> AppearanceSettings(
                    config = state.config,
                    onUpdate = viewModel::updateConfig
                )
                2 -> Text("Plugin System coming in Phase 3...", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // --- Footer (Save Button) ---
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.save()
                    onDismiss() // 保存后关闭
                },
                enabled = state.canSave
            ) {
                Text("Apply & Save")
            }
        }
    }
}

@Composable
fun LlmSettings(
    config: AppConfig,
    isTesting: Boolean,
    connectionError: String?,
    connectionSuccess: Boolean,
    availableModels: List<String>,
    onUpdate: (AppConfig) -> Unit,
    onTestConnection: () -> Unit
) {
    val llm = config.llm
    var showApiKey by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Base URL
        OutlinedTextField(
            value = llm.baseUrl,
            onValueChange = { onUpdate(config.copy(llm = llm.copy(baseUrl = it))) },
            label = { Text("Base URL") },
            placeholder = { Text("https://api.siliconflow.cn/v1") }, // 添加占位符
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Link, null) }
        )

        // API Key
        OutlinedTextField(
            value = llm.apiKey,
            onValueChange = { onUpdate(config.copy(llm = llm.copy(apiKey = it))) },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") }, // 添加占位符
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Key, null) },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        )

        // Test Connection Button & Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onTestConnection,
                enabled = !isTesting && llm.apiKey.isNotBlank()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Icon(Icons.Default.NetworkCheck, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }

            Spacer(Modifier.width(16.dp))

            if (connectionSuccess) {
                @Suppress("MagicNumber")
                Text("✅ Connected! Models fetched.", color = Color(0xFF006C4C)) // Fog Green
            } else if (connectionError != null) {
                Text("❌ $connectionError", color = MaterialTheme.colorScheme.error)
            }
        }

        HorizontalDivider()

        // Model Selectors
        // 简单的 Dropdown 实现
        ModelSelector(
            label = "Main Model (Reasoning)",
            selectedValue = llm.mainModel,
            options = availableModels,
            placeholderText = "e.g. deepseek-ai/DeepSeek-V3", // [修复 2] 传入占位符
            onValueChange = { onUpdate(config.copy(llm = llm.copy(mainModel = it))) }
        )

        ModelSelector(
            label = "Summary Model (Fast/Cheap)",
            selectedValue = llm.summaryModel,
            options = availableModels,
            placeholderText = "e.g. Qwen/Qwen2.5-7B-Instruct", // [修复 2] 传入占位符
            onValueChange = { onUpdate(config.copy(llm = llm.copy(summaryModel = it))) }
        )
    }
}

@Composable
fun ModelSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    placeholderText: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = onValueChange, // 允许手动输入
            label = { Text(label) },
            placeholder = { Text(placeholderText, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp) // 限制高度，支持滚动
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No models fetched (Run Test first)") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                options.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            onValueChange(model)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceSettings(
    config: AppConfig,
    onUpdate: (AppConfig) -> Unit
) {
    val ui = config.ui
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark Mode", modifier = Modifier.weight(1f))
            Switch(
                checked = ui.isDarkMode,
                onCheckedChange = { onUpdate(config.copy(ui = ui.copy(isDarkMode = it))) }
            )
        }

        Text("Theme Color Preset")

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemePreset.entries.forEach { preset ->
                FilterChip(
                    selected = ui.themeColor == preset,
                    onClick = { onUpdate(config.copy(ui = ui.copy(themeColor = preset))) },
                    label = { Text(preset.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier.size(
                                16.dp
                            ).background(Color(preset.colorHex), shape = RoundedCornerShape(4.dp))
                        )
                    }
                )
            }
        }
    }
}

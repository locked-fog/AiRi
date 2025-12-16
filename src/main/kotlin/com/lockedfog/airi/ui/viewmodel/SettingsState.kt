package com.lockedfog.airi.ui.viewmodel

import com.lockedfog.airi.data.config.AppConfig

data class SettingsState(
    val config: AppConfig,

    val isTestingConnection: Boolean = false,
    val connectionError: String? = null,
    val connectionSuccess: Boolean = false,

    val availableModels: List<String> = emptyList(),

    val canSave: Boolean = false
)

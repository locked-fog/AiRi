package com.lockedfog.airi.data.log

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LogBuffer {
    private const val MAX_LOGS = 500

    val logs = mutableStateListOf<String>()

    private val _thinkingContent = MutableStateFlow("")
    val thinkingContent = _thinkingContent.asStateFlow()

    fun add(log: String) {
        synchronized(this) {
            if (logs.size >= MAX_LOGS) {
                logs.removeAt(0)
            }
            logs.add(log)
        }
    }

    fun updateThinking(content: String) {
        _thinkingContent.value = content
    }

    fun clearThinking() {
        _thinkingContent.value = ""
    }
}

package com.lockedfog.airi.data.log

import androidx.compose.runtime.mutableStateListOf

object LogBuffer {
    private const val MAX_LOGS = 500

    val logs = mutableStateListOf<String>()

    fun add(log: String) {
        synchronized(this) {
            if (logs.size >= MAX_LOGS) {
                logs.removeAt(0)
            }
            logs.add(log)
        }
    }
}

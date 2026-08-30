package com.example.ui.util

import android.util.Log

object DebugNavigationLogger {
    private const val TAG = "DebugNavigationLog"
    private val eventHistory = mutableListOf<String>()

    fun logNavigation(fromScreen: String, toScreen: String) {
        val entry = "NAV_EVENT: Transitioned from [$fromScreen] to [$toScreen] at ${System.currentTimeMillis()}"
        eventHistory.add(0, entry)
        if (eventHistory.size > 50) eventHistory.removeAt(eventHistory.size - 1)
        Log.d(TAG, entry)
    }

    fun logScreenState(screen: String, stateDescription: String) {
        val entry = "STATE_CHANGE: Screen [$screen] state -> $stateDescription at ${System.currentTimeMillis()}"
        eventHistory.add(0, entry)
        if (eventHistory.size > 50) eventHistory.removeAt(eventHistory.size - 1)
        Log.d(TAG, entry)
    }

    fun getRecentLogs(): List<String> = eventHistory.toList()
}

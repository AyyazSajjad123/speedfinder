package com.example.speedfinder.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val KEY_DARK_MODE = "is_dark_mode"

    // StateFlow taake UI foran update ho jaye
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true)) // Default True (Dark)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun toggleTheme() {
        val newMode = !_isDarkMode.value
        prefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply()
        _isDarkMode.value = newMode
    }
}
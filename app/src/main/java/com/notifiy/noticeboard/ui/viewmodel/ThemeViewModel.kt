package com.notifiy.noticeboard.ui.viewmodel

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.noticeboard.data.preferences.PreferencesManager
import kotlinx.coroutines.launch

class ThemeViewModel : ViewModel() {
    
    private var preferencesManager: PreferencesManager? = null
    
    private var _isDarkMode by mutableStateOf(false)
    val isDarkMode: Boolean get() = _isDarkMode
    
    private var _themeMode by mutableStateOf(PreferencesManager.THEME_SYSTEM)
    val themeMode: Int get() = _themeMode
    
    fun initialize(context: android.content.Context) {
        preferencesManager = PreferencesManager(context)
        loadThemePreference()
    }
    
    private fun loadThemePreference() {
        preferencesManager?.let { prefs ->
            _themeMode = prefs.getThemeMode()
            _isDarkMode = prefs.isDarkMode()
        }
    }
    
    fun setThemeMode(mode: Int) {
        _themeMode = mode
        preferencesManager?.setThemeMode(mode)
        
        when (mode) {
            PreferencesManager.THEME_DARK -> _isDarkMode = true
            PreferencesManager.THEME_LIGHT -> _isDarkMode = false
            PreferencesManager.THEME_SYSTEM -> {
                // For system theme, we'll let the theme composable handle it
                _isDarkMode = false
            }
        }
    }
    
    fun toggleTheme() {
        val newMode = when (_themeMode) {
            PreferencesManager.THEME_SYSTEM -> PreferencesManager.THEME_LIGHT
            PreferencesManager.THEME_LIGHT -> PreferencesManager.THEME_DARK
            PreferencesManager.THEME_DARK -> PreferencesManager.THEME_SYSTEM
            else -> PreferencesManager.THEME_SYSTEM
        }
        setThemeMode(newMode)
    }
    
    fun getThemeModeString(): String {
        return when (_themeMode) {
            PreferencesManager.THEME_SYSTEM -> "System"
            PreferencesManager.THEME_LIGHT -> "Light"
            PreferencesManager.THEME_DARK -> "Dark"
            else -> "System"
        }
    }
}

package com.notifiy.noticeboard.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "noticeboard_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        
        // Theme mode constants
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }
    
    fun getThemeMode(): Int {
        return sharedPreferences.getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }
    
    fun setThemeMode(themeMode: Int) {
        sharedPreferences.edit {
            putInt(KEY_THEME_MODE, themeMode)
        }
    }
    
    fun isDarkMode(): Boolean {
        return when (getThemeMode()) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            THEME_SYSTEM -> false // Will be handled by system theme detection
            else -> false
        }
    }
    
    fun shouldUseSystemTheme(): Boolean {
        return getThemeMode() == THEME_SYSTEM
    }
}

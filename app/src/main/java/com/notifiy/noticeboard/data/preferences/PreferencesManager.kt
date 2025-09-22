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
        private const val KEY_PUSH_NOTIFICATIONS = "push_notifications"
        private const val KEY_EMAIL_NOTIFICATIONS = "email_notifications"
        private const val KEY_MARKETING_EMAILS = "marketing_emails"
        
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
    
    // Notification preferences
    fun getPushNotifications(): Boolean {
        return sharedPreferences.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
    }
    
    fun setPushNotifications(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_PUSH_NOTIFICATIONS, enabled)
        }
    }
    
    fun getEmailNotifications(): Boolean {
        return sharedPreferences.getBoolean(KEY_EMAIL_NOTIFICATIONS, false)
    }
    
    fun setEmailNotifications(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_EMAIL_NOTIFICATIONS, enabled)
        }
    }
    
    fun getMarketingEmails(): Boolean {
        return sharedPreferences.getBoolean(KEY_MARKETING_EMAILS, false)
    }
    
    fun setMarketingEmails(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_MARKETING_EMAILS, enabled)
        }
    }
}

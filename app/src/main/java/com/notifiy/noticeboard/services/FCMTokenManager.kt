package com.notifiy.noticeboard.services

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FCMTokenManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "fcm_token_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_TIMESTAMP = "token_timestamp"
    }
    
    private val sharedPref: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Get the current FCM token
     */
    suspend fun getCurrentToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            println("DEBUG: FCMTokenManager - Error getting FCM token: ${e.message}")
            null
        }
    }
    
    /**
     * Get the stored FCM token from SharedPreferences
     */
    fun getStoredToken(): String? {
        return sharedPref.getString(KEY_FCM_TOKEN, null)
    }
    
    /**
     * Store the FCM token locally
     */
    fun storeToken(token: String) {
        with(sharedPref.edit()) {
            putString(KEY_FCM_TOKEN, token)
            putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        println("DEBUG: FCMTokenManager - Token stored locally: $token")
    }
    
    /**
     * Check if the stored token needs to be refreshed (older than 24 hours)
     */
    fun shouldRefreshToken(): Boolean {
        val timestamp = sharedPref.getLong(KEY_TOKEN_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L
        
        return (currentTime - timestamp) > twentyFourHours
    }
    
    /**
     * Initialize FCM token - get current token and store it
     */
    suspend fun initializeToken(): String? {
        return try {
            val token = getCurrentToken()
            token?.let { storeToken(it) }
            token
        } catch (e: Exception) {
            println("DEBUG: FCMTokenManager - Error initializing FCM token: ${e.message}")
            null
        }
    }
    
    /**
     * Refresh the FCM token if needed
     */
    suspend fun refreshTokenIfNeeded(): String? {
        return try {
            if (shouldRefreshToken()) {
                println("DEBUG: FCMTokenManager - Refreshing FCM token")
                val newToken = getCurrentToken()
                newToken?.let { storeToken(it) }
                newToken
            } else {
                getStoredToken()
            }
        } catch (e: Exception) {
            println("DEBUG: FCMTokenManager - Error refreshing FCM token: ${e.message}")
            getStoredToken()
        }
    }
}




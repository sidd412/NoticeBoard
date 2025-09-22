package com.notifiy.noticeboard.services

import android.content.Context
import android.content.pm.PackageManager
import com.notifiy.noticeboard.data.model.UpdateConfig
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class UpdateService(private val context: Context) {
    
    private val firebaseRepository = FirebaseRepository(context)
    
    data class UpdateCheckResult(
        val needsUpdate: Boolean,
        val updateConfig: UpdateConfig?,
        val isForceUpdate: Boolean,
        val isSkipable: Boolean,
        val errorMessage: String? = null
    )
    
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            // Add timeout to prevent hanging
            val updateConfig = withTimeout(10000) { // 10 seconds timeout
                firebaseRepository.getUpdateConfig()
            }
            
            if (updateConfig == null) {
                return@withContext UpdateCheckResult(
                    needsUpdate = false,
                    updateConfig = null,
                    isForceUpdate = false,
                    isSkipable = false,
                    errorMessage = "Unable to check for updates. Please check your internet connection."
                )
            }
            
            // Validate update config data
            if (updateConfig.updateLink.isBlank() || updateConfig.latestVersionCode <= 0) {
                return@withContext UpdateCheckResult(
                    needsUpdate = false,
                    updateConfig = null,
                    isForceUpdate = false,
                    isSkipable = false,
                    errorMessage = "Update information is not available. Please try again later."
                )
            }
            
            val currentVersionCode = getCurrentVersionCode()
            val currentVersionName = getCurrentVersionName()
            
            // Additional validation
            if (currentVersionCode <= 0) {
                return@withContext UpdateCheckResult(
                    needsUpdate = false,
                    updateConfig = null,
                    isForceUpdate = false,
                    isSkipable = false,
                    errorMessage = "Unable to determine current app version. Please restart the app."
                )
            }
            
            // Check if update is needed based on version code or version name
            val versionCodeCheck = currentVersionCode < updateConfig.latestVersionCode
            val versionNameCheck = currentVersionCode == updateConfig.latestVersionCode && currentVersionName != updateConfig.latestVersionName
            val needsUpdate = versionCodeCheck || versionNameCheck
            
            UpdateCheckResult(
                needsUpdate = needsUpdate,
                updateConfig = updateConfig,
                isForceUpdate = updateConfig.forceUpdate,
                isSkipable = updateConfig.skipableUpdate
            )
            
        } catch (e: TimeoutCancellationException) {
            // Handle timeout gracefully
            UpdateCheckResult(
                needsUpdate = false,
                updateConfig = null,
                isForceUpdate = false,
                isSkipable = false,
                errorMessage = "Update check timed out. Please check your internet connection and try again."
            )
        } catch (e: Exception) {
            // Handle any other exceptions
            UpdateCheckResult(
                needsUpdate = false,
                updateConfig = null,
                isForceUpdate = false,
                isSkipable = false,
                errorMessage = "Unable to check for updates. Please try again later."
            )
        }
    }
    
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = packageInfo.longVersionCode.toInt()
            // Ensure version code is positive
            if (versionCode > 0) versionCode else 1
        } catch (e: PackageManager.NameNotFoundException) {
            1 // Default fallback version
        } catch (e: Exception) {
            1 // Handle any other package manager errors
        }
    }
    
    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0" // Default fallback version
        } catch (e: Exception) {
            "1.0.0" // Handle any other package manager errors
        }
    }
}

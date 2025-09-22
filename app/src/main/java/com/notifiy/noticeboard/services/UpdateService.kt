package com.notifiy.noticeboard.services

import android.content.Context
import android.content.pm.PackageManager
import com.notifiy.noticeboard.data.model.UpdateConfig
import com.notifiy.noticeboard.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateService(private val context: Context) {
    
    private val firebaseRepository = FirebaseRepository(context)
    
    data class UpdateCheckResult(
        val needsUpdate: Boolean,
        val updateConfig: UpdateConfig?,
        val isForceUpdate: Boolean,
        val isSkipable: Boolean
    )
    
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: UpdateService - Starting update check...")
            val updateConfig = firebaseRepository.getUpdateConfig()
            
            if (updateConfig == null) {
                println("DEBUG: UpdateService - No update config found")
                return@withContext UpdateCheckResult(
                    needsUpdate = false,
                    updateConfig = null,
                    isForceUpdate = false,
                    isSkipable = false
                )
            }
            
            println("DEBUG: UpdateService - Retrieved update config: $updateConfig")
            
            val currentVersionCode = getCurrentVersionCode()
            val currentVersionName = getCurrentVersionName()
            
            println("DEBUG: UpdateService - Current version: $currentVersionName ($currentVersionCode)")
            println("DEBUG: UpdateService - Latest version: ${updateConfig.latestVersionName} (${updateConfig.latestVersionCode})")
            
            // Check if update is needed based on version code or version name
            val versionCodeCheck = currentVersionCode < updateConfig.latestVersionCode
            val versionNameCheck = currentVersionCode == updateConfig.latestVersionCode && currentVersionName != updateConfig.latestVersionName
            val needsUpdate = versionCodeCheck || versionNameCheck
            
            println("DEBUG: UpdateService - Version code check (current < latest): $versionCodeCheck")
            println("DEBUG: UpdateService - Version name check (same code, different name): $versionNameCheck")
            println("DEBUG: UpdateService - Needs update: $needsUpdate")
            println("DEBUG: UpdateService - Force update: ${updateConfig.forceUpdate}")
            println("DEBUG: UpdateService - Skipable update: ${updateConfig.skipableUpdate}")
            
            UpdateCheckResult(
                needsUpdate = needsUpdate,
                updateConfig = updateConfig,
                isForceUpdate = updateConfig.forceUpdate,
                isSkipable = updateConfig.skipableUpdate
            )
            
        } catch (e: Exception) {
            println("DEBUG: UpdateService - Error checking for update: ${e.message}")
            e.printStackTrace()
            UpdateCheckResult(
                needsUpdate = false,
                updateConfig = null,
                isForceUpdate = false,
                isSkipable = false
            )
        }
    }
    
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            println("DEBUG: UpdateService - Error getting version code: ${e.message}")
            0
        }
    }
    
    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            println("DEBUG: UpdateService - Error getting version name: ${e.message}")
            "1.0.0"
        }
    }
}

package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.PropertyName

data class UpdateConfig(
    @PropertyName("update_link")
    val updateLink: String = "",
    
    @PropertyName("latest_version_code")
    val latestVersionCode: Int = 0,
    
    @PropertyName("latest_version_name")
    val latestVersionName: String = "",
    
    @PropertyName("force_update")
    val forceUpdate: Boolean = false,
    
    @PropertyName("skipable_update")
    val skipableUpdate: Boolean = true,
    
    val updatedAt: Long = System.currentTimeMillis()
)

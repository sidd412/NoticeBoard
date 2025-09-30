package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val subscribedBoards: List<String> = emptyList(), // List of notice board IDs
    val subscribedCodes: List<String> = emptyList(), // List of institute codes user subscribed to
    val instituteCodes: List<String> = emptyList(), // List of institute codes user owns
    val currentPlanId: String = "", // Current subscribed plan ID
    val planName: String = "", // Current plan name (e.g., "Basic", "Premium")
    val subscriptionPeriod: String = "", // monthly, annual
    val subscriptionExpiry: Long = 0, // Subscription expiry timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class NoticeBoard(
    @DocumentId
    val id: String = "",
    val organizationName: String = "",
    val organizationCode: String = "",
    val organizationEmail: String = "",
    val organizationLocation: String = "",
    val organizationWhatsapp: String = "",
    val qrCodeUrl: String = "",
    val pageUrl: String = "",
    val isActive: Boolean = true,
    val subscriptionPeriod: String = "", // monthly, annual
    val subscriptionExpiry: Long = 0,
    val currentPlanId: String = "", // Current subscribed plan ID for this board
    val planName: String = "", // Current plan name (e.g., "Basic", "Premium")
    val createdBy: String = "", // User ID who created this board
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


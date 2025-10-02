package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class Purchase(
    @DocumentId
    val id: String = "",
    val userId: String = "", // User who made the purchase
    val orgCode: String = "", // Organization code (if applicable)
    val planId: String = "", // Google Play product ID
    val planName: String = "", // Plan name (e.g., "Premium", "Basic")
    val subscriptionPeriod: String = "", // "monthly" or "annual"
    val purchaseToken: String = "", // Google Play purchase token
    val orderId: String = "", // Google Play order ID
    val purchaseTime: Long = 0, // Purchase timestamp
    val expiryTime: Long = 0, // Subscription expiry timestamp
    val purchaseState: String = "", // "purchased", "pending", "cancelled", "refunded"
    val autoRenewing: Boolean = false, // Whether subscription auto-renews
    val price: String = "", // Purchase price
    val currency: String = "", // Currency code (e.g., "USD", "INR")
    val packageName: String = "", // App package name
    val acknowledgementState: String = "", // "acknowledged", "unacknowledged"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PurchaseState {
    PURCHASED,
    PENDING,
    CANCELLED,
    REFUNDED
}

enum class AcknowledgementState {
    ACKNOWLEDGED,
    UNACKNOWLEDGED
}


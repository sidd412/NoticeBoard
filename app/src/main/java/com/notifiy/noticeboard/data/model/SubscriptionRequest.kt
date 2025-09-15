package com.notifiy.noticeboard.data.model

data class SubscriptionRequest(
    val whatsappNumber: String = "",
    val email: String = "",
    val noticeBoardCode: String = "",
    val qrCodeData: String = "",
    val subscriptionMethod: SubscriptionMethod = SubscriptionMethod.NONE
)

enum class SubscriptionMethod {
    NONE,
    WHATSAPP,
    EMAIL,
    CODE,
    QR_CODE
}


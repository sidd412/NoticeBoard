package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class Plan(
    @DocumentId
    val id: String = "",
    val planName: String = "",
    val planId: String = "", // Product ID for Google Play Console (e.g., "notexp_plus")
    val amount: List<String> = emptyList(), // ["INR 149/month", "INR 1490/year"]
    val basePlanId: List<String> = emptyList(), // ["notexp-plus-monthly", "notexp-plus-yearly"]
    val pages: Int = 0,
    val boards: Int = 0, // Number of boards user can create
    val images: Int = 0,
    val pdfs: Int = 0,
    val links: Int = 0,
    val invitePeople: Int = 0,
    val boardCreation: Boolean = false,
    val boardMonetization: Boolean = false,
    val realTimeNotifictaion: Boolean = false,
    val queryByUser: Boolean = false,
    val queryForAnalyticsReport: Boolean = false,
    val aiNoteFeature: Boolean = false
)

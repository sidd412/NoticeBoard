package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class Plan(
    @DocumentId
    val id: String = "",
    val planName: String = "",
    val amount: String = "",
    val annualPrice: String = "",
    val planId: List<String> = emptyList(), // [monthlyPlanId, annualPlanId]
    val pages: Int = 0,
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

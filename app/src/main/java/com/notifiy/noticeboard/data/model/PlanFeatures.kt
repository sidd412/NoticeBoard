package com.notifiy.noticeboard.data.model

data class PlanFeatures(
    val maxBoards: Int,
    val maxPages: Int,
    val boardCreation: Boolean,
    val boardMonetization: Boolean,
    val realTimeNotification: Boolean,
    val queryByUser: Boolean,
    val queryForAnalyticsReport: Boolean,
    val aiNoteFeature: Boolean
)

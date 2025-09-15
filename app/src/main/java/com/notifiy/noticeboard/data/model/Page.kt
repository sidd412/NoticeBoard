package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class Page(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val infoPoints: List<String> = emptyList(), // Array of info points
    val additionalInfo: String = "",
    val code: Int = 0, // Notice board code
    val priority: String = "LOW", // LOW, NORMAL, HIGH, URGENT
    val userId: String = "", // User ID who created this page
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)


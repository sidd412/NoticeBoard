package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class Notice(
    @DocumentId
    val id: String = "",
    val noticeBoardId: String = "", // ID of the notice board this notice belongs to
    val title: String = "",
    val subtitle: String = "",
    val infoPoints: List<String> = emptyList(), // Array of info points
    val content: String = "", // Additional content/description
    val priority: NoticePriority = NoticePriority.NORMAL,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "", // User ID who created this notice
    val publishedAt: Long = System.currentTimeMillis()
)

enum class NoticePriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
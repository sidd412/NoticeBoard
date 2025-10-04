package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class UserNotification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val boardId: String = "",
    val boardCode: String = "",
    val unreadCount: Int = 0,
    val title: String = "",
    val body: String = "",
    val type: String = "", // "notice", "query", "query_resolved"
    val lastViewedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

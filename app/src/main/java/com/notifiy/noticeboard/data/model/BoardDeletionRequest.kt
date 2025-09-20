package com.notifiy.noticeboard.data.model

import java.util.Date

data class BoardDeletionRequest(
    val id: String = "",
    val boardId: String = "",
    val organizationName: String = "",
    val organizationCode: String = "",
    val organizationEmail: String = "",
    val organizationLocation: String = "",
    val organizationWhatsapp: String = "",
    val requestedBy: String = "", // User ID who requested deletion
    val requestReason: String = "",
    val status: DeletionRequestStatus = DeletionRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DeletionRequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    REJECTED
}

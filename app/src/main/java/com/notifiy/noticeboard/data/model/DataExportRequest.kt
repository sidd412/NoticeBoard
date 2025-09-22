package com.notifiy.noticeboard.data.model

data class DataExportRequest(
    val id: String = "",
    val userId: String = "", // User ID who requested export
    val userEmail: String = "",
    val userName: String = "",
    val requestReason: String = "",
    val status: ExportRequestStatus = ExportRequestStatus.PENDING,
    val requestedDataTypes: List<String> = listOf("profile", "notices", "boards", "subscriptions"), // Types of data to export
    val downloadUrl: String = "", // URL for downloading the exported data
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days from now
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val processedAt: Long = 0L, // When the export was completed
    val fileSize: Long = 0L, // Size of the exported file in bytes
    val adminNotes: String = "" // Admin notes about the request
)

enum class ExportRequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    EXPIRED
}

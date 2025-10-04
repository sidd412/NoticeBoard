package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class UserQuery(
    @DocumentId
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val organisationCode: String = "",
    val raiserEmail: String = "",
    val raiserId: String = "",
    val raiserLocation: String = "",
    val raiserMobile: String = "",
    val raiserName: String = "",
    val status: String = "created", // "created" or "resolved"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class QueryStatus {
    CREATED,
    RESOLVED
}

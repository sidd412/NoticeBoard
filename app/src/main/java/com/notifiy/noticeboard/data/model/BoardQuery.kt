package com.notifiy.noticeboard.data.model

import com.google.firebase.firestore.DocumentId

data class BoardQuery(
    @DocumentId
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val orgCode: String = "",
    val orgEmail: String = "",
    val orgMobile: String = "",
    val orgName: String = "",
    val status: String = "created", // "created" or "resolved"
    val type: String = "", // "verification", "subscribers data", etc.
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

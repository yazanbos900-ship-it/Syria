package com.example.domain.model

data class SubscriptionRequest(
    val requestId: String = "",
    val userId: String = "",
    val storeId: String = "",
    val requestedTier: String = "",
    val requestDate: Long = System.currentTimeMillis(),
    val status: String = "pending"
)

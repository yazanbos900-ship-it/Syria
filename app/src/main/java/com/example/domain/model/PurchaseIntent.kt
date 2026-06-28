package com.example.domain.model

import com.google.firebase.Timestamp

data class PurchaseIntent(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val timestamp: Timestamp? = null
)

package com.example.domain.model

data class PaymentTransaction(
    val transactionId: String,
    val userId: String,
    val orderId: String,
    val paymentMethod: String,
    val phoneNumber: String,
    val otpCode: String,
    val amount: Double,
    val currency: String,
    val status: String, // "Pending", "Paid", "Failed", "Cancelled"
    val createdAt: Long,
    val expiresAt: Long
)

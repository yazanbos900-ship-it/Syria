package com.example.domain.model

data class Store(
    val id: String,
    val name: String,
    val ownerId: String,
    val ownerUsername: String = "",
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val description: String,
    val categoryId: String = "",
    val followersCount: Int = 0,
    val status: String = "active",
    val rating: Float = 5.0f,
    val isVerified: Boolean = false,
    val usdExchangeRate: Double = 13500.0,
    val subscriptionTier: String = "Starter", // "Starter", "Growth", "Pro"
    val verificationStatus: String = "Pending", // "Pending", "Verified", "Rejected"
    val sellerBadge: String = "None", // "None", "Verified Seller", "Pro Seller"
    val createdAt: Long = System.currentTimeMillis()
)

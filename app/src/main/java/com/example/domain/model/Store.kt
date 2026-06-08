package com.example.domain.model

import com.google.firebase.Timestamp

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
    val defaultCurrency: String = "USD", // "USD" or "SYP"
    val deliveryAreas: List<String> = emptyList(),
    val shippingCosts: Map<String, Double> = emptyMap(), // Map of city/area name -> fee in SYP
    val workingHours: String = "9:00 AM - 9:00 PM",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String = "",
    val district: String = "",
    val fullAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Exchange Rate Management fields
    val exchangeRate: Double = 12500.0,
    val storeCurrency: String = "USD",
    val exchangeRateUpdatedAt: Timestamp? = null,
    val usingGlobalRate: Boolean = true
)


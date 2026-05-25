package com.example.domain.model

data class Store(
    val id: String,
    val name: String,
    val ownerId: String,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val description: String,
    val rating: Float = 5.0f,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

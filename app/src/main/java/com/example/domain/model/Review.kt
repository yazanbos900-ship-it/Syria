package com.example.domain.model

data class Review(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val images: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

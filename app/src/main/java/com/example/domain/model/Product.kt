package com.example.domain.model

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrls: List<String>,
    val categoryId: String,
    val storeId: String,
    val rating: Float = 4.5f,
    val reviewCount: Int = 0,
    val isAvailable: Boolean = true,
    val stockCount: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

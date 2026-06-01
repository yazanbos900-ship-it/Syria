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
    val currency: String = "USD", // "USD" or "SYP"
    val createdAt: Long = System.currentTimeMillis()
)

fun Product.getPriceInUSD(exchangeRate: Double): Double {
    val rate = if (exchangeRate <= 0) 13500.0 else exchangeRate
    return if (currency == "SYP") price / rate else price
}

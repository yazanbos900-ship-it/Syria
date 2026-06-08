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
    val isApproved: Boolean = true,
    val isFlagged: Boolean = false,
    val flagReason: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val condition: String = "new",
    val sellerType: String = "",
    val sellerId: String = "",
    
    // Exchange Rate Management integration
    val priceUSD: Double = 0.0,
    val priceSYP: Double = 0.0,
    val storeCurrency: String = "USD",
    val exchangeRateUsed: Double = 12500.0
)

fun Product.getPriceInUSD(exchangeRate: Double): Double {
    val rate = if (exchangeRate <= 0) 13500.0 else exchangeRate
    return if (currency == "SYP") price / rate else price
}


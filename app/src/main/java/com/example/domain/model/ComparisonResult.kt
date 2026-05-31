package com.example.domain.model

data class ComparableProductInfo(
    val product: Product,
    val storeName: String,
    val storeRating: Float
)

data class ComparisonResult(
    val baseProductPrice: Double,
    val avgPrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val percentageDifference: Double, // negative if lower, positive if higher than average
    val position: String, // "Low" (below 90% of avg), "Average" (90% to 110%), "High" (above 110%)
    val comparableProducts: List<ComparableProductInfo>
)

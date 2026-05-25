package com.example.domain.repository

import com.example.features.marketplace.MarketProduct
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun getWishlistItems(uid: String): Flow<List<MarketProduct>>
    suspend fun addToWishlist(uid: String, product: MarketProduct)
    suspend fun removeFromWishlist(uid: String, productId: String)
}

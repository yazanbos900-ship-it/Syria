package com.example.domain.repository

import com.example.features.marketplace.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(uid: String): Flow<List<CartItem>>
    suspend fun addToCart(uid: String, item: CartItem, quantity: Int)
    suspend fun updateQuantity(uid: String, productId: String, quantity: Int)
    suspend fun removeFromCart(uid: String, productId: String)
}

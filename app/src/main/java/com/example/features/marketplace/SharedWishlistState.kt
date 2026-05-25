package com.example.features.marketplace

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

object SharedWishlistState {
    // We pre-populate with 2 items for a high-retention default experience, showing real catalog items
    val wishlistItems = mutableStateListOf<MarketProduct>(
        productCatalog.find { it.id == "apple_watch_ultra_2" } ?: productCatalog[0],
        productCatalog.find { it.id == "woolen_trench_coat" } ?: productCatalog[1],
        productCatalog.find { it.id == "cashmere_scarf" } ?: productCatalog[5]
    )

    fun isWishlisted(product: MarketProduct): Boolean {
        return wishlistItems.any { it.id == product.id }
    }

    fun toggleWishlist(product: MarketProduct): Boolean {
        val exists = wishlistItems.any { it.id == product.id }
        if (exists) {
            wishlistItems.removeAll { it.id == product.id }
            return false
        } else {
            wishlistItems.add(product)
            return true
        }
    }

    fun removeProduct(product: MarketProduct) {
        wishlistItems.removeAll { it.id == product.id }
    }
}

object SharedCartState {
    val cartItems = mutableStateListOf<CartItem>(
        CartItem(
            id = "chrono_leather_strap",
            name = "Chrono Leather Loop Strap",
            price = 35.0,
            originalPrice = null,
            image = "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=400&q=80",
            size = "Tan Oak Leather",
            storeName = "Bespoke Horology Lab"
        ),
        CartItem(
            id = "nordic_dining_chair",
            name = "Nordic Oak Dining Chair",
            price = 120.0,
            originalPrice = 180.0,
            image = "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?auto=format&fit=crop&w=400&q=80",
            size = "Oak Light",
            storeName = "Minimalist Living"
        )
    )

    val itemQuantities = mutableStateMapOf<String, Int>(
        "chrono_leather_strap" to 1,
        "nordic_dining_chair" to 1
    )

    fun addProductToCart(product: MarketProduct) {
        val existingItemIndex = cartItems.indexOfFirst { it.id == product.id }
        if (existingItemIndex != -1) {
            val currentQty = itemQuantities[product.id] ?: 1
            itemQuantities[product.id] = currentQty + 1
        } else {
            cartItems.add(
                CartItem(
                    id = product.id,
                    name = product.name,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    image = product.imageUrl,
                    size = "Free Size",
                    storeName = product.storeName,
                    keyStockLimit = 10
                )
            )
            itemQuantities[product.id] = 1
        }
    }
}

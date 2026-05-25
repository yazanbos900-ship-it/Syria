package com.example.features.marketplace

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.example.core.di.ServiceLocator
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CartRepository
import com.example.domain.repository.WishlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object SharedWishlistState {
    private var currentUserUid: String? = null
    val wishlistItems = mutableStateListOf<MarketProduct>()
    
    private var wishlistJob: Job? = null

    fun init(authRepository: AuthRepository, wishlistRepository: WishlistRepository, scope: CoroutineScope) {
        wishlistJob?.cancel()
        wishlistJob = authRepository.currentUser.onEach { user ->
            currentUserUid = user?.id
            if (user != null) {
                wishlistRepository.getWishlistItems(user.id).collect { items ->
                    wishlistItems.clear()
                    wishlistItems.addAll(items)
                }
            } else {
                wishlistItems.clear()
            }
        }.launchIn(scope)
    }

    fun isWishlisted(product: MarketProduct): Boolean {
        return wishlistItems.any { it.id == product.id }
    }

    fun toggleWishlist(product: MarketProduct) {
        val uid = currentUserUid ?: return
        val exists = wishlistItems.any { it.id == product.id }
        CoroutineScope(Dispatchers.IO).launch {
            if (exists) {
                ServiceLocator.wishlistRepository.removeFromWishlist(uid, product.id)
            } else {
                ServiceLocator.wishlistRepository.addToWishlist(uid, product)
            }
        }
    }
}

object SharedCartState {
    private var currentUserUid: String? = null
    val cartItems = mutableStateListOf<CartItem>()
    
    private var cartJob: Job? = null

    fun init(authRepository: AuthRepository, cartRepository: CartRepository, scope: CoroutineScope) {
        cartJob?.cancel()
        cartJob = authRepository.currentUser.onEach { user ->
            currentUserUid = user?.id
            if (user != null) {
                cartRepository.getCartItems(user.id).collect { items ->
                    cartItems.clear()
                    cartItems.addAll(items)
                }
            } else {
                cartItems.clear()
            }
        }.launchIn(scope)
    }

    fun addProductToCart(product: MarketProduct) {
        val uid = currentUserUid ?: return
        val existingItem = cartItems.find { it.id == product.id }
        
        CoroutineScope(Dispatchers.IO).launch {
            if (existingItem != null) {
                ServiceLocator.cartRepository.updateQuantity(uid, product.id, existingItem.quantity + 1)
            } else {
                val cartItem = CartItem(
                    id = product.id,
                    name = product.name,
                    price = product.price,
                    originalPrice = product.originalPrice,
                    image = product.imageUrl,
                    size = "Free Size",
                    storeName = product.storeName,
                    quantity = 1,
                    keyStockLimit = 10
                )
                ServiceLocator.cartRepository.addToCart(uid, cartItem, 1)
            }
        }
    }
    
    fun removeProductFromCart(product: CartItem) {
        val uid = currentUserUid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.cartRepository.removeFromCart(uid, product.id)
        }
    }
    
    fun updateQuantity(productId: String, quantity: Int) {
        val uid = currentUserUid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.cartRepository.updateQuantity(uid, productId, quantity)
        }
    }
}

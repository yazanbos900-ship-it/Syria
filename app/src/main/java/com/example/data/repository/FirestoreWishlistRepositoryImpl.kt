package com.example.data.repository

import com.example.domain.repository.WishlistRepository
import com.example.features.marketplace.MarketProduct
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreWishlistRepositoryImpl : WishlistRepository {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    override fun getWishlistItems(uid: String): Flow<List<MarketProduct>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("users").document(uid).collection("wishlist")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        MarketProduct(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            originalPrice = doc.getDouble("originalPrice"),
                            imageUrl = doc.getString("image") ?: "",
                            storeName = doc.getString("storeName") ?: "",
                            rating = doc.getDouble("rating") ?: 0.0,
                            reviewsCount = (doc.getLong("reviewsCount") ?: 0).toInt(),
                            category = doc.getString("category") ?: "Unknown",
                            deliveryTime = doc.getString("deliveryTime") ?: "Standard",
                            dateAdded = doc.getString("dateAdded") ?: "2026-05-25"
                        )
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addToWishlist(uid: String, product: MarketProduct) {
        val db = firestore ?: return
        val wishlistMap = hashMapOf(
            "productId" to product.id,
            "name" to product.name,
            "price" to product.price,
            "originalPrice" to product.originalPrice,
            "image" to product.imageUrl,
            "storeName" to product.storeName
        )
        db.collection("users").document(uid).collection("wishlist").document(product.id).set(wishlistMap).await()
    }

    override suspend fun removeFromWishlist(uid: String, productId: String) {
        firestore?.collection("users")?.document(uid)?.collection("wishlist")?.document(productId)?.delete()?.await()
    }
}

package com.example.data.repository

import com.example.domain.repository.CartRepository
import com.example.features.marketplace.CartItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCartRepositoryImpl : CartRepository {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCartItems(uid: String): Flow<List<CartItem>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("users").document(uid).collection("cart")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        CartItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            originalPrice = doc.getDouble("originalPrice"),
                            image = doc.getString("image") ?: "",
                            size = doc.getString("size"),
                            storeName = doc.getString("storeName") ?: ""
                        )
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun addToCart(uid: String, item: CartItem, quantity: Int) {
        val db = firestore ?: return
        val cartMap = hashMapOf(
            "productId" to item.id,
            "name" to item.name,
            "price" to item.price,
            "originalPrice" to item.originalPrice,
            "image" to item.image,
            "quantity" to quantity,
            "size" to item.size,
            "storeName" to item.storeName
        )
        db.collection("users").document(uid).collection("cart").document(item.id).set(cartMap).await()
    }

    override suspend fun updateQuantity(uid: String, productId: String, quantity: Int) {
        firestore?.collection("users")?.document(uid)?.collection("cart")?.document(productId)
            ?.update("quantity", quantity)?.await()
    }

    override suspend fun removeFromCart(uid: String, productId: String) {
        firestore?.collection("users")?.document(uid)?.collection("cart")?.document(productId)?.delete()?.await()
    }
}

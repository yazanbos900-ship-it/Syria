package com.example.features.marketplace

import android.util.Log
import com.example.domain.model.Store
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoreRepositoryImpl : StoreRepository {
    private val tag = "StoreRepositoryImpl"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore service is not initialized or unavailable", e)
            null
        }
    }

    override suspend fun checkIfStoreExists(ownerId: String): Boolean {
        val db = firestore ?: return false
        val snapshot = db.collection("stores")
            .whereEqualTo("ownerId", ownerId)
            .limit(1)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    override fun getActiveStores(): Flow<Result<List<Store>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service not available")))
            close()
            return@callbackFlow
        }

        val subscription = db.collection("stores")
            .whereEqualTo("status", "active")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val stores = snapshot.documents.mapNotNull { doc ->
                        try {
                            Store(
                                id = doc.id,
                                name = doc.getString("storeName") ?: "Unnamed Store",
                                ownerId = doc.getString("ownerId") ?: "",
                                ownerUsername = doc.getString("ownerUsername") ?: "",
                                logoUrl = doc.getString("logoUrl"),
                                description = doc.getString("description") ?: "",
                                followersCount = (doc.getLong("followersCount") ?: 0).toInt(),
                                status = doc.getString("status") ?: "active",
                                createdAt = (doc.getTimestamp("createdAt") ?: Timestamp.now()).toDate().time
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to parse store document: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(Result.success(stores))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createStoreAndFirstProduct(
        storeId: String,
        ownerId: String,
        ownerUsername: String,
        storeName: String,
        categoryId: String,
        categoryName: String,
        description: String,
        logoUrl: String,
        bannerUrl: String?,
        productName: String,
        productPrice: Double,
        productDescription: String,
        productImages: List<String>
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service not available"))

        return try {
            val storeRef = db.collection("stores").document(storeId)
            val productRef = db.collection("products").document() // top-level collection

            val storeMap = hashMapOf(
                "ownerId" to ownerId,
                "ownerUsername" to ownerUsername,
                "storeName" to storeName,
                "categoryId" to categoryId,
                "categoryName" to categoryName,
                "description" to description,
                "logoUrl" to logoUrl,
                "bannerUrl" to bannerUrl,
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val productMap = hashMapOf(
                "productId" to productRef.id,
                "storeId" to storeId,
                "ownerUid" to ownerId,
                "name" to productName,
                "title" to productName,
                "price" to productPrice,
                "description" to productDescription,
                "images" to productImages,
                "imageUrls" to productImages,
                "coverImage" to (productImages.firstOrNull() ?: ""),
                "category" to categoryId,
                "isAvailable" to true,
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Dynamic batch write for transactions and atomic consistency
            val batch = db.batch()
            batch.set(storeRef, storeMap)
            batch.set(productRef, productMap)
            
            batch.commit().await()
            Log.d(tag, "Store and product created successfully in Firestore: $storeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to write store or first product to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}

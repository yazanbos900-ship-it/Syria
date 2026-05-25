package com.example.features.marketplace

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
                "price" to productPrice,
                "description" to productDescription,
                "images" to productImages,
                "coverImage" to (productImages.firstOrNull() ?: ""),
                "category" to categoryId,
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

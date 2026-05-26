package com.example.data.repository

import android.util.Log
import com.example.domain.model.Store
import com.example.domain.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseStoreRepositoryImpl : StoreRepository {
    private val tag = "FirebaseStore"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is currently unavailable", e)
            null
        }
    }

    override fun getAllStores(): Flow<Result<List<Store>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        val subscription = db.collection("stores")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                handleSnapshot(snapshot, error, ::trySend)
            }
        awaitClose { subscription.remove() }
    }

    override fun getActiveStores(): Flow<Result<List<Store>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        val subscription = db.collection("stores")
            .whereEqualTo("status", "active")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                handleSnapshot(snapshot, error, ::trySend)
            }
        awaitClose { subscription.remove() }
    }

    private fun mapFirestoreDocToStore(doc: com.google.firebase.firestore.DocumentSnapshot): Store? {
        if (!doc.exists()) return null
        return try {
            Store(
                id = doc.id,
                name = doc.getString("storeName") ?: doc.getString("name") ?: "",
                ownerId = doc.getString("ownerId") ?: "",
                ownerUsername = doc.getString("ownerUsername") ?: "",
                logoUrl = doc.getString("logoUrl"),
                bannerUrl = doc.getString("bannerUrl"),
                description = doc.getString("description") ?: "",
                categoryId = doc.getString("categoryId") ?: "",
                followersCount = (doc.getLong("followersCount") ?: 0).toInt(),
                status = doc.getString("status") ?: "active",
                rating = doc.getDouble("rating")?.toFloat() ?: 5.0f,
                isVerified = doc.getBoolean("isVerified") ?: false,
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun handleSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot?,
        error: com.google.firebase.firestore.FirebaseFirestoreException?,
        trySend: (Result<List<Store>>) -> Unit
    ) {
        if (error != null) {
            trySend(Result.failure(error))
            return
        }
        if (snapshot != null) {
            val list = snapshot.documents.mapNotNull { doc ->
                mapFirestoreDocToStore(doc)
            }
            trySend(Result.success(list))
        } else {
            trySend(Result.success(emptyList()))
        }
    }

    override suspend fun getStoreById(storeId: String): Store? {
        if (storeId.isBlank()) return null
        val db = firestore ?: return null
        return try {
            // 1) Primary: document ID (matches list + createStoreAndFirstProduct)
            var doc = db.collection("stores").document(storeId).get().await()
            if (!doc.exists()) {
                // 2) Fallback: legacy docs with separate storeId field
                val q = db.collection("stores")
                    .whereEqualTo("storeId", storeId)
                    .limit(1)
                    .get().await()
                doc = q.documents.firstOrNull() ?: return null
            }
            mapFirestoreDocToStore(doc)
        } catch (e: Exception) {
            Log.e(tag, "getStoreById failed for $storeId", e)
            null
        }
    }

    override suspend fun createStore(store: Store): Result<Store> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val storeMap = hashMapOf(
                "name" to store.name,
                "ownerId" to store.ownerId,
                "logoUrl" to store.logoUrl,
                "bannerUrl" to store.bannerUrl,
                "description" to store.description,
                "categoryId" to store.categoryId,
                "rating" to store.rating,
                "isVerified" to store.isVerified,
                "status" to "active",
                "createdAt" to store.createdAt
            )
            db.collection("stores").document(store.id).set(storeMap).await()
            Result.success(store)
        } catch (e: Exception) {
            Log.e(tag, "Could not create store", e)
            Result.failure(e)
        }
    }

    override suspend fun getStoreByOwnerId(ownerId: String): Store? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection("stores")
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            mapFirestoreDocToStore(doc)
        } catch (e: Exception) {
            Log.e(tag, "getStoreByOwnerId failed for $ownerId", e)
            null
        }
    }

    override suspend fun updateStore(store: Store): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val storeMap = hashMapOf(
                "storeName" to store.name,
                "name" to store.name,
                "description" to store.description,
                "categoryId" to store.categoryId,
                "logoUrl" to store.logoUrl,
                "bannerUrl" to store.bannerUrl,
                "status" to store.status
            )
            db.collection("stores").document(store.id).update(storeMap as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "updateStore failed for ${store.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun checkIfStoreExists(ownerId: String): Boolean {
        val db = firestore ?: return false
        return try {
            val snapshot = db.collection("stores")
                .whereEqualTo("ownerId", ownerId)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
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
            val productRef = db.collection("products").document()

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
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
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
                "categoryId" to categoryId,
                "isAvailable" to true,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(storeRef, storeMap)
            batch.set(productRef, productMap)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

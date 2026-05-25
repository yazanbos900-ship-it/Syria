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
                try {
                    Store(
                        id = doc.id,
                        name = doc.getString("storeName") ?: doc.getString("name") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerUsername = doc.getString("ownerUsername") ?: "",
                        logoUrl = doc.getString("logoUrl"),
                        bannerUrl = doc.getString("bannerUrl"),
                        description = doc.getString("description") ?: "",
                        followersCount = (doc.getLong("followersCount") ?: 0).toInt(),
                        status = doc.getString("status") ?: "active",
                        rating = doc.getDouble("rating")?.toFloat() ?: 5.0f,
                        isVerified = doc.getBoolean("isVerified") ?: false,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            trySend(Result.success(list))
        } else {
            trySend(Result.success(emptyList()))
        }
    }

    override suspend fun getStoreById(storeId: String): Store? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("stores").document(storeId).get().await()
            if (doc.exists()) {
                Store(
                    id = doc.id,
                    name = doc.getString("name") ?: doc.getString("storeName") ?: "",
                    ownerId = doc.getString("ownerId") ?: "",
                    logoUrl = doc.getString("logoUrl"),
                    bannerUrl = doc.getString("bannerUrl"),
                    description = doc.getString("description") ?: "",
                    rating = doc.getDouble("rating")?.toFloat() ?: 5.0f,
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading store details for ID $storeId", e)
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
}

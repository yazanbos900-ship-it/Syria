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

    override fun getStores(): Flow<List<Store>> = callbackFlow {
        val db = firestore ?: {
            trySend(emptyList<Store>())
            close()
        }
        if (db is Function0<*>) return@callbackFlow

        val dbInstance = db as FirebaseFirestore
        val subscription = dbInstance.collection("stores")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        Store(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            ownerId = doc.getString("ownerId") ?: "",
                            logoUrl = doc.getString("logoUrl"),
                            bannerUrl = doc.getString("bannerUrl"),
                            description = doc.getString("description") ?: "",
                            rating = doc.getDouble("rating")?.toFloat() ?: 5.0f,
                            isVerified = doc.getBoolean("isVerified") ?: false,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getStoreById(storeId: String): Store? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("stores").document(storeId).get().await()
            if (doc.exists()) {
                Store(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
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

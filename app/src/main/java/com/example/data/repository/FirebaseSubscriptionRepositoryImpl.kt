package com.example.data.repository

import android.util.Log
import com.example.domain.model.SubscriptionRequest
import com.example.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseSubscriptionRepositoryImpl : SubscriptionRepository {
    private val tag = "FirebaseSubscription"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is currently unavailable", e)
            null
        }
    }

    override suspend fun submitSubscriptionRequest(request: SubscriptionRequest): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val docRef = if (request.requestId.isNotEmpty()) {
                db.collection("subscription_requests").document(request.requestId)
            } else {
                db.collection("subscription_requests").document()
            }
            val id = docRef.id
            val finalRequest = request.copy(requestId = id)
            
            val requestMap = mapOf(
                "requestId" to id,
                "userId" to finalRequest.userId,
                "storeId" to finalRequest.storeId,
                "requestedTier" to finalRequest.requestedTier,
                "requestDate" to finalRequest.requestDate,
                "status" to finalRequest.status
            )
            docRef.set(requestMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSubscriptionRequestsByStore(storeId: String): Flow<Result<List<SubscriptionRequest>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        if (storeId.isEmpty()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val subscription = db.collection("subscription_requests")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            SubscriptionRequest(
                                requestId = doc.getString("requestId") ?: doc.id,
                                userId = doc.getString("userId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                requestedTier = doc.getString("requestedTier") ?: "",
                                requestDate = doc.getLong("requestDate") ?: 0L,
                                status = doc.getString("status") ?: "pending"
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing SubscriptionRequest", e)
                            null
                        }
                    }
                    trySend(Result.success(requests))
                }
            }
        awaitClose { subscription.remove() }
    }
}

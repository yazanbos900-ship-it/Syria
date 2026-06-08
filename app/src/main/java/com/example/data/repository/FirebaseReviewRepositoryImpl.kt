package com.example.data.repository

import com.example.domain.model.Review
import com.example.domain.repository.ReviewRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseReviewRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ReviewRepository {

    override fun getReviews(productId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection("products")
            .document(productId)
            .collection("reviews")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reviews = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Review::class.java)?.copy(id = doc.id, productId = productId)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(reviews)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addReview(review: Review): Result<Unit> = runCatching {
        val productRef = firestore.collection("products").document(review.productId)
        val reviewRef = productRef.collection("reviews").document()
        
        val newReview = review.copy(id = reviewRef.id)

        firestore.runTransaction { transaction ->
            val productSnapshot = transaction.get(productRef)
            val currentTotalRatings = productSnapshot.getLong("totalRatings") ?: productSnapshot.getLong("reviewCount") ?: 0L
            val currentAverageRating = productSnapshot.getDouble("rating") ?: 0.0

            val newTotalRatings = currentTotalRatings + 1
            val newAverageRating = if (newTotalRatings > 0) {
                ((currentAverageRating * currentTotalRatings) + review.rating) / newTotalRatings
            } else {
                review.rating.toDouble()
            }

            transaction.set(reviewRef, newReview)
            transaction.update(
                productRef,
                mapOf(
                    "rating" to newAverageRating,
                    "reviewCount" to newTotalRatings,
                    "totalRatings" to newTotalRatings
                )
            )
        }.await()
    }

    override suspend fun updateReview(review: Review): Result<Unit> = runCatching {
        val productRef = firestore.collection("products").document(review.productId)
        val reviewRef = productRef.collection("reviews").document(review.id)

        firestore.runTransaction { transaction ->
            val oldReviewSnapshot = transaction.get(reviewRef)
            val oldRating = oldReviewSnapshot.getLong("rating")?.toInt() ?: 0

            val productSnapshot = transaction.get(productRef)
            val currentTotalRatings = productSnapshot.getLong("totalRatings") ?: productSnapshot.getLong("reviewCount") ?: 0L
            val currentAverageRating = productSnapshot.getDouble("rating") ?: 0.0

            var newAverageRating = currentAverageRating
            if (currentTotalRatings > 0) {
                val totalSum = (currentAverageRating * currentTotalRatings) - oldRating + review.rating
                newAverageRating = totalSum / currentTotalRatings
            }

            transaction.set(reviewRef, review)
            transaction.update(
                productRef,
                mapOf(
                    "rating" to newAverageRating
                )
            )
        }.await()
    }

    override suspend fun deleteReview(reviewId: String, productId: String): Result<Unit> = runCatching {
        val productRef = firestore.collection("products").document(productId)
        val reviewRef = productRef.collection("reviews").document(reviewId)

        firestore.runTransaction { transaction ->
            val reviewSnapshot = transaction.get(reviewRef)
            val oldRating = reviewSnapshot.getLong("rating")?.toInt() ?: 0

            val productSnapshot = transaction.get(productRef)
            val currentTotalRatings = productSnapshot.getLong("totalRatings") ?: productSnapshot.getLong("reviewCount") ?: 0L
            val currentAverageRating = productSnapshot.getDouble("rating") ?: 0.0

            val newTotalRatings = if (currentTotalRatings > 0) currentTotalRatings - 1 else 0
            val newAverageRating = if (newTotalRatings > 0) {
                ((currentAverageRating * currentTotalRatings) - oldRating) / newTotalRatings
            } else {
                0.0
            }

            transaction.delete(reviewRef)
            transaction.update(
                productRef,
                mapOf(
                    "rating" to newAverageRating,
                    "reviewCount" to newTotalRatings,
                    "totalRatings" to newTotalRatings
                )
            )
        }.await()
    }

    override suspend fun getUserReviewForProduct(productId: String, userId: String): Result<Review?> = runCatching {
        val snapshot = firestore.collection("products")
            .document(productId)
            .collection("reviews")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .await()
            
        if (snapshot.isEmpty) {
            null
        } else {
            val doc = snapshot.documents.first()
            doc.toObject(Review::class.java)?.copy(id = doc.id, productId = productId)
        }
    }
}

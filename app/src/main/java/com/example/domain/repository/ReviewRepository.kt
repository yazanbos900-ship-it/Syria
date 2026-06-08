package com.example.domain.repository

import com.example.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getReviews(productId: String): Flow<List<Review>>
    suspend fun addReview(review: Review): Result<Unit>
    suspend fun updateReview(review: Review): Result<Unit>
    suspend fun deleteReview(reviewId: String, productId: String): Result<Unit>
    suspend fun getUserReviewForProduct(productId: String, userId: String): Result<Review?>
}

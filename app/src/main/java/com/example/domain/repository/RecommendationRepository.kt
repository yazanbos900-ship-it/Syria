package com.example.domain.repository

import com.example.domain.model.Product
import com.example.domain.model.RecommendationCriteria
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun getRecommendations(
        criteria: RecommendationCriteria,
        userId: String?,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<Result<List<Product>>>

    suspend fun trackProductInteraction(
        productId: String,
        categoryId: String,
        storeId: String,
        userId: String?,
        interactionType: String // e.g., "view", "favorite", "click"
    ): Result<Unit>
}

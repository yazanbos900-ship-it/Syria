package com.example.domain.usecase

import com.example.domain.model.Product
import com.example.domain.model.RecommendationCriteria
import com.example.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow

class GetRecommendationsUseCase(
    private val recommendationRepository: RecommendationRepository
) {
    operator fun invoke(
        criteria: RecommendationCriteria,
        userId: String?,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<Result<List<Product>>> {
        return recommendationRepository.getRecommendations(criteria, userId, limit, offset)
    }
}

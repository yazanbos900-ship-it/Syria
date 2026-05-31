package com.example.domain.usecase

import com.example.domain.repository.RecommendationRepository

class TrackProductInteractionUseCase(
    private val recommendationRepository: RecommendationRepository
) {
    suspend operator fun invoke(
        productId: String,
        categoryId: String,
        storeId: String,
        userId: String?,
        interactionType: String = "view"
    ): Result<Unit> {
        return recommendationRepository.trackProductInteraction(
            productId = productId,
            categoryId = categoryId,
            storeId = storeId,
            userId = userId,
            interactionType = interactionType
        )
    }
}

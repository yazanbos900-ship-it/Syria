package com.example.domain.usecase

import com.example.domain.model.SubscriptionRequest
import com.example.domain.repository.SubscriptionRepository

class SubmitSubscriptionRequestUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(request: SubscriptionRequest): Result<Unit> {
        if (request.userId.isBlank() || request.storeId.isBlank() || request.requestedTier.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID, Store ID, and Requested Tier must not be blank"))
        }
        return repository.submitSubscriptionRequest(request)
    }
}

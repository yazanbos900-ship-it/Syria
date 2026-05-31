package com.example.domain.usecase

import com.example.domain.model.SubscriptionRequest
import com.example.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class GetSubscriptionRequestsByStoreUseCase(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(storeId: String): Flow<Result<List<SubscriptionRequest>>> {
        return repository.getSubscriptionRequestsByStore(storeId)
    }
}

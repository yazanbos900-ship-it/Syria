package com.example.domain.repository

import com.example.domain.model.SubscriptionRequest
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    suspend fun submitSubscriptionRequest(request: SubscriptionRequest): Result<Unit>
    fun getSubscriptionRequestsByStore(storeId: String): Flow<Result<List<SubscriptionRequest>>>
}

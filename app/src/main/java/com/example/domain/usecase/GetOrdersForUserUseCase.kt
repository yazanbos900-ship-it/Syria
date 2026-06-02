package com.example.domain.usecase

import com.example.domain.model.Order
import com.example.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow

class GetOrdersForUserUseCase(
    private val repository: OrderRepository
) {
    operator fun invoke(userId: String): Flow<Result<List<Order>>> {
        return repository.getOrdersForUser(userId)
    }
}

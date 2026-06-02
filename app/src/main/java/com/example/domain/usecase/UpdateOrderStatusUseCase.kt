package com.example.domain.usecase

import com.example.domain.repository.OrderRepository

class UpdateOrderStatusUseCase(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, status: String): Result<Unit> {
        return repository.updateOrderStatus(orderId, status)
    }
}

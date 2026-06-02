package com.example.domain.usecase

import com.example.domain.model.Order
import com.example.domain.repository.OrderRepository

class CreateOrderUseCase(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(order: Order): Result<Unit> {
        return repository.createOrder(order)
    }
}

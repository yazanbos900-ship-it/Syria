package com.example.domain.repository

import com.example.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrdersForUser(userId: String): Flow<Result<List<Order>>>
    fun getOrdersForStore(storeId: String): Flow<Result<List<Order>>>
    fun getAllOrders(): Flow<Result<List<Order>>>
    suspend fun createOrder(order: Order): Result<Unit>
    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit>
}

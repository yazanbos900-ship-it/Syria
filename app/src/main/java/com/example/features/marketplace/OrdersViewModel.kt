package com.example.features.marketplace

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Order
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CartRepository
import com.example.domain.usecase.GetOrdersForUserUseCase
import com.example.domain.usecase.UpdateOrderStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrderStatusTab(val key: String, val labelEn: String, val labelAr: String) {
    ALL("All", "All", "الكل"),
    PENDING("Pending", "Pending", "قيد الانتظار"),
    PROCESSING("Processing", "Processing", "قيد التحضير"),
    SHIPPED("Shipped", "Shipped", "تم الشحن"),
    DELIVERED("Delivered", "Delivered", "تم التوصيل"),
    CANCELLED("Cancelled", "Cancelled", "تم الإلغاء")
}

data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val filteredOrders: List<Order> = emptyList(),
    val selectedTab: OrderStatusTab = OrderStatusTab.ALL,
    val error: String? = null,
    val reorderSuccess: Boolean = false,
    val userId: String = ""
)

class OrdersViewModel(
    private val getOrdersForUserUseCase: GetOrdersForUserUseCase,
    private val authRepo: AuthRepository,
    private val cartRepo: CartRepository,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase
) : ViewModel() {

    private val tag = "OrdersViewModel"
    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    private var ordersJob: Job? = null

    init {
        loadUserSessionAndOrders()
    }

    private fun loadUserSessionAndOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val session = authRepo.getCurrentUserSession()
                if (session != null) {
                    _state.update { it.copy(userId = session.id) }
                    listenToOrders(session.id)
                } else {
                    _state.update { it.copy(isLoading = false, error = "User session expired or not found") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to authenticate session") }
            }
        }
    }

    private fun listenToOrders(userId: String) {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            getOrdersForUserUseCase(userId).collect { result ->
                if (result.isSuccess) {
                    val list = result.getOrDefault(emptyList())
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            orders = list,
                            filteredOrders = filterOrdersList(list, currentState.selectedTab)
                        )
                    }
                } else {
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to orders feed"
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: OrderStatusTab) {
        _state.update { currentState ->
            currentState.copy(
                selectedTab = tab,
                filteredOrders = filterOrdersList(currentState.orders, tab)
            )
        }
    }

    private fun filterOrdersList(list: List<Order>, tab: OrderStatusTab): List<Order> {
        if (tab == OrderStatusTab.ALL) return list
        return list.filter { order ->
            order.status.equals(tab.key, ignoreCase = true)
        }
    }

    fun reorder(order: Order) {
        val userId = _state.value.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, reorderSuccess = false) }
            try {
                order.items.forEach { orderItem ->
                    val cartItem = CartItem(
                        id = orderItem.productId,
                        name = orderItem.productName,
                        price = orderItem.unitPrice,
                        originalPrice = null,
                        image = orderItem.productImage,
                        size = null,
                        storeName = order.storeName,
                        quantity = orderItem.quantity
                    )
                    cartRepo.addToCart(userId, cartItem, orderItem.quantity)
                }
                _state.update { it.copy(isLoading = false, reorderSuccess = true) }
            } catch (e: Exception) {
                Log.e(tag, "Reordering items failed", e)
                _state.update { it.copy(isLoading = false, error = "Failed to copy items into shopping cart") }
            }
        }
    }

    fun dismissReorderSuccess() {
        _state.update { it.copy(reorderSuccess = false) }
    }

    fun resetError() {
        _state.update { it.copy(error = null) }
    }

    fun simulateStatusTransition(orderId: String, currentStatus: String) {
        val nextStatus = when (currentStatus.lowercase()) {
            "pending" -> "Processing"
            "processing" -> "Shipped"
            "shipped" -> "Delivered"
            "delivered" -> "Cancelled"
            else -> "Pending"
        }
        viewModelScope.launch {
            updateOrderStatusUseCase(orderId, nextStatus)
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            updateOrderStatusUseCase(orderId, "Cancelled")
        }
    }
}

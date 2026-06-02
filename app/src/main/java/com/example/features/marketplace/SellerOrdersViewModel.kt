package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Order
import com.example.domain.model.Store
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.StoreRepository
import com.example.domain.usecase.GetOrdersForStoreUseCase
import com.example.domain.usecase.UpdateOrderStatusUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SellerOrdersUiState(
    val isLoading: Boolean = false,
    val store: Store? = null,
    val orders: List<Order> = emptyList(),
    val error: String? = null,
    val actionSuccess: Boolean = false
)

class SellerOrdersViewModel(
    private val authRepo: AuthRepository,
    private val storeRepo: StoreRepository,
    private val getOrdersForStoreUseCase: GetOrdersForStoreUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SellerOrdersUiState())
    val state: StateFlow<SellerOrdersUiState> = _state.asStateFlow()

    private var ordersJob: Job? = null

    init {
        loadSellerStoreAndOrders()
    }

    fun loadSellerStoreAndOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val currentUser = authRepo.getCurrentUserSession()
                if (currentUser == null) {
                    _state.update { it.copy(isLoading = false, error = "User session not found") }
                    return@launch
                }

                val store = storeRepo.getStoreByOwnerId(currentUser.id)
                if (store == null) {
                    _state.update { it.copy(isLoading = false, error = "You do not own an active store") }
                    return@launch
                }

                _state.update { it.copy(store = store) }

                // Cancel existing job if active
                ordersJob?.cancel()

                // Register real-time listener for this bookstore/store orders
                ordersJob = getOrdersForStoreUseCase(store.id)
                    .onEach { result ->
                        result.fold(
                            onSuccess = { orderList ->
                                _state.update { it.copy(isLoading = false, orders = orderList) }
                            },
                            onFailure = { throwable ->
                                _state.update { it.copy(isLoading = false, error = throwable.message) }
                            }
                        )
                    }
                    .launchIn(viewModelScope)

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Unknown error loading store") }
            }
        }
    }

    fun updateOrderStatus(orderId: String, nextStatus: String, onSuccessEvent: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = updateOrderStatusUseCase(orderId, nextStatus)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, actionSuccess = true) }
                    onSuccessEvent()
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, error = throwable.message) }
                }
            )
        }
    }

    fun resetActionState() {
        _state.update { it.copy(actionSuccess = false, error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        ordersJob?.cancel()
    }
}

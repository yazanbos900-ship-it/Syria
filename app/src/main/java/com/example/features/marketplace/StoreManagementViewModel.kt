package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.domain.model.SubscriptionRequest
import com.example.core.di.ServiceLocator
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.StoreRepository
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StoreManagementUiState(
    val isLoading: Boolean = false,
    val store: Store? = null,
    val products: List<Product> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false
)

class StoreManagementViewModel(
    private val storeRepo: StoreRepository,
    private val productRepo: ProductRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoreManagementUiState())
    val state: StateFlow<StoreManagementUiState> = _state.asStateFlow()

    fun loadStoreAndProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val currentUser = authRepo.getCurrentUserSession()
            if (currentUser == null) {
                _state.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            val store = storeRepo.getStoreByOwnerId(currentUser.id)
            if (store == null) {
                _state.update { it.copy(isLoading = false, error = "Store not found") }
                return@launch
            }

            _state.update { it.copy(store = store) }

            // Real-time products flow
            productRepo.getProductsByStoreId(store.id)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { products ->
                    _state.update { it.copy(isLoading = false, products = products) }
                }
        }
    }

    fun addProduct(title: String, price: Double, description: String, imageUrls: List<String>, categoryId: String, currency: String = "USD") {
        val storeId = state.value.store?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val product = Product(
                id = "", // Firestore will generate
                title = title,
                description = description,
                price = price,
                imageUrls = imageUrls,
                categoryId = categoryId,
                storeId = storeId,
                rating = 4.5f,
                reviewCount = 0,
                isAvailable = true,
                stockCount = 100,
                currency = currency,
                createdAt = System.currentTimeMillis()
            )
            val result = productRepo.addProduct(product)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun resetSuccess() {
        _state.update { it.copy(isSuccess = false) }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = productRepo.updateProduct(product)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = productRepo.deleteProduct(productId)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun updateStore(name: String, description: String, usdExchangeRate: Double, categoryId: String, logoUrl: String?, bannerUrl: String?, defaultCurrency: String) {
        val currentStore = state.value.store ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val updatedStore = currentStore.copy(
                name = name,
                description = description,
                categoryId = categoryId,
                logoUrl = logoUrl ?: currentStore.logoUrl,
                bannerUrl = bannerUrl ?: currentStore.bannerUrl,
                usdExchangeRate = usdExchangeRate,
                defaultCurrency = defaultCurrency
            )
            val result = storeRepo.updateStore(updatedStore)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, store = updatedStore) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun requestSubscription(tier: String) {
        val currentStore = state.value.store ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val request = SubscriptionRequest(
                requestId = java.util.UUID.randomUUID().toString(),
                userId = currentStore.ownerId,
                storeId = currentStore.id,
                requestedTier = tier,
                requestDate = System.currentTimeMillis(),
                status = "pending"
            )
            val result = ServiceLocator.subscriptionRepository.submitSubscriptionRequest(request)
            if (result.isSuccess) {
                val updatedStore = currentStore.copy(subscriptionTier = tier)
                _state.update { it.copy(isLoading = false, store = updatedStore, isSuccess = true) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

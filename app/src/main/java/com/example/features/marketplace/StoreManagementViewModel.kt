package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Store
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

    fun addProduct(title: String, price: Double, description: String, imageUrls: List<String>, categoryId: String) {
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
                rating = 0f,
                reviewCount = 0,
                isAvailable = true,
                stockCount = 100,
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

    fun updateStore(name: String, description: String, categoryId: String, logoUrl: String?) {
        val currentStore = state.value.store ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val updatedStore = currentStore.copy(
                name = name,
                description = description,
                categoryId = categoryId,
                logoUrl = logoUrl ?: currentStore.logoUrl
            )
            val result = storeRepo.updateStore(updatedStore)
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, store = updatedStore) }
            } else {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

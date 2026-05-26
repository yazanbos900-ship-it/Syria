package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val store: Store? = null,
    val products: List<Product> = emptyList(),
    val isFollowing: Boolean = false,
    val currentUserId: String? = null
)

class StoreDetailViewModel(
    private val storeRepo: StoreRepository,
    private val productRepo: ProductRepository,
    private val authRepo: com.example.domain.repository.AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoreDetailUiState())
    val state: StateFlow<StoreDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.currentUser.collect { user ->
                _state.update { it.copy(currentUserId = user?.id) }
            }
        }
    }

    fun loadStore(storeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                android.util.Log.d("DEBUG_STORE", "=== loadStore called with storeId: $storeId ===")
                
                val store = storeRepo.getStoreById(storeId)
                if (store == null) {
                    android.util.Log.d("DEBUG_STORE", "Store NOT FOUND for id: $storeId")
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = "لم يتم العثور على المتجر"
                        )
                    }
                    return@launch
                }
                
                android.util.Log.d("DEBUG_STORE", "Store found: ${store.name}, id: ${store.id}")
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        store = store
                    )
                }
                
                loadProducts(storeId)
                
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_STORE", "Exception in loadStore: ${e.message}", e)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "حدث خطأ"
                    )
                }
            }
        }
    }
    
    private fun loadProducts(storeId: String) {
        viewModelScope.launch {
            productRepo.getProductsByStoreId(storeId)
                .catch { e ->
                    android.util.Log.e("DEBUG_STORE", "Products flow error: ${e.message}", e)
                    _state.update { it.copy(products = emptyList()) }
                }
                .collect { products ->
                    android.util.Log.d("DEBUG_STORE", "Products received: ${products.size}")
                    products.forEach { 
                        android.util.Log.d("DEBUG_STORE", "  Product: ${it.title}, storeId: ${it.storeId}")
                    }
                    _state.update { it.copy(products = products) }
                }
        }
    }
}

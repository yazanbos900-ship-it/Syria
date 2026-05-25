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
import kotlinx.coroutines.launch

sealed class StoreDetailUiState {
    object Loading : StoreDetailUiState()
    data class Error(val msg: String) : StoreDetailUiState()
    data class Success(
        val store: Store,
        val products: List<Product>,
        val isFollowing: Boolean = false
    ) : StoreDetailUiState()
}

class StoreDetailViewModel(
    private val storeRepo: StoreRepository,
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow<StoreDetailUiState>(StoreDetailUiState.Loading)
    val state: StateFlow<StoreDetailUiState> = _state.asStateFlow()

    fun loadStore(storeId: String) {
        viewModelScope.launch {
            _state.value = StoreDetailUiState.Loading
            try {
                val store = storeRepo.getStoreById(storeId)
                if (store == null) {
                    _state.value = StoreDetailUiState.Error("لم يتم العثور على المتجر")
                    return@launch
                }
                
                productRepo.getProductsByStoreId(storeId)
                    .catch { e ->
                        _state.value = StoreDetailUiState.Success(store, emptyList())
                    }
                    .collect { products ->
                        _state.value = StoreDetailUiState.Success(store, products)
                    }

            } catch (e: Exception) {
                _state.value = StoreDetailUiState.Error(e.message ?: "حدث خطأ غير معروف")
            }
        }
    }
}

package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val error: String? = null
)

class HomeProductsViewModel(
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductListUiState())
    val state: StateFlow<ProductListUiState> = _state.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            productRepo.getProducts()
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المنتجات") }
                }
                .collect { products ->
                    _state.update { it.copy(isLoading = false, products = products) }
                }
        }
    }
}

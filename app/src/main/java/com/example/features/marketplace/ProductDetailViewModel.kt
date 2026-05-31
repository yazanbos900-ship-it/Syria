package com.example.features.marketplace

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.RecommendationCriteria
import com.example.domain.model.Store
import com.example.domain.model.ComparisonResult
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.RecommendationRepository
import com.example.domain.repository.StoreRepository
import com.example.domain.repository.ComparisonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val store: Store? = null,
    val error: String? = null,
    
    // Recommendations business flow states
    val recsLoading: Boolean = false,
    val recsAppending: Boolean = false,
    val recommendations: List<Product> = emptyList(),
    val recsError: String? = null,
    val selectedCriteria: RecommendationCriteria = RecommendationCriteria.BEST_RATED,
    val pageLimit: Int = 8,
    val currentOffset: Int = 0,
    val hasReachedEnd: Boolean = false,

    // Real-time market comparison states
    val comparisonLoading: Boolean = false,
    val comparisonResult: ComparisonResult? = null,
    val comparisonError: String? = null
)

class ProductDetailViewModel(
    private val productRepo: ProductRepository,
    private val storeRepo: StoreRepository,
    private val recommendationRepo: RecommendationRepository,
    private val comparisonRepo: ComparisonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    private var recommendationsJob: Job? = null
    private var comparisonJob: Job? = null

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val product = productRepo.getProductDetails(productId)
                if (product != null) {
                    val store = storeRepo.getStoreById(product.storeId)
                    _state.update { it.copy(isLoading = false, product = product, store = store) }

                    // Automatically track interaction
                    trackProductInteraction(productId, product.categoryId, product.storeId)

                    // Seed recommendations matching initial best option, or preference
                    loadRecommendations(
                        criteria = _state.value.selectedCriteria,
                        isRefresh = true
                    )

                    // Seed real-time product comparisons from Firestore
                    loadComparison(product)
                } else {
                    _state.update { it.copy(isLoading = false, error = "المنتج غير موجود") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المنتج") }
            }
        }
    }

    private fun loadComparison(product: Product) {
        comparisonJob?.cancel()
        _state.update { it.copy(comparisonLoading = true, comparisonError = null) }
        comparisonJob = viewModelScope.launch {
            comparisonRepo.getProductComparison(product)
                .catch { exception ->
                    _state.update { it.copy(comparisonLoading = false, comparisonError = exception.message) }
                }
                .collect { result ->
                    if (result.isSuccess) {
                        _state.update { it.copy(comparisonLoading = false, comparisonResult = result.getOrNull()) }
                    } else {
                        _state.update { it.copy(comparisonLoading = false, comparisonError = result.exceptionOrNull()?.message) }
                    }
                }
        }
    }

    fun changeRecommendationCriteria(newCriteria: RecommendationCriteria) {
        if (_state.value.selectedCriteria == newCriteria) return
        _state.update { it.copy(selectedCriteria = newCriteria, recommendations = emptyList(), currentOffset = 0, hasReachedEnd = false) }
        loadRecommendations(criteria = newCriteria, isRefresh = true)
    }

    fun loadMoreRecommendations() {
        val currentState = _state.value
        if (currentState.recsLoading || currentState.recsAppending || currentState.hasReachedEnd) return
        
        val nextOffset = currentState.recommendations.size
        _state.update { it.copy(currentOffset = nextOffset, recsAppending = true) }
        
        loadRecommendations(criteria = currentState.selectedCriteria, isRefresh = false)
    }

    private fun loadRecommendations(criteria: RecommendationCriteria, isRefresh: Boolean) {
        recommendationsJob?.cancel()
        
        val limit = _state.value.pageLimit
        val offset = if (isRefresh) 0 else _state.value.currentOffset
        
        if (isRefresh) {
            _state.update { it.copy(recsLoading = true, recsError = null) }
        }
        
        recommendationsJob = viewModelScope.launch {
            recommendationRepo.getRecommendations(
                criteria = criteria,
                userId = null, // Anonymous or placeholder user context
                limit = limit,
                offset = offset
            ).catch { exception ->
                _state.update { 
                    it.copy(
                        recsLoading = false, 
                        recsAppending = false, 
                        recsError = exception.message ?: "فشلت عملية جلب التواصي"
                    ) 
                }
            }.collect { result ->
                if (result.isSuccess) {
                    val incomingList = result.getOrDefault(emptyList())
                    // Filter out current active product for variety
                    val currentProductId = _state.value.product?.id
                    val filteredIncoming = incomingList.filter { it.id != currentProductId }
                    
                    _state.update { currentState ->
                        val updatedList = if (isRefresh) {
                            filteredIncoming
                        } else {
                            currentState.recommendations + filteredIncoming
                        }
                        
                        currentState.copy(
                            recsLoading = false,
                            recsAppending = false,
                            recommendations = updatedList,
                            hasReachedEnd = incomingList.size < limit
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            recsLoading = false,
                            recsAppending = false,
                            recsError = result.exceptionOrNull()?.message ?: "عذراً، فشلت عملية تحميل التواصي"
                        )
                    }
                }
            }
        }
    }

    fun trackProductInteraction(productId: String, categoryId: String, storeId: String) {
        viewModelScope.launch {
            try {
                recommendationRepo.trackProductInteraction(
                    productId = productId,
                    categoryId = categoryId,
                    storeId = storeId,
                    userId = null,
                    interactionType = "view"
                )
            } catch (e: Exception) {
                Log.e("ProductDetailVM", "Telemetry tracking view error", e)
            }
        }
    }
    
    fun trackRecommendationClick(productId: String, categoryId: String, storeId: String) {
        viewModelScope.launch {
            try {
                recommendationRepo.trackProductInteraction(
                    productId = productId,
                    categoryId = categoryId,
                    storeId = storeId,
                    userId = null,
                    interactionType = "click"
                )
            } catch (e: Exception) {
                Log.e("ProductDetailVM", "Telemetry tracking click error", e)
            }
        }
    }
}

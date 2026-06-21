package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Job
import com.example.core.di.ServiceLocator
import com.example.domain.model.RecommendationCriteria
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.RecommendationRepository
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val error: String? = null
)

data class JobListUiState(
    val isLoading: Boolean = false,
    val jobs: List<Job> = emptyList(),
    val error: String? = null
)

class HomeProductsViewModel(
    private val productRepo: ProductRepository,
    private val recommendationRepo: RecommendationRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    // 1. Featured / All Products
    private val _featuredState = MutableStateFlow(ProductListUiState(isLoading = true))
    val featuredState: StateFlow<ProductListUiState> = _featuredState.asStateFlow()

    // 2. New Arrivals
    private val _newArrivalsState = MutableStateFlow(ProductListUiState(isLoading = true))
    val newArrivalsState: StateFlow<ProductListUiState> = _newArrivalsState.asStateFlow()

    // 3. Trending Now
    private val _trendingState = MutableStateFlow(ProductListUiState(isLoading = true))
    val trendingState: StateFlow<ProductListUiState> = _trendingState.asStateFlow()

    // 4. Community Recommendations (existing engine)
    private val _recommendationsState = MutableStateFlow(ProductListUiState(isLoading = true))
    val recommendationsState: StateFlow<ProductListUiState> = _recommendationsState.asStateFlow()

    // 5. Best Rated Products
    private val _bestRatedState = MutableStateFlow(ProductListUiState(isLoading = true))
    val bestRatedState: StateFlow<ProductListUiState> = _bestRatedState.asStateFlow()

    // 6. Featured Jobs
    private val _featuredJobsState = MutableStateFlow(JobListUiState(isLoading = true))
    val featuredJobsState: StateFlow<JobListUiState> = _featuredJobsState.asStateFlow()

    // 7. Latest Jobs
    private val _latestJobsState = MutableStateFlow(JobListUiState(isLoading = true))
    val latestJobsState: StateFlow<JobListUiState> = _latestJobsState.asStateFlow()

    init {
        loadAllSections()
    }

    fun loadAllSections() {
        loadFeatured()
        loadNewArrivals()
        loadTrending()
        loadRecommendations()
        loadBestRated()
        loadFeaturedJobs()
        loadLatestJobs()
    }

    private fun loadFeatured() {
        viewModelScope.launch {
            _featuredState.update { it.copy(isLoading = true, error = null) }
            productRepo.getProducts()
                .catch { e ->
                    _featuredState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المنتجات المميزة") }
                }
                .collect { products ->
                    _featuredState.update { it.copy(isLoading = false, products = products) }
                }
        }
    }

    private fun loadNewArrivals() {
        viewModelScope.launch {
            _newArrivalsState.update { it.copy(isLoading = true, error = null) }
            productRepo.getNewArrivals(10)
                .catch { e ->
                    _newArrivalsState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل أحدث المنتجات") }
                }
                .collect { products ->
                    _newArrivalsState.update { it.copy(isLoading = false, products = products) }
                }
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _trendingState.update { it.copy(isLoading = true, error = null) }
            val currentUserId = authRepo.getCurrentUserSession()?.id
            recommendationRepo.getRecommendations(RecommendationCriteria.TRENDING, currentUserId, limit = 10, offset = 0)
                .catch { e ->
                    _trendingState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المنتجات الشائعة") }
                }
                .collect { result ->
                    result.onSuccess { products ->
                        _trendingState.update { it.copy(isLoading = false, products = products, error = null) }
                    }.onFailure { e ->
                        _trendingState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ") }
                    }
                }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _recommendationsState.update { it.copy(isLoading = true, error = null) }
            val currentUserId = authRepo.getCurrentUserSession()?.id
            recommendationRepo.getRecommendations(RecommendationCriteria.CATEGORY_PREFERENCE, currentUserId, limit = 10, offset = 0)
                .catch { e ->
                    _recommendationsState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المقترحات") }
                }
                .collect { result ->
                    result.onSuccess { products ->
                        _recommendationsState.update { it.copy(isLoading = false, products = products, error = null) }
                    }.onFailure { e ->
                        _recommendationsState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ") }
                    }
                }
        }
    }

    private fun loadBestRated() {
        viewModelScope.launch {
            _bestRatedState.update { it.copy(isLoading = true, error = null) }
            productRepo.getBestRatedProducts(10)
                .catch { e ->
                    _bestRatedState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل المنتجات الأعلى تقييماً") }
                }
                .collect { products ->
                    _bestRatedState.update { it.copy(isLoading = false, products = products) }
                }
        }
    }

    private fun loadFeaturedJobs() {
        viewModelScope.launch {
            _featuredJobsState.update { it.copy(isLoading = true, error = null) }
            ServiceLocator.jobRepository.getActiveJobs()
                .catch { e ->
                    _featuredJobsState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل الوظائف المميزة") }
                }
                .collect { activeJobs ->
                    val featured = activeJobs.filter { it.isFeatured }
                    val finalJobs = if (featured.isNotEmpty()) featured else activeJobs.filter { it.isStoreVerified }
                    _featuredJobsState.update { it.copy(isLoading = false, jobs = finalJobs.take(5)) }
                }
        }
    }

    private fun loadLatestJobs() {
        viewModelScope.launch {
            _latestJobsState.update { it.copy(isLoading = true, error = null) }
            ServiceLocator.jobRepository.getActiveJobs()
                .catch { e ->
                    _latestJobsState.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ أثناء تحميل أحدث الوظائف") }
                }
                .collect { activeJobs ->
                    val sorted = activeJobs.sortedByDescending { it.createdAt }
                    _latestJobsState.update { it.copy(isLoading = false, jobs = sorted.take(5)) }
                }
        }
    }
}

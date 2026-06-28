package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.StoreRepository
import com.example.domain.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class StoreListUiState(
    val stores: List<Store> = emptyList(),
    val topStores: List<Store> = emptyList(),
    val reputationScores: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoadingTopStores: Boolean = false,
    val error: String? = null,
    val errorTopStores: String? = null
)

class HomeStoresViewModel(private val repository: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(StoreListUiState(isLoading = true, isLoadingTopStores = true))
    val state: StateFlow<StoreListUiState> = _state.asStateFlow()

    init {
        loadStores()
        loadTopStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            repository.getAllStores().collectLatest { result ->
                result.onSuccess { stores ->
                    _state.update { it.copy(stores = stores, isLoading = false, error = null) }
                    triggerReputationCalculation()
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ ما") }
                }
            }
        }
    }

    fun loadTopStores() {
        viewModelScope.launch {
            repository.getTopStores(10).collectLatest { result ->
                result.onSuccess { stores ->
                    _state.update { it.copy(topStores = stores, isLoadingTopStores = false, errorTopStores = null) }
                    triggerReputationCalculation()
                }.onFailure { e ->
                    _state.update { it.copy(isLoadingTopStores = false, errorTopStores = e.message ?: "حدث خطأ ما") }
                }
            }
        }
    }

    private fun triggerReputationCalculation() {
        viewModelScope.launch {
            val currentStores = _state.value.stores
            if (currentStores.isNotEmpty()) {
                val scores = calculateAllReputationScores(currentStores)
                _state.update { state ->
                    val sortedStores = state.stores.sortedByDescending { scores[it.id] ?: 0.0 }
                    val sortedTopStores = state.topStores.sortedByDescending { scores[it.id] ?: 0.0 }
                    state.copy(
                        stores = sortedStores,
                        topStores = sortedTopStores,
                        reputationScores = scores
                    )
                }
            }
        }
    }

    private suspend fun calculateAllReputationScores(stores: List<Store>): Map<String, Double> {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        return try {
            val interactionsSnap = db.collection("interactions").get().await()
            val intentsSnap = db.collection("purchase_intents").get().await()
            val chatsSnap = db.collection("chats").get().await()
            val reviewsSnap = db.collection("reviews").get().await()
            val productsSnap = db.collection("products").get().await()

            val viewsMap = interactionsSnap.documents
                .filter { it.getString("interactionType") == "store_view" || it.getString("interactionType") == "product_view" }
                .groupBy { it.getString("storeId") ?: "" }
                .mapValues { it.value.size }

            val buyClicksMap = intentsSnap.documents
                .groupBy { it.getString("storeId") ?: "" }
                .mapValues { it.value.size }

            val chatsMap = chatsSnap.documents
                .groupBy { it.getString("sellerUid") ?: "" }
                .mapValues { it.value.size }

            val savesMap = interactionsSnap.documents
                .filter { it.getString("interactionType") == "favorite" }
                .groupBy { it.getString("storeId") ?: "" }
                .mapValues { it.value.size }

            val reviewsMap = reviewsSnap.documents
                .groupBy { it.getString("storeId") ?: "" }
                .mapValues { it.value.size }

            val productsMap = productsSnap.documents
                .groupBy { it.getString("storeId") ?: "" }
                .mapValues { it.value.size }

            stores.associate { store ->
                val views = viewsMap[store.id] ?: 0
                val buyClicks = buyClicksMap[store.id] ?: 0
                val chatInquiries = chatsMap[store.ownerId] ?: 0
                val productSaves = savesMap[store.id] ?: 0
                val reviewsCount = reviewsMap[store.id] ?: 0
                val recentProductsCount = productsMap[store.id] ?: 0

                val score = com.example.domain.utils.StoreReputationCalculator.calculateReputationScore(
                    store = store,
                    viewsCount = views,
                    buyNowClicks = buyClicks,
                    chatInquiries = chatInquiries,
                    productSaves = productSaves,
                    reviewsCount = reviewsCount,
                    recentProductsCount = recentProductsCount
                )
                store.id to score
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeStoresVM", "Failed to calculate reputation scores", e)
            stores.associate { it.id to (it.rating * 40.0 + it.followersCount * 15.0) }
        }
    }
}

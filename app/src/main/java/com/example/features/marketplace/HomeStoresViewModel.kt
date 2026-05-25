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

data class StoreListUiState(
    val stores: List<Store> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeStoresViewModel(private val repository: StoreRepository) : ViewModel() {
    private val _state = MutableStateFlow(StoreListUiState(isLoading = true))
    val state: StateFlow<StoreListUiState> = _state.asStateFlow()

    init {
        loadStores()
    }

    fun loadStores() {
        viewModelScope.launch {
            repository.getActiveStores().collectLatest { result ->
                result.onSuccess { stores ->
                    _state.update { it.copy(stores = stores, isLoading = false, error = null) }
                }.onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "حدث خطأ ما") }
                }
            }
        }
    }
}

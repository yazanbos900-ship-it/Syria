package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUiState(
    val userStoreId: String? = null,
    val hasStore: Boolean = false,
    val isLoading: Boolean = false
)

class MarketplaceViewModel(
    private val authRepo: AuthRepository,
    private val storeRepo: StoreRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketplaceUiState())
    val state: StateFlow<MarketplaceUiState> = _state.asStateFlow()

    init {
        checkUserStore()
    }

    private fun checkUserStore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentUser = authRepo.getCurrentUserSession()
            if (currentUser != null) {
                val store = storeRepo.getStoreByOwnerId(currentUser.id)
                _state.update { 
                    it.copy(
                        isLoading = false,
                        hasStore = store != null,
                        userStoreId = store?.id
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, hasStore = false, userStoreId = null) }
            }
        }
    }
}

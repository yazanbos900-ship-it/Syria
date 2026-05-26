package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUiState(
    val user: User? = null,
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
        observeCurrentUserAndStore()
    }

    private fun observeCurrentUserAndStore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            authRepo.currentUser.collect { user ->
                _state.update { it.copy(user = user) }
                if (user != null) {
                    val store = storeRepo.getStoreByOwnerId(user.id)
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            hasStore = store != null,
                            userStoreId = store?.id
                        )
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            hasStore = false,
                            userStoreId = null
                        )
                    }
                }
            }
        }
    }
}

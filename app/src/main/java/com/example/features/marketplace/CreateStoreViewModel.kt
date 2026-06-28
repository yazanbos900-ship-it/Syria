package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreateStoreViewModel(
    private val storeRepository: StoreRepository = ServiceLocator.storeRepository,
    private val uploader: CloudinaryUploader = CloudinaryUploader()
) : ViewModel() {

    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    init {
        fetchCurrentUserSession()
        fetchCategoriesList()
    }

    private fun fetchCurrentUserSession() {
        viewModelScope.launch {
            // Stream the current logged in user from ServiceLocator.authRepository
            ServiceLocator.authRepository.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
        viewModelScope.launch {
            try {
                val sessionUser = ServiceLocator.authRepository.getCurrentUserSession()
                if (sessionUser != null) {
                    _state.update { it.copy(currentUser = sessionUser) }
                }
            } catch (e: Exception) {
                // Fail silently or log
            }
        }
    }

    private fun fetchCategoriesList() {
        viewModelScope.launch {
            _state.update { it.copy(isCategoriesLoading = true) }
            try {
                ServiceLocator.productRepository.getCategories().collect { categoryList ->
                    _state.update { it.copy(categories = categoryList, isCategoriesLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isCategoriesLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun onStoreNameChange(name: String) {
        if (name.length <= 50) {
            _state.update { it.copy(storeName = name) }
        }
    }

    fun onCategorySelected(id: String, name: String) {
        _state.update { it.copy(categoryId = id, categoryName = name) }
    }

    fun onStoreDescriptionChange(description: String) {
        _state.update { it.copy(storeDescription = description) }
    }

    fun setMapVisible(visible: Boolean) {
        _state.update { it.copy(mapVisible = visible) }
    }

    fun selectLocationCoordinates(lat: Double, lng: Double, country: String, city: String, district: String) {
        _state.update { 
            it.copy(
                latitude = lat,
                longitude = lng,
                city = city,
                district = district,
                fullAddress = "$district, $city, $country"
            )
        }
    }

    fun onLogoSelected(uriString: String?) {
        if (uriString == null) {
            _state.update { it.copy(logoUriString = null) }
            return
        }
        val uid = _state.value.currentUser?.id ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            _state.update { it.copy(isLogoUploading = true, error = null) }
            try {
                val uploadResult = uploader.uploadFile(
                    localUriString = uriString
                )
                val url = uploadResult.getOrThrow()
                _state.update { it.copy(logoUriString = url, isLogoUploading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLogoUploading = false, error = "فشل رفع شعار المتجر. الرجاء المحاولة مرة أخرى.") }
            }
        }
    }

    fun onBannerSelected(uriString: String?) {
        if (uriString == null) {
            _state.update { it.copy(bannerUriString = null) }
            return
        }
        val uid = _state.value.currentUser?.id ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            _state.update { it.copy(isBannerUploading = true, error = null) }
            try {
                val uploadResult = uploader.uploadFile(
                    localUriString = uriString
                )
                val url = uploadResult.getOrThrow()
                _state.update { it.copy(bannerUriString = url, isBannerUploading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isBannerUploading = false, error = "فشل رفع غلاف المتجر. الرجاء المحاولة مرة أخرى.") }
            }
        }
    }


    fun nextStep() {
        val currentStep = _state.value.currentStep
        if (currentStep == 1 && _state.value.isStep1Valid) {
            _state.update { it.copy(currentStep = 2) }
        }
    }

    fun prevStep() {
        val currentStep = _state.value.currentStep
        if (currentStep > 1) {
            _state.update { it.copy(currentStep = currentStep - 1) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun resetState() {
        _state.update { StoreUiState() }
        fetchCurrentUserSession()
        fetchCategoriesList()
    }

    fun submitStore() {
        val currentState = _state.value
        val uid = currentState.currentUser?.id ?: UUID.randomUUID().toString()
        // Auto-formulate a consistent read-only clean handles username representation
        val cleanName = currentState.currentUser?.name?.replace("\\s+".toRegex(), "")?.lowercase() ?: "user_${uid.take(4)}"
        val username = "@$cleanName"
        val storeId = UUID.randomUUID().toString()

        if (!currentState.isStep1Valid || !currentState.isStep2Valid) {
            _state.update { it.copy(error = "يرجى إكمال وتدقيق جميع الخيارات والبيانات المطلوبة قبل الإرسال.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Check if store already exists
                if (storeRepository.checkIfStoreExists(uid)) {
                    _state.update { it.copy(isLoading = false, error = "لديك متجر نشط بالفعل.") }
                    return@launch
                }
                
                // 1. Store Logo is already uploaded, just use its URL
                val logoUrl = currentState.logoUriString!!
                
                // 2. Store Banner is already uploaded if provided
                val bannerUrl = currentState.bannerUriString

                // 3. Create Store object
                val store = com.example.domain.model.Store(
                    id = storeId,
                    name = currentState.storeName,
                    ownerId = uid,
                    ownerUsername = username,
                    logoUrl = logoUrl,
                    bannerUrl = bannerUrl,
                    description = currentState.storeDescription,
                    categoryId = currentState.categoryId,
                    latitude = currentState.latitude,
                    longitude = currentState.longitude,
                    city = currentState.city,
                    district = currentState.district,
                    fullAddress = currentState.fullAddress
                )

                // 4. Save Store to Firestore
                val dbResult = storeRepository.createStore(store)

                if (dbResult.isSuccess) {
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    val excMsg = dbResult.exceptionOrNull()?.localizedMessage ?: "Firestore Exception"
                    _state.update { it.copy(isLoading = false, error = excMsg) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}

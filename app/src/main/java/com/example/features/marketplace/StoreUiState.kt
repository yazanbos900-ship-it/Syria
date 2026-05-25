package com.example.features.marketplace

import com.example.domain.model.Category
import com.example.domain.model.User

data class StoreUiState(
    val currentStep: Int = 1,
    
    // Step 1 - Store Info
    val storeName: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val storeDescription: String = "",
    
    // Step 2 - Store Identity (Images)
    val logoUriString: String? = null,
    val bannerUriString: String? = null,
    
    // Step 3 - First Product
    val productName: String = "",
    val productPrice: String = "",
    val productDescription: String = "",
    val productImageUris: List<String> = emptyList(),
    
    // Remote Data & User Sessions
    val categories: List<Category> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val currentUser: User? = null,
    
    // Loading & Submitting Flow state
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
) {
    // Computed screen validations to keep code clean and testable
    val isStep1Valid: Boolean
        get() = storeName.isNotBlank() && 
                storeName.length <= 50 && 
                categoryId.isNotBlank() && 
                storeDescription.isNotBlank() && 
                storeDescription.length >= 50

    val isStep2Valid: Boolean
        get() = logoUriString != null

    val isStep3Valid: Boolean
        get() = productName.isNotBlank() && 
                productPrice.isNotBlank() && 
                productPrice.toDoubleOrNull() != null && 
                productPrice.toDouble() > 0 &&
                productDescription.isNotBlank() && 
                productImageUris.isNotEmpty()
}

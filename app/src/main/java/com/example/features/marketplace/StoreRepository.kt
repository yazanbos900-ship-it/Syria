package com.example.features.marketplace

interface StoreRepository {
    suspend fun checkIfStoreExists(ownerId: String): Boolean
    
    suspend fun createStoreAndFirstProduct(
        storeId: String,
        ownerId: String,
        ownerUsername: String,
        storeName: String,
        categoryId: String,
        categoryName: String,
        description: String,
        logoUrl: String,
        bannerUrl: String?,
        productName: String,
        productPrice: Double,
        productDescription: String,
        productImages: List<String>
    ): Result<Unit>
}

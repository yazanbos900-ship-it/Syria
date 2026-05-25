package com.example.features.marketplace

import com.example.domain.model.Store
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    suspend fun checkIfStoreExists(ownerId: String): Boolean

    fun getActiveStores(): Flow<Result<List<Store>>>

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

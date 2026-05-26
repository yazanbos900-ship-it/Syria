package com.example.domain.repository

import com.example.domain.model.Store
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getAllStores(): Flow<Result<List<Store>>>
    fun getActiveStores(): Flow<Result<List<Store>>>
    suspend fun getStoreById(storeId: String): Store?
    suspend fun getStoreByOwnerId(ownerId: String): Store?
    suspend fun createStore(store: Store): Result<Store>
    suspend fun updateStore(store: Store): Result<Unit>
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

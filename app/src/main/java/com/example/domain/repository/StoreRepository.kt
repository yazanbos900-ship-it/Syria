package com.example.domain.repository

import com.example.domain.model.Store
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getStores(): Flow<List<Store>>
    suspend fun getStoreById(storeId: String): Store?
    suspend fun createStore(store: Store): Result<Store>
}

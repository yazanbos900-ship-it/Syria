package com.example.domain.repository

import com.example.domain.model.Category
import com.example.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    fun getProductsByStoreId(storeId: String): Flow<List<Product>>
    fun getCategories(): Flow<List<Category>>
    suspend fun getProductDetails(productId: String): Product?
    suspend fun addProduct(product: Product): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
}

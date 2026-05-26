package com.example.data.repository

import android.util.Log
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseProductRepositoryImpl : ProductRepository {
    private val tag = "FirebaseProduct"

    private fun getCreatedAt(doc: com.google.firebase.firestore.DocumentSnapshot): Long {
        return try {
            val timestamp = doc.getTimestamp("createdAt")
            if (timestamp != null) {
                timestamp.toDate().time
            } else {
                doc.getLong("createdAt") ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun DocumentSnapshot.toProduct(): Product? {
        return try {
            val id = id
            val title = getString("title") ?: getString("name") ?: ""
            val description = getString("description") ?: ""
            
            // Safe number casting
            val price = (get("price") as? Number)?.toDouble() ?: 0.0
            
            val imageUrls = (get("imageUrls") as? List<String>)
                ?: (get("images") as? List<String>)
                ?: (get("coverImage") as? String)?.let { listOf(it) }
                ?: emptyList()
                
            val categoryId = getString("categoryId") ?: getString("category") ?: ""
            val storeId = getString("storeId") ?: ""
            
            val rating = (get("rating") as? Number)?.toFloat() ?: 4.5f
            val reviewCount = (get("reviewCount") as? Number)?.toInt() ?: 0
            val isAvailable = getBoolean("isAvailable") ?: true
            val stockCount = (get("stockCount") as? Number)?.toInt() ?: 10
            
            Product(
                id = id,
                title = title,
                description = description,
                price = price,
                imageUrls = imageUrls,
                categoryId = categoryId,
                storeId = storeId,
                rating = rating,
                reviewCount = reviewCount,
                isAvailable = isAvailable,
                stockCount = stockCount,
                createdAt = getCreatedAt(this)
            )
        } catch (e: Exception) {
            Log.e("FirebaseProduct", "Error parsing product $id", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is not initialized yet or unavailable", e)
            null
        }
    }

    override fun getProducts(): Flow<List<Product>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList<Product>())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to products collection", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toProduct() }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<Product>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList<Product>())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("products")
            .whereEqualTo("categoryId", categoryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toProduct() }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun searchProducts(query: String): Flow<List<Product>> = flow {
        val db = firestore
        if (db == null) {
            emit(emptyList())
            return@flow
        }
        try {
            val snapshot = db.collection("products")
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { it.toProduct() }
            emit(products)
        } catch (e: Exception) {
            Log.e(tag, "Search products query failed", e)
            emit(emptyList())
        }
    }

    override fun getProductsByStoreId(storeId: String): Flow<List<Product>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList<Product>())
            close()
            return@callbackFlow
        }
        Log.d(tag, "=== getProductsByStoreId CALLED with storeId: '$storeId' ===")
        
        val subscription = db.collection("products")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Firestore error for storeId $storeId: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    Log.d(tag, "Products found for storeId $storeId: ${snapshot.documents.size}")
                    val products = snapshot.documents.mapNotNull { it.toProduct() }
                    trySend(products)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun getCategories(): Flow<List<Category>> = callbackFlow {
        val db = firestore ?: run {
            trySend(emptyList<Category>())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        Category(
                            id = doc.id,
                            nameAr = doc.getString("nameAr") ?: doc.getString("name") ?: "",
                            nameEn = doc.getString("nameEn") ?: doc.getString("name") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            iconName = doc.getString("iconName")
                        )
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getProductDetails(productId: String): Product? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("products").document(productId).get().await()
            if (doc.exists()) {
                doc.toProduct()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching details for product $productId", e)
            null
        }
    }

    override suspend fun addProduct(product: Product): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not available"))
        return try {
            val productMap = hashMapOf(
                "title" to product.title,
                "name" to product.title,
                "description" to product.description,
                "price" to product.price,
                "imageUrls" to product.imageUrls,
                "images" to product.imageUrls,
                "coverImage" to (product.imageUrls.firstOrNull() ?: ""),
                "categoryId" to product.categoryId,
                "category" to product.categoryId,
                "storeId" to product.storeId,
                "rating" to product.rating,
                "reviewCount" to product.reviewCount,
                "isAvailable" to product.isAvailable,
                "stockCount" to product.stockCount,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.collection("products").add(productMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error adding product", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProduct(product: Product): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not available"))
        return try {
            val productMap = hashMapOf(
                "title" to product.title,
                "name" to product.title,
                "description" to product.description,
                "price" to product.price,
                "imageUrls" to product.imageUrls,
                "images" to product.imageUrls,
                "coverImage" to (product.imageUrls.firstOrNull() ?: ""),
                "categoryId" to product.categoryId,
                "category" to product.categoryId,
                "storeId" to product.storeId,
                "rating" to product.rating,
                "reviewCount" to product.reviewCount,
                "isAvailable" to product.isAvailable,
                "stockCount" to product.stockCount
            )
            db.collection("products").document(product.id).update(productMap as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating product ${product.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not available"))
        return try {
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting product $productId", e)
            Result.failure(e)
        }
    }

    override suspend fun seedCategories(): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not available"))
        return try {
            val snapshot = db.collection("categories").limit(1).get().await()
            if (snapshot.isEmpty) {
                val categories = listOf(
                    mapOf("id" to "electronics", "nameAr" to "إلكترونيات", "nameEn" to "Electronics", "iconName" to "electronics"),
                    mapOf("id" to "fashion", "nameAr" to "أزياء", "nameEn" to "Fashion", "iconName" to "fashion"),
                    mapOf("id" to "home", "nameAr" to "المنزل", "nameEn" to "Home", "iconName" to "home"),
                    mapOf("id" to "beauty", "nameAr" to "الجمال", "nameEn" to "Beauty", "iconName" to "beauty"),
                    mapOf("id" to "sports", "nameAr" to "رياضة", "nameEn" to "Sports", "iconName" to "sports"),
                    mapOf("id" to "cars", "nameAr" to "سيارات", "nameEn" to "Cars", "iconName" to "cars"),
                    mapOf("id" to "other", "nameAr" to "أخرى", "nameEn" to "Other", "iconName" to "other")
                )

                val batch = db.batch()
                categories.forEach { cat ->
                    val id = cat["id"] as String
                    batch.set(db.collection("categories").document(id), cat)
                }
                batch.commit().await()
                Log.d(tag, "Successfully seeded ${categories.size} categories")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error seeding categories", e)
            Result.failure(e)
        }
    }
}

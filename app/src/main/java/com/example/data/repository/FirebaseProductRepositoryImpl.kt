package com.example.data.repository

import android.util.Log
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.repository.ProductRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseProductRepositoryImpl : ProductRepository {
    private val tag = "FirebaseProduct"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is not initialized yet or unavailable", e)
            null
        }
    }

    override fun getProducts(): Flow<List<Product>> = callbackFlow {
        val db = firestore ?: {
            trySend(emptyList<Product>())
            close()
        }
        
        if (db is Function0<*>) return@callbackFlow

        val dbInstance = db as FirebaseFirestore
        val subscription = dbInstance.collection("products")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to products collection", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Product(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                categoryId = doc.getString("categoryId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                rating = doc.getDouble("rating")?.toFloat() ?: 4.5f,
                                reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                                isAvailable = doc.getBoolean("isAvailable") ?: true,
                                stockCount = doc.getLong("stockCount")?.toInt() ?: 10,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        } catch (ex: Exception) {
                            Log.e(tag, "Failed casting product document", ex)
                            null
                        }
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<Product>> = callbackFlow {
        val db = firestore ?: {
            trySend(emptyList<Product>())
            close()
        }
        if (db is Function0<*>) return@callbackFlow

        val dbInstance = db as FirebaseFirestore
        val subscription = dbInstance.collection("products")
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Product(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                                categoryId = doc.getString("categoryId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                rating = doc.getDouble("rating")?.toFloat() ?: 4.5f,
                                reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                                isAvailable = doc.getBoolean("isAvailable") ?: true,
                                stockCount = doc.getLong("stockCount")?.toInt() ?: 10,
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { subscription.remove() }
    }

    override fun searchProducts(query: String): Flow<List<Product>> = flow {
        // Advanced client-side or partial firestore queries
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

            val products = snapshot.documents.mapNotNull { doc ->
                try {
                    Product(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                        categoryId = doc.getString("categoryId") ?: "",
                        storeId = doc.getString("storeId") ?: "",
                        rating = doc.getDouble("rating")?.toFloat() ?: 4.5f,
                        reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                        stockCount = doc.getLong("stockCount")?.toInt() ?: 10,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            emit(products)
        } catch (e: Exception) {
            Log.e(tag, "Search products query failed", e)
            emit(emptyList())
        }
    }

    override fun getCategories(): Flow<List<Category>> = callbackFlow {
        val db = firestore ?: {
            trySend(emptyList<Category>())
            close()
        }
        if (db is Function0<*>) return@callbackFlow

        val dbInstance = db as FirebaseFirestore
        val subscription = dbInstance.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
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
                Product(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getDouble("price") ?: 0.0,
                    imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList(),
                    categoryId = doc.getString("categoryId") ?: "",
                    storeId = doc.getString("storeId") ?: "",
                    rating = doc.getDouble("rating")?.toFloat() ?: 4.5f,
                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                    isAvailable = doc.getBoolean("isAvailable") ?: true,
                    stockCount = doc.getLong("stockCount")?.toInt() ?: 10,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed fetching details for product $productId", e)
            null
        }
    }
}

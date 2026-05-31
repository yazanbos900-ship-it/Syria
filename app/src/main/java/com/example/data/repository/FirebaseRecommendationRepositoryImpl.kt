package com.example.data.repository

import android.util.Log
import com.example.domain.model.Product
import com.example.domain.model.RecommendationCriteria
import com.example.domain.repository.RecommendationRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRecommendationRepositoryImpl : RecommendationRepository {
    private val tag = "FirebaseRecommendation"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is not initialized yet or unavailable", e)
            null
        }
    }

    private fun getCreatedAt(doc: DocumentSnapshot): Long {
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
            Log.e(tag, "Error parsing product $id in recommendation engine", e)
            null
        }
    }

    override fun getRecommendations(
        criteria: RecommendationCriteria,
        userId: String?,
        limit: Int,
        offset: Int
    ): Flow<Result<List<Product>>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(Result.failure(Exception("Firestore not initialized")))
            close()
            return@callbackFlow
        }

        val subscription = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val allProducts = snapshot.documents.mapNotNull { it.toProduct() }

                // Fire async aggregation for interactions telemetry to perform high-fidelity ranking
                db.collection("interactions")
                    .get()
                    .addOnCompleteListener { interactionTask ->
                        val interactions = if (interactionTask.isSuccessful && interactionTask.result != null) {
                            interactionTask.result.documents.map { doc ->
                                mapOf(
                                    "productId" to (doc.getString("productId") ?: ""),
                                    "categoryId" to (doc.getString("categoryId") ?: ""),
                                    "storeId" to (doc.getString("storeId") ?: ""),
                                    "userId" to (doc.getString("userId") ?: ""),
                                    "interactionType" to (doc.getString("interactionType") ?: "")
                                )
                            }
                        } else {
                            emptyList()
                        }

                        val resolvedUserId = if (!userId.isNullOrEmpty()) {
                            userId
                        } else {
                            try {
                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val targetUserInteractions = if (!resolvedUserId.isNullOrEmpty()) {
                            interactions.filter { it["userId"] == resolvedUserId }
                        } else {
                            interactions
                        }

                        val processedProducts = when (criteria) {
                            RecommendationCriteria.MOST_VIEWED -> {
                                val viewCounts = interactions.filter { it["interactionType"] == "view" }
                                    .groupingBy { it["productId"] ?: "" }
                                    .eachCount()
                                allProducts.sortedByDescending { viewCounts[it.id] ?: 0 }
                            }
                            RecommendationCriteria.MOST_FAVORITED -> {
                                val favoriteCounts = interactions.filter { it["interactionType"] == "favorite" }
                                    .groupingBy { it["productId"] ?: "" }
                                    .eachCount()
                                allProducts.sortedByDescending { favoriteCounts[it.id] ?: 0 }
                            }
                            RecommendationCriteria.BEST_RATED -> {
                                allProducts.sortedWith(
                                    compareByDescending<Product> { it.rating }
                                        .thenByDescending { it.reviewCount }
                                )
                            }
                            RecommendationCriteria.TRENDING -> {
                                // Trending uses an interactive weight formula: Rating * log(Reviews + 2) + interactionCount
                                val interactionCounts = interactions.groupingBy { it["productId"] ?: "" }.eachCount()
                                allProducts.sortedByDescending { prod ->
                                    val count = interactionCounts[prod.id] ?: 0
                                    (prod.rating * kotlin.math.ln(prod.reviewCount.toDouble() + 2.0)) + count
                                }
                            }
                            RecommendationCriteria.RECENTLY_POPULAR -> {
                                // Combination of recency (createdAt) and ratings
                                allProducts.sortedWith(
                                    compareByDescending<Product> { it.createdAt }
                                        .thenByDescending { it.rating }
                                )
                            }
                            RecommendationCriteria.CATEGORY_PREFERENCE -> {
                                val categoryCounts = targetUserInteractions
                                    .groupingBy { it["categoryId"] ?: "" }
                                    .eachCount()
                                if (categoryCounts.isNotEmpty()) {
                                    val favoriteCategories = categoryCounts.entries.sortedByDescending { it.value }
                                        .map { it.key }
                                    allProducts.sortedWith { p1, p2 ->
                                        val idx1 = favoriteCategories.indexOf(p1.categoryId)
                                        val idx2 = favoriteCategories.indexOf(p2.categoryId)
                                        val rank1 = if (idx1 != -1) idx1 else Integer.MAX_VALUE
                                        val rank2 = if (idx2 != -1) idx2 else Integer.MAX_VALUE
                                        rank1.compareTo(rank2)
                                    }
                                } else {
                                    allProducts.sortedByDescending { it.rating }
                                }
                            }
                            RecommendationCriteria.STORE_PREFERENCE -> {
                                val storeCounts = targetUserInteractions
                                    .groupingBy { it["storeId"] ?: "" }
                                    .eachCount()
                                if (storeCounts.isNotEmpty()) {
                                    val favoriteStores = storeCounts.entries.sortedByDescending { it.value }
                                        .map { it.key }
                                    allProducts.sortedWith { p1, p2 ->
                                        val idx1 = favoriteStores.indexOf(p1.storeId)
                                        val idx2 = favoriteStores.indexOf(p2.storeId)
                                        val rank1 = if (idx1 != -1) idx1 else Integer.MAX_VALUE
                                        val rank2 = if (idx2 != -1) idx2 else Integer.MAX_VALUE
                                        rank1.compareTo(rank2)
                                    }
                                } else {
                                    allProducts.sortedByDescending { it.reviewCount }
                                }
                            }
                        }

                        // Apply clean pagination
                        val paginatedList = processedProducts.asSequence()
                            .drop(offset)
                            .take(limit)
                            .toList()

                        trySend(Result.success(paginatedList))
                    }
            }

        awaitClose {
            subscription.remove()
        }
    }

    override suspend fun trackProductInteraction(
        productId: String,
        categoryId: String,
        storeId: String,
        userId: String?,
        interactionType: String
    ): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val resolvedUserId = if (!userId.isNullOrEmpty()) {
                userId
            } else {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                } catch (e: Exception) {
                    null
                }
            } ?: "anonymous"

            var resolvedCategoryId = categoryId
            var resolvedStoreId = storeId
            if (productId.isNotEmpty() && (resolvedCategoryId.isEmpty() || resolvedStoreId.isEmpty())) {
                try {
                    val prodDoc = db.collection("products").document(productId).get().await()
                    if (prodDoc.exists()) {
                        if (resolvedCategoryId.isEmpty()) {
                            resolvedCategoryId = prodDoc.getString("categoryId") ?: prodDoc.getString("category") ?: ""
                        }
                        if (resolvedStoreId.isEmpty()) {
                            resolvedStoreId = prodDoc.getString("storeId") ?: ""
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to resolve product/category/store inside trackProductInteraction", e)
                }
            }

            val interactionMap = hashMapOf(
                "productId" to productId,
                "categoryId" to resolvedCategoryId,
                "storeId" to resolvedStoreId,
                "userId" to resolvedUserId,
                "interactionType" to interactionType,
                "timestamp" to System.currentTimeMillis()
            )
            
            db.collection("interactions")
                .add(interactionMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to track product interaction", e)
            Result.failure(e)
        }
    }
}

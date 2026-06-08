package com.example.data.repository

import android.util.Log
import com.example.domain.model.ComparableProductInfo
import com.example.domain.model.ComparisonResult
import com.example.domain.model.Product
import com.example.domain.repository.ComparisonRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseComparisonRepositoryImpl : ComparisonRepository {
    private val tag = "FirebaseComparison"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is not initialized yet or unavailable", e)
            null
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
            val condition = getString("condition") ?: "new"
            
            val createdAt = try {
                val timestamp = getTimestamp("createdAt")
                if (timestamp != null) timestamp.toDate().time else getLong("createdAt") ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            
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
                createdAt = createdAt,
                condition = condition
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing product in comparison", e)
            null
        }
    }

    // High quality token-based Jaccard similarity helper for bilingual (Arabic / English) market matching
    private fun getSimilarityScore(title1: String, title2: String): Double {
        val stopWords = setOf(
            "and", "the", "with", "for", "from", "in", "on", "at", "of", "to", "by", "a", "an",
            "ال", "في", "من", "مع", "على", "أو", "و", "بـ", "لـ", "عن", "هذا", "هذه", "أن", "إن", "ما", "تشكيلة", "فاخرة"
        )
        // Clean and extract meaningful tokens
        val tokens1 = title1.lowercase().split("\\s+".toRegex())
            .map { it.replace("[^a-zA-Z0-9ا-ي]".toRegex(), "") }
            .filter { it.length > 1 && !stopWords.contains(it) }
            .toSet()
        val tokens2 = title2.lowercase().split("\\s+".toRegex())
            .map { it.replace("[^a-zA-Z0-9ا-ي]".toRegex(), "") }
            .filter { it.length > 1 && !stopWords.contains(it) }
            .toSet()
            
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0
        val intersection = tokens1.intersect(tokens2).size
        return intersection.toDouble() / (tokens1.size + tokens2.size - intersection)
    }

    override fun getProductComparison(product: Product): Flow<Result<ComparisonResult?>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(Result.failure(Exception("Firestore not initialized")))
            close()
            return@callbackFlow
        }

        // We listen dynamically to all products in the same category
        val subscription = db.collection("products")
            .whereEqualTo("categoryId", product.categoryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(Result.success(null))
                    return@addSnapshotListener
                }

                val categoryProducts = snapshot.documents.mapNotNull { it.toProduct() }
                    .filter { it.id != product.id } // exclude current product

                // Async task to filter and fetch store ratings/names for high quality market comparison
                db.collection("stores")
                    .get()
                    .addOnCompleteListener { storesTask ->
                        if (!storesTask.isSuccessful || storesTask.result == null) {
                            trySend(Result.success(null))
                            return@addOnCompleteListener
                        }

                        val storesMap = storesTask.result.documents.associateBy { it.id }

                        // Apply advanced intelligence comparison criteria:
                        // 1. Must be same category (already queried in Firestore)
                        // 2. Similar price segment (price between 0.4x and 2.5x of base price)
                        // 3. Similar product type: lexical similarity overlap > 0.05
                        val comparableItems = categoryProducts.mapNotNull { other ->
                            val priceRatio = other.price / product.price
                            val isPriceSimilar = priceRatio in 0.4..2.5
                            val textSimilarity = getSimilarityScore(product.title, other.title)
                            
                            // Check similarity criteria compliance
                            if (isPriceSimilar && textSimilarity > 0.05) {
                                val storeDoc = storesMap[other.storeId]
                                val storeName = storeDoc?.getString("name") ?: storeDoc?.getString("storeName") ?: "مستودع تجاري"
                                val storeRating = (storeDoc?.get("rating") as? Number)?.toFloat() ?: 4.5f
                                ComparableProductInfo(other, storeName, storeRating) to textSimilarity
                            } else {
                                null
                            }
                        }
                        .sortedByDescending { it.second } // Sort by similarity score descending
                        .map { it.first }

                        // Quality Rule check: Must have at least 2 comparable products for a genuine market range
                        val minSampleSize = 2
                        if (comparableItems.size < minSampleSize) {
                            Log.d(tag, "Insufficient comparable products: found ${comparableItems.size}, required $minSampleSize")
                            trySend(Result.success(null))
                            return@addOnCompleteListener
                        }

                        // We include the base product in calculations to properly locate its position in the range
                        val allComparedPrices = comparableItems.map { it.product.price } + product.price
                        val minPrice = allComparedPrices.minOrNull() ?: product.price
                        val maxPrice = allComparedPrices.maxOrNull() ?: product.price
                        val avgPrice = allComparedPrices.average()
                        
                        val percentageDifference = ((product.price - avgPrice) / avgPrice) * 100.0
                        
                        val position = when {
                            product.price < avgPrice * 0.9 -> "Low"
                            product.price > avgPrice * 1.1 -> "High"
                            else -> "Average"
                        }

                        val result = ComparisonResult(
                            baseProductPrice = product.price,
                            avgPrice = avgPrice,
                            minPrice = minPrice,
                            maxPrice = maxPrice,
                            percentageDifference = percentageDifference,
                            position = position,
                            comparableProducts = comparableItems.take(4) // restrict to top 4 matches for design cleaniness
                        )

                        trySend(Result.success(result))
                    }
            }

        awaitClose {
            subscription.remove()
        }
    }
}

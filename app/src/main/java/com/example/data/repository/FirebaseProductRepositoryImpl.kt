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

            // Seed Trial/Test Stores if empty
            val storeSnapshot = db.collection("stores").limit(1).get().await()
            if (storeSnapshot.isEmpty) {
                val storesList = listOf(
                    mapOf(
                        "id" to "store_ozone",
                        "name" to "متجر أوزون للإلكترونيات",
                        "ownerId" to "owner_ozone",
                        "ownerUsername" to "ozone.store",
                        "logoUrl" to "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400",
                        "bannerUrl" to "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1000",
                        "description" to "أحدث الهواتف الذكية، الملحقات التقنية، والمنتجات الذكية بضمان معتمد وبأفضل الأسعار.",
                        "categoryId" to "electronics",
                        "followersCount" to 342,
                        "status" to "active",
                        "rating" to 4.8,
                        "isVerified" to true,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "store_elegance",
                        "name" to "بوتيك الأناقة الراقية",
                        "ownerId" to "owner_elegance",
                        "ownerUsername" to "elegance",
                        "logoUrl" to "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=400",
                        "bannerUrl" to "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1000",
                        "description" to "أحدث الأزياء الفاخرة، الملابس العصرية، والملابس الكلاسيكية المناسبة لجميع المناسبات.",
                        "categoryId" to "fashion",
                        "followersCount" to 198,
                        "status" to "active",
                        "rating" to 4.9,
                        "isVerified" to true,
                        "createdAt" to System.currentTimeMillis() - 60000
                    ),
                    mapOf(
                        "id" to "store_beauty",
                        "name" to "دار المسك والجمال",
                        "ownerId" to "owner_beauty",
                        "ownerUsername" to "mesk.beauty",
                        "logoUrl" to "https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=400",
                        "bannerUrl" to "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=1000",
                        "description" to "العطور الشرقية والغربية الفاخرة، مستحضرات العناية العضوية والتجميل بلمسات ساحرة وطبيعية.",
                        "categoryId" to "beauty",
                        "followersCount" to 512,
                        "status" to "active",
                        "rating" to 4.7,
                        "isVerified" to true,
                        "createdAt" to System.currentTimeMillis() - 120000
                    )
                )

                val storesBatch = db.batch()
                storesList.forEach { store ->
                    val id = store["id"] as String
                    storesBatch.set(db.collection("stores").document(id), store)
                }
                storesBatch.commit().await()
                Log.d(tag, "Successfully seeded ${storesList.size} trial stores")
            }

            // Seed Trial/Test Products if empty
            val productSnapshot = db.collection("products").limit(1).get().await()
            if (productSnapshot.isEmpty) {
                val productsList = listOf(
                    mapOf(
                        "id" to "prod_iphone15",
                        "title" to "آيفون 15 برو ماكس (256 جيجابايت) - تيتانيوم طبيعي",
                        "description" to "شريحة A17 Pro المبتكرة، كاميرا رئيسية بدقة 48 ميجابكسل، وشاشة Super Retina XDR تقدم لك تجربة استخدام فائقة السرعة والوضوح.",
                        "price" to 4899.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500", "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500", "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=500",
                        "categoryId" to "electronics",
                        "category" to "electronics",
                        "storeId" to "store_ozone",
                        "rating" to 4.8f,
                        "reviewCount" to 45,
                        "isAvailable" to true,
                        "stockCount" to 15,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    mapOf(
                        "id" to "prod_airpods",
                        "title" to "سماعات لاسلكية برو الجيل الثاني",
                        "description" to "ميزة إلغاء الضوضاء النشط الفائق، تتبع حركة الرأس لمحيط صوتي رائع، وعمر بطارية طويل يصل لغاية 30 ساعة مع علبة الشحن.",
                        "price" to 949.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1588449668365-d15e397f6787?w=500",
                        "categoryId" to "electronics",
                        "category" to "electronics",
                        "storeId" to "store_ozone",
                        "rating" to 4.7f,
                        "reviewCount" to 28,
                        "isAvailable" to true,
                        "stockCount" to 24,
                        "createdAt" to System.currentTimeMillis() - 10000
                    ),
                    mapOf(
                        "id" to "prod_jacket",
                        "title" to "معطف فاخر من الصوف الإيطالي الناعم",
                        "description" to "معطف كلاسيكي دافئ مصمم بحرفية بالغة من أجود أنواع الصوف الخالص ليمنحك مظهراً جذاباً ومفعماً بالدفء خلال الأيام الباردة.",
                        "price" to 349.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500", "https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500", "https://images.unsplash.com/photo-1485230895905-ec40ba36b9bc?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=500",
                        "categoryId" to "fashion",
                        "category" to "fashion",
                        "storeId" to "store_elegance",
                        "rating" to 4.9f,
                        "reviewCount" to 19,
                        "isAvailable" to true,
                        "stockCount" to 8,
                        "createdAt" to System.currentTimeMillis() - 20000
                    ),
                    mapOf(
                        "id" to "prod_watch",
                        "title" to "ساعة كلاسيكية رياضية باللون الأسود",
                        "description" to "ساعة مقاومة للماء مع حزام جلدي فاخر وهيكل قوي من الفولاذ المقاوم للصدأ ومؤشرات واضحة مطلية بالذهب.",
                        "price" to 189.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=500",
                        "categoryId" to "fashion",
                        "category" to "fashion",
                        "storeId" to "store_elegance",
                        "rating" to 4.6f,
                        "reviewCount" to 12,
                        "isAvailable" to true,
                        "stockCount" to 30,
                        "createdAt" to System.currentTimeMillis() - 30000
                    ),
                    mapOf(
                        "id" to "prod_perfume",
                        "title" to "عطر \"الشرق الفاخر\" - مسك وعود ملكي",
                        "description" to "توليفة ساحرة تجمع عمق العود ودهن الورد الطائفي مع لمسة المسك الأبيض النقي، تم تصميمه ليناسب الأوقات الراقية والمناسبات الهامة.",
                        "price" to 299.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1541643600914-78b084683601?w=500", "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1541643600914-78b084683601?w=500", "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1541643600914-78b084683601?w=500",
                        "categoryId" to "beauty",
                        "category" to "beauty",
                        "storeId" to "store_beauty",
                        "rating" to 4.8f,
                        "reviewCount" to 64,
                        "isAvailable" to true,
                        "stockCount" to 11,
                        "createdAt" to System.currentTimeMillis() - 40000
                    ),
                    mapOf(
                        "id" to "prod_serum",
                        "title" to "سيروم الهيالورونيك وفيتامين C لترطيب البشرة",
                        "description" to "تركيبة غنية ومغذية تمنح بشرتك نضارة لا تضاهى، وتعالج الجفاف وتوحد لون البشرة ليتركها ناعمة ومشرقة كالحرير.",
                        "price" to 120.0,
                        "imageUrls" to listOf("https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=500", "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=500"),
                        "images" to listOf("https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=500", "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?w=500"),
                        "coverImage" to "https://images.unsplash.com/photo-1608248597279-f99d160bfcbc?w=500",
                        "categoryId" to "beauty",
                        "category" to "beauty",
                        "storeId" to "store_beauty",
                        "rating" to 4.5f,
                        "reviewCount" to 33,
                        "isAvailable" to true,
                        "stockCount" to 40,
                        "createdAt" to System.currentTimeMillis() - 50000
                    )
                )

                val productsBatch = db.batch()
                productsList.forEach { prod ->
                    val id = prod["id"] as String
                    productsBatch.set(db.collection("products").document(id), prod)
                }
                productsBatch.commit().await()
                Log.d(tag, "Successfully seeded ${productsList.size} trial products")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error seeding categories", e)
            Result.failure(e)
        }
    }
}

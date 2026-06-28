package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class StoreDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val store: Store? = null,
    val products: List<Product> = emptyList(),
    val isFollowing: Boolean = false,
    val currentUserId: String? = null,
    val reputationScore: Double = 0.0
)

class StoreDetailViewModel(
    private val storeRepo: StoreRepository,
    private val productRepo: ProductRepository,
    private val authRepo: com.example.domain.repository.AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoreDetailUiState())
    val state: StateFlow<StoreDetailUiState> = _state.asStateFlow()

    private var storeListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var followListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        viewModelScope.launch {
            authRepo.currentUser.collect { user ->
                _state.update { it.copy(currentUserId = user?.id) }
                _state.value.store?.id?.let { storeId ->
                    setupFollowListener(storeId)
                }
            }
        }
    }

    fun loadStore(storeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            storeListener?.remove()
            followListener?.remove()
            
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // Listen to store in real-time
            storeListener = db.collection("stores").document(storeId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _state.update { it.copy(error = error.localizedMessage, isLoading = false) }
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val store = mapFirestoreDocToStore(snapshot)
                        _state.update { it.copy(store = store, isLoading = false) }
                        
                        setupFollowListener(storeId)
                        fetchStoreReputation(store)
                    } else {
                        _state.update { it.copy(error = "لم يتم العثور على المتجر", isLoading = false) }
                    }
                }
                
            loadProducts(storeId)
        }
    }

    private fun fetchStoreReputation(store: Store) {
        viewModelScope.launch {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            try {
                val storeId = store.id
                val interactionsSnap = db.collection("interactions").whereEqualTo("storeId", storeId).get().await()
                val intentsSnap = db.collection("purchase_intents").whereEqualTo("storeId", storeId).get().await()
                val chatsSnap = db.collection("chats").whereEqualTo("sellerUid", store.ownerId).get().await()
                val reviewsSnap = db.collection("reviews").whereEqualTo("storeId", storeId).get().await()
                val productsSnap = db.collection("products").whereEqualTo("storeId", storeId).get().await()

                val viewsCount = interactionsSnap.documents.count { 
                    val type = it.getString("interactionType")
                    type == "store_view" || type == "product_view"
                }

                val productSaves = interactionsSnap.documents.count { 
                    it.getString("interactionType") == "favorite"
                }

                val buyNowClicks = intentsSnap.size()
                val chatInquiries = chatsSnap.size()
                val reviewsCount = reviewsSnap.size()
                val recentProductsCount = productsSnap.size()

                val score = com.example.domain.utils.StoreReputationCalculator.calculateReputationScore(
                    store = store,
                    viewsCount = viewsCount,
                    buyNowClicks = buyNowClicks,
                    chatInquiries = chatInquiries,
                    productSaves = productSaves,
                    reviewsCount = reviewsCount,
                    recentProductsCount = recentProductsCount
                )

                _state.update { it.copy(reputationScore = score) }
            } catch (e: Exception) {
                android.util.Log.e("StoreDetailVM", "Failed to load reputation metrics", e)
                val baseScore = com.example.domain.utils.StoreReputationCalculator.calculateReputationScore(store)
                _state.update { it.copy(reputationScore = baseScore) }
            }
        }
    }

    private fun setupFollowListener(storeId: String) {
        val userId = _state.value.currentUserId ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        followListener?.remove()
        followListener = db.collection("follows").document("${userId}_${storeId}")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    _state.update { it.copy(isFollowing = snapshot.exists()) }
                }
            }
    }

    fun toggleFollow() {
        val userId = _state.value.currentUserId ?: return
        val store = _state.value.store ?: return
        if (store.ownerId == userId) {
            return
        }
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = db.collection("follows").document("${userId}_${store.id}")
        val storeRef = db.collection("stores").document(store.id)
        
        val currentlyFollowing = _state.value.isFollowing
        
        db.runTransaction { transaction ->
            val followExists = transaction.get(docRef).exists()
            val currentStoreSnap = transaction.get(storeRef)
            val currentCount = (currentStoreSnap.get("followersCount") as? Number)?.toLong() ?: 0L
            
            if (currentlyFollowing || followExists) {
                transaction.delete(docRef)
                transaction.update(storeRef, "followersCount", (currentCount - 1).coerceAtLeast(0L))
            } else {
                val followMap = hashMapOf(
                    "userId" to userId,
                    "storeId" to store.id,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(docRef, followMap)
                transaction.update(storeRef, "followersCount", currentCount + 1)
            }
        }.addOnSuccessListener {
            // Updated successfully via real-time listeners
        }.addOnFailureListener { e ->
            android.util.Log.e("StoreDetailVM", "toggleFollow transaction failed", e)
        }
    }

    private fun mapFirestoreDocToStore(doc: com.google.firebase.firestore.DocumentSnapshot): Store {
        val dbRate = (doc.get("exchangeRate") as? Number)?.toDouble() ?: (doc.get("usdExchangeRate") as? Number)?.toDouble() ?: 12500.0
        val dbCurrency = doc.getString("storeCurrency") ?: doc.getString("defaultCurrency") ?: "USD"
        return Store(
            id = doc.id,
            name = doc.getString("storeName") ?: doc.getString("name") ?: "",
            ownerId = doc.getString("ownerId") ?: "",
            ownerUsername = doc.getString("ownerUsername") ?: "",
            logoUrl = doc.getString("logoUrl"),
            bannerUrl = doc.getString("bannerUrl"),
            description = doc.getString("description") ?: "",
            categoryId = doc.getString("categoryId") ?: "",
            followersCount = (doc.get("followersCount") as? Number)?.toInt() ?: 0,
            status = doc.getString("status") ?: "active",
            rating = (doc.get("rating") as? Number)?.toFloat() ?: 5.0f,
            isVerified = doc.getBoolean("isVerified") ?: false,
            usdExchangeRate = dbRate,
            subscriptionTier = doc.getString("subscriptionTier") ?: "Starter",
            verificationStatus = doc.getString("verificationStatus") ?: "Pending",
            sellerBadge = doc.getString("sellerBadge") ?: "None",
            defaultCurrency = dbCurrency,
            deliveryAreas = doc.get("deliveryAreas") as? List<String> ?: emptyList(),
            shippingCosts = (doc.get("shippingCosts") as? Map<String, Any>)?.mapValues { (it.value as? Number)?.toDouble() ?: 0.0 } ?: emptyMap(),
            workingHours = doc.getString("workingHours") ?: "9:00 AM - 9:00 PM",
            latitude = doc.getDouble("latitude"),
            longitude = doc.getDouble("longitude"),
            city = doc.getString("city") ?: "",
            district = doc.getString("district") ?: "",
            fullAddress = doc.getString("fullAddress") ?: "",
            createdAt = try {
                doc.getTimestamp("createdAt")?.toDate()?.time 
                    ?: doc.getLong("createdAt") 
                    ?: System.currentTimeMillis()
            } catch (e: Exception) {
                doc.getLong("createdAt") ?: System.currentTimeMillis()
            },
            exchangeRate = dbRate,
            storeCurrency = dbCurrency,
            exchangeRateUpdatedAt = doc.getTimestamp("exchangeRateUpdatedAt"),
            usingGlobalRate = doc.getBoolean("usingGlobalRate") ?: true
        )
    }
    
    private fun loadProducts(storeId: String) {
        viewModelScope.launch {
            productRepo.getProductsByStoreId(storeId)
                .catch { e ->
                    android.util.Log.e("DEBUG_STORE", "Products flow error: ${e.message}", e)
                    _state.update { it.copy(products = emptyList()) }
                }
                .collect { products ->
                    _state.update { it.copy(products = products) }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        storeListener?.remove()
        followListener?.remove()
    }
}

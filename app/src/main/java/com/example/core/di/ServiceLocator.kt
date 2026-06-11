package com.example.core.di

import android.content.Context
import com.example.data.repository.FirebaseAuthRepositoryImpl
import com.example.data.repository.FirebaseProductRepositoryImpl
import com.example.data.repository.FirebaseReviewRepositoryImpl
import com.example.data.repository.FirebaseStoreRepositoryImpl
import com.example.data.repository.FirestoreCartRepositoryImpl
import com.example.data.repository.FirestoreWishlistRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CartRepository
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.ReviewRepository
import com.example.domain.repository.StoreRepository
import com.example.domain.repository.SubscriptionRepository
import com.example.domain.repository.WishlistRepository
import com.example.domain.repository.OrderRepository
import com.example.data.repository.FirebaseSubscriptionRepositoryImpl
import com.example.data.repository.FirebaseOrderRepositoryImpl
import com.example.data.repository.FirebasePaymentRepositoryImpl
import com.example.domain.repository.PaymentRepository
import com.example.firebase.FirebaseInitializer
import com.google.firebase.firestore.FirebaseFirestore

object ServiceLocator {
    
    private var isInitialized = false

    lateinit var applicationContext: Context
        private set

    // Lazy repository initializations to maintain Clean Architecture principles
    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepositoryImpl()
    }

    val productRepository: ProductRepository by lazy {
        FirebaseProductRepositoryImpl()
    }

    val reviewRepository: ReviewRepository by lazy {
        FirebaseReviewRepositoryImpl(FirebaseFirestore.getInstance())
    }

    val storeRepository: StoreRepository by lazy {
        FirebaseStoreRepositoryImpl()
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        FirebaseSubscriptionRepositoryImpl()
    }

    val cartRepository: CartRepository by lazy {
        FirestoreCartRepositoryImpl()
    }

    val wishlistRepository: WishlistRepository by lazy {
        FirestoreWishlistRepositoryImpl()
    }

    val orderRepository: OrderRepository by lazy {
        FirebaseOrderRepositoryImpl()
    }

    val recommendationRepository: com.example.domain.repository.RecommendationRepository by lazy {
        com.example.data.repository.FirebaseRecommendationRepositoryImpl()
    }

    val comparisonRepository: com.example.domain.repository.ComparisonRepository by lazy {
        com.example.data.repository.FirebaseComparisonRepositoryImpl()
    }

    val paymentRepository: PaymentRepository by lazy {
        FirebasePaymentRepositoryImpl()
    }

    val jobRepository: com.example.domain.repository.JobRepository by lazy {
        com.example.data.repository.FirebaseJobRepositoryImpl()
    }

    /**
     * Call this inside Application class or MainActivity's onCreate to set up base clients safely.
     */
    fun init(context: Context) {
        if (isInitialized) return
        applicationContext = context.applicationContext
        FirebaseInitializer.initialize(context)
        isInitialized = true
    }
}

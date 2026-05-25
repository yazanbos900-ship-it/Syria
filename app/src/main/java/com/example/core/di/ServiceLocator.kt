package com.example.core.di

import android.content.Context
import com.example.data.repository.FirebaseAuthRepositoryImpl
import com.example.data.repository.FirebaseProductRepositoryImpl
import com.example.data.repository.FirebaseStoreRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ProductRepository
import com.example.domain.repository.StoreRepository
import com.example.firebase.FirebaseInitializer

object ServiceLocator {
    
    private var isInitialized = false

    // Lazy repository initializations to maintain Clean Architecture principles
    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepositoryImpl()
    }

    val productRepository: ProductRepository by lazy {
        FirebaseProductRepositoryImpl()
    }

    val storeRepository: StoreRepository by lazy {
        FirebaseStoreRepositoryImpl()
    }

    /**
     * Call this inside Application class or MainActivity's onCreate to set up base clients safely.
     */
    fun init(context: Context) {
        if (isInitialized) return
        FirebaseInitializer.initialize(context)
        isInitialized = true
    }
}

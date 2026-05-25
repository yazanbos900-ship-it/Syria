package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.BuildConfig

object FirebaseInitializer {
    private const val TAG = "FirebaseInitializer"
    private var isInitialized = false

    fun initialize(context: Context): Boolean {
        if (isInitialized) return true

        try {
            // Attempt standard automatic initialization
            FirebaseApp.initializeApp(context)
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully via standard configuration.")
        } catch (e: Exception) {
            Log.w(TAG, "Default Firebase configuration failed. Attempting developer configuration fallback.")
            try {
                // Dynamic fallback config using parameters to avoid crash if google-services.json is missing
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef") // Placeholder or BuildConfig
                    .setApiKey("mock-api-key") // Placeholder
                    .setProjectId("wasetplus-app-mock")
                    .build()
                FirebaseApp.initializeApp(context, options)
                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully using customized fallback options.")
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed both standard and dynamic Firebase initialization.", fallbackEx)
                isInitialized = false
            }
        }
        return isInitialized
    }
}

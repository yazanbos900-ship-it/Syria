package com.example.core.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object MarketplaceSettingsManager {
    private const val TAG = "MarketplaceSettings"

    data class MarketplaceSettings(
        val platformFeePercent: Double = 5.0,
        val vatPercent: Double = 3.0,
        val defaultShippingFeeSyp: Double = 20000.0,
        val supportedCities: List<String> = listOf("Damascus", "Aleppo", "Homs", "Hama", "Latakia", "Tartous"),
        val supportedPaymentMethods: List<String> = listOf("Cash On Delivery", "Syriatel Cash", "MTN Cash"),
        val defaultCurrency: String = "USD",
        val defaultExchangeRate: Double = 13500.0
    )

    private val _settings = MutableStateFlow(MarketplaceSettings())
    val settings: StateFlow<MarketplaceSettings> = _settings.asStateFlow()

    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    init {
        observeSettings()
    }

    fun observeSettings() {
        val db = firestore ?: return
        db.collection("settings").document("marketplace")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to marketplace settings", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val platformFee = snapshot.getDouble("platformFeePercent") ?: 5.0
                        val vat = snapshot.getDouble("vatPercent") ?: 3.0
                        val shipping = snapshot.getDouble("defaultShippingFeeSyp") ?: 20000.0
                        val cities = snapshot.get("supportedCities") as? List<String> ?: listOf("Damascus", "Aleppo", "Homs", "Hama", "Latakia", "Tartous")
                        val methods = snapshot.get("supportedPaymentMethods") as? List<String> ?: listOf("Cash On Delivery", "Syriatel Cash", "MTN Cash")
                        val currency = snapshot.getString("defaultCurrency") ?: "USD"
                        val rate = snapshot.getDouble("defaultExchangeRate") ?: 13500.0

                        _settings.value = MarketplaceSettings(
                            platformFeePercent = platformFee,
                            vatPercent = vat,
                            defaultShippingFeeSyp = shipping,
                            supportedCities = cities,
                            supportedPaymentMethods = methods,
                            defaultCurrency = currency,
                            defaultExchangeRate = rate
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding settings", e)
                    }
                }
            }
    }

    suspend fun saveSettings(newSettings: MarketplaceSettings): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore is unavailable"))
        return try {
            val data = mapOf(
                "platformFeePercent" to newSettings.platformFeePercent,
                "vatPercent" to newSettings.vatPercent,
                "defaultShippingFeeSyp" to newSettings.defaultShippingFeeSyp,
                "supportedCities" to newSettings.supportedCities,
                "supportedPaymentMethods" to newSettings.supportedPaymentMethods,
                "defaultCurrency" to newSettings.defaultCurrency,
                "defaultExchangeRate" to newSettings.defaultExchangeRate
            )
            db.collection("settings").document("marketplace").set(data).await()
            _settings.value = newSettings
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save marketplace settings", e)
            Result.failure(e)
        }
    }
}

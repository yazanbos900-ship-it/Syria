package com.example.core.utils

import android.content.Context
import com.example.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CurrencyManager {
    enum class Currency(val code: String, val symbolAr: String, val symbolEn: String) {
        SYP("SYP", "ل.س", "SYP"),
        USD("USD", "$", "USD")
    }

    private const val PREFS_NAME = "currency_prefs"
    private const val KEY_CURRENCY = "selected_currency"

    private val _currentCurrency = MutableStateFlow(Currency.SYP)
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CURRENCY, Currency.SYP.code) ?: Currency.SYP.code
        val currency = Currency.values().find { it.code == saved } ?: Currency.SYP
        _currentCurrency.value = currency
    }

    fun setCurrency(context: Context, currency: Currency) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY, currency.code).apply()
        _currentCurrency.value = currency
    }

    fun getCurrency(context: Context): Currency {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CURRENCY, Currency.SYP.code) ?: Currency.SYP.code
        return Currency.values().find { it.code == saved } ?: Currency.SYP
    }

    fun formatPrice(priceInUSD: Double, exchangeRate: Double, isArabic: Boolean): String {
        val rate = if (exchangeRate <= 0) 13500.0 else exchangeRate
        return when (_currentCurrency.value) {
            Currency.USD -> {
                val symbol = if (isArabic) "$" else "USD"
                String.format("%.2f %s", priceInUSD, symbol)
            }
            Currency.SYP -> {
                val sypPrice = priceInUSD * rate
                val symbol = if (isArabic) "ل.س" else "SYP"
                String.format("%,d %s", sypPrice.toLong(), symbol)
            }
        }
    }

    fun formatProductPrice(product: Product, exchangeRate: Double, isArabic: Boolean): String {
        val rate = if (exchangeRate <= 0) 13500.0 else exchangeRate
        val priceInUSD = if (product.currency == "SYP") {
            product.price / rate
        } else {
            product.price
        }
        return formatPrice(priceInUSD, rate, isArabic)
    }
}

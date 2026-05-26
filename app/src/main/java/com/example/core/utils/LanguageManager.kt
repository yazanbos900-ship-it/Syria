package com.example.core.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    enum class Language(val code: String) {
        ARABIC("ar"),
        ENGLISH("en")
    }

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun setLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        updateBaseContextLocale(context, lang)
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "ar") ?: "ar" // Default to Arabic
    }

    fun updateBaseContextLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
    
    fun isArabic(context: Context): Boolean = getLanguage(context) == "ar"
}

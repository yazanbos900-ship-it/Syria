package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Marketplace Color System
val BrandPrimary = Color(0xFF1FAF5A)      // Soft, vibrant premium green

val BrandBackground: Color
    get() = if (ThemeManager.isDark) Color(0xFF141413) else Color(0xFFF7F7F5)   // Warm, editorial cream-white or dark premium black

val BrandSurface: Color
    get() = if (ThemeManager.isDark) Color(0xFF1E1E1D) else Color(0xFFFFFFFF)      // Pure white or dark card background

val BrandTextPrimary: Color
    get() = if (ThemeManager.isDark) Color(0xFFECECEC) else Color(0xFF111111)  // Smooth carbon or premium light gray

val BrandTextMuted: Color
    get() = if (ThemeManager.isDark) Color(0xFF9E9E9E) else Color(0xFF7A7A7A)    // Warm medium gray

val BrandSoftGray: Color
    get() = if (ThemeManager.isDark) Color(0xFF2E2E2D) else Color(0xFFECECEC)     // Soft separator

// Secondary colors supporting clean Material 3 compliance
val BrandSecondary = Color(0xFF2E7D32)
val BrandTertiary = Color(0xFF81C784)
val BrandOnPrimary = Color(0xFFFFFFFF)

val BrandOnBackground: Color
    get() = if (ThemeManager.isDark) Color(0xFFECECEC) else Color(0xFF111111)

val BrandOnSurface: Color
    get() = if (ThemeManager.isDark) Color(0xFFECECEC) else Color(0xFF111111)

val BrandError = Color(0xFFD32F2F)


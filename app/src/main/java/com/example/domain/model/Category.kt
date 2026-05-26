package com.example.domain.model

data class Category(
    val id: String,
    val nameAr: String = "",
    val nameEn: String = "",
    val imageUrl: String? = null,
    val iconName: String? = null
) {
    fun getName(isArabic: Boolean): String = if (isArabic) nameAr else nameEn
}

package com.example.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val phoneNumber: String? = null,
    val isStoreOwner: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
)

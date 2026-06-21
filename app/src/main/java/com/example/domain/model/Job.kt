package com.example.domain.model

data class Job(
    val id: String = "",
    val storeId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val requirements: String = "",
    val responsibilities: String = "",
    val location: String = "",
    val employmentType: String = "",
    val category: String = "",
    val experienceLevel: String = "",
    val salary: String = "", // Make it a string so they can specify "Negotiable" or range
    val status: String = "active", // active, paused, closed
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long = 0L,
    
    // Store inherited data
    val storeName: String = "",
    val storeLogoUrl: String? = null,
    val isStoreVerified: Boolean = false,
    
    val contactWhatsApp: String = ""
)

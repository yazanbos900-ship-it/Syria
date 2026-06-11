package com.example.domain.model

data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val storeId: String = "",
    val applicantId: String = "",
    val applicantName: String = "",
    val phone: String = "",
    val email: String = "",
    val message: String = "",
    val cvUrl: String? = null,
    val status: String = "pending", // pending, reviewed, accepted, rejected
    val createdAt: Long = System.currentTimeMillis()
)

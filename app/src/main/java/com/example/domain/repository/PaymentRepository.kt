package com.example.domain.repository

import com.example.domain.model.PaymentTransaction

interface PaymentRepository {
    suspend fun createPaymentTransaction(transaction: PaymentTransaction): Result<Unit>
    suspend fun getPaymentTransaction(transactionId: String): Result<PaymentTransaction?>
    suspend fun updatePaymentTransactionStatus(transactionId: String, status: String): Result<Unit>
}

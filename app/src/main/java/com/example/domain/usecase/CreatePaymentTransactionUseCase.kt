package com.example.domain.usecase

import com.example.domain.model.PaymentTransaction
import com.example.domain.repository.PaymentRepository

class CreatePaymentTransactionUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(transaction: PaymentTransaction): Result<Unit> {
        return paymentRepository.createPaymentTransaction(transaction)
    }
}

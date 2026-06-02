package com.example.domain.usecase

import com.example.domain.repository.PaymentRepository
import com.example.domain.repository.OrderRepository

sealed class OtpVerificationResult {
    object Success : OtpVerificationResult()
    object Incorrect : OtpVerificationResult()
    object Expired : OtpVerificationResult()
    data class Error(val message: String) : OtpVerificationResult()
}

class VerifyPaymentOtpUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        transactionId: String,
        enteredOtp: String
    ): OtpVerificationResult {
        return try {
            val result = paymentRepository.getPaymentTransaction(transactionId)
            val transaction = result.getOrNull() ?: return OtpVerificationResult.Error("Transaction not found")
            
            if (System.currentTimeMillis() > transaction.expiresAt) {
                paymentRepository.updatePaymentTransactionStatus(transactionId, "Failed")
                return OtpVerificationResult.Expired
            }
            
            if (transaction.otpCode == enteredOtp) {
                paymentRepository.updatePaymentTransactionStatus(transactionId, "Paid")
                OtpVerificationResult.Success
            } else {
                OtpVerificationResult.Incorrect
            }
        } catch (e: Exception) {
            OtpVerificationResult.Error(e.message ?: "Verification failed")
        }
    }
}

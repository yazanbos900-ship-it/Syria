package com.example.data.repository

import android.util.Log
import com.example.domain.model.PaymentTransaction
import com.example.domain.repository.PaymentRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebasePaymentRepositoryImpl : PaymentRepository {
    private val tag = "FirebasePaymentRepo"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is currently unavailable", e)
            null
        }
    }

    override suspend fun createPaymentTransaction(transaction: PaymentTransaction): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val docRef = db.collection("payment_transactions").document(transaction.transactionId)
            
            val data = mapOf(
                "transactionId" to transaction.transactionId,
                "userId" to transaction.userId,
                "orderId" to transaction.orderId,
                "paymentMethod" to transaction.paymentMethod,
                "phoneNumber" to transaction.phoneNumber,
                "otpCode" to transaction.otpCode,
                "amount" to transaction.amount,
                "currency" to transaction.currency,
                "status" to transaction.status,
                "createdAt" to transaction.createdAt,
                "expiresAt" to transaction.expiresAt
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to create payment transaction in Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun getPaymentTransaction(transactionId: String): Result<PaymentTransaction?> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val snapshot = db.collection("payment_transactions").document(transactionId).get().await()
            if (!snapshot.exists()) {
                return Result.success(null)
            }
            val transaction = PaymentTransaction(
                transactionId = snapshot.getString("transactionId") ?: snapshot.id,
                userId = snapshot.getString("userId") ?: "",
                orderId = snapshot.getString("orderId") ?: "",
                paymentMethod = snapshot.getString("paymentMethod") ?: "",
                phoneNumber = snapshot.getString("phoneNumber") ?: "",
                otpCode = snapshot.getString("otpCode") ?: "",
                amount = snapshot.getDouble("amount") ?: 0.0,
                currency = snapshot.getString("currency") ?: "USD",
                status = snapshot.getString("status") ?: "Pending",
                createdAt = snapshot.getLong("createdAt") ?: 0L,
                expiresAt = snapshot.getLong("expiresAt") ?: 0L
            )
            Result.success(transaction)
        } catch (e: Exception) {
            Log.e(tag, "Failed to read payment transaction from Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePaymentTransactionStatus(transactionId: String, status: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            db.collection("payment_transactions").document(transactionId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update payment transaction status in Firestore", e)
            Result.failure(e)
        }
    }
}

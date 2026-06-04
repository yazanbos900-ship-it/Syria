package com.example.data.repository

import android.util.Log
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.domain.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseOrderRepositoryImpl : OrderRepository {
    private val tag = "FirebaseOrderRepo"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseFirestore is currently unavailable", e)
            null
        }
    }

    override fun getOrdersForUser(userId: String): Flow<Result<List<Order>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        if (userId.isEmpty()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val orderItems = itemsList.map { itemMap ->
                                OrderItem(
                                    productId = itemMap["productId"] as? String ?: "",
                                    productName = itemMap["productName"] as? String ?: "",
                                    productImage = itemMap["productImage"] as? String ?: "",
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                    unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0
                                )
                            }
                            Order(
                                orderId = doc.getString("orderId") ?: doc.id,
                                userId = doc.getString("userId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                storeName = doc.getString("storeName") ?: "WasetPlus Store",
                                status = doc.getString("status") ?: "Pending",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                currency = doc.getString("currency") ?: "USD",
                                items = orderItems,
                                customerName = doc.getString("customerName") ?: "",
                                customerPhone = doc.getString("customerPhone") ?: "",
                                shippingAddress = doc.getString("shippingAddress") ?: "",
                                paymentMethod = doc.getString("paymentMethod") ?: "Cash On Delivery",
                                paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                                subtotal = doc.getDouble("subtotal") ?: 0.0,
                                vatAmount = doc.getDouble("vatAmount") ?: 0.0,
                                shippingFee = doc.getDouble("shippingFee") ?: 0.0,
                                grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                                selectedDeliveryArea = doc.getString("selectedDeliveryArea") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing Order document", e)
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(Result.success(orders))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getOrdersForStore(storeId: String): Flow<Result<List<Order>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        if (storeId.isEmpty()) {
            trySend(Result.success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val listener = db.collection("orders")
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val orderItems = itemsList.map { itemMap ->
                                OrderItem(
                                    productId = itemMap["productId"] as? String ?: "",
                                    productName = itemMap["productName"] as? String ?: "",
                                    productImage = itemMap["productImage"] as? String ?: "",
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                    unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0
                                )
                            }
                            Order(
                                orderId = doc.getString("orderId") ?: doc.id,
                                userId = doc.getString("userId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                storeName = doc.getString("storeName") ?: "WasetPlus Store",
                                status = doc.getString("status") ?: "Pending",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                currency = doc.getString("currency") ?: "USD",
                                items = orderItems,
                                customerName = doc.getString("customerName") ?: "",
                                customerPhone = doc.getString("customerPhone") ?: "",
                                shippingAddress = doc.getString("shippingAddress") ?: "",
                                paymentMethod = doc.getString("paymentMethod") ?: "Cash On Delivery",
                                paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                                subtotal = doc.getDouble("subtotal") ?: 0.0,
                                vatAmount = doc.getDouble("vatAmount") ?: 0.0,
                                shippingFee = doc.getDouble("shippingFee") ?: 0.0,
                                grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                                selectedDeliveryArea = doc.getString("selectedDeliveryArea") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing Order document", e)
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(Result.success(orders))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getAllOrders(): Flow<Result<List<Order>>> = callbackFlow {
        val db = firestore ?: run {
            trySend(Result.failure(Exception("Firestore service is unavailable")))
            close()
            return@callbackFlow
        }

        val listener = db.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val itemsList = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val orderItems = itemsList.map { itemMap ->
                                OrderItem(
                                    productId = itemMap["productId"] as? String ?: "",
                                    productName = itemMap["productName"] as? String ?: "",
                                    productImage = itemMap["productImage"] as? String ?: "",
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                    unitPrice = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0
                                )
                            }
                            Order(
                                orderId = doc.getString("orderId") ?: doc.id,
                                userId = doc.getString("userId") ?: "",
                                storeId = doc.getString("storeId") ?: "",
                                storeName = doc.getString("storeName") ?: "WasetPlus Store",
                                status = doc.getString("status") ?: "Pending",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                                currency = doc.getString("currency") ?: "USD",
                                items = orderItems,
                                customerName = doc.getString("customerName") ?: "",
                                customerPhone = doc.getString("customerPhone") ?: "",
                                shippingAddress = doc.getString("shippingAddress") ?: "",
                                paymentMethod = doc.getString("paymentMethod") ?: "Cash On Delivery",
                                paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                                subtotal = doc.getDouble("subtotal") ?: 0.0,
                                vatAmount = doc.getDouble("vatAmount") ?: 0.0,
                                shippingFee = doc.getDouble("shippingFee") ?: 0.0,
                                grandTotal = doc.getDouble("grandTotal") ?: 0.0,
                                selectedDeliveryArea = doc.getString("selectedDeliveryArea") ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error parsing Order document", e)
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(Result.success(orders))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createOrder(order: Order): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            val docRef = if (order.orderId.isNotEmpty()) {
                db.collection("orders").document(order.orderId)
            } else {
                db.collection("orders").document()
            }
            val id = docRef.id
            val finalOrder = order.copy(orderId = id)

            val itemsMaps = finalOrder.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "productImage" to item.productImage,
                    "quantity" to item.quantity,
                    "unitPrice" to item.unitPrice
                )
            }

            val orderData = mapOf(
                "orderId" to id,
                "userId" to finalOrder.userId,
                "storeId" to finalOrder.storeId,
                "storeName" to finalOrder.storeName,
                "status" to finalOrder.status,
                "createdAt" to finalOrder.createdAt,
                "totalAmount" to finalOrder.totalAmount,
                "currency" to finalOrder.currency,
                "items" to itemsMaps,
                "customerName" to finalOrder.customerName,
                "customerPhone" to finalOrder.customerPhone,
                "shippingAddress" to finalOrder.shippingAddress,
                "paymentMethod" to finalOrder.paymentMethod,
                "paymentStatus" to finalOrder.paymentStatus,
                "subtotal" to finalOrder.subtotal,
                "vatAmount" to finalOrder.vatAmount,
                "shippingFee" to finalOrder.shippingFee,
                "grandTotal" to finalOrder.grandTotal,
                "selectedDeliveryArea" to finalOrder.selectedDeliveryArea,
                "latitude" to finalOrder.latitude,
                "longitude" to finalOrder.longitude,
                "city" to finalOrder.city,
                "district" to finalOrder.district
            )

            docRef.set(orderData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore service is unavailable"))
        return try {
            db.collection("orders").document(orderId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.domain.model

data class OrderItem(
    val productId: String,
    val productName: String,
    val productImage: String,
    val quantity: Int,
    val unitPrice: Double
)

data class Order(
    val orderId: String,
    val userId: String,
    val storeId: String,
    val storeName: String,
    val status: String, // "Pending", "Processing", "Shipped", "Delivered", "Cancelled"
    val createdAt: Long,
    val totalAmount: Double,
    val currency: String, // "USD", "SYP"
    val items: List<OrderItem>,
    val customerName: String = "",
    val customerPhone: String = "",
    val shippingAddress: String = "",
    val paymentMethod: String = "Cash On Delivery",
    val paymentStatus: String = "Pending", // "Pending", "Paid", "Failed", "Cancelled"
    val subtotal: Double = 0.0,
    val vatAmount: Double = 0.0,
    val shippingFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val selectedDeliveryArea: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String = "",
    val district: String = ""
)

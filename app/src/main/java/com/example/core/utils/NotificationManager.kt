package com.example.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationManager {
    private const val TAG = "WasetPlusNotification"
    private const val CHANNEL_ID = "wasetplus_order_notifications"
    private const val CHANNEL_NAME = "Order Updates"

    // Set up channel for Android Oreo and above
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Real-time updates regarding your purchases and sales on WasetPlus"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification Channel successfully created")
        }
    }

    fun dispatchOrderStatusNotification(
        context: Context,
        orderId: String,
        status: String,
        storeName: String,
        customerName: String,
        itemName: String
    ) {
        Log.d(TAG, "Notification prepared: status=$status, order=$orderId, store=$storeName, customer=$customerName")

        val title: String
        val body: String

        val isAr = LanguageManager.isArabic(context)

        when (status) {
            "Processing" -> {
                title = if (isAr) "تم قبول طلبك! 🎉" else "Order Accepted! 🎉"
                body = if (isAr) {
                    "بدأ متجر $storeName في تجهيز منتج $itemName."
                } else {
                    "Store $storeName is now processing your order for $itemName."
                }
            }
            "Shipped" -> {
                title = if (isAr) "تم شحن طلبك! 🚚" else "Order Shipped! 🚚"
                body = if (isAr) {
                    "طلبك ذو الرقم $orderId في طريقه إليك الآن."
                } else {
                    "Your order $orderId has been shipped and is on its way."
                }
            }
            "Delivered" -> {
                title = if (isAr) "تم تسليم الطلب!" else "Order Delivered!"
                body = if (isAr) {
                    "تم تسليم $itemName بنجاح. شكراً لتسوقك معنا!"
                } else {
                    "Your item $itemName has been delivered successfully. Thank you!"
                }
            }
            "Cancelled" -> {
                title = if (isAr) "تم إلغاء الطلب ⚠️" else "Order Cancelled ⚠️"
                body = if (isAr) {
                    "تم إلغاء الطلب رقم $orderId من قبل البائع."
                } else {
                    "Order $orderId has been cancelled by the seller."
                }
            }
            else -> return
        }

        // Standard notification dispatcher
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // In case channel is not registered
            createNotificationChannel(context)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done) // system icon default for robust compile
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Distinct ID per order so notifications don't overwrite each other
            val notificationId = orderId.hashCode()
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notification successfully dispatched to system tray: ID=$notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post system notification", e)
        }
    }
}

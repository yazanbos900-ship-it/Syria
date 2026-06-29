package com.example.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            updateTokenInFirestore(userId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Always check for data payload so we can construct the notification locally with proper styling, images and links.
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: $data")
            val title = data["title"] ?: "WasetPlus"
            val body = data["body"] ?: ""
            val imageUrl = data["imageUrl"] ?: data["image"]
            val deepLink = data["deepLink"] ?: data["click_action"]
            
            sendNotification(title, body, imageUrl, deepLink)
        } else {
            // Fallback to standard notification block
            remoteMessage.notification?.let {
                Log.d(TAG, "Message Notification Body: ${it.body}")
                sendNotification(it.title ?: "WasetPlus", it.body ?: "", null, null)
            }
        }
    }

    private fun sendNotification(title: String, body: String, imageUrl: String?, deepLink: String?) {
        val channelId = "wasetplus_broadcast_notifications"
        val channelName = "Broadcast Notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a High Importance channel for Heads-Up Notifications (like WhatsApp/Telegram)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General and administrative broadcast announcements"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Setup intent to launch MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!deepLink.isNullOrBlank()) {
                putExtra("deepLink", deepLink)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // fallback stable drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Necessary for Heads-Up UI
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        // Synchronously download and attach the image if present (on FCM background thread)
        if (!imageUrl.isNullOrBlank()) {
            try {
                val loader = coil.Coil.imageLoader(this)
                val request = coil.request.ImageRequest.Builder(this)
                    .data(imageUrl)
                    .allowHardware(false) // Required for notification bitmaps
                    .build()
                
                // Synchronous load in background
                val result = kotlinx.coroutines.runBlocking {
                    (loader.execute(request) as? coil.request.SuccessResult)?.drawable
                }
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    notificationBuilder.setLargeIcon(bitmap)
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(null as Bitmap?)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load notification image synchronously: ${e.localizedMessage}")
            }
        }

        // Post notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification successfully dispatched: title=$title, id=$notificationId")
    }

    companion object {
        private const val TAG = "FCMService"

        fun updateTokenInFirestore(userId: String, token: String) {
            val db = FirebaseFirestore.getInstance()
            val updates = mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully registered for user: $userId")
                }
                .addOnFailureListener { e ->
                    // Fallback to setting if update fails (e.g. document does not have fields yet)
                    db.collection("users").document(userId)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM token successfully set (merged) for user: $userId")
                        }
                        .addOnFailureListener { err ->
                            Log.e(TAG, "Failed to set/update FCM token in Firestore", err)
                        }
                }
        }
    }
}

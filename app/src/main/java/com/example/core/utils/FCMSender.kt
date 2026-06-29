package com.example.core.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object FCMSender {
    private const val TAG = "FCMSender"
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendFCMNotification(
        serverKey: String,
        tokens: List<String>,
        title: String,
        body: String,
        imageUrl: String?,
        deepLink: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        if (serverKey.isBlank()) {
            return@withContext Result.failure(Exception("FCM Server Key is blank."))
        }
        if (tokens.isEmpty()) {
            return@withContext Result.success("No target device tokens found.")
        }

        try {
            // Split tokens into chunks of 1000 (FCM limit for registration_ids)
            val chunks = tokens.chunked(1000)
            var lastResponse = ""
            
            for (chunk in chunks) {
                val payload = JSONObject().apply {
                    put("registration_ids", JSONArray(chunk))
                    put("priority", "high")
                    put("data", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                        if (!imageUrl.isNullOrBlank()) {
                            put("imageUrl", imageUrl)
                        }
                        if (!deepLink.isNullOrBlank()) {
                            put("deepLink", deepLink)
                        }
                    })
                }

                val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .addHeader("Authorization", "key=$serverKey")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseStr = response.body?.string() ?: ""
                    lastResponse = responseStr
                    if (!response.isSuccessful) {
                        Log.e(TAG, "FCM partial chunk failure: Code ${response.code}, Response: $responseStr")
                        return@withContext Result.failure(Exception("FCM failed with status code ${response.code}: $responseStr"))
                    }
                    Log.d(TAG, "FCM partial chunk success: $responseStr")
                }
            }
            Result.success(lastResponse)
        } catch (e: IOException) {
            Log.e(TAG, "FCM connection network error", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "FCM delivery build exception", e)
            Result.failure(e)
        }
    }
}

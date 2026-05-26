package com.example.features.marketplace

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.core.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class CloudinaryUploader {
    private val tag = "CloudinaryUploader"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val cloudName = "dnp7vrwws"
    private val uploadPreset = "wasetplus_upload"

    suspend fun uploadFile(localUriString: String): Result<String> = 
        withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(localUriString)
            val context = ServiceLocator.applicationContext

            val mimeType = context.contentResolver.getType(uri) ?: "image/*"
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType) ?: "jpg"

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    Exception("Cannot open local URI stream")
                )

            val tempFile = File(
                context.cacheDir, 
                "upload_${UUID.randomUUID()}.$ext"
            )
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                tempFile.delete()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val secureUrl = json.getString("secure_url")
                    Log.d(tag, "Upload success: $secureUrl")
                    Result.success(secureUrl)
                } else {
                    Log.e(tag, "Upload failed: ${response.code} $responseBody")
                    Result.failure(
                        Exception("Cloudinary upload failed: ${response.code}")
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(tag, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

package com.example.features.marketplace

import android.net.Uri
import android.util.Log
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

class CloudinaryUploader {
    private val tag = "CloudinaryUploader"
    private val client = OkHttpClient()

    // 🔴 IMPORTANT: Replace with your actual Cloudinary Cloud Name and Unsigned Upload Preset
    // Configure them in Cloudinary Dashboard -> Settings -> Upload -> Enable Unsigned Uploading
    private val cloudName = "your_cloud_name" // e.g. "dxxxxxxxx"
    private val uploadPreset = "your_unsigned_preset" // e.g. "ml_default" or "unsigned_preset"

    /**
     * Uploads an image from local URI to Cloudinary via unsigned HTTP request.
     * Returns the secure download URL.
     */
    suspend fun uploadFile(localUriString: String): Result<String> = withContext(Dispatchers.IO) {
        if (cloudName == "your_cloud_name" || uploadPreset == "your_unsigned_preset") {
            Log.e(tag, "Cloudinary configuration missing. Please add your cloud_name and unsigned upload_preset.")
            // Falling back to a dummy URL so the flow doesn't break for testing if not configured
            return@withContext Result.success("https://via.placeholder.com/500?text=Cloudinary+Not+Configured")
        }

        try {
            val uri = Uri.parse(localUriString)
            val context = ServiceLocator.applicationContext
            
            // 1. Copy Content Uri to a temporary java.io.File for OkHttp
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open local URI stream"))
            
            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            // 2. Build Multipart Body for Cloudinary
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file", 
                    tempFile.name,
                    tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            // 3. Make HTTP POST
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            tempFile.delete() // Clean up temp file

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val secureUrl = json.getString("secure_url")
                Log.d(tag, "Successful upload to Cloudinary. Url: $secureUrl")
                Result.success(secureUrl)
            } else {
                Log.e(tag, "Cloudinary upload failed: ${response.code} $responseBody")
                Result.failure(Exception("Cloudinary upload failed: ${response.code}"))
            }

        } catch (e: Exception) {
            Log.e(tag, "Exception during Cloudinary upload: ${e.message}", e)
            Result.failure(e)
        }
    }
}

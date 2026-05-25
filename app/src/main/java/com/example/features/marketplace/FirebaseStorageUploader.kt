package com.example.features.marketplace

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageUploader {
    private val tag = "FirebaseStorageUploader"
    
    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firebase Storage instance could not be fetched", e)
            null
        }
    }

    /**
     * Uploads an image from local URI and returns download URL.
     * Graces fallback URLs if storage is unavailable or permission fails.
     */
    suspend fun uploadFile(localUriString: String, storagePath: String): Result<String> {
        val sInstance = storage ?: return Result.failure(Exception("Storage not available"))
        return try {
            val uri = Uri.parse(localUriString)
            val ref = sInstance.reference.child(storagePath)
            
            // Upload
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(tag, "Successful upload to path: $storagePath. Url: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(tag, "Failed to upload to real Firebase Storage path $storagePath: ${e.message}", e)
            Result.failure(e)
        }
    }
}

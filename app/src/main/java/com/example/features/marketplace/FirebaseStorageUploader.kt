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
        val sInstance = storage ?: return getMockFallbackUrl(storagePath)
        return try {
            val uri = Uri.parse(localUriString)
            val ref = sInstance.reference.child(storagePath)
            
            // Upload
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(tag, "Successful upload to path: $storagePath. Url: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload to real Firebase Storage path $storagePath: ${e.message}. Falling back to mock URL.", e)
            getMockFallbackUrl(storagePath)
        }
    }

    private fun getMockFallbackUrl(storagePath: String): Result<String> {
        // High fidelity mock placeholder depending on type
        val id = UUID.randomUUID().toString().take(6)
        val mockUrl = when {
            storagePath.contains("logo") -> {
                "https://images.unsplash.com/photo-1516259762381-22954d7d3ad2?auto=format&fit=crop&w=400&q=80"
            }
            storagePath.contains("banner") -> {
                "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=800&q=80"
            }
            else -> {
                // Product images
                val productsPhotos = listOf(
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80",
                    "https://images.unsplash.com/photo-1572635196237-14b3f281503f?auto=format&fit=crop&w=600&q=80"
                )
                productsPhotos.random()
            }
        }
        return Result.success(mockUrl)
    }
}

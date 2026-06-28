package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import com.example.domain.model.JobApplication
import com.example.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobDetailsViewModel : ViewModel() {

    private val jobRepository = ServiceLocator.jobRepository
    private val authRepository = ServiceLocator.authRepository

    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _applyState = MutableStateFlow<UiState>(UiState.Idle)
    val applyState: StateFlow<UiState> = _applyState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var jobListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var saveListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUser.value = user
                _job.value?.id?.let { jobId ->
                    setupSaveListener(jobId)
                }
            }
        }
    }

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            jobListener?.remove()
            saveListener?.remove()
            
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val docRef = db.collection("jobs").document(jobId)
            
            // Increment view count instantly
            docRef.update("viewsCount", com.google.firebase.firestore.FieldValue.increment(1))
            
            // Listen in real-time
            jobListener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = error.localizedMessage
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val parsed = snapshot.toObject(Job::class.java)
                    if (parsed != null) {
                        val baseJob = if (parsed.contactWhatsApp.isBlank()) parsed.copy(contactWhatsApp = "+963930111157") else parsed
                        if (baseJob.storeId.isNotBlank()) {
                            db.collection("stores").document(baseJob.storeId)
                                .get()
                                .addOnSuccessListener { storeSnap ->
                                    if (storeSnap != null && storeSnap.exists()) {
                                        val storeName = storeSnap.getString("storeName") ?: storeSnap.getString("name") ?: baseJob.storeName
                                        val storeLogoUrl = storeSnap.getString("logoUrl") ?: baseJob.storeLogoUrl
                                        val isStoreVerified = storeSnap.getBoolean("isVerified") ?: (storeSnap.getString("verificationStatus") == "Verified") ?: baseJob.isStoreVerified
                                        _job.value = baseJob.copy(
                                            storeName = storeName,
                                            storeLogoUrl = storeLogoUrl,
                                            isStoreVerified = isStoreVerified
                                        )
                                    } else {
                                        _job.value = baseJob
                                    }
                                }
                                .addOnFailureListener {
                                    _job.value = baseJob
                                }
                        } else {
                            _job.value = baseJob
                        }
                    }
                    _isLoading.value = false
                    setupSaveListener(jobId)
                } else {
                    _error.value = "الوظيفة غير موجودة"
                    _isLoading.value = false
                }
            }
        }
    }

    private fun setupSaveListener(jobId: String) {
        val user = _currentUser.value ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        saveListener?.remove()
        saveListener = db.collection("job_saves").document("${user.id}_$jobId")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    _isSaved.value = snapshot.exists()
                }
            }
    }

    fun toggleSaveJob() {
        val user = _currentUser.value ?: return
        val currentJob = _job.value ?: return
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val saveDocRef = db.collection("job_saves").document("${user.id}_${currentJob.id}")
        val jobDocRef = db.collection("jobs").document(currentJob.id)
        
        val currentlySaved = _isSaved.value
        
        db.runTransaction { transaction ->
            val saveExists = transaction.get(saveDocRef).exists()
            val jobSnap = transaction.get(jobDocRef)
            val currentCount = (jobSnap.get("savesCount") as? Number)?.toLong() ?: 0L
            
            if (currentlySaved || saveExists) {
                transaction.delete(saveDocRef)
                transaction.update(jobDocRef, "savesCount", (currentCount - 1).coerceAtLeast(0L))
            } else {
                val saveMap = hashMapOf(
                    "userId" to user.id,
                    "jobId" to currentJob.id,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(saveDocRef, saveMap)
                transaction.update(jobDocRef, "savesCount", currentCount + 1)
            }
        }.addOnSuccessListener {
            // Updated successfully
        }.addOnFailureListener { e ->
            android.util.Log.e("JobDetailsVM", "toggleSaveJob transaction failed", e)
        }
    }

    fun applyForJob(job: Job, name: String, phone: String, email: String, message: String, cvUrl: String?) {
        viewModelScope.launch {
            _applyState.value = UiState.Loading
            val user = _currentUser.value
            
            if (user == null) {
                _applyState.value = UiState.Error("You must be logged in to apply.")
                return@launch
            }
            
            val app = JobApplication(
                jobId = job.id,
                storeId = job.storeId,
                applicantId = user.id,
                applicantName = name,
                phone = phone,
                email = email,
                message = message,
                cvUrl = cvUrl
            )
            
            val result = jobRepository.applyForJob(app)
            if (result.isSuccess) {
                _applyState.value = UiState.Success("Application submitted successfully!")
                
                // Increment applicationsCount in firestore
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("jobs")
                        .document(job.id)
                        .update("applicationsCount", com.google.firebase.firestore.FieldValue.increment(1))
                } catch (e: Exception) {
                    android.util.Log.e("JobDetailsVM", "Failed to increment applicationsCount", e)
                }
            } else {
                _applyState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to submit application.")
            }
        }
    }
    
    fun resetApplyState() {
        _applyState.value = UiState.Idle
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    override fun onCleared() {
        super.onCleared()
        jobListener?.remove()
        saveListener?.remove()
    }
}

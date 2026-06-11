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

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUserSession()
        }
    }

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fetchedJob = jobRepository.getJobById(jobId)
                if (fetchedJob != null) {
                    _job.value = fetchedJob
                } else {
                    _error.value = "Job not found."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while fetching job details."
            } finally {
                _isLoading.value = false
            }
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
}

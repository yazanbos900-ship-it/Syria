package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateEditJobViewModel : ViewModel() {
    private val jobRepository = ServiceLocator.jobRepository
    private val storeRepository = ServiceLocator.storeRepository
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()
    
    fun loadJob(jobId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val fetchedJob = jobRepository.getJobById(jobId)
                if (fetchedJob != null) {
                    _job.value = fetchedJob
                    _uiState.value = UiState.Idle
                } else {
                    _uiState.value = UiState.Error("Job not found.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load job.")
            }
        }
    }

    fun saveJob(
        storeId: String,
        jobId: String?,
        title: String,
        description: String,
        requirements: String,
        responsibilities: String,
        location: String,
        employmentType: String,
        salary: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val store = storeRepository.getStoreById(storeId) ?: throw Exception("Store not found")
                
                val existingJob = if (jobId != null) jobRepository.getJobById(jobId) else null
                
                val jobToSave = Job(
                    id = existingJob?.id ?: "",
                    storeId = store.id,
                    ownerId = store.ownerId,
                    title = title,
                    description = description,
                    requirements = requirements,
                    responsibilities = responsibilities,
                    location = location,
                    employmentType = employmentType,
                    salary = salary,
                    status = existingJob?.status ?: "active",
                    createdAt = existingJob?.createdAt ?: System.currentTimeMillis(),
                    storeName = store.name,
                    storeLogoUrl = store.logoUrl,
                    isStoreVerified = store.isVerified
                )
                
                if (existingJob != null) {
                    val result = jobRepository.updateJob(jobToSave)
                    if (result.isSuccess) {
                        _uiState.value = UiState.Success("Job updated successfully.")
                    } else {
                        _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Update failed.")
                    }
                } else {
                    val result = jobRepository.createJob(jobToSave)
                    if (result.isSuccess) {
                        _uiState.value = UiState.Success("Job created successfully.")
                    } else {
                        _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Creation failed.")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to save job.")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = UiState.Idle
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}

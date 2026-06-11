package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import com.example.domain.model.JobApplication
import com.example.domain.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ManageJobsViewModel : ViewModel() {
    private val jobRepository = ServiceLocator.jobRepository
    private val storeRepository = ServiceLocator.storeRepository
    private val authRepository = ServiceLocator.authRepository

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _store = MutableStateFlow<Store?>(null)
    val store: StateFlow<Store?> = _store.asStateFlow()

    fun loadData(storeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedStore = storeRepository.getStoreById(storeId)
                _store.value = fetchedStore

                launch {
                    jobRepository.getJobsByStoreId(storeId)
                        .catch { e ->
                            _error.value = e.message
                        }
                        .collect { jobList ->
                            _jobs.value = jobList
                        }
                }
                
                launch {
                    jobRepository.getApplicationsByStoreId(storeId)
                        .catch { e -> _error.value = e.message }
                        .collect { appList -> _applications.value = appList }
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            jobRepository.deleteJob(jobId)
        }
    }
    
    fun updateJobStatus(job: Job, newStatus: String) {
        viewModelScope.launch {
            jobRepository.updateJob(job.copy(status = newStatus))
        }
    }

    fun updateApplicationStatus(applicationId: String, status: String) {
        viewModelScope.launch {
            jobRepository.updateApplicationStatus(applicationId, status)
        }
    }
}

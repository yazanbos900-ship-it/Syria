package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class JobsMarketplaceViewModel : ViewModel() {
    private val jobRepository = ServiceLocator.jobRepository
    
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadJobs()
    }

    private fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            jobRepository.getActiveJobs()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { jobList ->
                    _jobs.value = jobList
                    _isLoading.value = false
                }
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _isLoading.value = true
            jobRepository.searchJobs(query)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { jobList ->
                    _jobs.value = jobList
                    _isLoading.value = false
                }
        }
    }
}

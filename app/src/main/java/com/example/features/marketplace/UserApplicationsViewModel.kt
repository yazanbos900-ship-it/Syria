package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.JobApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class UserApplicationsViewModel : ViewModel() {
    private val jobRepository = ServiceLocator.jobRepository
    private val authRepository = ServiceLocator.authRepository

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            val session = authRepository.getCurrentUserSession()
            if (session != null) {
                jobRepository.getApplicationsByUserId(session.id)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { appList ->
                        _applications.value = appList
                        _isLoading.value = false
                    }
            } else {
                _error.value = "You must be logged in to view applications."
                _isLoading.value = false
            }
        }
    }
}

package com.example.domain.repository

import com.example.domain.model.Job
import com.example.domain.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    suspend fun createJob(job: Job): Result<String>
    suspend fun updateJob(job: Job): Result<Unit>
    suspend fun deleteJob(jobId: String): Result<Unit>
    suspend fun getJobById(jobId: String): Job?
    
    fun getActiveJobs(): Flow<List<Job>>
    fun getJobsByStoreId(storeId: String): Flow<List<Job>>
    fun searchJobs(query: String, category: String? = null, location: String? = null, employmentType: String? = null): Flow<List<Job>>
    
    suspend fun applyForJob(application: JobApplication): Result<String>
    suspend fun updateApplicationStatus(applicationId: String, status: String): Result<Unit>
    
    fun getApplicationsForJob(jobId: String): Flow<List<JobApplication>>
    fun getApplicationsByStoreId(storeId: String): Flow<List<JobApplication>>
    fun getApplicationsByUserId(userId: String): Flow<List<JobApplication>>
}

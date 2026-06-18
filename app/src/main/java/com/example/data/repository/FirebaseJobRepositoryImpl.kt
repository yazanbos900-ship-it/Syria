package com.example.data.repository

import com.example.domain.model.Job
import com.example.domain.model.JobApplication
import com.example.domain.repository.JobRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseJobRepositoryImpl : JobRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val jobsCollection = firestore.collection("jobs")
    private val applicationsCollection = firestore.collection("job_applications")

    override suspend fun createJob(job: Job): Result<String> {
        return try {
            val jobId = UUID.randomUUID().toString()
            val newJob = job.copy(id = jobId)
            jobsCollection.document(jobId).set(newJob).await()
            Result.success(jobId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateJob(job: Job): Result<Unit> {
        return try {
            jobsCollection.document(job.id).set(job).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteJob(jobId: String): Result<Unit> {
        return try {
            jobsCollection.document(jobId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJobById(jobId: String): Job? {
        return try {
            val document = jobsCollection.document(jobId).get().await()
            document.toObject(Job::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getAllJobs(): Flow<List<Job>> = callbackFlow {
        val listener = jobsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
                trySend(jobs.sortedByDescending { it.createdAt })
            }
            
        awaitClose { listener.remove() }
    }

    override fun getAllApplications(): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getActiveJobs(): Flow<List<Job>> = callbackFlow {
        val listener = jobsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) }?.filter { it.status == "active" } ?: emptyList()
                trySend(jobs)
            }
            
        awaitClose { listener.remove() }
    }

    override fun getJobsByStoreId(storeId: String): Flow<List<Job>> = callbackFlow {
        val listener = jobsCollection
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
                trySend(jobs.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun searchJobs(
        query: String,
        category: String?,
        location: String?,
        employmentType: String?,
        experienceLevel: String?
    ): Flow<List<Job>> = callbackFlow {
        val firestoreQuery: Query = jobsCollection

        val listener = firestoreQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            var jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
            jobs = jobs.filter { it.status == "active" }.sortedByDescending { it.createdAt }
            
            if (category != null && category.isNotBlank()) {
                jobs = jobs.filter { it.category == category }
            }
            if (location != null && location.isNotBlank()) {
                jobs = jobs.filter { it.location == location }
            }
            if (employmentType != null && employmentType.isNotBlank()) {
                jobs = jobs.filter { it.employmentType == employmentType }
            }
            if (experienceLevel != null && experienceLevel.isNotBlank()) {
                jobs = jobs.filter { it.experienceLevel == experienceLevel }
            }
            
            // Client side filtering for title because Firestore lacks full text search
            if (query.isNotBlank()) {
                val lowercaseQuery = query.lowercase()
                jobs = jobs.filter { 
                    it.title.lowercase().contains(lowercaseQuery) || 
                    it.description.lowercase().contains(lowercaseQuery) 
                }
            }
            trySend(jobs)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun applyForJob(application: JobApplication): Result<String> {
        return try {
            val applicationId = UUID.randomUUID().toString()
            val newApp = application.copy(id = applicationId)
            applicationsCollection.document(applicationId).set(newApp).await()
            
            // TODO (Notifications): Notify Store Owner that a new application was submitted
            // Example: notificationService.sendNotification(toStoreId = application.storeId, message = "New application for job ${application.jobId}")
            
            Result.success(applicationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateApplicationStatus(applicationId: String, status: String): Result<Unit> {
        return try {
            applicationsCollection.document(applicationId).update("status", status).await()
            
            // TODO (Notifications): Fetch application details and Notify Applicant
            // Example: val app = applicationsCollection.document(applicationId).get().await().toObject(JobApplication::class.java)
            // if (app != null) notificationService.sendNotification(toUserId = app.applicantId, message = "Your application status changed to $status")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getApplicationsForJob(jobId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("jobId", jobId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getApplicationsByStoreId(storeId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("storeId", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    override fun getApplicationsByUserId(userId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("applicantId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }
}

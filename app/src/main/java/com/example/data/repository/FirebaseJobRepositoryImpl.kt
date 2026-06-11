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

    override fun getActiveJobs(): Flow<List<Job>> = callbackFlow {
        val listener = jobsCollection
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
                trySend(jobs)
            }
            
        awaitClose { listener.remove() }
    }

    override fun getJobsByStoreId(storeId: String): Flow<List<Job>> = callbackFlow {
        val listener = jobsCollection
            .whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
                trySend(jobs)
            }
        awaitClose { listener.remove() }
    }

    override fun searchJobs(
        query: String,
        category: String?,
        location: String?,
        employmentType: String?
    ): Flow<List<Job>> = callbackFlow {
        var firestoreQuery: Query = jobsCollection.whereEqualTo("status", "active")
        
        if (location != null && location.isNotBlank()) {
            firestoreQuery = firestoreQuery.whereEqualTo("location", location)
        }
        if (employmentType != null && employmentType.isNotBlank()) {
            firestoreQuery = firestoreQuery.whereEqualTo("employmentType", employmentType)
        }
        
        firestoreQuery = firestoreQuery.orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = firestoreQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            var jobs = snapshot?.documents?.mapNotNull { it.toObject(Job::class.java) } ?: emptyList()
            
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
            Result.success(applicationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateApplicationStatus(applicationId: String, status: String): Result<Unit> {
        return try {
            applicationsCollection.document(applicationId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getApplicationsForJob(jobId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("jobId", jobId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }

    override fun getApplicationsByStoreId(storeId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("storeId", storeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }

    override fun getApplicationsByUserId(userId: String): Flow<List<JobApplication>> = callbackFlow {
        val listener = applicationsCollection
            .whereEqualTo("applicantId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { it.toObject(JobApplication::class.java) } ?: emptyList()
                trySend(apps)
            }
        awaitClose { listener.remove() }
    }
}

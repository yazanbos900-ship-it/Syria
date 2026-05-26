package com.example.data.repository

import android.util.Log
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl : AuthRepository {
    private val tag = "FirebaseAuthRepository"
    
    // Lazy reference to prevent crashing if FirebaseApp isn't initialized yet
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseAuth not available", e)
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Firestore not available", e)
            null
        }
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUserFlow.asStateFlow()

    private suspend fun syncAndGetUser(
        uid: String,
        fallbackEmail: String,
        fallbackName: String,
        fallbackJoinedAt: Long
    ): User {
        val db = firestore
        val isDefaultAdmin = fallbackEmail.equals("yazan.bos900@gmail.com", ignoreCase = true) || fallbackEmail.equals("admin@gmail.com", ignoreCase = true)
        val defaultRole = if (isDefaultAdmin) "admin" else "client"

        if (db == null) {
            return User(
                id = uid,
                email = fallbackEmail,
                name = fallbackName,
                isStoreOwner = false,
                role = defaultRole,
                joinedAt = fallbackJoinedAt
            )
        }

        return try {
            val docRef = db.collection("users").document(uid)
            val doc = docRef.get().await()
            if (doc.exists()) {
                val email = doc.getString("email") ?: fallbackEmail
                val name = doc.getString("name") ?: fallbackName
                val profileImageUrl = doc.getString("profileImageUrl")
                val phoneNumber = doc.getString("phoneNumber")
                val isStoreOwner = doc.getBoolean("isStoreOwner") ?: false
                val storedRole = doc.getString("role") ?: defaultRole
                val role = if (isDefaultAdmin) "admin" else storedRole
                if (isDefaultAdmin && storedRole != "admin") {
                    try { docRef.update("role", "admin").await() } catch (e: Exception) { Log.e(tag, "Failed to update role", e) }
                }
                val joinedAt = doc.getLong("joinedAt") ?: fallbackJoinedAt
                User(
                    id = uid,
                    email = email,
                    name = name,
                    profileImageUrl = profileImageUrl,
                    phoneNumber = phoneNumber,
                    isStoreOwner = isStoreOwner,
                    role = role,
                    joinedAt = joinedAt
                )
            } else {
                val userMap = hashMapOf(
                    "id" to uid,
                    "email" to fallbackEmail,
                    "name" to fallbackName,
                    "isStoreOwner" to false,
                    "role" to defaultRole,
                    "joinedAt" to fallbackJoinedAt
                )
                docRef.set(userMap).await()
                User(
                    id = uid,
                    email = fallbackEmail,
                    name = fallbackName,
                    isStoreOwner = false,
                    role = defaultRole,
                    joinedAt = fallbackJoinedAt
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing user doc", e)
            User(
                id = uid,
                email = fallbackEmail,
                name = fallbackName,
                isStoreOwner = false,
                role = defaultRole,
                joinedAt = fallbackJoinedAt
            )
        }
    }

    init {
        // Automatically seed admin account if not existing
        repositoryScope.launch {
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val result = auth.createUserWithEmailAndPassword("admin@gmail.com", "yazan_225").await()
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        val db = firestore
                        if (db != null) {
                            val userMap = hashMapOf(
                                "id" to firebaseUser.uid,
                                "email" to "admin@gmail.com",
                                "name" to "yazan",
                                "isStoreOwner" to false,
                                "role" to "admin",
                                "joinedAt" to System.currentTimeMillis()
                            )
                            db.collection("users").document(firebaseUser.uid).set(userMap).await()
                            Log.d(tag, "Seeded yazan as admin successfully!")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(tag, "Admin seeding check completed: ${e.localizedMessage}")
            }
        }

        // Listen to auth state changes from Firebase
        firebaseAuth?.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                repositoryScope.launch {
                    val user = syncAndGetUser(
                        uid = firebaseUser.uid,
                        fallbackEmail = firebaseUser.email ?: "",
                        fallbackName = firebaseUser.displayName ?: "User",
                        fallbackJoinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                    )
                    _currentUserFlow.value = user
                }
            } else {
                _currentUserFlow.value = null
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Authentication service is unavailable."))
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Login failed: User empty")
            val user = syncAndGetUser(
                uid = firebaseUser.uid,
                fallbackEmail = firebaseUser.email ?: "",
                fallbackName = firebaseUser.displayName ?: "WasetPlus User",
                fallbackJoinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
            _currentUserFlow.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Sign in failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Authentication service is unavailable."))
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Signup failed: User empty")
            val user = syncAndGetUser(
                uid = firebaseUser.uid,
                fallbackEmail = firebaseUser.email ?: "",
                fallbackName = name,
                fallbackJoinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
            )
            _currentUserFlow.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Sign up failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Authentication service is unavailable."))
        return try {
            auth.signOut()
            _currentUserFlow.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Sign out failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserSession(): User? {
        val firebaseUser = firebaseAuth?.currentUser ?: return null
        return syncAndGetUser(
            uid = firebaseUser.uid,
            fallbackEmail = firebaseUser.email ?: "",
            fallbackName = firebaseUser.displayName ?: "WasetPlus User",
            fallbackJoinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Authentication service is unavailable."))
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Password reset failed", e)
            Result.failure(e)
        }
    }
}

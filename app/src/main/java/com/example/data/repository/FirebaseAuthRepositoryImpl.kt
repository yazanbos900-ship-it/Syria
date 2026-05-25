package com.example.data.repository

import android.util.Log
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _currentUserFlow = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUserFlow.asStateFlow()

    init {
        // Listen to auth state changes from Firebase
        firebaseAuth?.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                _currentUserFlow.value = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "User",
                    joinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                )
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
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "WasetPlus User",
                joinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
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
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = name,
                joinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
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
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "WasetPlus User",
            joinedAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }
}

package com.focusflow.app.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
    fun login(email: String, password: String): Flow<Result<Unit>>
    fun register(email: String, password: String): Flow<Result<Unit>>
    fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : AuthRepository {

    // Local fallback accounts and the signed-in session survive app restarts here
    private val prefs = context.getSharedPreferences("focusflow_local_auth", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth?
        get() = runCatching { FirebaseAuth.getInstance() }.getOrNull()


    init {
        prefs.getString("session_email", null)?.let { email ->
            localUserId = "local_" + email.hashCode()
            _localAuthState.value = true
        }
    }

    private companion object {
        val _localAuthState = MutableStateFlow(false)
        @Volatile var localUserId: String? = null
    }

    private fun hashPassword(email: String, password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$email:$password".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun localRegister(email: String, password: String): Result<Unit> {
        val key = email.trim().lowercase()
        if (key.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email and password are required."))
        }
        if (prefs.contains("cred_$key")) {
            return Result.failure(IllegalStateException("An account with this email already exists."))
        }
        prefs.edit().putString("cred_$key", hashPassword(key, password)).putString("session_email", key).apply()
        localUserId = "local_" + key.hashCode()
        _localAuthState.value = true
        return Result.success(Unit)
    }

    private fun localLogin(email: String, password: String): Result<Unit> {
        val key = email.trim().lowercase()
        val stored = prefs.getString("cred_$key", null)
            ?: return Result.failure(IllegalStateException("No account found for this email. Please register first."))
        if (stored != hashPassword(key, password)) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        prefs.edit().putString("session_email", key).apply()
        localUserId = "local_" + key.hashCode()
        _localAuthState.value = true
        return Result.success(Unit)
    }

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            val job = launch {
                _localAuthState.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = FirebaseAuth.AuthStateListener { fa ->
                trySend(fa.currentUser != null || _localAuthState.value)
            }
            val localJob = launch {
                _localAuthState.collect { trySend(auth.currentUser != null || it) }
            }
            auth.addAuthStateListener(listener)
            trySend(auth.currentUser != null || _localAuthState.value)
            awaitClose {
                localJob.cancel()
                auth.removeAuthStateListener(listener)
            }
        }
    }

    override fun getCurrentUserId(): String? {
        val fbUser = runCatching { firebaseAuth?.currentUser }.getOrNull()
        return fbUser?.uid ?: localUserId
    }

    override fun login(email: String, password: String): Flow<Result<Unit>> = flow {
        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                emit(Result.success(Unit))
            } catch (e: Exception) {
                val causeMsg = e.message ?: ""
                if (causeMsg.contains("FirebaseApp") || causeMsg.contains("not initialized") || causeMsg.contains("API key")) {
                    emit(localLogin(email, password))
                } else {
                    emit(Result.failure(e))
                }
            }
        } else {
            emit(localLogin(email, password))
        }
    }

    override fun register(email: String, password: String): Flow<Result<Unit>> = flow {
        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                emit(Result.success(Unit))
            } catch (e: Exception) {
                val causeMsg = e.message ?: ""
                if (causeMsg.contains("FirebaseApp") || causeMsg.contains("not initialized") || causeMsg.contains("API key")) {
                    emit(localRegister(email, password))
                } else {
                    emit(Result.failure(e))
                }
            }
        } else {
            emit(localRegister(email, password))
        }
    }

    override fun logout() {
        prefs.edit().remove("session_email").apply()
        localUserId = null
        _localAuthState.value = false
        runCatching { firebaseAuth?.signOut() }
    }
}

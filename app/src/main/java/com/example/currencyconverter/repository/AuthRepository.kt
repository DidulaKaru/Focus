package com.example.currencyconverter.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
    fun login(email: String, password: String): Flow<Result<Unit>>
    fun register(email: String, password: String): Flow<Result<Unit>>
    fun logout()
}

class AuthRepositoryImpl : AuthRepository {

    private val firebaseAuth: FirebaseAuth?
        get() = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val _localAuthState = MutableStateFlow(false)
    private var localUserId: String? = null

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
            auth.addAuthStateListener(listener)
            trySend(auth.currentUser != null || _localAuthState.value)
            awaitClose { auth.removeAuthStateListener(listener) }
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
                    localUserId = "local_" + email.hashCode()
                    _localAuthState.value = true
                    emit(Result.success(Unit))
                } else {
                    emit(Result.failure(e))
                }
            }
        } else {
            localUserId = "local_" + email.hashCode()
            _localAuthState.value = true
            emit(Result.success(Unit))
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
                    localUserId = "local_" + email.hashCode()
                    _localAuthState.value = true
                    emit(Result.success(Unit))
                } else {
                    emit(Result.failure(e))
                }
            }
        } else {
            localUserId = "local_" + email.hashCode()
            _localAuthState.value = true
            emit(Result.success(Unit))
        }
    }

    override fun logout() {
        runCatching { firebaseAuth?.signOut() }
        localUserId = null
        _localAuthState.value = false
    }
}

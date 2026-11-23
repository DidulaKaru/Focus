package com.example.currencyconverter.repository

import com.example.currencyconverter.model.PomodoroSessionModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface PomodoroRepository {
    suspend fun recordSession(userId: String, session: PomodoroSessionModel): Result<Unit>
    fun getSessions(userId: String): Flow<List<PomodoroSessionModel>>
}

class PomodoroRepositoryImpl : PomodoroRepository {

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val localSessionsMap = ConcurrentHashMap<String, MutableStateFlow<List<PomodoroSessionModel>>>()

    private fun getLocalSessionsFlow(userId: String): MutableStateFlow<List<PomodoroSessionModel>> {
        return localSessionsMap.getOrPut(userId) { MutableStateFlow(emptyList()) }
    }

    override fun getSessions(userId: String): Flow<List<PomodoroSessionModel>> = callbackFlow {
        val localFlow = getLocalSessionsFlow(userId)
        val db = firestore
        if (db == null || userId.isEmpty()) {
            val job = launch {
                localFlow.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val collectionRef = db.collection("users").document(userId).collection("pomodoro_sessions")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(localFlow.value)
                return@addSnapshotListener
            }
            val sessions = snapshot.toObjects(PomodoroSessionModel::class.java)
            localFlow.value = sessions
            trySend(sessions)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun recordSession(userId: String, session: PomodoroSessionModel): Result<Unit> {
        val finalSession = if (session.id.isEmpty()) {
            session.copy(id = UUID.randomUUID().toString())
        } else {
            session
        }

        val localFlow = getLocalSessionsFlow(userId)
        localFlow.value = localFlow.value.filterNot { it.id == finalSession.id } + finalSession

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("pomodoro_sessions")
                    .document(finalSession.id)
                    .set(finalSession)
                    .await()
            } catch (e: Exception) {
                // Local fallback
            }
        }
        return Result.success(Unit)
    }
}

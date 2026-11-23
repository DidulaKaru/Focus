package com.focusflow.app.repository

import com.focusflow.app.model.PomodoroSessionModel
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface PomodoroRepository {
    suspend fun recordSession(userId: String, session: PomodoroSessionModel): Result<Unit>
    fun getSessions(userId: String): Flow<List<PomodoroSessionModel>>
}

@Singleton
class PomodoroRepositoryImpl @Inject constructor() : PomodoroRepository {

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private companion object {
        val localSessionsMap = ConcurrentHashMap<String, MutableStateFlow<List<PomodoroSessionModel>>>()
    }

    private fun getLocalSessionsFlow(userId: String): MutableStateFlow<List<PomodoroSessionModel>> {
        return localSessionsMap.computeIfAbsent(userId) {
            LocalStore.createFlow("sessions", userId, LocalStore::sessionToJson, LocalStore::sessionFromJson)
        }
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

        val collectionRef = db.collection("users").document(userId).collection("pomodoroSessions")
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
                    .collection("pomodoroSessions")
                    .document(finalSession.id)
                    .set(finalSession)
                    .addOnFailureListener { e ->
                        Log.w("PomodoroRepo", "Firestore write failed", e)
                        RepositoryErrors.report("Couldn't save your session to the cloud. It is kept on this device.")
                    }
            } catch (e: Exception) {
                // Local fallback
            }
        }
        return Result.success(Unit)
    }
}

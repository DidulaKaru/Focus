package com.focusflow.app.repository

import com.focusflow.app.model.TaskModel
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

interface TaskRepository {
    fun getTasks(userId: String): Flow<List<TaskModel>>
    suspend fun addTask(userId: String, task: TaskModel): Result<Unit>
    suspend fun updateTask(userId: String, task: TaskModel): Result<Unit>
    suspend fun deleteTask(userId: String, taskId: String): Result<Unit>
    suspend fun toggleTaskCompletion(userId: String, taskId: String, isCompleted: Boolean): Result<Unit>
}

@Singleton
class TaskRepositoryImpl @Inject constructor() : TaskRepository {

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private companion object {
        // Shared across instances so every ViewModel sees the same local fallback data
        val localTasksMap = ConcurrentHashMap<String, MutableStateFlow<List<TaskModel>>>()
    }

    private fun getLocalTasksFlow(userId: String): MutableStateFlow<List<TaskModel>> {
        return localTasksMap.computeIfAbsent(userId) {
            LocalStore.createFlow("tasks", userId, LocalStore::taskToJson, LocalStore::taskFromJson)
        }
    }

    override fun getTasks(userId: String): Flow<List<TaskModel>> = callbackFlow {
        val localFlow = getLocalTasksFlow(userId)
        val db = firestore
        if (db == null || userId.isEmpty()) {
            val job = launch {
                localFlow.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val collectionRef = db.collection("users").document(userId).collection("tasks")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(localFlow.value)
                return@addSnapshotListener
            }
            val tasks = snapshot.toObjects(TaskModel::class.java)
            localFlow.value = tasks
            trySend(tasks)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addTask(userId: String, task: TaskModel): Result<Unit> {
        val finalTask = if (task.id.isEmpty()) {
            task.copy(id = UUID.randomUUID().toString())
        } else {
            task
        }

        val localFlow = getLocalTasksFlow(userId)
        localFlow.value = localFlow.value.filterNot { it.id == finalTask.id } + finalTask

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(finalTask.id)
                    .set(finalTask)
                    .addOnFailureListener { e ->
                        Log.w("TaskRepo", "Firestore write failed", e)
                        RepositoryErrors.report("Couldn't save your task to the cloud. It is kept on this device.")
                    }
            } catch (e: Exception) {
                // Fallback to local
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateTask(userId: String, task: TaskModel): Result<Unit> {
        val localFlow = getLocalTasksFlow(userId)
        localFlow.value = localFlow.value.map { if (it.id == task.id) task else it }

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(task.id)
                    .set(task)
                    .addOnFailureListener { e ->
                        Log.w("TaskRepo", "Firestore write failed", e)
                        RepositoryErrors.report("Couldn't save your task to the cloud. It is kept on this device.")
                    }
            } catch (e: Exception) {
                // Fallback to local
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val localFlow = getLocalTasksFlow(userId)
        localFlow.value = localFlow.value.filterNot { it.id == taskId }

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(taskId)
                    .delete()
                    .addOnFailureListener { e ->
                        Log.w("TaskRepo", "Firestore write failed", e)
                        RepositoryErrors.report("Couldn't save your task to the cloud. It is kept on this device.")
                    }
            } catch (e: Exception) {
                // Fallback to local
            }
        }
        return Result.success(Unit)
    }

    override suspend fun toggleTaskCompletion(userId: String, taskId: String, isCompleted: Boolean): Result<Unit> {
        val localFlow = getLocalTasksFlow(userId)
        localFlow.value = localFlow.value.map {
            if (it.id == taskId) it.copy(isCompleted = isCompleted) else it
        }

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .document(taskId)
                    .update("isCompleted", isCompleted)
                    .addOnFailureListener { e ->
                        // Doc may not exist yet: fall back to a full (non-blocking) write
                        val updatedTask = localFlow.value.find { it.id == taskId }
                        if (updatedTask != null) {
                            db.collection("users")
                                .document(userId)
                                .collection("tasks")
                                .document(taskId)
                                .set(updatedTask)
                                .addOnFailureListener { ex ->
                                    Log.w("TaskRepo", "write failed", ex)
                                    RepositoryErrors.report("Couldn't save your task to the cloud. It is kept on this device.")
                                }
                        } else {
                            Log.w("TaskRepo", "update failed", e)
                        }
                    }
            } catch (e: Exception) {
                Log.w("TaskRepo", "write failed", e)
            }
        }
        return Result.success(Unit)
    }
}

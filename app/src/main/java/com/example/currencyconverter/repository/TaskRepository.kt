package com.example.currencyconverter.repository

import com.example.currencyconverter.model.TaskModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface TaskRepository {
    fun getTasks(userId: String): Flow<List<TaskModel>>
    suspend fun addTask(userId: String, task: TaskModel): Result<Unit>
    suspend fun updateTask(userId: String, task: TaskModel): Result<Unit>
    suspend fun deleteTask(userId: String, taskId: String): Result<Unit>
    suspend fun toggleTaskCompletion(userId: String, taskId: String, isCompleted: Boolean): Result<Unit>
}

class TaskRepositoryImpl : TaskRepository {

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val localTasksMap = ConcurrentHashMap<String, MutableStateFlow<List<TaskModel>>>()

    private fun getLocalTasksFlow(userId: String): MutableStateFlow<List<TaskModel>> {
        return localTasksMap.getOrPut(userId) { MutableStateFlow(emptyList()) }
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
                    .await()
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
                    .await()
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
                    .await()
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
                    .update("completed", isCompleted)
                    .await()
            } catch (e: Exception) {
                try {
                    val updatedTask = localFlow.value.find { it.id == taskId }
                    if (updatedTask != null) {
                        db.collection("users")
                            .document(userId)
                            .collection("tasks")
                            .document(taskId)
                            .set(updatedTask)
                            .await()
                    }
                } catch (ex: Exception) {
                    // Fallback to local
                }
            }
        }
        return Result.success(Unit)
    }
}

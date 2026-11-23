package com.example.currencyconverter.repository

import com.example.currencyconverter.model.ModuleModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface ModuleRepository {
    fun getModules(userId: String): Flow<List<ModuleModel>>
    suspend fun addModule(userId: String, module: ModuleModel): Result<Unit>
    suspend fun deleteModule(userId: String, moduleId: String): Result<Unit>
}

class ModuleRepositoryImpl : ModuleRepository {

    private val firestore: FirebaseFirestore?
        get() = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val localModulesMap = ConcurrentHashMap<String, MutableStateFlow<List<ModuleModel>>>()

    private fun getLocalModulesFlow(userId: String): MutableStateFlow<List<ModuleModel>> {
        return localModulesMap.getOrPut(userId) { MutableStateFlow(emptyList()) }
    }

    override fun getModules(userId: String): Flow<List<ModuleModel>> = callbackFlow {
        val localFlow = getLocalModulesFlow(userId)
        val db = firestore
        if (db == null || userId.isEmpty()) {
            val job = launch {
                localFlow.collect { trySend(it) }
            }
            awaitClose { job.cancel() }
            return@callbackFlow
        }

        val collectionRef = db.collection("users").document(userId).collection("modules")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(localFlow.value)
                return@addSnapshotListener
            }
            val modules = snapshot.toObjects(ModuleModel::class.java)
            localFlow.value = modules
            trySend(modules)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addModule(userId: String, module: ModuleModel): Result<Unit> {
        val finalModule = if (module.id.isEmpty()) {
            module.copy(id = UUID.randomUUID().toString())
        } else {
            module
        }

        val localFlow = getLocalModulesFlow(userId)
        localFlow.value = localFlow.value.filterNot { it.id == finalModule.id } + finalModule

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("modules")
                    .document(finalModule.id)
                    .set(finalModule)
                    .await()
            } catch (e: Exception) {
                // Firestore offline/error fallback
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteModule(userId: String, moduleId: String): Result<Unit> {
        val localFlow = getLocalModulesFlow(userId)
        localFlow.value = localFlow.value.filterNot { it.id == moduleId }

        val db = firestore
        if (db != null && userId.isNotEmpty()) {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("modules")
                    .document(moduleId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Firestore offline/error fallback
            }
        }
        return Result.success(Unit)
    }
}

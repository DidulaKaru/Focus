package com.focusflow.app.repository

import android.content.Context
import android.util.Log
import com.focusflow.app.model.ModuleModel
import com.focusflow.app.model.PomodoroSessionModel
import com.focusflow.app.model.Priority
import com.focusflow.app.model.SubtaskModel
import com.focusflow.app.model.TaskModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * On-device persistence for the offline/local fallback data (used when Firestore isn't available).
 * Each (kind, user) pair is stored as one JSON file in the app's private storage.
 */
object LocalStore {
    private const val TAG = "LocalStore"
    private var dir: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        dir = context.filesDir
    }

    private fun file(kind: String, userId: String): File? {
        val safeId = userId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        if (safeId.isEmpty()) return null
        return dir?.let { File(it, "local_${kind}_$safeId.json") }
    }

    /** Creates a flow pre-filled from disk that writes itself back to disk on every change. */
    fun <T> createFlow(
        kind: String,
        userId: String,
        toJson: (T) -> JSONObject,
        fromJson: (JSONObject) -> T
    ): MutableStateFlow<List<T>> {
        val f = file(kind, userId) ?: return MutableStateFlow(emptyList())
        val initial = runCatching {
            if (!f.exists()) emptyList()
            else {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }
        }.getOrElse {
            Log.w(TAG, "Could not read ${f.name}", it)
            emptyList()
        }
        val flow = MutableStateFlow(initial)
        scope.launch {
            flow.drop(1).collect { items ->
                runCatching {
                    val arr = JSONArray()
                    items.forEach { arr.put(toJson(it)) }
                    val tmp = File(f.parentFile, f.name + ".tmp")
                    tmp.writeText(arr.toString())
                    if (!tmp.renameTo(f)) {
                        f.writeText(arr.toString())
                        tmp.delete()
                    }
                }.onFailure { Log.w(TAG, "Could not write ${f.name}", it) }
            }
        }
        return flow
    }

    // ---- codecs ----

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    fun taskToJson(t: TaskModel) = JSONObject().apply {
        put("id", t.id); put("moduleId", t.moduleId); put("title", t.title)
        put("notes", t.notes ?: JSONObject.NULL)
        put("deadlineEpoch", t.deadlineEpoch ?: JSONObject.NULL)
        put("isCompleted", t.isCompleted); put("pomodoroMinutes", t.pomodoroMinutes)
        put("priority", t.priority.name); put("createdAt", t.createdAt)
        put("subtasks", JSONArray().apply {
            t.subtasks.forEach {
                put(JSONObject().apply { put("id", it.id); put("title", it.title); put("isCompleted", it.isCompleted) })
            }
        })
    }

    fun taskFromJson(o: JSONObject) = TaskModel(
        id = o.getString("id"),
        moduleId = o.optString("moduleId", ""),
        title = o.optString("title", ""),
        notes = o.optStringOrNull("notes"),
        deadlineEpoch = o.optLongOrNull("deadlineEpoch"),
        isCompleted = o.optBoolean("isCompleted", false),
        pomodoroMinutes = o.optInt("pomodoroMinutes", 25),
        priority = runCatching { Priority.valueOf(o.optString("priority")) }.getOrDefault(Priority.MEDIUM),
        subtasks = o.optJSONArray("subtasks")?.let { a ->
            (0 until a.length()).map {
                val s = a.getJSONObject(it)
                SubtaskModel(s.getString("id"), s.optString("title", ""), s.optBoolean("isCompleted", false))
            }
        } ?: emptyList(),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun moduleToJson(m: ModuleModel) = JSONObject().apply {
        put("id", m.id); put("code", m.code); put("name", m.name)
        put("colorHex", m.colorHex); put("createdAt", m.createdAt)
    }

    fun moduleFromJson(o: JSONObject) = ModuleModel(
        id = o.getString("id"), code = o.optString("code", ""), name = o.optString("name", ""),
        colorHex = o.optString("colorHex", "#3B82F6"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    fun sessionToJson(s: PomodoroSessionModel) = JSONObject().apply {
        put("id", s.id); put("taskId", s.taskId ?: JSONObject.NULL); put("moduleId", s.moduleId ?: JSONObject.NULL)
        put("durationMinutes", s.durationMinutes); put("completedAt", s.completedAt)
    }

    fun sessionFromJson(o: JSONObject) = PomodoroSessionModel(
        id = o.getString("id"), taskId = o.optStringOrNull("taskId"), moduleId = o.optStringOrNull("moduleId"),
        durationMinutes = o.optInt("durationMinutes", 25),
        completedAt = o.optLong("completedAt", System.currentTimeMillis())
    )
}

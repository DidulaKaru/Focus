package com.focusflow.app.model

data class PomodoroSessionModel(
    val id: String = "",
    val taskId: String? = null,
    val moduleId: String? = null,
    val durationMinutes: Int = 25,
    val completedAt: Long = System.currentTimeMillis()
)

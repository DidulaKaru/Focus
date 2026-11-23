package com.example.currencyconverter.model

import java.util.UUID

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

data class SubtaskModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val isCompleted: Boolean = false
)

data class TaskModel(
    val id: String = "",
    val moduleId: String = "",
    val title: String = "",
    val notes: String? = null,
    val deadlineEpoch: Long? = null,
    val isCompleted: Boolean = false,
    val pomodoroMinutes: Int = 25,
    val priority: Priority = Priority.MEDIUM,
    val subtasks: List<SubtaskModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

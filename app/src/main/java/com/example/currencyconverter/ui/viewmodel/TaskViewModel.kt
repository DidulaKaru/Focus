package com.example.currencyconverter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyconverter.model.ModuleModel
import com.example.currencyconverter.model.Priority
import com.example.currencyconverter.model.SubtaskModel
import com.example.currencyconverter.model.TaskModel
import com.example.currencyconverter.repository.AuthRepository
import com.example.currencyconverter.repository.AuthRepositoryImpl
import com.example.currencyconverter.repository.ModuleRepository
import com.example.currencyconverter.repository.ModuleRepositoryImpl
import com.example.currencyconverter.repository.TaskRepository
import com.example.currencyconverter.repository.TaskRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    DEADLINE,
    PRIORITY,
    CREATED
}

sealed interface TasksUiState {
    object Loading : TasksUiState
    data class Success(
        val tasks: List<TaskModel>,
        val modules: List<ModuleModel>,
        val selectedModuleId: String?,
        val searchQuery: String = "",
        val sortOption: SortOption = SortOption.CREATED
    ) : TasksUiState
    data class Error(val message: String) : TasksUiState
}

class TaskViewModel @JvmOverloads constructor(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val taskRepository: TaskRepository = TaskRepositoryImpl(),
    private val moduleRepository: ModuleRepository = ModuleRepositoryImpl()
) : ViewModel() {

    private val selectedModuleIdFlow = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow<String>("")
    private val sortOptionFlow = MutableStateFlow<SortOption>(SortOption.CREATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksState: StateFlow<TasksUiState> = authRepository.observeAuthState()
        .flatMapLatest { isLoggedIn ->
            val userId = authRepository.getCurrentUserId() ?: ""
            if (userId.isEmpty() || !isLoggedIn) {
                flowOf(TasksUiState.Success(emptyList(), emptyList(), null, "", SortOption.CREATED))
            } else {
                combine(
                    taskRepository.getTasks(userId),
                    moduleRepository.getModules(userId),
                    selectedModuleIdFlow,
                    searchQueryFlow,
                    sortOptionFlow
                ) { tasks, modules, selectedModuleId, searchQuery, sortOption ->
                    val filteredTasks = tasks
                        .filter { task ->
                            selectedModuleId.isNullOrEmpty() || task.moduleId == selectedModuleId
                        }
                        .filter { task ->
                            if (searchQuery.isBlank()) {
                                true
                            } else {
                                task.title.contains(searchQuery, ignoreCase = true) ||
                                        (task.notes?.contains(searchQuery, ignoreCase = true) == true)
                            }
                        }

                    val sortedTasks = when (sortOption) {
                        SortOption.DEADLINE -> filteredTasks.sortedWith(
                            compareBy<TaskModel> { it.deadlineEpoch == null }.thenBy { it.deadlineEpoch }
                        )
                        SortOption.PRIORITY -> filteredTasks.sortedBy { it.priority.ordinal }
                        SortOption.CREATED -> filteredTasks.sortedByDescending { it.createdAt }
                    }

                    TasksUiState.Success(
                        tasks = sortedTasks,
                        modules = modules,
                        selectedModuleId = selectedModuleId,
                        searchQuery = searchQuery,
                        sortOption = sortOption
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TasksUiState.Loading
        )

    fun selectModuleFilter(moduleId: String?) {
        selectedModuleIdFlow.value = moduleId
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setSortOption(option: SortOption) {
        sortOptionFlow.value = option
    }

    fun addTask(
        title: String,
        moduleId: String,
        notes: String? = null,
        deadlineEpoch: Long? = null,
        pomodoroMinutes: Int = 25,
        priority: Priority = Priority.MEDIUM,
        subtasks: List<SubtaskModel> = emptyList()
    ) {
        val userId = authRepository.getCurrentUserId() ?: return
        val task = TaskModel(
            moduleId = moduleId,
            title = title,
            notes = notes,
            deadlineEpoch = deadlineEpoch,
            pomodoroMinutes = pomodoroMinutes,
            priority = priority,
            subtasks = subtasks
        )
        viewModelScope.launch {
            taskRepository.addTask(userId, task)
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(userId, taskId, isCompleted)
        }
    }

    fun deleteTask(taskId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(userId, taskId)
        }
    }

    fun addModule(code: String, name: String, colorHex: String = "#3B82F6") {
        val userId = authRepository.getCurrentUserId() ?: return
        val module = ModuleModel(
            code = code,
            name = name,
            colorHex = colorHex
        )
        viewModelScope.launch {
            moduleRepository.addModule(userId, module)
        }
    }

    fun deleteModule(moduleId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            moduleRepository.deleteModule(userId, moduleId)
            if (selectedModuleIdFlow.value == moduleId) {
                selectedModuleIdFlow.value = null
            }
        }
    }

    fun toggleSubtaskCompletion(taskId: String, subtaskId: String, isCompleted: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val tasks = taskRepository.getTasks(userId).firstOrNull() ?: return@launch
            val task = tasks.find { it.id == taskId } ?: return@launch
            val updatedSubtasks = task.subtasks.map { subtask ->
                if (subtask.id == subtaskId) subtask.copy(isCompleted = isCompleted) else subtask
            }
            taskRepository.updateTask(userId, task.copy(subtasks = updatedSubtasks))
        }
    }

    fun addSubtask(taskId: String, subtaskTitle: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val tasks = taskRepository.getTasks(userId).firstOrNull() ?: return@launch
            val task = tasks.find { it.id == taskId } ?: return@launch
            val newSubtask = SubtaskModel(title = subtaskTitle)
            val updatedTask = task.copy(subtasks = task.subtasks + newSubtask)
            taskRepository.updateTask(userId, updatedTask)
        }
    }

    fun deleteSubtask(taskId: String, subtaskId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val tasks = taskRepository.getTasks(userId).firstOrNull() ?: return@launch
            val task = tasks.find { it.id == taskId } ?: return@launch
            val updatedSubtasks = task.subtasks.filterNot { it.id == subtaskId }
            taskRepository.updateTask(userId, task.copy(subtasks = updatedSubtasks))
        }
    }

    fun updateTaskPriority(taskId: String, priority: Priority) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val tasks = taskRepository.getTasks(userId).firstOrNull() ?: return@launch
            val task = tasks.find { it.id == taskId } ?: return@launch
            taskRepository.updateTask(userId, task.copy(priority = priority))
        }
    }
}

package com.focusflow.app.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.model.PomodoroSessionModel
import com.focusflow.app.model.TaskModel
import com.focusflow.app.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.focusflow.app.repository.AmbientSound
import com.focusflow.app.repository.AuthRepository
import com.focusflow.app.repository.PomodoroRepository
import com.focusflow.app.repository.ThemeMode
import com.focusflow.app.repository.UserPreferencesRepository
import com.focusflow.app.service.PomodoroService
import com.focusflow.app.service.TimerState
import com.focusflow.app.util.FocusSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val pomodoroRepository: PomodoroRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository
) : AndroidViewModel(application) {

    val timerState: StateFlow<TimerState> = PomodoroService.timerStateFlow

    val workDurationMinutes: StateFlow<Int> = userPreferencesRepository.workDurationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val breakDurationMinutes: StateFlow<Int> = userPreferencesRepository.breakDurationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    private val currentTaskId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTask: StateFlow<TaskModel?> = currentTaskId
        .flatMapLatest { id ->
            val userId = authRepository.getCurrentUserId()
            if (id.isNullOrEmpty() || userId.isNullOrEmpty()) {
                flowOf(null)
            } else {
                taskRepository.getTasks(userId).map { tasks -> tasks.firstOrNull { it.id == id } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setCurrentTask(taskId: String?) {
        currentTaskId.value = taskId
    }

    val themeModeStateFlow: StateFlow<ThemeMode> = userPreferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val ambientSoundStateFlow: StateFlow<AmbientSound> = userPreferencesRepository.ambientSoundFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AmbientSound.OFF)

    init {
        viewModelScope.launch {
            combine(timerState.map { it is TimerState.Running }.distinctUntilChanged(), ambientSoundStateFlow) { running, sound ->
                running to sound
            }.collect { (running, sound) ->
                if (running) {
                    FocusSoundManager.playSound(getApplication(), sound)
                } else {
                    FocusSoundManager.stopSound()
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setAmbientSound(sound: AmbientSound) {
        viewModelScope.launch {
            userPreferencesRepository.setAmbientSound(sound)
        }
    }

    fun startWorkTimer(durationMs: Long = (workDurationMinutes.value * 60 * 1000L)) {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
            putExtra(PomodoroService.EXTRA_DURATION_MS, durationMs)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun startBreakTimer(durationMs: Long = (breakDurationMinutes.value * 60 * 1000L)) {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_START
            putExtra(PomodoroService.EXTRA_DURATION_MS, durationMs)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun pauseTimer() {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), PomodoroService::class.java).apply {
            action = PomodoroService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun updatePreferences(workMins: Int, breakMins: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setWorkDuration(workMins)
            userPreferencesRepository.setBreakDuration(breakMins)
        }
    }

    fun recordSession(taskId: String?, moduleId: String?, durationMins: Int) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val session = PomodoroSessionModel(
                taskId = taskId,
                moduleId = moduleId,
                durationMinutes = durationMins,
                completedAt = System.currentTimeMillis()
            )
            pomodoroRepository.recordSession(userId, session)
        }
    }

    override fun onCleared() {
        super.onCleared()
        FocusSoundManager.stopSound()
    }
}

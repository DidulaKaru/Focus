package com.example.currencyconverter.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyconverter.model.PomodoroSessionModel
import com.example.currencyconverter.repository.AmbientSound
import com.example.currencyconverter.repository.AuthRepository
import com.example.currencyconverter.repository.AuthRepositoryImpl
import com.example.currencyconverter.repository.PomodoroRepository
import com.example.currencyconverter.repository.PomodoroRepositoryImpl
import com.example.currencyconverter.repository.ThemeMode
import com.example.currencyconverter.repository.UserPreferencesRepository
import com.example.currencyconverter.service.PomodoroService
import com.example.currencyconverter.service.TimerState
import com.example.currencyconverter.util.FocusSoundManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PomodoroViewModel @JvmOverloads constructor(
    application: Application,
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val pomodoroRepository: PomodoroRepository = PomodoroRepositoryImpl(),
    private val userPreferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application)
) : AndroidViewModel(application) {

    val timerState: StateFlow<TimerState> = PomodoroService.timerStateFlow

    val workDurationMinutes: StateFlow<Int> = userPreferencesRepository.workDurationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val breakDurationMinutes: StateFlow<Int> = userPreferencesRepository.breakDurationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val themeModeStateFlow: StateFlow<ThemeMode> = userPreferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val ambientSoundStateFlow: StateFlow<AmbientSound> = userPreferencesRepository.ambientSoundFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AmbientSound.OFF)

    init {
        viewModelScope.launch {
            combine(timerState, ambientSoundStateFlow) { state, sound ->
                state to sound
            }.collect { (state, sound) ->
                if (state is TimerState.Running) {
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

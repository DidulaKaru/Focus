package com.example.currencyconverter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyconverter.model.ModuleModel
import com.example.currencyconverter.model.PomodoroSessionModel
import com.example.currencyconverter.repository.AuthRepository
import com.example.currencyconverter.repository.AuthRepositoryImpl
import com.example.currencyconverter.repository.ModuleRepository
import com.example.currencyconverter.repository.ModuleRepositoryImpl
import com.example.currencyconverter.repository.PomodoroRepository
import com.example.currencyconverter.repository.PomodoroRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

sealed interface AnalyticsUiState {
    object Loading : AnalyticsUiState
    data class Success(
        val totalMinutesToday: Int,
        val totalMinutesThisWeek: Int,
        val streakDays: Int,
        val sessions: List<PomodoroSessionModel>,
        val modulesMap: Map<String, ModuleModel>
    ) : AnalyticsUiState
}

class AnalyticsViewModel @JvmOverloads constructor(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val pomodoroRepository: PomodoroRepository = PomodoroRepositoryImpl(),
    private val moduleRepository: ModuleRepository = ModuleRepositoryImpl()
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val analyticsUiState: StateFlow<AnalyticsUiState> = authRepository.observeAuthState()
        .flatMapLatest { isLoggedIn ->
            val userId = authRepository.getCurrentUserId() ?: ""
            if (userId.isEmpty() || !isLoggedIn) {
                flowOf(AnalyticsUiState.Success(0, 0, 0, emptyList(), emptyMap()))
            } else {
                combine(
                    pomodoroRepository.getSessions(userId),
                    moduleRepository.getModules(userId)
                ) { sessions, modules ->
                    val modulesMap = modules.associateBy { it.id }
                    val sortedSessions = sessions.sortedByDescending { it.completedAt }
                    val totalMinutesToday = calculateTotalMinutesToday(sessions)
                    val totalMinutesThisWeek = calculateTotalMinutesThisWeek(sessions)
                    val streakDays = calculateStreakDays(sessions)

                    AnalyticsUiState.Success(
                        totalMinutesToday = totalMinutesToday,
                        totalMinutesThisWeek = totalMinutesThisWeek,
                        streakDays = streakDays,
                        sessions = sortedSessions,
                        modulesMap = modulesMap
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsUiState.Loading
        )

    val uiState: StateFlow<AnalyticsUiState> = analyticsUiState

    private fun calculateTotalMinutesToday(sessions: List<PomodoroSessionModel>): Int {
        val startOfToday = getStartOfDay(System.currentTimeMillis())
        return sessions
            .filter { it.completedAt >= startOfToday }
            .sumOf { it.durationMinutes }
    }

    private fun calculateTotalMinutesThisWeek(sessions: List<PomodoroSessionModel>): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        if (cal.timeInMillis > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        val startOfWeek = cal.timeInMillis
        return sessions
            .filter { it.completedAt >= startOfWeek }
            .sumOf { it.durationMinutes }
    }

    private fun calculateStreakDays(sessions: List<PomodoroSessionModel>): Int {
        if (sessions.isEmpty()) return 0

        val sessionDays = sessions.map { getStartOfDay(it.completedAt) }.toSet()
        val todayStart = getStartOfDay(System.currentTimeMillis())

        val cal = Calendar.getInstance().apply {
            timeInMillis = todayStart
        }

        var streak = 0
        if (!sessionDays.contains(cal.timeInMillis)) {
            // Check if yesterday has a session
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (sessionDays.contains(cal.timeInMillis)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

package com.focusflow.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.focusflow.app.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FocusTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val timerState = PomodoroService.timerStateFlow.value
        if (timerState is TimerState.Running) {
            val intent = Intent(this, PomodoroService::class.java).apply {
                action = PomodoroService.ACTION_STOP
            }
            startService(intent)
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Start Focus"
        } else {
            val intent = Intent(this, PomodoroService::class.java).apply {
                action = PomodoroService.ACTION_START
                if (timerState !is TimerState.Paused) {
                    putExtra(PomodoroService.EXTRA_DURATION_MS, getWorkDurationMinutes() * 60 * 1000L)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Focusing..."
        }
        tile.updateTile()
    }

    private fun getWorkDurationMinutes(): Long {
        // DataStore is a process-wide singleton per file, so a fresh repository instance reads the same data.
        val minutes = runCatching {
            runBlocking { UserPreferencesRepository(applicationContext).workDurationFlow.first() }
        }.getOrDefault(25)
        return (if (minutes > 0) minutes else 25).toLong()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = PomodoroService.timerStateFlow.value is TimerState.Running
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Focusing..." else "Start Focus"
        tile.updateTile()
    }
}

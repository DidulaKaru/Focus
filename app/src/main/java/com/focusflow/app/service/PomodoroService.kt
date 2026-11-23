package com.focusflow.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.focusflow.app.MainActivity
import com.focusflow.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

sealed class TimerState {
    object Idle : TimerState()
    data class Running(val remainingMs: Long, val totalMs: Long) : TimerState()
    data class Paused(val remainingMs: Long, val totalMs: Long) : TimerState()
    object Completed : TimerState()
}

class PomodoroService : Service() {

    companion object {
        const val ACTION_START = "com.focusflow.app.ACTION_START"
        const val ACTION_PAUSE = "com.focusflow.app.ACTION_PAUSE"
        const val ACTION_STOP = "com.focusflow.app.ACTION_STOP"
        const val ACTION_RESET = "com.focusflow.app.ACTION_RESET"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "focusflow_pomodoro_channel"
        val timerStateFlow = MutableStateFlow<TimerState>(TimerState.Idle)

        /** Resets Completed back to Idle (no-op in any other state). Safe to call from the UI without starting the service. */
        fun resetIfCompleted() {
            timerStateFlow.compareAndSet(TimerState.Completed, TimerState.Idle)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val intentDuration = intent.getLongExtra(EXTRA_DURATION_MS, -1L)
                val currentState = timerStateFlow.value

                val (remainingMs, totalMs) = when {
                    intentDuration > 0L -> intentDuration to intentDuration
                    currentState is TimerState.Paused -> currentState.remainingMs to currentState.totalMs
                    else -> (25 * 60 * 1000L) to (25 * 60 * 1000L)
                }

                startTimer(remainingMs, totalMs)
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_STOP -> {
                stopTimer()
            }
            ACTION_RESET -> {
                resetIfCompleted()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        if (timerStateFlow.value is TimerState.Running) {
            timerStateFlow.value = TimerState.Idle
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FocusFlow Pomodoro Channel",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Channel for FocusFlow Pomodoro timer notifications"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startTimer(initialRemainingMs: Long, totalMs: Long) {
        timerJob?.cancel()

        val initialNotification = buildNotification(TimerState.Running(initialRemainingMs, totalMs))
        startForegroundInternal(initialNotification)

        timerJob = serviceScope.launch {
            var currentRemaining = initialRemainingMs
            while (currentRemaining > 0) {
                ensureActive()
                val state = TimerState.Running(currentRemaining, totalMs)
                timerStateFlow.value = state
                updateNotification(state)
                delay(1000L)
                currentRemaining -= 1000L
            }

            ensureActive()
            timerStateFlow.value = TimerState.Completed
            triggerCompletionFeedback()
            stopForegroundService()
        }
    }

    private fun pauseTimer() {
        val currentState = timerStateFlow.value
        if (currentState is TimerState.Running) {
            timerJob?.cancel()
            val pausedState = TimerState.Paused(currentState.remainingMs, currentState.totalMs)
            timerStateFlow.value = pausedState
            updateNotification(pausedState)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerStateFlow.value = TimerState.Idle
        stopForegroundService()
    }

    private fun startForegroundInternal(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification, 0)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateNotification(state: TimerState) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: TimerState): Notification {
        val remainingMs = when (state) {
            is TimerState.Running -> state.remainingMs
            is TimerState.Paused -> state.remainingMs
            else -> 0L
        }

        val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val mainIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusFlow Pomodoro Engine")
            .setContentText(timeFormatted)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .setOngoing(state is TimerState.Running || state is TimerState.Paused)
            .setOnlyAlertOnce(true)

        if (state is TimerState.Running) {
            val pauseIntent = Intent(this, PomodoroService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            val playIntent = Intent(this, PomodoroService::class.java).apply {
                action = ACTION_START
            }
            val playPendingIntent = PendingIntent.getService(
                this,
                2,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
        }

        val stopIntent = Intent(this, PomodoroService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)

        return builder.build()
    }

    private fun triggerCompletionFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

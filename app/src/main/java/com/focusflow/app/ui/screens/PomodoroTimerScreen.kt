package com.focusflow.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.focusflow.app.repository.AmbientSound
import com.focusflow.app.service.PomodoroService
import com.focusflow.app.service.TimerState
import com.focusflow.app.ui.viewmodel.PomodoroViewModel
import java.util.Locale

@Composable
fun PauseIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onPrimary) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width * 0.22f
            val barHeight = size.height * 0.65f
            val top = (size.height - barHeight) / 2
            drawRect(
                color = tint,
                topLeft = Offset(size.width * 0.2f, top),
                size = Size(barWidth, barHeight)
            )
            drawRect(
                color = tint,
                topLeft = Offset(size.width * 0.58f, top),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun StopIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension * 0.55f
            val offset = (size.minDimension - side) / 2
            drawRect(
                color = tint,
                topLeft = Offset(offset, offset),
                size = Size(side, side)
            )
        }
    }
}

@Composable
fun EqualizerPulseIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val heightScale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val heightScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val heightScale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier
            .height(18.dp)
            .width(20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale1)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale2)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(heightScale3)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimerScreen(
    pomodoroViewModel: PomodoroViewModel,
    taskId: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val timerState by pomodoroViewModel.timerState.collectAsState()
    val globalWorkMins by pomodoroViewModel.workDurationMinutes.collectAsState()
    LaunchedEffect(taskId) { pomodoroViewModel.setCurrentTask(taskId) }
    val currentTask by pomodoroViewModel.currentTask.collectAsState()
    val workMins = currentTask?.pomodoroMinutes?.takeIf { it > 0 } ?: globalWorkMins
    val breakMins by pomodoroViewModel.breakDurationMinutes.collectAsState()
    val currentAmbientSound by pomodoroViewModel.ambientSoundStateFlow.collectAsState()

    var zenModeEnabled by remember { mutableStateOf(false) }
    var lastTotalMs by remember { mutableLongStateOf(workMins * 60 * 1000L) }

    val remainingMs = when (val state = timerState) {
        is TimerState.Running -> {
            lastTotalMs = state.totalMs
            state.remainingMs
        }
        is TimerState.Paused -> {
            lastTotalMs = state.totalMs
            state.remainingMs
        }
        TimerState.Idle -> {
            lastTotalMs = workMins * 60 * 1000L
            lastTotalMs
        }
        else -> lastTotalMs
    }

    val isRunning = timerState is TimerState.Running

    // Breathing pulse scale animation when timer is running
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val totalMs = lastTotalMs.coerceAtLeast(1L)
    val rawProgress = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 300),
        label = "RadialProgress"
    )

    var sawRunning by remember { mutableStateOf(false) }
    LaunchedEffect(timerState) {
        if (timerState is TimerState.Running) sawRunning = true
        if (timerState is TimerState.Completed && sawRunning) {
            sawRunning = false
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val sessionMins = (lastTotalMs / (60 * 1000L)).toInt().coerceAtLeast(1)
            pomodoroViewModel.recordSession(taskId = taskId, moduleId = currentTask?.moduleId?.takeIf { it.isNotEmpty() }, durationMins = sessionMins)
        }
    }

    // Return the service to Idle once the user leaves the screen after a finished session
    DisposableEffect(Unit) {
        onDispose { PomodoroService.resetIfCompleted() }
    }

    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    val sessionStatusText = when (timerState) {
        is TimerState.Running -> "Focusing..."
        is TimerState.Paused -> "Session Paused"
        is TimerState.Completed -> "Completed!"
        TimerState.Idle -> "Ready to Focus"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        topBar = {
            if (!zenModeEnabled) {
                TopAppBar(
                    title = { Text("Pomodoro Timer") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sessionStatusText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FilterChip(
                    selected = zenModeEnabled,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        zenModeEnabled = !zenModeEnabled
                    },
                    label = { Text(if (zenModeEnabled) "Exit Zen" else "Zen Mode") }
                )
            }

            // Canvas Radial Progress Ring with Breathing Scale Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(270.dp)
                    .scale(breathingScale)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRunning) pomodoroViewModel.pauseTimer() else pomodoroViewModel.startWorkTimer(remainingMs)
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )

                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) "Tap to pause" else "Tap to start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!zenModeEnabled) {
                // Atmospheric Focus Chips
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Atmospheric Focus Sound",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currentAmbientSound != AmbientSound.OFF) {
                                EqualizerPulseIndicator()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AmbientSound.entries.forEach { sound ->
                                val isSelected = currentAmbientSound == sound
                                val label = when (sound) {
                                    AmbientSound.OFF -> "Off"
                                    AmbientSound.RAIN -> "Rain"
                                    AmbientSound.WHITE_NOISE -> "Noise"
                                    AmbientSound.FOREST -> "Forest"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pomodoroViewModel.setAmbientSound(sound)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isRunning) {
                                pomodoroViewModel.pauseTimer()
                            } else {
                                pomodoroViewModel.startWorkTimer(remainingMs)
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                    ) {
                        if (isRunning) {
                            PauseIcon(tint = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRunning) "Pause" else "Start")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            pomodoroViewModel.stopTimer()
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f)
                    ) {
                        StopIcon(tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

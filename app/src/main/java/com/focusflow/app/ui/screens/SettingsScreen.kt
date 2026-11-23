package com.focusflow.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focusflow.app.repository.AmbientSound
import com.focusflow.app.repository.ThemeMode
import com.focusflow.app.ui.viewmodel.PomodoroViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pomodoroViewModel: PomodoroViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val currentWorkMins by pomodoroViewModel.workDurationMinutes.collectAsState()
    val currentBreakMins by pomodoroViewModel.breakDurationMinutes.collectAsState()
    val themeMode by pomodoroViewModel.themeModeStateFlow.collectAsState()
    val ambientSound by pomodoroViewModel.ambientSoundStateFlow.collectAsState()

    var workMinsState by remember(currentWorkMins) { mutableFloatStateOf(currentWorkMins.toFloat()) }
    var breakMinsState by remember(currentBreakMins) { mutableFloatStateOf(currentBreakMins.toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { pomodoroViewModel.setThemeMode(mode) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Timer Durations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Work Duration: ${workMinsState.roundToInt()} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = workMinsState,
                        onValueChange = { workMinsState = it },
                        valueRange = 15f..60f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Break Duration: ${breakMinsState.roundToInt()} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = breakMinsState,
                        onValueChange = { breakMinsState = it },
                        valueRange = 5f..30f,
                        steps = 4
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            pomodoroViewModel.updatePreferences(
                                workMins = workMinsState.roundToInt(),
                                breakMins = breakMinsState.roundToInt()
                            )
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Preferences")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ambient Sound Default",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AmbientSound.entries.forEach { sound ->
                            val isSelected = ambientSound == sound
                            val label = when (sound) {
                                AmbientSound.OFF -> "Off"
                                AmbientSound.RAIN -> "Rain"
                                AmbientSound.WHITE_NOISE -> "Noise"
                                AmbientSound.FOREST -> "Forest"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { pomodoroViewModel.setAmbientSound(sound) },
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
        }
    }
}

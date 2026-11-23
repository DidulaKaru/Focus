package com.focusflow.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusflow.app.repository.ThemeMode
import com.focusflow.app.ui.components.AddModuleDialog
import com.focusflow.app.ui.components.AddTaskDialog
import com.focusflow.app.ui.components.ModuleChipRow
import com.focusflow.app.ui.components.TaskItem
import com.focusflow.app.ui.viewmodel.AnalyticsUiState
import com.focusflow.app.ui.viewmodel.AnalyticsViewModel
import com.focusflow.app.ui.viewmodel.AuthViewModel
import com.focusflow.app.ui.viewmodel.DAILY_FOCUS_TARGET_MINUTES
import com.focusflow.app.ui.viewmodel.PomodoroViewModel
import com.focusflow.app.ui.viewmodel.SortOption
import com.focusflow.app.ui.viewmodel.TaskViewModel
import com.focusflow.app.ui.viewmodel.TasksUiState

@Composable
fun SunIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(modifier = modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.25f
            drawCircle(color = tint, radius = radius, center = center)
            val rayLength = size.minDimension * 0.15f
            val strokeWidth = 2.dp.toPx()
            for (i in 0 until 8) {
                val angle = i * (Math.PI / 4)
                val start = Offset(
                    (center.x + (radius + 2.dp.toPx()) * Math.cos(angle)).toFloat(),
                    (center.y + (radius + 2.dp.toPx()) * Math.sin(angle)).toFloat()
                )
                val end = Offset(
                    (center.x + (radius + 2.dp.toPx() + rayLength) * Math.cos(angle)).toFloat(),
                    (center.y + (radius + 2.dp.toPx() + rayLength) * Math.sin(angle)).toFloat()
                )
                drawLine(color = tint, start = start, end = end, strokeWidth = strokeWidth)
            }
        }
    }
}

@Composable
fun MoonIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(modifier = modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.45f, size.height * 0.5f)
            val radius = size.minDimension * 0.38f
            val path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }
            val cutCenter = Offset(size.width * 0.65f, size.height * 0.4f)
            val cutRadius = size.minDimension * 0.35f
            val cutPath = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(cutCenter.x - cutRadius, cutCenter.y - cutRadius, cutCenter.x + cutRadius, cutCenter.y + cutRadius))
            }
            val resultPath = Path.combine(
                PathOperation.Difference,
                path,
                cutPath
            )
            drawPath(path = resultPath, color = tint)
        }
    }
}

private fun formatFocusMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    authViewModel: AuthViewModel,
    pomodoroViewModel: PomodoroViewModel,
    onNavigateToPomodoro: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    val tasksUiState by taskViewModel.tasksState.collectAsState()
    val analyticsState by analyticsViewModel.analyticsUiState.collectAsState()
    val minutesToday = (analyticsState as? AnalyticsUiState.Success)?.totalMinutesToday ?: 0
    val streakDays = (analyticsState as? AnalyticsUiState.Success)?.streakDays ?: 0
    val themeMode by pomodoroViewModel.themeModeStateFlow.collectAsState()
    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddModuleDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var quickTaskTitle by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FocusFlow") },
                actions = {
                    IconButton(onClick = {
                        if (isDark) {
                            pomodoroViewModel.setThemeMode(ThemeMode.LIGHT)
                        } else {
                            pomodoroViewModel.setThemeMode(ThemeMode.DARK)
                        }
                    }) {
                        if (isDark) {
                            SunIcon(tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            MoonIcon(tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = tasksUiState) {
                is TasksUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TasksUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is TasksUiState.Success -> {
                    // Daily Focus Ring & Streak Bar Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    val progress = (minutesToday.toFloat() / DAILY_FOCUS_TARGET_MINUTES).coerceIn(0f, 1f)
                                    val animProgress by animateFloatAsState(
                                        targetValue = progress,
                                        animationSpec = tween(600),
                                        label = "dailyGoal"
                                    )
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = trackColor,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                        drawArc(
                                            color = primaryColor,
                                            startAngle = -90f,
                                            sweepAngle = animProgress * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = "🎯",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Today: ${formatFocusMinutes(minutesToday)} / ${formatFocusMinutes(DAILY_FOCUS_TARGET_MINUTES)} Focus",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "🔥 $streakDays Day Streak",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val activeCount = state.activeTaskCount
                            Text(
                                text = "$activeCount Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Search bar
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { taskViewModel.setSearchQuery(it) },
                        placeholder = { Text("Search tasks...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { taskViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = textFieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Workspace Tasks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sort dropdown
                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sort: ${state.sortOption.name}")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.name) },
                                        onClick = {
                                            taskViewModel.setSortOption(option)
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    ModuleChipRow(
                        modules = state.modules,
                        selectedModuleId = state.selectedModuleId,
                        onSelectModule = { moduleId -> taskViewModel.selectModuleFilter(moduleId) },
                        onAddModuleClick = { showAddModuleDialog = true }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (state.tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks found. Tap + to create one!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            items(state.tasks, key = { it.id }) { task ->
                                val module = state.modules.find { it.id == task.moduleId }
                                TaskItem(
                                    task = task,
                                    module = module,
                                    onToggleCompletion = { isChecked ->
                                        taskViewModel.toggleTaskCompletion(task.id, isChecked)
                                    },
                                    onStartFocus = { taskId ->
                                        onNavigateToPomodoro(taskId)
                                    },
                                    onDeleteTask = {
                                        taskViewModel.deleteTask(task.id)
                                    },
                                    onToggleSubtask = { subtaskId, isCompleted ->
                                        taskViewModel.toggleSubtaskCompletion(task.id, subtaskId, isCompleted)
                                    },
                                    onAddSubtask = { subtaskTitle ->
                                        taskViewModel.addSubtask(task.id, subtaskTitle)
                                    },
                                    onDeleteSubtask = { subtaskId ->
                                        taskViewModel.deleteSubtask(task.id, subtaskId)
                                    },
                                    onPriorityChange = { priority ->
                                        taskViewModel.updateTaskPriority(task.id, priority)
                                    }
                                )
                            }
                        }
                    }

                    // Sleek Quick-Add Input Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(end = 80.dp) // keep clear of the FAB
                        ) {
                            OutlinedTextField(
                                value = quickTaskTitle,
                                onValueChange = { quickTaskTitle = it },
                                placeholder = { Text("Quick add task...") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                                    if (quickTaskTitle.isNotBlank()) {
                                        taskViewModel.addTask(quickTaskTitle.trim(), state.selectedModuleId ?: state.modules.firstOrNull()?.id ?: "")
                                        quickTaskTitle = ""
                                    }
                                }),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val defaultMod = state.selectedModuleId ?: state.modules.firstOrNull()?.id ?: ""
                                    if (quickTaskTitle.isNotBlank()) {
                                        taskViewModel.addTask(quickTaskTitle.trim(), defaultMod)
                                        quickTaskTitle = ""
                                    }
                                },
                                enabled = quickTaskTitle.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add quick task", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (showAddTaskDialog) {
                        AddTaskDialog(
                            modules = state.modules,
                            initialModuleId = state.selectedModuleId,
                            onDismiss = { showAddTaskDialog = false },
                            onConfirm = { title, moduleId, notes, deadlineEpoch, pomodoroMinutes, priority, subtasks ->
                                taskViewModel.addTask(title, moduleId, notes, deadlineEpoch, pomodoroMinutes, priority, subtasks)
                                showAddTaskDialog = false
                            }
                        )
                    }

                    if (showAddModuleDialog) {
                        AddModuleDialog(
                            onDismiss = { showAddModuleDialog = false },
                            onConfirm = { code, name, colorHex ->
                                taskViewModel.addModule(code, name, colorHex)
                                showAddModuleDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

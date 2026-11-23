package com.example.currencyconverter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import com.example.currencyconverter.repository.ThemeMode
import com.example.currencyconverter.ui.components.AddModuleDialog
import com.example.currencyconverter.ui.components.AddTaskDialog
import com.example.currencyconverter.ui.components.ModuleChipRow
import com.example.currencyconverter.ui.components.TaskItem
import com.example.currencyconverter.ui.viewmodel.AuthViewModel
import com.example.currencyconverter.ui.viewmodel.PomodoroViewModel
import com.example.currencyconverter.ui.viewmodel.SortOption
import com.example.currencyconverter.ui.viewmodel.TaskViewModel
import com.example.currencyconverter.ui.viewmodel.TasksUiState

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    authViewModel: AuthViewModel,
    pomodoroViewModel: PomodoroViewModel,
    onNavigateToPomodoro: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tasksUiState by taskViewModel.tasksState.collectAsState()
    val themeMode by pomodoroViewModel.themeModeStateFlow.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddModuleDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

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
                    // Theme Toggle Button (toggles LIGHT <-> DARK)
                    IconButton(onClick = {
                        if (themeMode == ThemeMode.DARK) {
                            pomodoroViewModel.setThemeMode(ThemeMode.LIGHT)
                        } else {
                            pomodoroViewModel.setThemeMode(ThemeMode.DARK)
                        }
                    }) {
                        if (themeMode == ThemeMode.DARK) {
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
                    // Search bar with clean outlined text field styling
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
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activeCount = state.tasks.count { !it.isCompleted }
                        val completedCount = state.tasks.count { it.isCompleted }

                        Text(
                            text = "Tasks ($activeCount active, $completedCount done)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sort dropdown menu
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

                    // Statistics card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeCount = state.tasks.count { !it.isCompleted }
                            val completedCount = state.tasks.count { it.isCompleted }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$activeCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Active Tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$completedCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ModuleChipRow(
                        modules = state.modules,
                        selectedModuleId = state.selectedModuleId,
                        onSelectModule = { moduleId -> taskViewModel.selectModuleFilter(moduleId) },
                        onAddModuleClick = { showAddModuleDialog = true }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks found. Tap + to create a task!",
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

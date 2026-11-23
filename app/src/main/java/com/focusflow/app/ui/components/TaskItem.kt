package com.focusflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.focusflow.app.model.ModuleModel
import com.focusflow.app.model.Priority
import com.focusflow.app.model.TaskModel
import com.focusflow.app.util.CalendarExportUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getPriorityColor(priority: Priority): Color {
    return when (priority) {
        Priority.HIGH -> Color(0xFFC84B31)
        Priority.MEDIUM -> Color(0xFFD97706)
        Priority.LOW -> Color(0xFF4A7C59)
    }
}

fun isTaskOverdue(task: TaskModel): Boolean {
    val deadline = task.deadlineEpoch ?: return false
    if (task.isCompleted) return false
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return deadline < startOfToday
}

fun formatDeadline(epoch: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epoch))

fun getPriorityText(priority: Priority): String {
    return when (priority) {
        Priority.HIGH -> "High"
        Priority.MEDIUM -> "Medium"
        Priority.LOW -> "Low"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskModel,
    module: ModuleModel?,
    onToggleCompletion: (Boolean) -> Unit,
    onStartFocus: (String) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSubtask: (subtaskId: String, isCompleted: Boolean) -> Unit = { _, _ -> },
    onAddSubtask: (subtaskTitle: String) -> Unit = {},
    onDeleteSubtask: (subtaskId: String) -> Unit = {},
    onPriorityChange: (Priority) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val moduleColor = module?.colorHex?.let { parseColorHex(it) } ?: MaterialTheme.colorScheme.secondary

    var showBottomSheet by remember { mutableStateOf(false) }

    val completedSubtasks = task.subtasks.count { it.isCompleted }
    val totalSubtasks = task.subtasks.size
    val subtaskProgress = if (totalSubtasks > 0) completedSubtasks.toFloat() / totalSubtasks else 0f

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleCompletion(!task.isCompleted)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteTask()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = alignment
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Check, contentDescription = "Complete", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    ) {
        // Minimalist Frameless Task Row
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { showBottomSheet = true }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { isChecked ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleCompletion(isChecked)
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Priority Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(getPriorityColor(task.priority))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!task.notes.isNullOrBlank()) {
                            Text(
                                text = task.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (task.deadlineEpoch != null) {
                            val overdue = isTaskOverdue(task)
                            Text(
                                text = (if (overdue) "Overdue: " else "Due: ") + formatDeadline(task.deadlineEpoch),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (module != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = moduleColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = module.code.ifBlank { module.name },
                                style = MaterialTheme.typography.labelSmall,
                                color = moduleColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = { onStartFocus(task.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Focus",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (totalSubtasks > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { subtaskProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    if (showBottomSheet) {
        TaskDetailBottomSheet(
            task = task,
            module = module,
            onDismiss = { showBottomSheet = false },
            onStartFocus = {
                showBottomSheet = false
                onStartFocus(task.id)
            },
            onDeleteTask = {
                showBottomSheet = false
                onDeleteTask()
            },
            onToggleSubtask = onToggleSubtask,
            onAddSubtask = onAddSubtask,
            onDeleteSubtask = onDeleteSubtask,
            onPriorityChange = onPriorityChange,
            onExportCalendar = { CalendarExportUtil.exportTaskToSystemCalendar(context, task) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    task: TaskModel,
    module: ModuleModel?,
    onDismiss: () -> Unit,
    onStartFocus: () -> Unit,
    onDeleteTask: () -> Unit,
    onToggleSubtask: (subtaskId: String, isCompleted: Boolean) -> Unit,
    onAddSubtask: (subtaskTitle: String) -> Unit,
    onDeleteSubtask: (subtaskId: String) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onExportCalendar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!task.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.deadlineEpoch != null) {
                val overdue = isTaskOverdue(task)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (if (overdue) "Overdue: " else "Due: ") + formatDeadline(task.deadlineEpoch),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Selector
                Box {
                    val pColor = getPriorityColor(task.priority)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = pColor.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { priorityMenuExpanded = true }
                    ) {
                        Text(
                            text = "Priority: ${getPriorityText(task.priority)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = pColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false }
                    ) {
                        Priority.entries.forEach { priorityOption ->
                            DropdownMenuItem(
                                text = { Text(getPriorityText(priorityOption), color = getPriorityColor(priorityOption)) },
                                onClick = {
                                    onPriorityChange(priorityOption)
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "${task.pomodoroMinutes} min focus session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Subtasks Checklist", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            task.subtasks.forEach { subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = subtask.isCompleted,
                        onCheckedChange = { isChecked -> onToggleSubtask(subtask.id, isChecked) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtask.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Delete subtask", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newSubtaskTitle,
                    onValueChange = { newSubtaskTitle = it },
                    placeholder = { Text("Add subtask item...") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            onAddSubtask(newSubtaskTitle.trim())
                            newSubtaskTitle = ""
                        }
                    },
                    enabled = newSubtaskTitle.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCalendar,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calendar")
                }

                OutlinedButton(
                    onClick = onStartFocus,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Focus Now")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDeleteTask,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Task")
            }
        }
    }
}

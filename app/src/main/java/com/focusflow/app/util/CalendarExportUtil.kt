package com.focusflow.app.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.focusflow.app.model.TaskModel

object CalendarExportUtil {

    fun exportTaskToSystemCalendar(context: Context, task: TaskModel) {
        val startTime = task.deadlineEpoch ?: System.currentTimeMillis()
        val endTime = startTime + (task.pomodoroMinutes * 60 * 1000L)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, task.title)
            task.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    putExtra(CalendarContract.Events.DESCRIPTION, notes)
                }
            }
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

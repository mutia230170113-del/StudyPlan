package com.example.studyplan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationHelper {

    fun scheduleReminder(context: Context, task: Task, reminderOption: String) {
        if (reminderOption == "Tanpa Pengingat" || reminderOption == "Atur Waktu Sendiri...") return

        val triggerTimeMillis = calculateTriggerTime(task.deadline, reminderOption) ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_USER_ID", task.userId)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_COURSE", task.course)
            putExtra("TASK_DESC", task.description)
            putExtra("TASK_DEADLINE", task.deadline)
            putExtra("TASK_PRIORITY", task.priority)
            putExtra("TASK_STATUS", task.status)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimeMillis,
            pendingIntent
        )
    }

    private fun calculateTriggerTime(deadlineStr: String, reminderOption: String): Long? {
        if (reminderOption == "Tes 10 Detik (Demo)") {
            return System.currentTimeMillis() + 10_000L
        }

        // Pengingat Custom (pilih tanggal & jam sendiri)
        if (reminderOption.startsWith("Custom: ")) {
            return try {
                val customDateTime = reminderOption.removePrefix("Custom: ").trim()
                val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                val date = sdf.parse(customDateTime) ?: return null
                if (date.time <= System.currentTimeMillis()) {
                    System.currentTimeMillis() + 10_000L
                } else {
                    date.time
                }
            } catch (e: Exception) {
                null
            }
        }

        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = sdf.parse(deadlineStr) ?: return null
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (reminderOption == "1 Hari Sebelum (08:00)") {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                System.currentTimeMillis() + 10_000L
            } else {
                calendar.timeInMillis
            }
        } catch (e: Exception) {
            null
        }
    }
}
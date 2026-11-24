package com.example.studyplan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: ""
        val userId = intent.getStringExtra("TASK_USER_ID") ?: ""
        val title = intent.getStringExtra("TASK_TITLE") ?: "Pengingat Tugas"
        val course = intent.getStringExtra("TASK_COURSE") ?: ""
        val taskDescription = intent.getStringExtra("TASK_DESC") ?: ""
        val deadline = intent.getStringExtra("TASK_DEADLINE") ?: ""
        val priority = intent.getStringExtra("TASK_PRIORITY") ?: "Sedang"
        val status = intent.getStringExtra("TASK_STATUS") ?: "Belum Dikerjakan"

        val task = Task(taskId, userId, title, course, taskDescription, deadline, priority, status)

        // Saat notifikasi diketuk, buka halaman Detail Tugas
        val detailIntent = Intent(context, TaskDetailActivity::class.java).apply {
            putExtra("EXTRA_TASK", task)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "studyplan_deadline_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Deadline StudyPlan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Notifikasi pengingat deadline tugas kuliah"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pengingat Deadline: $title")
            .setContentText("$course — Deadline: $deadline")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
package com.matfess.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.matfess.app.R
import com.matfess.app.data.Course
import com.matfess.app.data.Task
import com.matfess.app.ui.main.MainActivity
import com.matfess.app.util.DateUtils

object NotificationHelper {

    const val CHANNEL_TASKS = "matfess_tasks"
    const val CHANNEL_CLASS = "matfess_class"
    const val CHANNEL_FOCUS = "matfess_focus"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASKS,
                "Deadline tugas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat sebelum deadline tugas kuliah"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CLASS,
                "Jadwal kuliah",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat 10 menit sebelum jam kuliah berakhir"
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOCUS,
                "Sesi fokus",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status timer fokus Matfess"
            }
        )
    }

    private fun contentIntent(context: Context, destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", destination)
        }
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyTask(context: Context, task: Task) {
        val body = buildString {
            if (task.course.isNotBlank()) append("${task.course} · ")
            append(DateUtils.relativeDeadline(task.deadline))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Deadline: ${task.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (task.description.isBlank()) body else "${task.description}\n\n$body"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "tasks"))
            .addAction(0, "Buka tugas", contentIntent(context, "tasks"))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt() + 10_000, notification)
        } catch (_: SecurityException) {
            // Notification permission was revoked; the app stays functional.
        }
    }

    fun notifyClassEnding(context: Context, courseName: String, room: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CLASS)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Kuliah hampir selesai")
            .setContentText("$courseName berakhir 10 menit lagi${if (room.isBlank()) "" else " · $room"}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "schedule"))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                (courseName.hashCode() and 0xFFFF) + 20_000,
                notification
            )
        } catch (_: SecurityException) {
        }
    }

    fun notifyFocusFinished(context: Context, minutes: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentTitle("Sesi fokus selesai")
            .setContentText("$minutes menit fokus tercatat. Waktunya istirahat sejenak.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, "focus"))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(30_001, notification)
        } catch (_: SecurityException) {
        }
    }
}

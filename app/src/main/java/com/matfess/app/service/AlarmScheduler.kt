package com.matfess.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.matfess.app.data.Course
import com.matfess.app.data.DbHelper
import com.matfess.app.data.Task
import com.matfess.app.util.DateUtils
import java.util.Calendar

/**
 * Schedules exact(ish) alarms for task reminders and weekly class-end alerts.
 */
object AlarmScheduler {

    const val EXTRA_TYPE = "alarm_type"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val TYPE_TASK = "task"
    const val TYPE_CLASS = "class"

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(
        context: Context,
        requestCode: Int,
        type: String,
        id: Long,
        title: String = "",
        text: String = ""
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_TASK_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleTask(context: Context, task: Task) {
        if (task.done) {
            cancelTask(context, task.id)
            return
        }
        val trigger = task.deadline - task.reminderMinutes * 60_000L
        if (task.deadline <= System.currentTimeMillis()) return
        val fireAt = if (trigger <= System.currentTimeMillis()) {
            System.currentTimeMillis() + 4_000L
        } else {
            trigger
        }
        val pending = pendingIntent(context, 10_000 + task.id.toInt(), TYPE_TASK, task.id)
        val manager = alarmManager(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
        }
    }

    fun cancelTask(context: Context, taskId: Long) {
        alarmManager(context).cancel(
            pendingIntent(context, 10_000 + taskId.toInt(), TYPE_TASK, taskId)
        )
    }

    fun scheduleClassReminder(context: Context, course: Course) {
        cancelClassReminder(context, course.id)
        if (!course.reminder) return
        val (endHour, endMinute) = DateUtils.parseTime(course.endTime)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, calendarDay(course.day))
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -10)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }
        val pending = pendingIntent(
            context,
            20_000 + course.id.toInt(),
            TYPE_CLASS,
            course.id,
            title = course.name,
            text = course.room
        )
        alarmManager(context).setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pending
        )
    }

    fun cancelClassReminder(context: Context, courseId: Long) {
        alarmManager(context).cancel(
            pendingIntent(context, 20_000 + courseId.toInt(), TYPE_CLASS, courseId)
        )
    }

    fun rescheduleAll(context: Context) {
        val db = DbHelper(context)
        db.getUpcomingTasks().forEach { scheduleTask(context, it) }
        db.getCourses().filter { it.reminder }.forEach { scheduleClassReminder(context, it) }
    }

    private fun calendarDay(courseDay: Int): Int =
        if (courseDay == 7) Calendar.SUNDAY else courseDay + 1
}

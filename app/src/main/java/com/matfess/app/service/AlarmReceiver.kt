package com.matfess.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.matfess.app.data.DbHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannels(context)
        when (intent.getStringExtra(AlarmScheduler.EXTRA_TYPE)) {
            AlarmScheduler.TYPE_TASK -> {
                val id = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
                if (id <= 0L) return
                val task = DbHelper(context).getTask(id) ?: return
                if (!task.done) NotificationHelper.notifyTask(context, task)
            }
            AlarmScheduler.TYPE_CLASS -> {
                val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty()
                val room = intent.getStringExtra(AlarmScheduler.EXTRA_TEXT).orEmpty()
                NotificationHelper.notifyClassEnding(context, title, room)
            }
        }
    }
}

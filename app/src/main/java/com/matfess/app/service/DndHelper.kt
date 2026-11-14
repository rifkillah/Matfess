package com.matfess.app.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DndHelper {

    fun hasAccess(context: Context): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .isNotificationPolicyAccessGranted

    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) return false
        return manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    fun openAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Returns false when the user still needs to grant notification-policy access. */
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.isNotificationPolicyAccessGranted) {
            openAccessSettings(context)
            return false
        }
        manager.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }
}

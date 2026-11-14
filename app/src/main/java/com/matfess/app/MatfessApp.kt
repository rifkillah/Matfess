package com.matfess.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.matfess.app.data.Prefs
import com.matfess.app.service.NotificationHelper

class MatfessApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        AppCompatDelegate.setDefaultNightMode(
            when (Prefs(this).themeMode) {
                Prefs.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Prefs.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}

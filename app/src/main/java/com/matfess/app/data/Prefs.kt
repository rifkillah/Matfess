package com.matfess.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

/**
 * Single source of truth for lightweight, non-relational preferences:
 * session, theme, semester timeline and cached network data.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("matfess_prefs", Context.MODE_PRIVATE)

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(value) = sp.edit().putBoolean("onboarding_done", value).apply()

    var isLoggedIn: Boolean
        get() = sp.getBoolean("logged_in", false)
        set(value) = sp.edit().putBoolean("logged_in", value).apply()

    var userId: Long
        get() = sp.getLong("user_id", -1L)
        set(value) = sp.edit().putLong("user_id", value).apply()

    var userName: String
        get() = sp.getString("user_name", "Mahasiswa") ?: "Mahasiswa"
        set(value) = sp.edit().putString("user_name", value).apply()

    var userNim: String
        get() = sp.getString("user_nim", "") ?: ""
        set(value) = sp.edit().putString("user_nim", value).apply()

    var userMajor: String
        get() = sp.getString("user_major", "") ?: ""
        set(value) = sp.edit().putString("user_major", value).apply()

    var userSemester: Int
        get() = sp.getInt("user_semester", 1)
        set(value) = sp.edit().putInt("user_semester", value).apply()

    var themeMode: Int
        get() = sp.getInt("theme_mode", THEME_SYSTEM)
        set(value) {
            sp.edit().putInt("theme_mode", value).apply()
            AppCompatDelegate.setDefaultNightMode(
                when (value) {
                    THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }

    // Semester timeline, stored as epoch millis at 00:00.
    var semesterStart: Long
        get() = sp.getLong("semester_start", defaultSemesterStart())
        set(value) = sp.edit().putLong("semester_start", value).apply()

    var semesterEnd: Long
        get() = sp.getLong("semester_end", defaultSemesterEnd())
        set(value) = sp.edit().putLong("semester_end", value).apply()

    var utsStart: Long
        get() = sp.getLong("uts_start", defaultUtsStart())
        set(value) = sp.edit().putLong("uts_start", value).apply()

    var utsEnd: Long
        get() = sp.getLong("uts_end", defaultUtsStart() + 6L * 86_400_000L)
        set(value) = sp.edit().putLong("uts_end", value).apply()

    var uasStart: Long
        get() = sp.getLong("uas_start", defaultUasStart())
        set(value) = sp.edit().putLong("uas_start", value).apply()

    var uasEnd: Long
        get() = sp.getLong("uas_end", defaultUasStart() + 6L * 86_400_000L)
        set(value) = sp.edit().putLong("uas_end", value).apply()

    var focusMinutesTotal: Int
        get() = sp.getInt("focus_minutes", 0)
        set(value) = sp.edit().putInt("focus_minutes", value).apply()

    var focusSessionsTotal: Int
        get() = sp.getInt("focus_sessions", 0)
        set(value) = sp.edit().putInt("focus_sessions", value).apply()

    var focusDuration: Int
        get() = sp.getInt("focus_duration", 25)
        set(value) = sp.edit().putInt("focus_duration", value).apply()

    var breakDuration: Int
        get() = sp.getInt("break_duration", 5)
        set(value) = sp.edit().putInt("break_duration", value).apply()

    var dndWhileFocus: Boolean
        get() = sp.getBoolean("dnd_while_focus", false)
        set(value) = sp.edit().putBoolean("dnd_while_focus", value).apply()

    var quoteText: String
        get() = sp.getString("quote_text", "") ?: ""
        set(value) = sp.edit().putString("quote_text", value).apply()

    var quoteAuthor: String
        get() = sp.getString("quote_author", "") ?: ""
        set(value) = sp.edit().putString("quote_author", value).apply()

    var lastQuoteFetch: Long
        get() = sp.getLong("last_quote_fetch", 0L)
        set(value) = sp.edit().putLong("last_quote_fetch", value).apply()

    /** Serialized rows for the offline IPK calculator. */
    var ipkDraft: String
        get() = sp.getString("ipk_draft", "") ?: ""
        set(value) = sp.edit().putString("ipk_draft", value).apply()

    fun saveSession(id: Long, name: String, nim: String, major: String, semester: Int) {
        userId = id
        userName = name
        userNim = nim
        userMajor = major
        userSemester = semester
        isLoggedIn = true
    }

    fun logout() {
        isLoggedIn = false
        userId = -1L
        userName = "Mahasiswa"
        userNim = ""
        userMajor = ""
        userSemester = 1
    }

    private fun defaultSemesterStart(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -45)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun defaultSemesterEnd(): Long = defaultSemesterStart() + 140L * 86_400_000L

    private fun defaultUtsStart(): Long = defaultSemesterStart() + 56L * 86_400_000L

    private fun defaultUasStart(): Long = defaultSemesterStart() + 112L * 86_400_000L
}

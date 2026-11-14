package com.matfess.app.ui.focus

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.databinding.ActivityStudyTimerBinding
import com.matfess.app.service.DndHelper
import com.matfess.app.service.NotificationHelper
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.util.DateUtils
import com.matfess.app.util.applyVerticalInsets
import kotlin.math.ceil
import kotlin.math.max

/**
 * Full Pomodoro timer. State survives configuration changes, keeps running
 * while the activity is backgrounded, and records completed focus minutes.
 */
class StudyTimerActivity : BaseActivity() {

    companion object {
        private const val MODE_FOCUS = "focus"
        private const val MODE_BREAK = "break"
    }

    private lateinit var binding: ActivityStudyTimerBinding
    private val handler = Handler(Looper.getMainLooper())

    private var mode = MODE_FOCUS
    private var totalMillis = 25 * 60_000L
    private var remainingMillis = 25 * 60_000L
    private var deadline = 0L
    private var running = false
    private var dndActivated = false
    private var lastRenderedSecond = -1L

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            remainingMillis = max(0L, deadline - SystemClock.elapsedRealtime())
            if (remainingMillis <= 0L) {
                completeSession()
            } else {
                render()
                handler.postDelayed(this, 200L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.content.applyVerticalInsets(6, 24)

        mode = savedInstanceState?.getString("mode") ?: MODE_FOCUS
        totalMillis = savedInstanceState?.getLong("total") ?: durationFor(mode)
        remainingMillis = savedInstanceState?.getLong("remaining") ?: totalMillis
        running = savedInstanceState?.getBoolean("running") ?: false
        if (running) deadline = SystemClock.elapsedRealtime() + remainingMillis

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReset.setOnClickListener { resetTimer() }
        binding.btnStart.setOnClickListener {
            if (running) pauseTimer() else startTimer()
        }
        binding.segMode.setItems(listOf("Fokus", "Istirahat"), if (mode == MODE_BREAK) 1 else 0)
        binding.segMode.setOnSegmentChanged { index ->
            if (running) pauseTimer()
            mode = if (index == 1) MODE_BREAK else MODE_FOCUS
            totalMillis = durationFor(mode)
            remainingMillis = totalMillis
            lastRenderedSecond = -1L
            render()
        }

        render()
        if (running) {
            ensureKeepScreenOn()
            handler.post(ticker)
        }
    }

    override fun onResume() {
        super.onResume()
        if (running) {
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
        refreshTotals()
    }

    private fun durationFor(mode: String): Long =
        if (mode == MODE_BREAK) prefs.breakDuration * 60_000L else prefs.focusDuration * 60_000L

    private fun startTimer() {
        if (remainingMillis <= 0L) remainingMillis = totalMillis
        deadline = SystemClock.elapsedRealtime() + remainingMillis
        running = true
        ensureKeepScreenOn()
        if (mode == MODE_FOCUS && prefs.dndWhileFocus) {
            dndActivated = DndHelper.setEnabled(this, true)
            if (!dndActivated) toast("Timer berjalan, tetapi akses Jangan Ganggu belum aktif.")
        }
        binding.btnStart.text = "Jeda"
        binding.tvPhase.text = when (mode) {
            MODE_BREAK -> "Istirahatlah. Jangan buka media sosial dulu."
            else -> "Fokus sampai timer selesai. Jauhkan distraksi."
        }
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun pauseTimer() {
        remainingMillis = max(0L, deadline - SystemClock.elapsedRealtime())
        running = false
        handler.removeCallbacks(ticker)
        clearKeepScreenOn()
        deactivateDnd()
        binding.btnStart.text = "Lanjut"
        binding.tvPhase.text = "Timer dijeda. Lanjutkan saat kamu siap."
        render()
    }

    private fun resetTimer() {
        running = false
        handler.removeCallbacks(ticker)
        clearKeepScreenOn()
        deactivateDnd()
        totalMillis = durationFor(mode)
        remainingMillis = totalMillis
        lastRenderedSecond = -1L
        binding.btnStart.text = "Mulai"
        binding.tvPhase.text = "Tekan mulai, lalu jauhkan distraksi."
        render()
    }

    private fun completeSession() {
        running = false
        remainingMillis = 0L
        handler.removeCallbacks(ticker)
        clearKeepScreenOn()
        deactivateDnd()
        binding.btnStart.text = "Mulai lagi"
        render()

        val finishedMode = mode
        if (finishedMode == MODE_FOCUS) {
            repo.addFocusSession(prefs.focusDuration)
            NotificationHelper.notifyFocusFinished(this, prefs.focusDuration)
        }
        refreshTotals()
        binding.tvPhase.text = if (finishedMode == MODE_FOCUS) {
            "Sesi fokus selesai. Catat progresmu, lalu istirahat."
        } else {
            "Istirahat selesai. Siap kembali fokus?"
        }
        binding.ring.setText(DateUtils.formatClock(0))
        binding.ring.setProgress(0f, animate = true)

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(if (finishedMode == MODE_FOCUS) "Sesi fokus selesai" else "Istirahat selesai")
                .setMessage(
                    if (finishedMode == MODE_FOCUS) {
                        "${prefs.focusDuration} menit fokus tercatat. Ambil jeda ${prefs.breakDuration} menit?"
                    } else {
                        "Waktunya kembali fokus. Mulai sesi ${prefs.focusDuration} menit?"
                    }
                )
                .setNegativeButton("Tutup", null)
                .setPositiveButton(if (finishedMode == MODE_FOCUS) "Mulai istirahat" else "Mulai fokus") { _, _ ->
                    mode = if (finishedMode == MODE_FOCUS) MODE_BREAK else MODE_FOCUS
                    binding.segMode.select(if (mode == MODE_BREAK) 1 else 0, notify = false)
                    totalMillis = durationFor(mode)
                    remainingMillis = totalMillis
                    lastRenderedSecond = -1L
                    render()
                    startTimer()
                }
                .show()
        }
    }

    private fun render() {
        val seconds = ceil(remainingMillis / 1000.0).toLong()
        val progress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
        binding.ring.setProgress(progress.coerceIn(0f, 1f), animate = false)
        if (seconds != lastRenderedSecond) {
            binding.ring.setText(DateUtils.formatClock(seconds))
            lastRenderedSecond = seconds
        }
        val isBreak = mode == MODE_BREAK
        binding.ring.setSubText(if (isBreak) "ISTIRAHAT" else "FOKUS")
        binding.tvMode.text = if (isBreak) "ISTIRAHAT" else "SESI FOKUS"
        val accent = ContextCompat.getColor(
            this,
            if (isBreak) R.color.accent_mint else R.color.accent_primary
        )
        binding.tvMode.setTextColor(accent)
        binding.tvMode.backgroundTintList =
            android.content.res.ColorStateList.valueOf(prefs.let { com.matfess.app.util.Palette.withAlpha(accent, 30) })
        binding.ring.setColors(
            accent,
            ContextCompat.getColor(this, if (isBreak) R.color.accent_sky else R.color.accent_violet)
        )
        binding.tvSessionMinutes.text = (totalMillis / 60_000L).toString()
        if (!running && remainingMillis == totalMillis) {
            binding.btnStart.text = "Mulai"
        }
    }

    private fun refreshTotals() {
        binding.tvTotalSessions.text = prefs.focusSessionsTotal.toString()
    }

    private fun ensureKeepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun clearKeepScreenOn() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun deactivateDnd() {
        if (dndActivated) {
            DndHelper.setEnabled(this, false)
            dndActivated = false
        }
    }

    private fun toast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("mode", mode)
        outState.putLong("total", totalMillis)
        outState.putLong(
            "remaining",
            if (running) max(0L, deadline - SystemClock.elapsedRealtime()) else remainingMillis
        )
        outState.putBoolean("running", running)
    }

    override fun onPause() {
        // The deadline-based timer keeps running in the background; we only
        // stop ticking to save battery and resume precisely in onResume().
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        clearKeepScreenOn()
        deactivateDnd()
        super.onDestroy()
    }
}

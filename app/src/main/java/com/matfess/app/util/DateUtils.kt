package com.matfess.app.util

import com.matfess.app.data.Prefs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object DateUtils {

    private val id: Locale = Locale("id", "ID")

    private val fullDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", id)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", id)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", id)
    private val monthShort = DateTimeFormatter.ofPattern("MMM", id)
    private val dayOnly = DateTimeFormatter.ofPattern("EEEE", id)
    private val timeOnly = DateTimeFormatter.ofPattern("HH:mm", id)

    const val DAY_MILLIS = 86_400_000L

    fun now(): Long = System.currentTimeMillis()

    fun toLocal(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun startOfDay(millis: Long = now()): Long =
        toLocal(millis).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun endOfDay(millis: Long = now()): Long = startOfDay(millis) + DAY_MILLIS - 1

    fun formatFull(millis: Long): String = toLocal(millis).format(fullDate)

    fun formatShort(millis: Long): String = toLocal(millis).format(shortDate)

    fun formatMonthYear(millis: Long): String = toLocal(millis).format(monthYear)

    fun formatMonthShort(millis: Long): String =
        toLocal(millis).format(monthShort).uppercase(Locale("id", "ID"))

    fun dayOfMonth(millis: Long): Int = toLocal(millis).dayOfMonth

    fun formatTime(millis: Long): String = toLocal(millis).format(timeOnly)

    fun formatTime(hour: Int, minute: Int): String = String.format(id, "%02d:%02d", hour, minute)

    fun formatClock(totalSeconds: Long): String {
        val safe = max(0L, totalSeconds)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) String.format(id, "%d:%02d:%02d", h, m, s)
        else String.format(id, "%02d:%02d", m, s)
    }

    fun parseTime(value: String): Pair<Int, Int> {
        val parts = value.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 8) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun combine(dateMillis: Long, hour: Int, minute: Int): Long {
        val date = toLocal(dateMillis).toLocalDate()
        return date.atTime(LocalTime.of(hour, minute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun combine(dateMillis: Long, time: String): Long {
        val (h, m) = parseTime(time)
        return combine(dateMillis, h, m)
    }

    fun relativeDeadline(millis: Long, reference: Long = now()): String {
        val today = toLocal(reference).toLocalDate()
        val target = toLocal(millis).toLocalDate()
        val diff = ChronoUnit.DAYS.between(today, target)
        val time = formatTime(millis)
        return when {
            millis < reference -> "Terlambat"
            diff == 0L -> "Hari ini · $time"
            diff == 1L -> "Besok · $time"
            diff in 2..6 -> "$diff hari lagi · $time"
            diff < 0 -> "Terlambat"
            else -> "${formatShort(millis)} · $time"
        }
    }

    fun dayName(day: Int): String = when (day) {
        1 -> "Senin"
        2 -> "Selasa"
        3 -> "Rabu"
        4 -> "Kamis"
        5 -> "Jumat"
        6 -> "Sabtu"
        7 -> "Minggu"
        else -> "-"
    }

    fun dayNameOf(millis: Long): String = toLocal(millis).format(dayOnly)

    fun dayIndex(millis: Long = now()): Int = toLocal(millis).dayOfWeek.value

    fun dayShort(day: Int): String = dayName(day).take(3)

    fun greeting(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 4 -> "Selamat malam"
            hour < 11 -> "Selamat pagi"
            hour < 15 -> "Selamat siang"
            hour < 18 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    fun daysBetween(from: Long, to: Long): Long {
        val a = toLocal(from).toLocalDate()
        val b = toLocal(to).toLocalDate()
        return ChronoUnit.DAYS.between(a, b)
    }

    fun semesterDay(prefs: Prefs, reference: Long = now()): Int {
        val diff = daysBetween(prefs.semesterStart, reference)
        return max(1, diff.toInt() + 1)
    }

    fun semesterProgress(prefs: Prefs, reference: Long = now()): Float {
        val total = (prefs.semesterEnd - prefs.semesterStart).toFloat()
        if (total <= 0f) return 0f
        return min(1f, max(0f, (reference - prefs.semesterStart) / total))
    }

    data class ExamPhase(
        val phase: String,
        val title: String,
        val subtitle: String,
        val targetMillis: Long,
        val progress: Float
    )

    fun examPhase(prefs: Prefs, reference: Long = now()): ExamPhase {
        val utsStart = prefs.utsStart
        val utsEnd = max(prefs.utsEnd, utsStart)
        val uasStart = prefs.uasStart
        val uasEnd = max(prefs.uasEnd, uasStart)
        return when {
            reference < utsStart -> ExamPhase(
                phase = "PRE_UTS",
                title = "Menuju UTS",
                subtitle = "Siapkan strategi belajarmu",
                targetMillis = utsStart,
                progress = windowProgress(prefs.semesterStart, utsStart, reference)
            )
            reference <= utsEnd -> ExamPhase(
                phase = "UTS",
                title = "Masa UTS",
                subtitle = "Fokus, satu ujian dalam satu waktu",
                targetMillis = utsEnd,
                progress = windowProgress(utsStart, utsEnd, reference)
            )
            reference < uasStart -> ExamPhase(
                phase = "PRE_UAS",
                title = "Menuju UAS",
                subtitle = "Jaga ritme, jangan burnout",
                targetMillis = uasStart,
                progress = windowProgress(utsEnd, uasStart, reference)
            )
            reference <= uasEnd -> ExamPhase(
                phase = "UAS",
                title = "Masa UAS",
                subtitle = "Semua usaha bertemu di sini",
                targetMillis = uasEnd,
                progress = windowProgress(uasStart, uasEnd, reference)
            )
            else -> ExamPhase(
                phase = "DONE",
                title = "Semester selesai",
                subtitle = "Saatnya rehat dan evaluasi",
                targetMillis = prefs.semesterEnd,
                progress = 1f
            )
        }
    }

    private fun windowProgress(from: Long, to: Long, reference: Long): Float {
        val total = (to - from).toFloat()
        if (total <= 0f) return 0f
        return min(1f, max(0f, (reference - from) / total))
    }

    data class Countdown(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    fun countdown(target: Long, reference: Long = now()): Countdown {
        val diff = max(0L, target - reference) / 1000L
        return Countdown(
            days = diff / 86_400,
            hours = (diff % 86_400) / 3_600,
            minutes = (diff % 3_600) / 60,
            seconds = diff % 60
        )
    }

    fun formatTargetDate(millis: Long): String = formatFull(millis)

    fun startOfWeek(date: LocalDate = LocalDate.now()): LocalDate =
        date.with(DayOfWeek.MONDAY)

    fun sksPoint(grade: String): Double = when (grade.uppercase(Locale.ROOT)) {
        "A" -> 4.0
        "A-" -> 3.7
        "B+" -> 3.3
        "B" -> 3.0
        "B-" -> 2.7
        "C+" -> 2.3
        "C" -> 2.0
        "D" -> 1.0
        else -> 0.0
    }
}

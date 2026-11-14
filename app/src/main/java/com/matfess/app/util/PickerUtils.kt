package com.matfess.app.util

import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Custom date and time pickers shared by every form, so users can adjust
 * deadlines, class times, event dates and semester dates freely.
 */
fun Fragment.pickDate(
    initialMillis: Long = System.currentTimeMillis(),
    title: String = "Pilih tanggal",
    onPick: (Long) -> Unit
) {
    val utcSelection = Instant.ofEpochMilli(initialMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(title)
        .setSelection(utcSelection)
        .build()
    picker.addOnPositiveButtonClickListener { selection ->
        val localDate = Instant.ofEpochMilli(selection)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        onPick(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    picker.show(childFragmentManager, "date_picker")
}

fun Fragment.pickTime(
    hour: Int = 8,
    minute: Int = 0,
    title: String = "Pilih waktu",
    onPick: (Int, Int) -> Unit
) {
    val picker = MaterialTimePicker.Builder()
        .setTimeFormat(TimeFormat.CLOCK_24H)
        .setHour(hour)
        .setMinute(minute)
        .setTitleText(title)
        .build()
    picker.addOnPositiveButtonClickListener {
        onPick(picker.hour, picker.minute)
    }
    picker.show(childFragmentManager, "time_picker")
}

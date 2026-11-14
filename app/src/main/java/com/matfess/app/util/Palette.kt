package com.matfess.app.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.matfess.app.R

/** Shared pastel accent palette, deliberately calm rather than neon. */
object Palette {

    fun accents(context: Context): List<Int> = listOf(
        ContextCompat.getColor(context, R.color.accent_primary),
        ContextCompat.getColor(context, R.color.accent_sky),
        ContextCompat.getColor(context, R.color.accent_mint),
        ContextCompat.getColor(context, R.color.accent_coral),
        ContextCompat.getColor(context, R.color.accent_amber),
        ContextCompat.getColor(context, R.color.accent_violet)
    )

    fun accent(context: Context, index: Int): Int {
        val list = accents(context)
        return list[((index % list.size) + list.size) % list.size]
    }

    fun soft(context: Context, index: Int, alpha: Int = 42): Int =
        ColorUtils.setAlphaComponent(accent(context, index), alpha)

    fun chipBackground(context: Context, index: Int): ColorStateList =
        ColorStateList.valueOf(soft(context, index, 36))

    fun initialColor(context: Context, name: String): Int {
        val palette = accents(context)
        val hash = name.fold(0) { acc, c -> acc + c.code }
        return palette[hash % palette.size]
    }

    fun priorityColor(context: Context, priority: Int): Int = when (priority) {
        2 -> ContextCompat.getColor(context, R.color.accent_coral)
        1 -> ContextCompat.getColor(context, R.color.accent_amber)
        else -> ContextCompat.getColor(context, R.color.accent_mint)
    }

    fun priorityLabel(priority: Int): String = when (priority) {
        2 -> "Prioritas tinggi"
        1 -> "Prioritas sedang"
        else -> "Prioritas rendah"
    }

    fun withAlpha(color: Int, alpha: Int): Int = ColorUtils.setAlphaComponent(color, alpha)

    fun blend(a: Int, b: Int, ratio: Float): Int = ColorUtils.blendARGB(a, b, ratio)

    fun transparent(color: Int): Int = ColorUtils.setAlphaComponent(color, 0)

    @Suppress("unused")
    fun darken(color: Int): Int = ColorUtils.blendARGB(color, Color.BLACK, 0.18f)
}

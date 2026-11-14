package com.matfess.app.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun View.applyTopInset(extraDp: Int = 0) {
    val extra = context.dp(extraDp)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + extra
        view.updatePadding(top = top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomInset(extraDp: Int = 0) {
    val extra = context.dp(extraDp)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom + extra
        view.updatePadding(bottom = bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** Keeps a floating bar above the gesture/navigation bar without resizing it. */
/**
 * Applies top and bottom system-bar insets together. Calling [applyTopInset]
 * and [applyBottomInset] on the same view would replace the listener, so
 * screens that need both should use this helper.
 */
fun View.applyVerticalInsets(topExtraDp: Int = 0, bottomExtraDp: Int = 0) {
    val extraTop = context.dp(topExtraDp)
    val extraBottom = context.dp(bottomExtraDp)
    val baseTop = paddingTop
    val baseBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            top = baseTop + extraTop + bars.top,
            bottom = baseBottom + extraBottom + bars.bottom
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomMarginInset(extraDp: Int = 0) {
    val extra = context.dp(extraDp)
    val base = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        val lp = view.layoutParams as? ViewGroup.MarginLayoutParams
        if (lp != null) {
            lp.bottomMargin = base + extra + bottom
            view.layoutParams = lp
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.entrance(delay: Long = 0L) {
    alpha = 0f
    translationY = context.dp(14).toFloat()
    animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(420)
        .setInterpolator(DecelerateInterpolator(1.6f)).start()
}

fun View.pressScale() {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN ->
                view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL ->
                view.animate().scaleX(1f).scaleY(1f).setDuration(260)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
        }
        false
    }
}

fun Fragment.dp(value: Float): Int = requireContext().dp(value)

fun View.shake() {
    android.animation.ObjectAnimator.ofFloat(
        this, "translationX", 0f, 12f, -10f, 8f, -5f, 0f
    ).apply {
        duration = 360
        start()
    }
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}


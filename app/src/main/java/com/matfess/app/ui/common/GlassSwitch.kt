package com.matfess.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.matfess.app.R

/**
 * A springy iOS toggle. The knob is a clear lens over a tinted track.
 */
class GlassSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var checked = false
    private var fraction = 0f
    private var tint = ContextCompat.getColor(context, R.color.accent_primary)
    private var listener: ((Boolean) -> Unit)? = null

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassSwitch)
            checked = a.getBoolean(R.styleable.GlassSwitch_gsChecked, false)
            tint = a.getColor(R.styleable.GlassSwitch_gsTint, tint)
            a.recycle()
        }
        fraction = if (checked) 1f else 0f
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(1f)
    }

    fun isChecked(): Boolean = checked

    fun setChecked(value: Boolean, notify: Boolean = false, animate: Boolean = true) {
        if (checked == value && animate) return
        checked = value
        if (animate) {
            ValueAnimator.ofFloat(fraction, if (value) 1f else 0f).apply {
                duration = 260
                interpolator = OvershootInterpolator(2.4f)
                addUpdateListener {
                    fraction = (it.animatedValue as Float).coerceIn(0f, 1f)
                    invalidate()
                }
                start()
            }
        } else {
            fraction = if (value) 1f else 0f
            invalidate()
        }
        contentDescription = if (value) "Aktif" else "Nonaktif"
        if (notify) listener?.invoke(value)
    }

    fun setOnCheckedChange(callback: (Boolean) -> Unit) {
        listener = callback
    }

    fun setTintColor(color: Int) {
        tint = color
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(dp(50f).toInt(), widthMeasureSpec)
        val h = resolveSize(dp(30f).toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        val w = width.toFloat()
        val trackRect = RectF(dp(2f), dp(2f), w - dp(2f), h - dp(2f))
        trackPaint.color = ColorUtils.blendARGB(
            ContextCompat.getColor(context, R.color.glass_top),
            tint,
            fraction
        )
        if (fraction < 0.05f) {
            trackPaint.color = ColorUtils.setAlphaComponent(
                ContextCompat.getColor(context, R.color.text_tertiary), 70
            )
        }
        trackPaint.alpha = 255
        canvas.drawRoundRect(trackRect, trackRect.height() / 2f, trackRect.height() / 2f, trackPaint)
        rimPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, (40 + 80 * fraction).toInt())
        canvas.drawRoundRect(trackRect, trackRect.height() / 2f, trackRect.height() / 2f, rimPaint)

        val knobRadius = (h - dp(6f)) / 2f
        val startX = dp(2f) + knobRadius + dp(1f)
        val endX = w - dp(2f) - knobRadius - dp(1f)
        val cx = startX + (endX - startX) * fraction
        shadowPaint.color = ColorUtils.setAlphaComponent(Color.BLACK, 40)
        canvas.drawCircle(cx + dp(0.5f), h / 2f + dp(1.2f), knobRadius, shadowPaint)
        knobPaint.color = Color.WHITE
        canvas.drawCircle(cx, h / 2f, knobRadius, knobPaint)
        knobPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, 130)
        canvas.drawCircle(cx - knobRadius * 0.2f, h / 2f - knobRadius * 0.25f, knobRadius * 0.55f, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                return true
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                setChecked(!checked, notify = true)
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Switch"
        info.isCheckable = true
        info.isChecked = checked
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}

package com.matfess.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.matfess.app.R

/**
 * iOS-style segmented control with a lens that glides between segments.
 */
class LiquidSegmented @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private var items: List<String> = emptyList()
    private var selected = 0
    private var animatedIndex = 0f
    private var listener: ((Int) -> Unit)? = null
    private var animator: ValueAnimator? = null

    private val radius = dp(18f)
    private val pad = dp(4f)

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_700)
        textPaint.textSize = sp(12f)
        trackPaint.color = ContextCompat.getColor(context, R.color.glass_bottom)
        lensPaint.color = ContextCompat.getColor(context, R.color.accent_primary_soft)
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(1f)
        rimPaint.color = ContextCompat.getColor(context, R.color.glass_stroke)
        minimumHeight = dp(42f).toInt()
    }

    fun setItems(values: List<String>, selectedIndex: Int = 0) {
        items = values
        selected = selectedIndex.coerceIn(0, maxOf(0, values.size - 1))
        animatedIndex = selected.toFloat()
        contentDescription = values.joinToString(", ")
        invalidate()
    }

    fun setOnSegmentChanged(callback: (Int) -> Unit) {
        listener = callback
    }

    fun select(index: Int, notify: Boolean = true) {
        if (items.isEmpty()) return
        val target = index.coerceIn(0, items.lastIndex)
        if (target == selected && notify) return
        animateTo(target)
        selected = target
        if (notify) listener?.invoke(target)
    }

    fun selectedIndex(): Int = selected

    private fun animateTo(target: Int) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedIndex, target.toFloat()).apply {
            duration = 280
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                animatedIndex = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = maxOf(dp(42f).toInt(), resolvedHeight(heightMeasureSpec))
        setMeasuredDimension(width, height)
    }

    private fun resolvedHeight(spec: Int): Int = when (MeasureSpec.getMode(spec)) {
        MeasureSpec.EXACTLY -> MeasureSpec.getSize(spec)
        MeasureSpec.AT_MOST -> minOf(MeasureSpec.getSize(spec), dp(48f).toInt())
        else -> dp(42f).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        if (items.isEmpty() || width == 0) return
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.addRoundRect(rect, height / 2f, height / 2f, Path.Direction.CW)
        trackPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            ContextCompat.getColor(context, R.color.glass_top),
            ContextCompat.getColor(context, R.color.glass_bottom),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, trackPaint)
        canvas.drawPath(path, rimPaint)

        val segWidth = (width - pad * 2) / items.size
        val left = pad + animatedIndex * segWidth
        val lensRect = RectF(left + dp(1f), pad, left + segWidth - dp(1f), height - pad)
        lensPaint.shader = LinearGradient(
            lensRect.left, lensRect.top, lensRect.right, lensRect.bottom,
            ContextCompat.getColor(context, R.color.glass_top),
            ContextCompat.getColor(context, R.color.accent_primary_soft),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(lensRect, radius, radius, lensPaint)
        rimPaint.color = ColorUtils.setAlphaComponent(Color.WHITE, 90)
        canvas.drawRoundRect(lensRect, radius, radius, rimPaint)
        rimPaint.color = ContextCompat.getColor(context, R.color.glass_stroke)

        val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        items.forEachIndexed { index, label ->
            textPaint.color = if (index == selected) {
                ContextCompat.getColor(context, R.color.accent_primary)
            } else {
                ContextCompat.getColor(context, R.color.text_secondary)
            }
            canvas.drawText(label, pad + segWidth * index + segWidth / 2f, baseline, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                return true
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                if (items.isNotEmpty()) {
                    val segWidth = (width - pad * 2) / items.size
                    val index = ((event.x - pad) / segWidth).toInt().coerceIn(0, items.lastIndex)
                    select(index)
                    performClick()
                }
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

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}

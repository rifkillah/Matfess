package com.matfess.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.matfess.app.R

/**
 * Progress ring used for the semester tracker and focus timer.
 * The gradient is rotated so the arc starts at 12 o'clock.
 */
class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val matrix = Matrix()

    private var progress = 0f
    private var animatedProgress = 0f
    private var strokeWidth = dp(10f)
    private var trackColor = ContextCompat.getColor(context, R.color.divider)
    private var startColor = ContextCompat.getColor(context, R.color.grad_primary_start)
    private var endColor = ContextCompat.getColor(context, R.color.grad_mint_start)
    private var label = ""
    private var subLabel = ""
    private var textSize = sp(28f)
    private var subTextSize = sp(10f)
    private var showText = true
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_700)
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        subPaint.textAlign = Paint.Align.CENTER
        subPaint.typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_600)
        subPaint.color = ContextCompat.getColor(context, R.color.text_tertiary)
        arcPaint.style = Paint.Style.STROKE
        arcPaint.strokeCap = Paint.Cap.ROUND
        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeCap = Paint.Cap.ROUND
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.ProgressRingView)
            progress = a.getFloat(R.styleable.ProgressRingView_prProgress, 0f)
            strokeWidth = a.getDimension(R.styleable.ProgressRingView_prStrokeWidth, strokeWidth)
            trackColor = a.getColor(R.styleable.ProgressRingView_prTrackColor, trackColor)
            startColor = a.getColor(R.styleable.ProgressRingView_prColorStart, startColor)
            endColor = a.getColor(R.styleable.ProgressRingView_prColorEnd, endColor)
            label = a.getString(R.styleable.ProgressRingView_prText) ?: ""
            subLabel = a.getString(R.styleable.ProgressRingView_prSubText) ?: ""
            textSize = a.getDimension(R.styleable.ProgressRingView_prTextSize, textSize)
            subTextSize = a.getDimension(R.styleable.ProgressRingView_prSubTextSize, subTextSize)
            showText = a.getBoolean(R.styleable.ProgressRingView_prShowText, true)
            a.recycle()
        }
        animatedProgress = progress
        arcPaint.strokeWidth = strokeWidth
        trackPaint.strokeWidth = strokeWidth
        textPaint.textSize = textSize
        subPaint.textSize = subTextSize
    }

    fun setProgress(value: Float, animate: Boolean = true) {
        val target = value.coerceIn(0f, 1f)
        progress = target
        if (!animate) {
            animatedProgress = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedProgress, target).apply {
            duration = 900
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener {
                animatedProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setText(value: String) {
        label = value
        invalidate()
    }

    fun setSubText(value: String) {
        subLabel = value
        invalidate()
    }

    fun setColors(start: Int, end: Int) {
        startColor = start
        endColor = end
        rebuildShader()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShader()
    }

    private fun rebuildShader() {
        if (width == 0 || height == 0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) / 2f - strokeWidth
        val shader = SweepGradient(
            cx, cy,
            intArrayOf(startColor, endColor, startColor),
            floatArrayOf(0f, 0.65f, 1f)
        )
        matrix.setRotate(-90f, cx, cy)
        shader.setLocalMatrix(matrix)
        arcPaint.shader = shader
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        // A soft outer glow behind the arc.
        arcPaint.alpha = 255
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        trackPaint.color = trackColor
        trackPaint.strokeWidth = strokeWidth
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        if (animatedProgress > 0f) {
            arcPaint.strokeWidth = strokeWidth
            canvas.drawArc(rect, -90f, 360f * animatedProgress, false, arcPaint)
        }
        if (showText) {
            val cx = width / 2f
            val cy = height / 2f
            if (label.isNotEmpty()) {
                val offset = if (subLabel.isNotEmpty()) -textSize * 0.12f else 0f
                canvas.drawText(label, cx, cy + textSize * 0.34f + offset, textPaint)
            }
            if (subLabel.isNotEmpty()) {
                canvas.drawText(subLabel, cx, cy + textSize * 0.62f, subPaint)
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}

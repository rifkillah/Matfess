package com.matfess.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import com.matfess.app.R
import kotlin.math.min

/**
 * The core iOS 27-inspired surface: translucent fill, bright rim, a soft
 * top bloom and a gentle spring when pressed. It works from API 26 upwards.
 */
class GlassCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private var radius = dp(26f)
    private var tintColor = Color.TRANSPARENT
    private var strokeWidth = dp(1f)
    private var pressEnabled = true
    private var highlight = true
    private var pressedX = 0f
    private var pressedY = 0f
    private var sheenVisible = false

    init {
        setWillNotDraw(false)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        ViewCompat.setElevation(this, dp(8f))
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.GlassCardView)
            radius = a.getDimension(R.styleable.GlassCardView_gcRadius, radius)
            tintColor = a.getColor(R.styleable.GlassCardView_gcTint, Color.TRANSPARENT)
            strokeWidth = a.getDimension(R.styleable.GlassCardView_gcStrokeWidth, strokeWidth)
            pressEnabled = a.getBoolean(R.styleable.GlassCardView_gcPressEnabled, true)
            highlight = a.getBoolean(R.styleable.GlassCardView_gcHighlight, true)
            ViewCompat.setElevation(
                this,
                a.getDimension(R.styleable.GlassCardView_gcElevation, dp(8f))
            )
            a.recycle()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.glass_shadow)
            outlineSpotShadowColor = ContextCompat.getColor(context, R.color.glass_shadow)
        }
    }

    fun setGlassTint(color: Int) {
        tintColor = color
        invalidate()
    }

    fun setGlassRadius(value: Float) {
        radius = value
        invalidateOutline()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        buildShaders()
    }

    private fun buildShaders() {
        if (width == 0 || height == 0) return
        fillPaint.shader = LinearGradient(
            0f, 0f, width * 0.35f, height.toFloat(),
            ContextCompat.getColor(context, R.color.glass_top),
            ContextCompat.getColor(context, R.color.glass_bottom),
            Shader.TileMode.CLAMP
        )
        rimPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            ContextCompat.getColor(context, R.color.glass_stroke),
            ContextCompat.getColor(context, R.color.glass_stroke_soft),
            Shader.TileMode.CLAMP
        )
        bloomPaint.shader = LinearGradient(
            0f, 0f, 0f, height * 0.55f,
            ContextCompat.getColor(context, R.color.glass_sheen),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRect(rect, fillPaint)
        if (Color.alpha(tintColor) > 0) {
            tintPaint.color = tintColor
            canvas.drawRect(rect, tintPaint)
        }
        if (highlight) {
            canvas.drawRect(rect, bloomPaint)
        }
        if (sheenVisible) {
            sheenPaint.shader = RadialGradient(
                pressedX, pressedY, min(width, height) * 0.85f,
                intArrayOf(ContextCompat.getColor(context, R.color.glass_sheen), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(rect, sheenPaint)
        }
        canvas.restore()
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = strokeWidth
        canvas.drawPath(path, rimPaint)
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (pressEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressedX = event.x
                    pressedY = event.y
                    sheenVisible = true
                    animate().scaleX(0.975f).scaleY(0.975f).setDuration(110).start()
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sheenVisible = false
                    animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(320)
                        .setInterpolator(OvershootInterpolator(1.8f))
                        .start()
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}

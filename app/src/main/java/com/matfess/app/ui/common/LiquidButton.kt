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
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import com.matfess.app.R
import kotlin.math.min

/**
 * Pill/circle button with the same glass recipe as [GlassCardView], plus
 * filled and tinted variants for primary actions.
 */
class LiquidButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private var variant = 0
    private var tint = ContextCompat.getColor(context, R.color.accent_primary)
    private var radius = dp(20f)
    private var pressEnabled = true
    private var pressedX = 0f
    private var pressedY = 0f
    private var sheenVisible = false

    init {
        setWillNotDraw(false)
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        includeFontPadding = false
        minimumHeight = dp(48f).toInt()
        setPadding(dp(20f).toInt(), dp(11f).toInt(), dp(20f).toInt(), dp(11f).toInt())
        textSize = 13.5f
        typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_700)
        background = null
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        ViewCompat.setElevation(this, dp(4f))
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.LiquidButton)
            variant = a.getInt(R.styleable.LiquidButton_lbVariant, 0)
            tint = a.getColor(R.styleable.LiquidButton_lbTint, tint)
            radius = a.getDimension(R.styleable.LiquidButton_lbRadius, radius)
            pressEnabled = a.getBoolean(R.styleable.LiquidButton_lbPressEnabled, true)
            a.recycle()
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.glass_shadow)
            outlineSpotShadowColor = ContextCompat.getColor(context, R.color.glass_shadow)
        }
    }

    fun setTintColor(color: Int) {
        tint = color
        invalidate()
    }

    fun setVariant(value: Int) {
        variant = value
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        buildShaders()
    }

    private fun buildShaders() {
        if (width == 0 || height == 0) return
        when (variant) {
            1 -> {
                fillPaint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    tint, ColorUtils.blendARGB(tint, Color.BLACK, 0.22f),
                    Shader.TileMode.CLAMP
                )
            }
            2 -> {
                fillPaint.color = ColorUtils.setAlphaComponent(tint, 46)
            }
            3 -> {
                fillPaint.color = ContextCompat.getColor(context, R.color.glass_bottom)
            }
            else -> {
                fillPaint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    ContextCompat.getColor(context, R.color.glass_top),
                    ContextCompat.getColor(context, R.color.glass_bottom),
                    Shader.TileMode.CLAMP
                )
            }
        }
        rimPaint.shader = if (variant == 1 || variant == 2) {
            LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                ColorUtils.setAlphaComponent(Color.WHITE, 150),
                ColorUtils.setAlphaComponent(Color.WHITE, 30),
                Shader.TileMode.CLAMP
            )
        } else {
            LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                ContextCompat.getColor(context, R.color.glass_stroke),
                ContextCompat.getColor(context, R.color.glass_stroke_soft),
                Shader.TileMode.CLAMP
            )
        }
        bloomPaint.shader = LinearGradient(
            0f, 0f, 0f, height * 0.6f,
            ContextCompat.getColor(context, R.color.glass_sheen),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) {
            super.onDraw(canvas)
            return
        }
        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(path)
        canvas.drawRect(rect, fillPaint)
        if (variant != 1 && variant != 3) canvas.drawRect(rect, bloomPaint)
        if (sheenVisible) {
            sheenPaint.shader = RadialGradient(
                pressedX, pressedY, min(width, height) * 1.4f,
                intArrayOf(ContextCompat.getColor(context, R.color.glass_sheen), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(rect, sheenPaint)
        }
        canvas.restore()
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(1f)
        canvas.drawPath(path, rimPaint)
        super.onDraw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (pressEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressedX = event.x
                    pressedY = event.y
                    sheenVisible = true
                    animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sheenVisible = false
                    animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(340)
                        .setInterpolator(OvershootInterpolator(2.2f))
                        .start()
                    invalidate()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.45f
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}

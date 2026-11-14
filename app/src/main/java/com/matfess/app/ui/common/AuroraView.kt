package com.matfess.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.matfess.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slowly drifting aurora/mesh gradient used behind every screen. It gives the
 * glass surfaces something colourful to refract without using a single image.
 */
class AuroraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaders = mutableListOf<RadialGradient>()
    private val matrix = Matrix()
    private val blobs = mutableListOf<Blob>()
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private val isNight = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private data class Blob(
        val color: Int,
        val baseX: Float,
        val baseY: Float,
        val radius: Float,
        val dx: Float,
        val dy: Float,
        val speed: Float,
        val alpha: Int
    )

    init {
        val alpha = if (isNight) 130 else 88
        blobs += Blob(ContextCompat.getColor(context, R.color.bg_glow_2), 0.10f, 0.08f, 0.62f, 0.10f, 0.06f, 1.0f, alpha)
        blobs += Blob(ContextCompat.getColor(context, R.color.bg_glow_4), 0.92f, 0.20f, 0.58f, 0.08f, 0.10f, 1.2f, alpha)
        blobs += Blob(ContextCompat.getColor(context, R.color.bg_glow_3), 0.72f, 0.78f, 0.52f, 0.12f, 0.05f, 0.8f, alpha)
        blobs += Blob(ContextCompat.getColor(context, R.color.bg_glow_1), 0.18f, 0.86f, 0.55f, 0.06f, 0.08f, 1.1f, alpha)
        blobs += Blob(ContextCompat.getColor(context, R.color.bg_glow_5), 0.52f, 0.42f, 0.45f, 0.16f, 0.12f, 0.7f, alpha / 2)
        basePaint.color = ContextCompat.getColor(context, R.color.bg_base)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = maxOf(w, h).toFloat()
        shaders.clear()
        blobs.forEach { blob ->
            shaders += RadialGradient(
                0f, 0f, size * blob.radius,
                intArrayOf(
                    ColorUtils.withAlpha(blob.color, blob.alpha),
                    ColorUtils.withAlpha(blob.color, (blob.alpha * 0.45f).toInt()),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        startAnimating()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(basePaint.color)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || shaders.size != blobs.size) return
        val t = phase * 2f * Math.PI.toFloat()
        blobs.forEachIndexed { index, blob ->
            val cx = (blob.baseX + sin(t * blob.speed + index) * blob.dx) * w
            val cy = (blob.baseY + cos(t * blob.speed * 0.8f + index) * blob.dy) * h
            matrix.setTranslate(cx, cy)
            shaders[index].setLocalMatrix(matrix)
            paint.shader = shaders[index]
            canvas.drawCircle(cx, cy, maxOf(w, h) * blob.radius, paint)
        }
        paint.shader = null

        // A subtle vertical wash keeps text legible at the top and bottom.
        val wash = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                ColorUtils.withAlpha(basePaint.color, 100),
                Color.TRANSPARENT,
                ColorUtils.withAlpha(basePaint.color, 80)
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = wash
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun startAnimating() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 24_000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shaders.isNotEmpty()) startAnimating()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private object ColorUtils {
        fun withAlpha(color: Int, alpha: Int): Int =
            (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
    }
}

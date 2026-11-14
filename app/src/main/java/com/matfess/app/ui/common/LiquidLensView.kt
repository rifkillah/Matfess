package com.matfess.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.matfess.app.R

/**
 * The moving glass lens behind the selected tab.
 */
class LiquidLensView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private var tint = ContextCompat.getColor(context, R.color.accent_primary)

    init {
        setWillNotDraw(false)
        rimPaint.style = Paint.Style.STROKE
        rimPaint.strokeWidth = dp(1f)
    }

    fun setTintColor(color: Int) {
        tint = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        path.reset()
        path.addRoundRect(rect, height / 2f, height / 2f, Path.Direction.CW)
        val night = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            ColorUtils.setAlphaComponent(Color.WHITE, if (night) 32 else 150),
            ColorUtils.setAlphaComponent(tint, if (night) 54 else 34),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, paint)
        rimPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            ColorUtils.setAlphaComponent(Color.WHITE, if (night) 70 else 150),
            ColorUtils.setAlphaComponent(tint, 60),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(path, rimPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}

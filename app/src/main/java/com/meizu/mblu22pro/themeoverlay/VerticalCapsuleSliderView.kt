package com.meizu.mblu22pro.themeoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class VerticalCapsuleSliderView(
    context: Context,
    private var iconLabel: String,
    private var titleLabel: String
) : View(context) {

    var progress: Int = 65
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var trackColor: Int = Color.parseColor("#38FFFFFF")
        set(value) {
            field = value
            invalidate()
        }

    var fillColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    var iconColor: Int = Color.parseColor("#1C1C1E")
        set(value) {
            field = value
            invalidate()
        }

    var cornerRadiusPx: Float = 64f
        set(value) {
            field = value
            invalidate()
        }

    var onProgressChanged: ((Int, Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.parseColor("#35FFFFFF")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val rect = RectF()
    private val fillRect = RectF()
    private val clipPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        rect.set(0f, 0f, w, h)
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)

        val saveCount = canvas.save()
        canvas.clipPath(clipPath)

        // 1. Nền rãnh trượt (Frosted glass track)
        trackPaint.color = trackColor
        canvas.drawRect(rect, trackPaint)

        // 2. Mức fill từ dưới lên
        val fillHeight = (progress / 100f) * h
        fillRect.set(0f, h - fillHeight, w, h)
        fillPaint.color = fillColor
        canvas.drawRect(fillRect, fillPaint)

        canvas.restoreToCount(saveCount)

        // 3. Viền bo tròn tinh tế
        rect.inset(1.5f, 1.5f)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, borderPaint)

        // 4. Icon & phần trăm ở dưới đáy thanh trượt
        val density = resources.displayMetrics.density
        textPaint.textSize = 20f * density
        textPaint.color = if (progress > 26) iconColor else Color.WHITE
        canvas.drawText(iconLabel, w / 2f, h - 38f * density, textPaint)

        subTextPaint.textSize = 11.5f * density
        subTextPaint.color = if (progress > 15) iconColor else Color.parseColor("#CCFFFFFF")
        canvas.drawText("$progress%", w / 2f, h - 18f * density, subTextPaint)

        if (titleLabel.isNotEmpty()) {
            subTextPaint.textSize = 10.5f * density
            subTextPaint.color = Color.parseColor("#B0FFFFFF")
            canvas.drawText(titleLabel, w / 2f, 22f * density, subTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val ratio = 1f - (event.y / height.toFloat()).coerceIn(0f, 1f)
                val newProgress = (ratio * 100f).toInt().coerceIn(0, 100)
                if (newProgress != progress) {
                    progress = newProgress
                    onProgressChanged?.invoke(progress, true)
                }
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onProgressChanged?.invoke(progress, false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

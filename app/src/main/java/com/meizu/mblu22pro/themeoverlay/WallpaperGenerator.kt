package com.meizu.mblu22pro.themeoverlay

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build

object WallpaperGenerator {

    /**
     * Tạo hình nền độ phân giải gốc 720x1640 chuẩn màn hình 6.79" Meizu mblu 22 Pro
     * với hiệu ứng dải lụa ánh sáng iOS 18 & HyperOS 2.0
     */
    fun generateWallpaperBitmap(preset: ThemePreset, width: Int = 720, height: Int = 1640): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val colors = preset.wallpaperColors

        // 1. Lớp nền chuyển sắc sâu từ đỉnh xuống đáy
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(colors[0], colors[1], colors[0]),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Vầng sáng Radial Glow đặc trưng HyperOS / iOS 18
        val glow1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.8f, height * 0.28f, width * 0.85f,
                intArrayOf(
                    withAlpha(colors[2], 210),
                    withAlpha(colors[2], 70),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow1)

        val glow2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.2f, height * 0.72f, width * 0.9f,
                intArrayOf(
                    withAlpha(colors[3], 195),
                    withAlpha(colors[1], 80),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow2)

        // 3. Cánh hoa / Dải sóng lụa iOS 18 (Curved Glass Ribbons)
        val ribbonPath1 = Path().apply {
            moveTo(-width * 0.2f, height * 0.35f)
            cubicTo(
                width * 0.25f, height * 0.18f,
                width * 0.65f, height * 0.52f,
                width * 1.2f, height * 0.30f
            )
            lineTo(width * 1.2f, height * 0.78f)
            cubicTo(
                width * 0.70f, height * 0.62f,
                width * 0.30f, height * 0.85f,
                -width * 0.2f, height * 0.65f
            )
            close()
        }
        val ribbonPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, height * 0.2f, width.toFloat(), height * 0.8f,
                intArrayOf(
                    withAlpha(colors[2], 140),
                    withAlpha(colors[3], 95),
                    withAlpha(Color.WHITE, 30)
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(ribbonPath1, ribbonPaint1)

        // 4. Đường viền sáng mảnh (Specular Glass Highlight)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            shader = LinearGradient(
                0f, height * 0.25f, width.toFloat(), height * 0.45f,
                intArrayOf(
                    Color.TRANSPARENT,
                    withAlpha(Color.WHITE, 130),
                    withAlpha(colors[2], 150),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.4f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        val highlightCurve = Path().apply {
            moveTo(-width * 0.1f, height * 0.36f)
            cubicTo(
                width * 0.28f, height * 0.19f,
                width * 0.66f, height * 0.53f,
                width * 1.1f, height * 0.31f
            )
        }
        canvas.drawPath(highlightCurve, strokePaint)

        return bitmap
    }

    fun applyThemeWallpaper(
        context: Context,
        preset: ThemePreset,
        applyHome: Boolean = true,
        applyLock: Boolean = true
    ): Boolean {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val bmp = generateWallpaperBitmap(preset, 720, 1640)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                var flags = 0
                if (applyHome) flags = flags or WallpaperManager.FLAG_SYSTEM
                if (applyLock) flags = flags or WallpaperManager.FLAG_LOCK
                if (flags == 0) flags = WallpaperManager.FLAG_SYSTEM
                wm.setBitmap(bmp, null, true, flags)
            } else {
                wm.setBitmap(bmp)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}

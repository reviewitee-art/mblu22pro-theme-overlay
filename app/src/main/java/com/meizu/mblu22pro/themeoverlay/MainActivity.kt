package com.meizu.mblu22pro.themeoverlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private var statusBadgeTv: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainer = FrameLayout(this)
        setContentView(rootContainer)
        renderDashboard()
    }

    override fun onResume() {
        super.onResume()
        renderDashboard()
    }

    private fun renderDashboard() {
        rootContainer.removeAllViews()
        val preset = ThemeConfig.getCurrentPreset(this)
        rootContainer.setBackgroundColor(Color.parseColor("#080B14"))

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }

        val mainCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(40))
        }

        // =====================================================
        // 1. HERO BANNER: MEIZU MBLU 22 PRO THEME STUDIO
        // =====================================================
        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    preset.wallpaperColors[1],
                    Color.parseColor("#12182B"),
                    Color.parseColor("#0D1220")
                )
            ).apply {
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1.5f), withAlpha(preset.primaryColor, 170))
            }
        }

        val tagTv = TextView(this).apply {
            text = "MEIZU MBLU 22 PRO • ANDROID 15 • NO ROOT"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(preset.primaryColor)
        }
        val titleTv = TextView(this).apply {
            text = "iOS 18 & HyperOS\nSystem UI Overlay"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(6))
        }
        val descTv = TextView(this).apply {
            text = "Đổi Trung tâm điều khiển, Dynamic Island quanh camera đục lỗ, Thanh trạng thái, Volume HUD, Hiệu ứng sạc & Hình nền đồng bộ."
            textSize = 13f
            setTextColor(Color.parseColor("#B8C4E0"))
        }

        val serviceActive = isAccessibilityServiceEnabled()
        val statusTv = TextView(this).apply {
            text = if (serviceActive) {
                "● ĐANG HOẠT ĐỘNG — Vuốt góc trên bên phải xuống để mở Control Center"
            } else {
                "⚠ CHƯA BẬT DỊCH VỤ TRỢ NĂNG — Hãy hoàn tất 3 bước cấp quyền bên dưới"
            }
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (serviceActive) preset.accentGreen else preset.accentOrange)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundRect(Color.parseColor("#26FFFFFF"), 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }
        statusBadgeTv = statusTv

        heroCard.addView(tagTv)
        heroCard.addView(titleTv)
        heroCard.addView(descTv)
        heroCard.addView(statusTv)
        mainCol.addView(heroCard)

        // =====================================================
        // 2. DÙNG THỬ NHANH TRÊN MÀN HÌNH (QUICK TEST BAR)
        // =====================================================
        mainCol.addView(makeSectionHeader("⚡ KIỂM TRA & MỞ NHANH GIAO DIỆN"))

        val testGrid1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        testGrid1.addView(
            makeActionBtn("🎛 Mở Control Center", preset.primaryColor) {
                if (ensureServiceReady()) {
                    sendBroadcast(Intent(ThemeConfig.ACTION_OPEN_CONTROL_CENTER))
                }
            }
        )
        testGrid1.addView(
            makeActionBtn("💊 Bung Dynamic Island", preset.secondaryColor) {
                if (ensureServiceReady()) {
                    sendBroadcast(Intent(ThemeConfig.ACTION_EXPAND_ISLAND))
                }
            }
        )
        mainCol.addView(testGrid1)

        val testGrid2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
        testGrid2.addView(
            makeActionBtn("🔊 Thử Volume HUD", Color.parseColor("#24304C")) {
                if (ensureServiceReady()) {
                    sendBroadcast(Intent(ThemeConfig.ACTION_TEST_VOLUME_HUD))
                }
            }
        )
        testGrid2.addView(
            makeActionBtn("⚡ Thử Hiệu ứng Sạc", Color.parseColor("#24304C")) {
                if (ensureServiceReady()) {
                    sendBroadcast(Intent(ThemeConfig.ACTION_TEST_CHARGING))
                }
            }
        )
        mainCol.addView(testGrid2)

        // =====================================================
        // 3. CẤP QUYỀN ANDROID 15 CHO MEIZU MBLU 22 PRO
        // =====================================================
        mainCol.addView(makeSectionHeader("🔐 CẤP QUYỀN HỆ THỐNG (ANDROID 15)"))

        val permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundRect(Color.parseColor("#131A2C"), 24, stroke = true)
        }

        // Hướng dẫn mở khóa Cài đặt bị hạn chế trên Android 15
        permCard.addView(
            makePermissionRow(
                title = "Bước 1: Mở khóa 'Cài đặt bị hạn chế' (Android 15)",
                subtitle = "Bấm vào đây -> Chạm biểu tượng ⋮ góc trên phải -> Chọn 'Cho phép cài đặt bị hạn chế'",
                done = serviceActive,
                btnText = "Mở TT Ứng dụng",
                accent = preset.accentOrange
            ) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        )

        permCard.addView(makeDivider())

        // Bật Accessibility Service
        permCard.addView(
            makePermissionRow(
                title = "Bước 2: Bật Dịch vụ Trợ năng (System UI Overlay)",
                subtitle = if (serviceActive) "Đã kích hoạt thành công!" else "Tìm 'mblu 22 Pro iOS 18 / HyperOS System UI' và gạt Bật",
                done = serviceActive,
                btnText = if (serviceActive) "Đã bật ✓" else "Bật ngay",
                accent = preset.primaryColor
            ) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        permCard.addView(makeDivider())

        // Quyền sửa cài đặt hệ thống (Độ sáng & Xoay màn hình)
        val canWrite = Settings.System.canWrite(this)
        permCard.addView(
            makePermissionRow(
                title = "Bước 3: Quyền chỉnh Độ sáng & Xoay màn hình",
                subtitle = if (canWrite) "Đã cấp quyền chỉnh thanh trượt độ sáng" else "Cần thiết để kéo thanh Độ sáng trong Control Center",
                done = canWrite,
                btnText = if (canWrite) "Đã cấp ✓" else "Cấp quyền",
                accent = preset.secondaryColor
            ) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        )

        permCard.addView(makeDivider())

        // Quyền chạy nền không giới hạn pin
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBattery = pm.isIgnoringBatteryOptimizations(packageName)
        permCard.addView(
            makePermissionRow(
                title = "Bước 4: Giữ Theme luôn chạy 24/7 trên Flyme OS",
                subtitle = if (ignoringBattery) "Đã tắt giới hạn pin nền" else "Ngăn hệ thống tự tắt Dynamic Island & Control Center",
                done = ignoringBattery,
                btnText = if (ignoringBattery) "Tối ưu ✓" else "Cho phép",
                accent = preset.accentGreen
            ) {
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        )

        mainCol.addView(permCard)

        // =====================================================
        // 4. BỘ SƯU TẬP CHỦ ĐỀ iOS 18 & HYPEROS + HÌNH NỀN
        // =====================================================
        mainCol.addView(makeSectionHeader("🎨 BỘ CHỦ ĐỀ iOS 18 / HYPEROS & HÌNH NỀN HD+"))

        for (item in ThemeConfig.PRESETS) {
            val isSelected = item.id == preset.id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
                background = GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(Color.parseColor("#131A2C"))
                    setStroke(
                        dp(if (isSelected) 2f else 1f),
                        if (isSelected) item.primaryColor else Color.parseColor("#25FFFFFF")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            // Hàng tiêu đề + xem trước hình nền thu nhỏ
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val previewThumb = ImageView(this).apply {
                val bmp = WallpaperGenerator.generateWallpaperBitmap(item, 140, 260)
                setImageBitmap(bmp)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(88)).apply {
                    marginEnd = dp(14)
                }
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(this).apply {
                text = if (isSelected) "✓ ${item.name} (Đang dùng)" else item.name
                textSize = 15.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isSelected) item.primaryColor else Color.WHITE)
            }
            val subTv = TextView(this).apply {
                text = item.subtitle
                textSize = 12f
                setTextColor(Color.parseColor("#A8B5D1"))
                setPadding(0, dp(3), 0, dp(8))
            }

            // Các chấm màu chủ đạo
            val swatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            listOf(item.primaryColor, item.secondaryColor, item.accentGreen, item.accentOrange).forEach { c ->
                val dot = View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(c)
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                        marginEnd = dp(8)
                    }
                }
                swatchRow.addView(dot)
            }

            infoCol.addView(nameTv)
            infoCol.addView(subTv)
            infoCol.addView(swatchRow)
            topRow.addView(previewThumb)
            topRow.addView(infoCol)
            card.addView(topRow)

            // Nút áp dụng Theme & Hình nền
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            }

            val applyUiBtn = TextView(this).apply {
                text = if (isSelected) "Đã áp dụng UI" else "Áp dụng Giao diện"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundRect(item.primaryColor, 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(6)
                }
                setOnClickListener {
                    ThemeConfig.setCurrentPreset(this@MainActivity, item.id)
                    notifyOverlayUpdated()
                    renderDashboard()
                    Toast.makeText(
                        this@MainActivity,
                        "Đã áp dụng giao diện ${item.name}!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val applyFullBtn = TextView(this).apply {
                text = "🎨 Đổi cả Hình nền + UI"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(dp(12), dp(10), dp(12), dp(10))
                background = roundRect(Color.parseColor("#283556"), 16)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(6)
                }
                setOnClickListener {
                    ThemeConfig.setCurrentPreset(this@MainActivity, item.id)
                    val ok = WallpaperGenerator.applyThemeWallpaper(
                        this@MainActivity,
                        item,
                        applyHome = true,
                        applyLock = true
                    )
                    notifyOverlayUpdated()
                    renderDashboard()
                    val msg = if (ok) {
                        "Đã đồng bộ Hình nền HD+ & Giao diện ${item.name}!"
                    } else {
                        "Đã đổi Giao diện ${item.name}!"
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            btnRow.addView(applyUiBtn)
            btnRow.addView(applyFullBtn)
            card.addView(btnRow)
            mainCol.addView(card)
        }

        // =====================================================
        // 5. BẬT / TẮT TỪNG THÀNH PHẦN SYSTEM UI
        // =====================================================
        mainCol.addView(makeSectionHeader("⚙️ TÙY CHỌN THÀNH PHẦN SYSTEM UI"))

        val toggleCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundRect(Color.parseColor("#131A2C"), 24, stroke = true)
        }

        toggleCard.addView(
            makeSwitchRow(
                "🎛 Trung tâm điều khiển (Control Center)",
                "Vuốt từ góc trên bên phải màn hình xuống để mở",
                ThemeConfig.isControlCenterEnabled(this)
            ) { checked ->
                ThemeConfig.setControlCenterEnabled(this, checked)
                notifyOverlayUpdated()
            }
        )
        toggleCard.addView(makeDivider())

        toggleCard.addView(
            makeSwitchRow(
                "💊 Đảo thích ứng (Dynamic Island)",
                "Hiển thị quanh camera đục lỗ của Meizu mblu 22 Pro",
                ThemeConfig.isDynamicIslandEnabled(this)
            ) { checked ->
                ThemeConfig.setDynamicIslandEnabled(this, checked)
                notifyOverlayUpdated()
            }
        )
        toggleCard.addView(makeDivider())

        toggleCard.addView(
            makeSwitchRow(
                "📶 Thanh trạng thái iOS 18 (Custom Status Bar)",
                "Đè thanh trạng thái phong cách iOS 18 lên mép trên",
                ThemeConfig.isCustomStatusBarEnabled(this)
            ) { checked ->
                ThemeConfig.setCustomStatusBarEnabled(this, checked)
                notifyOverlayUpdated()
            }
        )
        toggleCard.addView(makeDivider())

        toggleCard.addView(
            makeSwitchRow(
                "🔊 Thanh trượt âm lượng nổi (Volume HUD)",
                "Thay thế thanh âm lượng gốc khi bấm phím cứng",
                ThemeConfig.isVolumeHudEnabled(this)
            ) { checked ->
                ThemeConfig.setVolumeHudEnabled(this, checked)
                notifyOverlayUpdated()
            }
        )
        toggleCard.addView(makeDivider())

        toggleCard.addView(
            makeSwitchRow(
                "⚡ Hiệu ứng sạc pin MagSafe / HyperCharge",
                "Tự động hiển thị vòng sạc 18W khi cắm cáp sạc",
                ThemeConfig.isChargingAnimEnabled(this)
            ) { checked ->
                ThemeConfig.setChargingAnimEnabled(this, checked)
                notifyOverlayUpdated()
            }
        )

        mainCol.addView(toggleCard)

        // =====================================================
        // 6. CĂN CHỈNH DYNAMIC ISLAND CHO CAMERA ĐỤC LỖ MBLU 22 PRO
        // =====================================================
        mainCol.addView(makeSectionHeader("📐 CĂN CHỈNH DYNAMIC ISLAND (MÀN 6.79\" MBLU 22 PRO)"))

        val calibCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundRect(Color.parseColor("#131A2C"), 24, stroke = true)
        }

        calibCard.addView(
            makeSeekBarControl(
                label = "Chiều rộng Dynamic Island",
                unit = "dp",
                min = 95,
                max = 200,
                current = ThemeConfig.getIslandWidthDp(this)
            ) { newVal ->
                ThemeConfig.setIslandWidthDp(this, newVal)
                notifyOverlayUpdated()
            }
        )

        calibCard.addView(
            makeSeekBarControl(
                label = "Chiều cao Dynamic Island",
                unit = "dp",
                min = 26,
                max = 48,
                current = ThemeConfig.getIslandHeightDp(this)
            ) { newVal ->
                ThemeConfig.setIslandHeightDp(this, newVal)
                notifyOverlayUpdated()
            }
        )

        calibCard.addView(
            makeSeekBarControl(
                label = "Khoảng cách từ mép trên (Y Offset)",
                unit = "dp",
                min = 0,
                max = 30,
                current = ThemeConfig.getIslandTopOffsetDp(this)
            ) { newVal ->
                ThemeConfig.setIslandTopOffsetDp(this, newVal)
                notifyOverlayUpdated()
            }
        )

        val resetCalibBtn = TextView(this).apply {
            text = "Khôi phục thông số chuẩn Meizu mblu 22 Pro (128 x 34dp, Offset 6dp)"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(preset.primaryColor)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundRect(Color.parseColor("#1D2742"), 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
            setOnClickListener {
                ThemeConfig.setIslandWidthDp(this@MainActivity, 128)
                ThemeConfig.setIslandHeightDp(this@MainActivity, 34)
                ThemeConfig.setIslandTopOffsetDp(this@MainActivity, 6)
                notifyOverlayUpdated()
                renderDashboard()
            }
        }
        calibCard.addView(resetCalibBtn)
        mainCol.addView(calibCard)

        scroll.addView(mainCol)
        rootContainer.addView(scroll)
    }

    private fun ensureServiceReady(): Boolean {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "Hãy bật 'mblu 22 Pro iOS 18 / HyperOS System UI' trong Cài đặt Trợ năng trước nhé!",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return false
        }
        return true
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        if (OverlayAccessibilityService.isRunning) return true
        val expected = "$packageName/${OverlayAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expected) ||
            enabledServices.contains(OverlayAccessibilityService::class.java.simpleName)
    }

    private fun notifyOverlayUpdated() {
        sendBroadcast(Intent(ThemeConfig.ACTION_THEME_UPDATED).setPackage(packageName))
        sendBroadcast(Intent(ThemeConfig.ACTION_THEME_UPDATED))
    }

    // =====================================================
    // UI COMPONENT HELPERS
    // =====================================================
    private fun makeSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#8FA2C7"))
            setPadding(dp(4), dp(22), dp(4), dp(10))
        }
    }

    private fun makeActionBtn(title: String, bgColor: Int, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            background = roundRect(bgColor, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun makePermissionRow(
        title: String,
        subtitle: String,
        done: Boolean,
        btnText: String,
        accent: Int,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(10)
                }
            }
            val tTv = TextView(context).apply {
                text = title
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            val sTv = TextView(context).apply {
                text = subtitle
                textSize = 11.5f
                setTextColor(Color.parseColor("#9AA8C7"))
                setPadding(0, dp(2), 0, 0)
            }
            textCol.addView(tTv)
            textCol.addView(sTv)

            val btn = TextView(context).apply {
                text = btnText
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = roundRect(
                    if (done) Color.parseColor("#234E37") else accent,
                    14
                )
                setOnClickListener { onClick() }
            }

            addView(textCol)
            addView(btn)
        }
    }

    private fun makeSwitchRow(
        title: String,
        subtitle: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))

            val textCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(10)
                }
            }
            val tTv = TextView(context).apply {
                text = title
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            val sTv = TextView(context).apply {
                text = subtitle
                textSize = 11.5f
                setTextColor(Color.parseColor("#9AA8C7"))
            }
            textCol.addView(tTv)
            textCol.addView(sTv)

            val sw = Switch(context).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, isChecked -> onChanged(isChecked) }
            }

            addView(textCol)
            addView(sw)
        }
    }

    private fun makeSeekBarControl(
        label: String,
        unit: String,
        min: Int,
        max: Int,
        current: Int,
        onValueChange: (Int) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))

            val header = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val lb = TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valTv = TextView(context).apply {
                text = "$current $unit"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#0A84FF"))
            }
            header.addView(lb)
            header.addView(valTv)

            val seek = SeekBar(context).apply {
                this.max = max - min
                this.progress = (current - min).coerceIn(0, max - min)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val realVal = min + progress
                        valTv.text = "$realVal $unit"
                        if (fromUser) onValueChange(realVal)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            addView(header)
            addView(seek)
        }
    }

    private fun makeDivider(): View {
        return View(this).apply {
            setBackgroundColor(Color.parseColor("#1F2942"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        }
    }

    private fun roundRect(color: Int, radiusDp: Int, stroke: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            if (stroke) {
                setStroke(dp(1), Color.parseColor("#2AFFFFFF"))
            }
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()
}

package com.meizu.mblu22pro.themeoverlay

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var sysController: SystemController
    private val mainHandler = Handler(Looper.getMainLooper())

    // Overlay views
    private var dynamicIslandRoot: FrameLayout? = null
    private var dynamicIslandParams: WindowManager.LayoutParams? = null
    private var isIslandExpanded = false

    private var topRightTriggerView: View? = null
    private var statusBarOverlayView: FrameLayout? = null

    private var controlCenterRoot: FrameLayout? = null
    private var isControlCenterVisible = false

    private var volumeHudRoot: FrameLayout? = null
    private var volumeSliderView: VerticalCapsuleSliderView? = null
    private var isVolumeHudVisible = false

    private var chargingOverlayRoot: FrameLayout? = null
    private var isChargingOverlayVisible = false

    private val hideVolumeRunnable = Runnable { hideVolumeHud() }
    private val hideChargingRunnable = Runnable { hideChargingAnimation() }
    private val collapseIslandRunnable = Runnable {
        if (isIslandExpanded) setDynamicIslandExpanded(false)
    }

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ThemeConfig.ACTION_THEME_UPDATED -> refreshAllOverlays()
                ThemeConfig.ACTION_OPEN_CONTROL_CENTER -> showControlCenter()
                ThemeConfig.ACTION_EXPAND_ISLAND -> {
                    if (!ThemeConfig.isDynamicIslandEnabled(this@OverlayAccessibilityService)) {
                        ThemeConfig.setDynamicIslandEnabled(this@OverlayAccessibilityService, true)
                        refreshAllOverlays()
                    }
                    setDynamicIslandExpanded(true)
                }
                ThemeConfig.ACTION_TEST_VOLUME_HUD -> showVolumeHud()
                ThemeConfig.ACTION_TEST_CHARGING -> showChargingAnimation()
                Intent.ACTION_POWER_CONNECTED -> {
                    if (ThemeConfig.isChargingAnimEnabled(this@OverlayAccessibilityService)) {
                        showChargingAnimation()
                    }
                    if (ThemeConfig.isDynamicIslandEnabled(this@OverlayAccessibilityService)) {
                        setDynamicIslandExpanded(true)
                    }
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    if (ThemeConfig.isVolumeHudEnabled(this@OverlayAccessibilityService) && !isControlCenterVisible) {
                        showVolumeHud()
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sysController = SystemController(this)

        val filter = IntentFilter().apply {
            addAction(ThemeConfig.ACTION_THEME_UPDATED)
            addAction(ThemeConfig.ACTION_OPEN_CONTROL_CENTER)
            addAction(ThemeConfig.ACTION_EXPAND_ISLAND)
            addAction(ThemeConfig.ACTION_TEST_VOLUME_HUD)
            addAction(ThemeConfig.ACTION_TEST_CHARGING)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(systemReceiver, filter)
        }

        refreshAllOverlays()
        Toast.makeText(
            this,
            "Đã kích hoạt iOS 18 / HyperOS System UI cho Meizu mblu 22 Pro!",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Khi có thông báo mới và Dynamic Island đang bật, nháy nhẹ đảo thích ứng
        if (event?.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != packageName && ThemeConfig.isDynamicIslandEnabled(this) && !isIslandExpanded) {
                pulseDynamicIsland()
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!ThemeConfig.isVolumeHudEnabled(this)) {
            return super.onKeyEvent(event)
        }
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                val dir = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                sysController.adjustVolumeStep(dir)
                showVolumeHud()
            }
            return true // Chặn thanh âm lượng mặc định của máy, hiển thị Volume HUD iOS 18 / HyperOS
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isRunning = false
        try {
            unregisterReceiver(systemReceiver)
        } catch (_: Exception) {
        }
        if (::sysController.isInitialized) {
            sysController.release()
        }
        removeAllOverlays()
        super.onDestroy()
    }

    private fun refreshAllOverlays() {
        removeAllOverlays()
        if (ThemeConfig.isCustomStatusBarEnabled(this)) {
            setupCustomStatusBarOverlay()
        }
        if (ThemeConfig.isDynamicIslandEnabled(this)) {
            setupDynamicIslandOverlay()
        }
        if (ThemeConfig.isControlCenterEnabled(this)) {
            setupTopRightSwipeTrigger()
        }
    }

    private fun removeAllOverlays() {
        safeRemoveView(dynamicIslandRoot)
        dynamicIslandRoot = null
        isIslandExpanded = false

        safeRemoveView(topRightTriggerView)
        topRightTriggerView = null

        safeRemoveView(statusBarOverlayView)
        statusBarOverlayView = null

        hideControlCenterImmediate()
        hideVolumeHud()
        hideChargingAnimation()
    }

    // =========================================================================
    // 1. DYNAMIC ISLAND (ĐẢO THÍCH ỨNG QUANH CAMERA ĐỤC LỖ MEIZU MBLU 22 PRO)
    // =========================================================================
    private fun setupDynamicIslandOverlay() {
        val preset = ThemeConfig.getCurrentPreset(this)
        val compactW = dp(ThemeConfig.getIslandWidthDp(this))
        val compactH = dp(ThemeConfig.getIslandHeightDp(this))
        val topMargin = dp(ThemeConfig.getIslandTopOffsetDp(this))

        val root = FrameLayout(this)
        val params = WindowManager.LayoutParams(
            compactW,
            compactH,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topMargin
        }

        renderDynamicIslandContent(root, preset, expanded = false)

        var downY = 0f
        var downX = 0f
        root.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dy = ev.rawY - downY
                    val dx = kotlin.math.abs(ev.rawX - downX)
                    if (dy > dp(28) && dx < dp(90)) {
                        // Vuốt xuống từ Dynamic Island -> Mở Control Center
                        setDynamicIslandExpanded(false)
                        showControlCenter()
                    } else {
                        // Chạm nhẹ -> Mở rộng / Thu gọn Dynamic Island
                        setDynamicIslandExpanded(!isIslandExpanded)
                    }
                    true
                }
                else -> false
            }
        }

        dynamicIslandRoot = root
        dynamicIslandParams = params
        safeAddView(root, params)
    }

    private fun renderDynamicIslandContent(
        container: FrameLayout,
        preset: ThemePreset,
        expanded: Boolean
    ) {
        container.removeAllViews()
        val (batteryPct, isCharging) = sysController.getBatteryInfo()

        if (!expanded) {
            container.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(50).toFloat()
                setColor(Color.parseColor("#F5050608"))
                setStroke(dp(1), withAlpha(preset.primaryColor, 150))
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), 0, dp(10), 0)
            }

            // Chấm trạng thái / Sóng nhạc bên trái
            val leftIndicator = TextView(this).apply {
                text = if (isCharging) "⚡" else "●"
                textSize = 10f
                setTextColor(if (isCharging) preset.accentGreen else preset.primaryColor)
            }
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            val rightText = TextView(this).apply {
                text = "$batteryPct%"
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(
                    if (isCharging) preset.accentGreen else Color.parseColor("#E0FFFFFF")
                )
            }

            row.addView(leftIndicator)
            row.addView(spacer)
            row.addView(rightText)
            container.addView(
                row,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        } else {
            // Giao diện Dynamic Island mở rộng chuẩn iOS 18
            container.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(34).toFloat()
                setColor(Color.parseColor("#F20A0D16"))
                setStroke(dp(1.5f), withAlpha(preset.primaryColor, 200))
            }

            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            // Hàng 1: Tiêu đề + Phần trăm pin
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val titleTv = TextView(this).apply {
                text = "🎵 ${preset.name}"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val batBadge = TextView(this).apply {
                text = if (isCharging) "⚡ $batteryPct%" else "🔋 $batteryPct%"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isCharging) preset.accentGreen else preset.primaryColor)
            }
            topRow.addView(titleTv)
            topRow.addView(batBadge)
            col.addView(topRow)

            // Hàng 2: Điều khiển nhạc & Tác vụ nhanh
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(12)
                }
            }

            fun makeIslandPill(label: String, bgColor: Int, onClick: () -> Unit): TextView {
                return TextView(this).apply {
                    text = label
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(18).toFloat()
                        setColor(bgColor)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dp(4)
                        marginEnd = dp(4)
                    }
                    setOnClickListener { onClick() }
                }
            }

            actionRow.addView(
                makeIslandPill("⏮", Color.parseColor("#28FFFFFF")) {
                    sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
            )
            actionRow.addView(
                makeIslandPill("⏯ Phát", preset.primaryColor) {
                    sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
            )
            actionRow.addView(
                makeIslandPill("⏭", Color.parseColor("#28FFFFFF")) {
                    sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
                }
            )
            actionRow.addView(
                makeIslandPill("🎛 Trung tâm", preset.secondaryColor) {
                    setDynamicIslandExpanded(false)
                    showControlCenter()
                }
            )
            col.addView(actionRow)

            // Hàng 3: Phím tắt nhanh Đèn pin / Chụp màn hình / Khóa
            val bottomRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            }
            bottomRow.addView(
                makeIslandPill(
                    if (sysController.isTorchOn) "🔦 Tắt Đèn" else "🔦 Đèn Pin",
                    if (sysController.isTorchOn) preset.accentOrange else Color.parseColor("#22FFFFFF")
                ) {
                    sysController.toggleTorch()
                    renderDynamicIslandContent(container, preset, true)
                }
            )
            bottomRow.addView(
                makeIslandPill("📸 Chụp MH", Color.parseColor("#22FFFFFF")) {
                    setDynamicIslandExpanded(false)
                    mainHandler.postDelayed({
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                    }, 220)
                }
            )
            bottomRow.addView(
                makeIslandPill("🔒 Khóa máy", Color.parseColor("#22FFFFFF")) {
                    setDynamicIslandExpanded(false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                }
            )
            col.addView(bottomRow)

            container.addView(
                col,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun setDynamicIslandExpanded(expanded: Boolean) {
        val root = dynamicIslandRoot ?: return
        val params = dynamicIslandParams ?: return
        val preset = ThemeConfig.getCurrentPreset(this)
        isIslandExpanded = expanded
        mainHandler.removeCallbacks(collapseIslandRunnable)

        val targetW = if (expanded) dp(340) else dp(ThemeConfig.getIslandWidthDp(this))
        val targetH = if (expanded) dp(156) else dp(ThemeConfig.getIslandHeightDp(this))

        val startW = params.width
        val startH = params.height

        renderDynamicIslandContent(root, preset, expanded)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = OvershootInterpolator(0.9f)
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                params.width = (startW + (targetW - startW) * f).toInt()
                params.height = (startH + (targetH - startH) * f).toInt()
                safeUpdateView(root, params)
            }
            start()
        }

        if (expanded) {
            mainHandler.postDelayed(collapseIslandRunnable, 6000)
        }
    }

    private fun pulseDynamicIsland() {
        val root = dynamicIslandRoot ?: return
        val params = dynamicIslandParams ?: return
        val baseW = dp(ThemeConfig.getIslandWidthDp(this))
        ValueAnimator.ofInt(baseW, baseW + dp(26), baseW).apply {
            duration = 450
            addUpdateListener {
                params.width = it.animatedValue as Int
                safeUpdateView(root, params)
            }
            start()
        }
    }

    // =========================================================================
    // 2. THANH TRẠNG THÁI TÙY CHỈNH (CUSTOM STATUS BAR iOS 18)
    // =========================================================================
    private fun setupCustomStatusBarOverlay() {
        val preset = ThemeConfig.getCurrentPreset(this)
        val bar = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#88070A12"))
            setPadding(dp(18), dp(4), dp(18), dp(4))
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val leftClock = TextView(this).apply {
            text = timeFormat.format(Date())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val (batPct, charging) = sysController.getBatteryInfo()
        val rightStatus = TextView(this).apply {
            text = "5G  Wi-Fi  ${if (charging) "⚡" else ""}$batPct%"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(preset.textPrimary)
        }

        bar.addView(
            leftClock,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            )
        )
        bar.addView(
            rightStatus,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(32),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        statusBarOverlayView = bar
        safeAddView(bar, params)
    }

    // =========================================================================
    // 3. VÙNG VUỐT GÓC TRÊN BÊN PHẢI & CONTROL CENTER iOS 18 / HYPEROS
    // =========================================================================
    private fun setupTopRightSwipeTrigger() {
        val trigger = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        var startY = 0f
        trigger.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.rawY - startY > dp(24)) {
                        showControlCenter()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (event.rawY - startY > dp(16)) {
                        showControlCenter()
                    }
                    true
                }
                else -> false
            }
        }

        val params = WindowManager.LayoutParams(
            dp(125),
            dp(30),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 0
        }

        topRightTriggerView = trigger
        safeAddView(trigger, params)
    }

    private fun showControlCenter() {
        if (isControlCenterVisible) return
        isControlCenterVisible = true

        val preset = ThemeConfig.getCurrentPreset(this)
        val root = FrameLayout(this).apply {
            setBackgroundColor(preset.panelBgColor)
            alpha = 0f
        }

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(42), dp(20), dp(34))
        }

        // --- HEADER: Ngày giờ + Pin + Nút đóng ---
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dateCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val clockTv = TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(preset.textPrimary)
        }
        val dateSubTv = TextView(this).apply {
            val df = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
            text = "${df.format(Date())} • ${preset.name}"
            textSize = 12.5f
            setTextColor(preset.textSecondary)
        }
        dateCol.addView(clockTv)
        dateCol.addView(dateSubTv)

        val (batPct, isCharging) = sysController.getBatteryInfo()
        val batteryPill = TextView(this).apply {
            text = "${if (isCharging) "⚡" else "🔋"} $batPct%"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundRect(preset.primaryColor, preset.cornerRadiusDp)
        }

        headerRow.addView(dateCol)
        headerRow.addView(batteryPill)
        content.addView(headerRow)

        // --- HÀNG 1: Cụm Kết nối 2x2 (Trái) & Trình phát nhạc iOS 18 (Phải) ---
        val topGridRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(168)
            ).apply {
                topMargin = dp(18)
            }
        }

        // Card Trái: Cụm Kết nối (Wi-Fi, Dữ liệu 4G, Bluetooth, Máy bay)
        val connCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundRect(preset.cardBgColor, preset.cornerRadiusDp, stroke = true)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(8)
            }
        }

        val wifiOn = sysController.isWifiEnabled()
        val cellOn = sysController.isCellularConnected()
        val airOn = sysController.isAirplaneModeOn()

        val connRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = dp(6)
            }
        }
        connRow1.addView(
            makeRoundToggle(
                icon = "📶",
                label = "Wi-Fi",
                active = wifiOn,
                activeColor = preset.primaryColor
            ) {
                hideControlCenter()
                sysController.openWifiPanel()
            }
        )
        connRow1.addView(
            makeRoundToggle(
                icon = "⇅",
                label = "4G LTE",
                active = cellOn,
                activeColor = preset.accentGreen
            ) {
                hideControlCenter()
                sysController.openInternetPanel()
            }
        )

        val connRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        connRow2.addView(
            makeRoundToggle(
                icon = "ᛒ",
                label = "Bluetooth",
                active = true,
                activeColor = preset.secondaryColor
            ) {
                hideControlCenter()
                sysController.openBluetoothSettings()
            }
        )
        connRow2.addView(
            makeRoundToggle(
                icon = "✈️",
                label = "Máy bay",
                active = airOn,
                activeColor = preset.accentOrange
            ) {
                hideControlCenter()
                sysController.openAirplaneSettings()
            }
        )
        connCard.addView(connRow1)
        connCard.addView(connRow2)

        // Card Phải: Media Player iOS 18 / HyperOS
        val mediaCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundRect(preset.cardBgColor, preset.cornerRadiusDp, stroke = true)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(8)
            }
        }
        val mediaBadge = TextView(this).apply {
            text = "🎵 TRÌNH PHÁT NHẠC"
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(preset.primaryColor)
        }
        val mediaTitle = TextView(this).apply {
            text = if (sysController.isMusicActive()) "Đang phát âm thanh" else "Sẵn sàng phát nhạc"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(6), 0, dp(2))
        }
        val mediaSub = TextView(this).apply {
            text = "Meizu mblu 22 Pro Audio"
            textSize = 11.5f
            setTextColor(preset.textSecondary)
        }
        val mediaControlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }
        fun makeMediaBtn(symbol: String, highlight: Boolean, action: () -> Unit): TextView {
            return TextView(this).apply {
                text = symbol
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = roundRect(
                    if (highlight) preset.primaryColor else Color.parseColor("#2AFFFFFF"),
                    18
                )
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginStart = dp(3)
                    marginEnd = dp(3)
                }
                setOnClickListener { action() }
            }
        }
        mediaControlsRow.addView(makeMediaBtn("⏮", false) {
            sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        })
        mediaControlsRow.addView(makeMediaBtn("⏯", true) {
            sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            mediaTitle.text = "Đang điều khiển nhạc"
        })
        mediaControlsRow.addView(makeMediaBtn("⏭", false) {
            sysController.sendMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
        })

        mediaCard.addView(mediaBadge)
        mediaCard.addView(mediaTitle)
        mediaCard.addView(mediaSub)
        mediaCard.addView(mediaControlsRow)

        topGridRow.addView(connCard)
        topGridRow.addView(mediaCard)
        content.addView(topGridRow)

        // --- HÀNG 2: Cụm 2 Thanh trượt dọc (Độ sáng & Âm lượng) + 4 Nút Tác vụ Nhanh ---
        val middleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(186)
            ).apply {
                topMargin = dp(16)
            }
        }

        // Thanh trượt Độ sáng (Brightness)
        val brightnessSlider = VerticalCapsuleSliderView(this, "☀️", "ĐỘ SÁNG").apply {
            progress = sysController.getBrightnessPercent()
            trackColor = preset.cardBgColor
            fillColor = Color.WHITE
            iconColor = Color.parseColor("#1B1F2B")
            cornerRadiusPx = dp(preset.cornerRadiusDp).toFloat()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(10)
            }
            onProgressChanged = { value, _ ->
                sysController.setBrightnessPercent(value)
            }
        }

        // Thanh trượt Âm lượng (Volume)
        val volumeSlider = VerticalCapsuleSliderView(this, "🔊", "ÂM LƯỢNG").apply {
            progress = sysController.getVolumePercent()
            trackColor = preset.cardBgColor
            fillColor = preset.primaryColor
            iconColor = Color.WHITE
            cornerRadiusPx = dp(preset.cornerRadiusDp).toFloat()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(12)
            }
            onProgressChanged = { value, _ ->
                sysController.setVolumePercent(value)
            }
        }

        // Cột bên phải: 2x2 Quick Toggles (Đèn pin, Xoay màn hình, Chuông, Khóa màn hình)
        val quickGridCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f)
        }

        val qRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = dp(10)
            }
        }
        val qRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        // 1. Nút Đèn Pin
        val torchTile = makeSquircleTile(
            icon = "🔦",
            title = "Đèn Pin",
            subtitle = if (sysController.isTorchOn) "Đang bật" else "Tắt",
            active = sysController.isTorchOn,
            preset = preset
        ) { tileView, subTv ->
            sysController.toggleTorch()
            val on = sysController.isTorchOn
            subTv.text = if (on) "Đang bật" else "Tắt"
            tileView.background = roundRect(
                if (on) preset.accentOrange else preset.cardBgColor,
                preset.cornerRadiusDp,
                stroke = !on
            )
        }

        // 2. Nút Xoay tự động
        val rotateOn = sysController.isAutoRotateEnabled()
        val rotateTile = makeSquircleTile(
            icon = "🔄",
            title = "Xoay MH",
            subtitle = if (rotateOn) "Tự động" else "Khóa dọc",
            active = rotateOn,
            preset = preset
        ) { tileView, subTv ->
            val nowOn = sysController.toggleAutoRotate()
            subTv.text = if (nowOn) "Tự động" else "Khóa dọc"
            tileView.background = roundRect(
                if (nowOn) preset.primaryColor else preset.cardBgColor,
                preset.cornerRadiusDp,
                stroke = !nowOn
            )
        }

        // 3. Nút Chế độ Âm thanh / Rung
        fun ringerLabel(mode: Int): String = when (mode) {
            AudioManager.RINGER_MODE_VIBRATE -> "Chỉ rung"
            AudioManager.RINGER_MODE_SILENT -> "Im lặng"
            else -> "Chuông"
        }
        val initialRinger = sysController.getRingerMode()
        val ringerTile = makeSquircleTile(
            icon = "🔔",
            title = "Âm báo",
            subtitle = ringerLabel(initialRinger),
            active = initialRinger != AudioManager.RINGER_MODE_NORMAL,
            preset = preset
        ) { tileView, subTv ->
            val nextMode = sysController.cycleRingerMode()
            subTv.text = ringerLabel(nextMode)
            val active = nextMode != AudioManager.RINGER_MODE_NORMAL
            tileView.background = roundRect(
                if (active) preset.secondaryColor else preset.cardBgColor,
                preset.cornerRadiusDp,
                stroke = !active
            )
        }

        // 4. Nút Khóa màn hình 1 chạm
        val lockTile = makeSquircleTile(
            icon = "🔒",
            title = "Khóa máy",
            subtitle = "1 chạm",
            active = false,
            preset = preset
        ) { _, _ ->
            hideControlCenter()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        }

        qRow1.addView(torchTile)
        qRow1.addView(rotateTile)
        qRow2.addView(ringerTile)
        qRow2.addView(lockTile)
        quickGridCol.addView(qRow1)
        quickGridCol.addView(qRow2)

        middleRow.addView(brightnessSlider)
        middleRow.addView(volumeSlider)
        middleRow.addView(quickGridCol)
        content.addView(middleRow)

        // --- HÀNG 3: 4 Nút Tiện Ích HyperOS (Chụp MH, Đổi Theme, Thông báo gốc, Cài đặt) ---
        val bottomGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(68)
            ).apply {
                topMargin = dp(16)
            }
        }

        fun makeBottomPill(icon: String, label: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = roundRect(preset.cardBgColor, preset.cornerRadiusDp - 4, stroke = true)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginStart = dp(4)
                    marginEnd = dp(4)
                }
                val ic = TextView(context).apply {
                    text = icon
                    textSize = 17f
                    gravity = Gravity.CENTER
                }
                val lb = TextView(context).apply {
                    text = label
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(preset.textPrimary)
                    gravity = Gravity.CENTER
                }
                addView(ic)
                addView(lb)
                setOnClickListener { onClick() }
            }
        }

        bottomGrid.addView(makeBottomPill("📸", "Chụp MH") {
            hideControlCenter()
            mainHandler.postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }, 240)
        })

        bottomGrid.addView(makeBottomPill("🎨", "Đổi Theme") {
            val all = ThemeConfig.PRESETS
            val idx = all.indexOfFirst { it.id == preset.id }
            val next = all[(idx + 1) % all.size]
            ThemeConfig.setCurrentPreset(this, next.id)
            hideControlCenterImmediate()
            refreshAllOverlays()
            showControlCenter()
            Toast.makeText(this, "Đã chuyển sang: ${next.name}", Toast.LENGTH_SHORT).show()
        })

        bottomGrid.addView(makeBottomPill("📩", "Thông báo") {
            hideControlCenter()
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        })

        bottomGrid.addView(makeBottomPill("⚙️", "Cài đặt") {
            hideControlCenter()
            sysController.openMainSettings()
        })

        content.addView(bottomGrid)

        // --- Thanh Gạt Đóng (Home Indicator Bar kiểu iOS 18) ---
        val closeBar = TextView(this).apply {
            text = "▲  Chạm hoặc vuốt lên để đóng Control Center  ▲"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(preset.textSecondary)
            setPadding(0, dp(22), 0, dp(10))
            setOnClickListener { hideControlCenter() }
        }
        content.addView(closeBar)

        scroll.addView(content)
        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.setOnClickListener { hideControlCenter() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        controlCenterRoot = root
        safeAddView(root, params)

        root.translationY = -dp(80).toFloat()
        root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideControlCenter() {
        val root = controlCenterRoot ?: return
        isControlCenterVisible = false
        root.animate()
            .alpha(0f)
            .translationY(-dp(60).toFloat())
            .setDuration(180)
            .withEndAction {
                safeRemoveView(root)
                if (controlCenterRoot === root) controlCenterRoot = null
            }
            .start()
    }

    private fun hideControlCenterImmediate() {
        isControlCenterVisible = false
        safeRemoveView(controlCenterRoot)
        controlCenterRoot = null
    }

    // =========================================================================
    // 4. THANH TRƯỢT ÂM LƯỢNG NỔI (CUSTOM VOLUME HUD iOS 18 / HYPEROS)
    // =========================================================================
    private fun showVolumeHud() {
        mainHandler.removeCallbacks(hideVolumeRunnable)
        val preset = ThemeConfig.getCurrentPreset(this)
        val currentVol = sysController.getVolumePercent()

        if (isVolumeHudVisible && volumeHudRoot != null) {
            volumeSliderView?.progress = currentVol
            mainHandler.postDelayed(hideVolumeRunnable, 2500)
            return
        }

        isVolumeHudVisible = true
        val root = FrameLayout(this).apply {
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = roundRect(Color.parseColor("#E6101522"), 30, stroke = true)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }

        val slider = VerticalCapsuleSliderView(this, "🔊", "").apply {
            progress = currentVol
            trackColor = Color.parseColor("#35FFFFFF")
            fillColor = preset.primaryColor
            iconColor = Color.WHITE
            cornerRadiusPx = dp(24).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(170))
            onProgressChanged = { pct, _ ->
                sysController.setVolumePercent(pct)
                mainHandler.removeCallbacks(hideVolumeRunnable)
                mainHandler.postDelayed(hideVolumeRunnable, 2500)
            }
        }
        volumeSliderView = slider

        val muteBtn = TextView(this).apply {
            text = "🔔"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            setOnClickListener {
                val mode = sysController.cycleRingerMode()
                text = when (mode) {
                    AudioManager.RINGER_MODE_VIBRATE -> "📳"
                    AudioManager.RINGER_MODE_SILENT -> "🔕"
                    else -> "🔔"
                }
                mainHandler.removeCallbacks(hideVolumeRunnable)
                mainHandler.postDelayed(hideVolumeRunnable, 2500)
            }
        }

        card.addView(slider)
        card.addView(muteBtn)
        root.addView(card)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(10)
        }

        volumeHudRoot = root
        safeAddView(root, params)
        mainHandler.postDelayed(hideVolumeRunnable, 2500)
    }

    private fun hideVolumeHud() {
        mainHandler.removeCallbacks(hideVolumeRunnable)
        isVolumeHudVisible = false
        volumeSliderView = null
        safeRemoveView(volumeHudRoot)
        volumeHudRoot = null
    }

    // =========================================================================
    // 5. HIỆU ỨNG SẠC PIN MAGSAFE / HYPEROS TURBO CHARGE
    // =========================================================================
    private fun showChargingAnimation() {
        mainHandler.removeCallbacks(hideChargingRunnable)
        hideChargingAnimation()
        isChargingOverlayVisible = true

        val preset = ThemeConfig.getCurrentPreset(this)
        val (batPct, _) = sysController.getBatteryInfo()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC060913"))
            setOnClickListener { hideChargingAnimation() }
        }

        val ringContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E60F172A"))
                setStroke(dp(6), preset.accentGreen)
            }
            layoutParams = FrameLayout.LayoutParams(dp(230), dp(230), Gravity.CENTER)
        }

        val boltTv = TextView(this).apply {
            text = "⚡"
            textSize = 40f
            gravity = Gravity.CENTER
        }
        val pctTv = TextView(this).apply {
            text = "$batPct%"
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val labelTv = TextView(this).apply {
            text = "18W HyperCharge • mblu 22 Pro"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(preset.accentGreen)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        ringContainer.addView(boltTv)
        ringContainer.addView(pctTv)
        ringContainer.addView(labelTv)
        root.addView(ringContainer)

        ringContainer.scaleX = 0.6f
        ringContainer.scaleY = 0.6f
        ringContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(420)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        chargingOverlayRoot = root
        safeAddView(root, params)
        mainHandler.postDelayed(hideChargingRunnable, 3500)
    }

    private fun hideChargingAnimation() {
        mainHandler.removeCallbacks(hideChargingRunnable)
        isChargingOverlayVisible = false
        safeRemoveView(chargingOverlayRoot)
        chargingOverlayRoot = null
    }

    // =========================================================================
    // HELPER UI BUILDERS
    // =========================================================================
    private fun makeRoundToggle(
        icon: String,
        label: String,
        active: Boolean,
        activeColor: Int,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundRect(
                if (active) activeColor else Color.parseColor("#26FFFFFF"),
                18
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            val ic = TextView(context).apply {
                text = icon
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }
            val lb = TextView(context).apply {
                text = label
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }
            addView(ic)
            addView(lb)
            setOnClickListener { onClick() }
        }
    }

    private fun makeSquircleTile(
        icon: String,
        title: String,
        subtitle: String,
        active: Boolean,
        preset: ThemePreset,
        onClick: (LinearLayout, TextView) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = roundRect(
                if (active) preset.primaryColor else preset.cardBgColor,
                preset.cornerRadiusDp - 4,
                stroke = !active
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }

            val icTv = TextView(context).apply {
                text = icon
                textSize = 18f
            }
            val titleTv = TextView(context).apply {
                text = title
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            val subTv = TextView(context).apply {
                text = subtitle
                textSize = 10.5f
                setTextColor(Color.parseColor("#D8FFFFFF"))
            }

            addView(icTv)
            addView(titleTv)
            addView(subTv)
            setOnClickListener { onClick(this, subTv) }
        }
    }

    private fun roundRect(color: Int, radiusDp: Int, stroke: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            if (stroke) {
                setStroke(dp(1), Color.parseColor("#35FFFFFF"))
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

    private fun safeAddView(view: View?, params: WindowManager.LayoutParams) {
        if (view == null) return
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {
        }
    }

    private fun safeUpdateView(view: View?, params: WindowManager.LayoutParams) {
        if (view == null) return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun safeRemoveView(view: View?) {
        if (view == null) return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
    }
}

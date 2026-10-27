package com.meizu.mblu22pro.themeoverlay

import android.content.Context
import android.graphics.Color

data class ThemePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val accentGreen: Int,
    val accentOrange: Int,
    val panelBgColor: Int,
    val cardBgColor: Int,
    val cardActiveColor: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val wallpaperColors: IntArray,
    val cornerRadiusDp: Int
)

object ThemeConfig {
    private const val PREF_NAME = "mblu22pro_theme_prefs"
    const val ACTION_THEME_UPDATED = "com.meizu.mblu22pro.themeoverlay.ACTION_THEME_UPDATED"
    const val ACTION_OPEN_CONTROL_CENTER = "com.meizu.mblu22pro.themeoverlay.ACTION_OPEN_CONTROL_CENTER"
    const val ACTION_EXPAND_ISLAND = "com.meizu.mblu22pro.themeoverlay.ACTION_EXPAND_ISLAND"
    const val ACTION_TEST_VOLUME_HUD = "com.meizu.mblu22pro.themeoverlay.ACTION_TEST_VOLUME_HUD"
    const val ACTION_TEST_CHARGING = "com.meizu.mblu22pro.themeoverlay.ACTION_TEST_CHARGING"

    val PRESETS: List<ThemePreset> = listOf(
        ThemePreset(
            id = "ios18_ultramarine",
            name = "iOS 18 Ultramarine Blur",
            subtitle = "Chuẩn kính mờ iOS 18 • Bo tròn mềm mại",
            primaryColor = Color.parseColor("#0A84FF"),
            secondaryColor = Color.parseColor("#5E5CE6"),
            accentGreen = Color.parseColor("#30D158"),
            accentOrange = Color.parseColor("#FF9F0A"),
            panelBgColor = Color.parseColor("#D9111625"),
            cardBgColor = Color.parseColor("#40FFFFFF"),
            cardActiveColor = Color.parseColor("#0A84FF"),
            textPrimary = Color.parseColor("#FFFFFF"),
            textSecondary = Color.parseColor("#B8C4E0"),
            wallpaperColors = intArrayOf(
                Color.parseColor("#090D1E"),
                Color.parseColor("#1B2B65"),
                Color.parseColor("#0A84FF"),
                Color.parseColor("#5E5CE6")
            ),
            cornerRadiusDp = 28
        ),
        ThemePreset(
            id = "hyperos_titanium",
            name = "HyperOS 2.0 Titanium",
            subtitle = "Phong cách Xiaomi HyperOS 2 • Squircle hiện đại",
            primaryColor = Color.parseColor("#267AF5"),
            secondaryColor = Color.parseColor("#00D2FF"),
            accentGreen = Color.parseColor("#28C76F"),
            accentOrange = Color.parseColor("#FF851B"),
            panelBgColor = Color.parseColor("#DE0E1520"),
            cardBgColor = Color.parseColor("#38D6E4FF"),
            cardActiveColor = Color.parseColor("#267AF5"),
            textPrimary = Color.parseColor("#FFFFFF"),
            textSecondary = Color.parseColor("#A8B9D0"),
            wallpaperColors = intArrayOf(
                Color.parseColor("#0A101D"),
                Color.parseColor("#16325B"),
                Color.parseColor("#267AF5"),
                Color.parseColor("#00D2FF")
            ),
            cornerRadiusDp = 24
        ),
        ThemePreset(
            id = "ios18_desert_gold",
            name = "iOS 18 Desert Titanium",
            subtitle = "Tông Vàng Sa Mạc iPhone 16 Pro • Ấm & Sang trọng",
            primaryColor = Color.parseColor("#E5A93C"),
            secondaryColor = Color.parseColor("#FF6B4A"),
            accentGreen = Color.parseColor("#34C759"),
            accentOrange = Color.parseColor("#FF9500"),
            panelBgColor = Color.parseColor("#DE1A1512"),
            cardBgColor = Color.parseColor("#38FFEAD5"),
            cardActiveColor = Color.parseColor("#D99B26"),
            textPrimary = Color.parseColor("#FFFDF9"),
            textSecondary = Color.parseColor("#D6C3B2"),
            wallpaperColors = intArrayOf(
                Color.parseColor("#140F0B"),
                Color.parseColor("#3D2B1F"),
                Color.parseColor("#C8965A"),
                Color.parseColor("#E5A93C")
            ),
            cornerRadiusDp = 28
        ),
        ThemePreset(
            id = "hyperos_mint_glass",
            name = "HyperOS Frost Mint",
            subtitle = "Xanh Ngọc Lục Bảo • Trong trẻo & Dịu mắt trên màn 120Hz",
            primaryColor = Color.parseColor("#00C7BE"),
            secondaryColor = Color.parseColor("#30D158"),
            accentGreen = Color.parseColor("#30D158"),
            accentOrange = Color.parseColor("#FFD60A"),
            panelBgColor = Color.parseColor("#DE0B1919"),
            cardBgColor = Color.parseColor("#38CCFBF1"),
            cardActiveColor = Color.parseColor("#00B4AB"),
            textPrimary = Color.parseColor("#FFFFFF"),
            textSecondary = Color.parseColor("#A7D7D2"),
            wallpaperColors = intArrayOf(
                Color.parseColor("#071414"),
                Color.parseColor("#0E3A3A"),
                Color.parseColor("#00C7BE"),
                Color.parseColor("#30D158")
            ),
            cornerRadiusDp = 26
        ),
        ThemePreset(
            id = "ios18_midnight_oled",
            name = "iOS 18 Dark Pro Max",
            subtitle = "Đen sâu tương phản cao • Điểm nhấn Tím Neon & Hồng",
            primaryColor = Color.parseColor("#BF5AF2"),
            secondaryColor = Color.parseColor("#FF375F"),
            accentGreen = Color.parseColor("#32D74B"),
            accentOrange = Color.parseColor("#FF9F0A"),
            panelBgColor = Color.parseColor("#EB07070C"),
            cardBgColor = Color.parseColor("#36E5D4FF"),
            cardActiveColor = Color.parseColor("#BF5AF2"),
            textPrimary = Color.parseColor("#FFFFFF"),
            textSecondary = Color.parseColor("#C2B5D8"),
            wallpaperColors = intArrayOf(
                Color.parseColor("#05050A"),
                Color.parseColor("#24123A"),
                Color.parseColor("#7A28CB"),
                Color.parseColor("#FF375F")
            ),
            cornerRadiusDp = 30
        )
    )

    fun getCurrentPreset(context: Context): ThemePreset {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString("preset_id", "ios18_ultramarine") ?: "ios18_ultramarine"
        return PRESETS.find { it.id == id } ?: PRESETS.first()
    }

    fun setCurrentPreset(context: Context, presetId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("preset_id", presetId)
            .apply()
    }

    fun isControlCenterEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("enable_control_center", true)

    fun setControlCenterEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("enable_control_center", enabled).apply()
    }

    fun isDynamicIslandEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("enable_dynamic_island", true)

    fun setDynamicIslandEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("enable_dynamic_island", enabled).apply()
    }

    fun isCustomStatusBarEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("enable_custom_statusbar", false)

    fun setCustomStatusBarEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("enable_custom_statusbar", enabled).apply()
    }

    fun isVolumeHudEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("enable_volume_hud", true)

    fun setVolumeHudEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("enable_volume_hud", enabled).apply()
    }

    fun isChargingAnimEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("enable_charging_anim", true)

    fun setChargingAnimEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("enable_charging_anim", enabled).apply()
    }

    // Tinh chỉnh vị trí Dynamic Island cho màn hình 6.79" (720x1640) của Meizu mblu 22 Pro
    fun getIslandWidthDp(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("island_width_dp", 128)

    fun setIslandWidthDp(context: Context, widthDp: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("island_width_dp", widthDp.coerceIn(90, 220)).apply()
    }

    fun getIslandHeightDp(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("island_height_dp", 34)

    fun setIslandHeightDp(context: Context, heightDp: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("island_height_dp", heightDp.coerceIn(24, 52)).apply()
    }

    fun getIslandTopOffsetDp(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("island_top_offset_dp", 6)

    fun setIslandTopOffsetDp(context: Context, offsetDp: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt("island_top_offset_dp", offsetDp.coerceIn(0, 36)).apply()
    }
}

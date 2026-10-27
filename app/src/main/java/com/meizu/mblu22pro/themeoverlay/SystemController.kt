package com.meizu.mblu22pro.themeoverlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast

class SystemController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var torchCameraId: String? = null
    var isTorchOn: Boolean = false
        private set

    var onTorchStateChanged: ((Boolean) -> Unit)? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                isTorchOn = enabled
                onTorchStateChanged?.invoke(enabled)
            }
        }
    }

    init {
        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    break
                }
            }
            if (torchCameraId == null) {
                torchCameraId = cameraManager.cameraIdList.firstOrNull()
            }
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {
        }
    }

    fun toggleTorch(): Boolean {
        val id = torchCameraId ?: return false
        return try {
            val nextState = !isTorchOn
            cameraManager.setTorchMode(id, nextState)
            isTorchOn = nextState
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể bật Đèn pin: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // Độ sáng màn hình (0..100%)
    fun getBrightnessPercent(): Int {
        return try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                140
            )
            ((raw / 255f) * 100f).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            55
        }
    }

    fun setBrightnessPercent(percent: Int): Boolean {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsPermission()
            return false
        }
        return try {
            val raw = ((percent.coerceIn(2, 100) / 100f) * 255f).toInt().coerceIn(5, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                raw
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    // Âm lượng Media (0..100%)
    fun getVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((cur.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt()
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
        }
    }

    fun adjustVolumeStep(direction: Int) {
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        } catch (_: Exception) {
        }
    }

    // Chế độ chuông (Bình thường / Rung / Im lặng)
    fun getRingerMode(): Int = audioManager.ringerMode

    fun cycleRingerMode(): Int {
        return try {
            val next = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = next
            next
        } catch (_: SecurityException) {
            // Nếu chưa cấp quyền DND để vào Silent, chuyển giữa Normal & Vibrate
            val fallback = if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                AudioManager.RINGER_MODE_VIBRATE
            } else {
                AudioManager.RINGER_MODE_NORMAL
            }
            try {
                audioManager.ringerMode = fallback
            } catch (_: Exception) {}
            fallback
        }
    }

    // Xoay màn hình tự động
    fun isAutoRotateEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun toggleAutoRotate(): Boolean {
        if (!Settings.System.canWrite(context)) {
            openWriteSettingsPermission()
            return isAutoRotateEnabled()
        }
        val next = if (isAutoRotateEnabled()) 0 else 1
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                next
            )
            next == 1
        } catch (_: Exception) {
            false
        }
    }

    // Trạng thái Wi-Fi & Mạng
    fun isWifiEnabled(): Boolean {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wm.isWifiEnabled
        } catch (_: Exception) {
            false
        }
    }

    fun isCellularConnected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }

    fun isAirplaneModeOn(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    // Điều khiển nhạc (Media Key Dispatch)
    fun sendMediaButton(keyCode: Int) {
        try {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(down)
            audioManager.dispatchMediaKeyEvent(up)
        } catch (_: Exception) {
        }
    }

    fun isMusicActive(): Boolean = audioManager.isMusicActive

    // Thông tin Pin
    fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 85) ?: 85
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            Pair(((level * 100f) / scale).toInt().coerceIn(0, 100), isCharging)
        } catch (_: Exception) {
            Pair(85, false)
        }
    }

    // Mở các bảng cài đặt nhanh của Android 15
    fun openWifiPanel() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemAction(Settings.ACTION_WIFI_SETTINGS)
        }
    }

    fun openInternetPanel() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemAction(Settings.ACTION_WIRELESS_SETTINGS)
        }
    }

    fun openBluetoothSettings() = openSystemAction(Settings.ACTION_BLUETOOTH_SETTINGS)
    fun openAirplaneSettings() = openSystemAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
    fun openMainSettings() = openSystemAction(Settings.ACTION_SETTINGS)

    fun openWriteSettingsPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Hãy bật 'Cho phép sửa đổi cài đặt hệ thống' để chỉnh Độ sáng & Xoay màn hình",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
        }
    }

    private fun openSystemAction(action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

package com.mhmdwaelanwr.keyviora

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import android.widget.Toast
import kotlin.math.roundToInt

class TouchAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: TouchAccessibilityService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuView: View? = null
    private var flashlightOn = false

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun prefs() = TouchPrefs.prefs(this)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
        setBubbleVisible(prefs().getBoolean(TouchPrefs.BUBBLE_VISIBLE, true))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        closeMenu()
        bubbleView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        bubbleView = null
        bubbleParams = null
        if (instance === this) instance = null
    }

    private fun background(color: Int, radiusDp: Int = 18, borderColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (borderColor != null) setStroke(dp(2), borderColor)
        }

    private fun createBubble() {
        if (bubbleView != null) return
        val diameter = dp(prefs().getInt(TouchPrefs.SIZE, 60))
        val button = FrameLayout(this).apply {
            background = TouchStyle.oval(this@TouchAccessibilityService, 0xFF1B3355.toInt(), 0xFF345783.toInt())
            elevation = dp(8).toFloat()
            contentDescription = "KEYVIORA floating button"
        }
        val center = FrameLayout(this).apply {
            background = TouchStyle.oval(this@TouchAccessibilityService, 0xFF2F5591.toInt())
        }
        val dot = View(this).apply {
            background = TouchStyle.oval(this@TouchAccessibilityService,
                prefs().getInt(TouchPrefs.ACCENT, TouchStyle.BLUE))
        }
        center.addView(dot, FrameLayout.LayoutParams(diameter * 2 / 5, diameter * 2 / 5, Gravity.CENTER))
        button.addView(center, FrameLayout.LayoutParams(
            diameter * 4 / 5, diameter * 4 / 5, Gravity.CENTER
        ))

        val metrics = resources.displayMetrics
        val defaultX = (metrics.widthPixels - diameter - dp(12)).coerceAtLeast(0)
        val defaultY = (metrics.heightPixels / 4).coerceAtLeast(dp(80))
        val layout = WindowManager.LayoutParams(
            diameter, diameter,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs().getInt(TouchPrefs.X, defaultX).coerceIn(0, (metrics.widthPixels - diameter).coerceAtLeast(0))
            y = prefs().getInt(TouchPrefs.Y, defaultY).coerceIn(0, (metrics.heightPixels - diameter).coerceAtLeast(0))
        }
        bubbleParams = layout
        bubbleView = button
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                runConfigured(TouchPrefs.SINGLE, "MENU")
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                runConfigured(TouchPrefs.DOUBLE, "SCREENSHOT")
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                runConfigured(TouchPrefs.LONG, "LOCK")
            }
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                val p = bubbleParams ?: return false
                p.x = (p.x - distanceX).roundToInt().coerceIn(0, (resources.displayMetrics.widthPixels - p.width).coerceAtLeast(0))
                p.y = (p.y - distanceY).roundToInt().coerceIn(0, (resources.displayMetrics.heightPixels - p.height).coerceAtLeast(0))
                try { windowManager.updateViewLayout(button, p) } catch (_: Exception) { }
                return true
            }
        })
        button.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                bubbleParams?.let {
                    prefs().edit().putInt(TouchPrefs.X, it.x).putInt(TouchPrefs.Y, it.y).apply()
                }
            }
            true
        }
        try {
            windowManager.addView(button, layout)
            refreshAppearance()
        } catch (ex: Exception) {
            bubbleView = null
            bubbleParams = null
            toast("تعذر عرض الزر: ${ex.message}")
        }
    }

    fun refreshAppearance() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val size = dp(prefs().getInt(TouchPrefs.SIZE, 60))
        params.width = size
        params.height = size
        val center = (view as? FrameLayout)?.getChildAt(0)
        center?.layoutParams = FrameLayout.LayoutParams(size * 4 / 5, size * 4 / 5, Gravity.CENTER)
        (center as? FrameLayout)?.getChildAt(0)?.let { dot ->
            dot.layoutParams = FrameLayout.LayoutParams(size * 2 / 5, size * 2 / 5, Gravity.CENTER)
            dot.background = TouchStyle.oval(this, prefs().getInt(TouchPrefs.ACCENT, TouchStyle.BLUE))
        }
        view.alpha = prefs().getInt(TouchPrefs.OPACITY, 85) / 100f
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    fun setBubbleVisible(visible: Boolean) {
        prefs().edit().putBoolean(TouchPrefs.BUBBLE_VISIBLE, visible).apply()
        if (visible && bubbleView == null) createBubble()
        bubbleView?.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) closeMenu()
    }

    private fun runConfigured(key: String, defaultAction: String) {
        execute(prefs().getString(key, defaultAction) ?: defaultAction)
    }

    /** Allow app dashboard cards to execute the same actions as the overlay. */
    fun performNamedAction(action: String) { execute(action) }

    private fun execute(action: String) {
        // Close first so system screenshots do not capture the expanded menu.
        if (action != "MENU") closeMenu()
        when (action) {
            "MENU" -> if (menuView == null) showMenu() else closeMenu()
            "LOCK" -> global(GLOBAL_ACTION_LOCK_SCREEN)
            "POWER" -> showPowerConfirmation()
            "SCREENSHOT" -> handler.postDelayed({ global(GLOBAL_ACTION_TAKE_SCREENSHOT) }, 160)
            "HOME" -> global(GLOBAL_ACTION_HOME)
            "BACK" -> global(GLOBAL_ACTION_BACK)
            "RECENTS" -> global(GLOBAL_ACTION_RECENTS)
            "NOTIFICATIONS" -> global(GLOBAL_ACTION_NOTIFICATIONS)
            "QUICK_SETTINGS" -> global(GLOBAL_ACTION_QUICK_SETTINGS)
            "VOLUME_UP" -> volume(AudioManager.ADJUST_RAISE)
            "VOLUME_DOWN" -> volume(AudioManager.ADJUST_LOWER)
            "FLASHLIGHT" -> toggleFlashlight()
            "WIFI" -> openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
            "BLUETOOTH" -> openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            "DISPLAY" -> openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
            "SOUND" -> openSystemSettings(Settings.ACTION_SOUND_SETTINGS)
            "APP" -> openApp()
            "HIDE" -> setBubbleVisible(false)
        }
    }

    /** Two-step confirmation: never casually power off a HONOR with a broken power key. */
    private fun showPowerConfirmation() {
        if (menuView != null) closeMenu()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = background(TouchStyle.WARN_BG, 24, 0xFF725063.toInt())
        }
        val heading = menuText("POWER BUTTON WARNING", 16f, TouchStyle.AMBER, true)
        box.addView(heading)
        val warning = menuText("Your physical power button is broken. If you completely switch off this phone, KEYVIORA cannot turn it back on.", 14f, TouchStyle.WHITE).apply {
            setPadding(0, dp(12), 0, dp(12))
        }
        box.addView(warning)
        val options = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cancel = menuText("Cancel", 14f, TouchStyle.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = background(TouchStyle.TILE, 14)
            setOnClickListener { closeMenu() }
        }
        options.addView(cancel, LinearLayout.LayoutParams(0, dp(48), 1f))
        val proceed = menuText("Open power menu", 14f, TouchStyle.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = background(0xFF60444D.toInt(), 14)
            setOnClickListener { closeMenu(); global(GLOBAL_ACTION_POWER_DIALOG) }
        }
        options.addView(proceed, LinearLayout.LayoutParams(0, dp(48), 1.4f).apply { leftMargin = dp(10) })
        box.addView(options)
        val width = minOf(resources.displayMetrics.widthPixels - dp(24), dp(380))
        val layout = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try { windowManager.addView(box, layout); menuView = box }
        catch (_: Exception) { toast("تعذر عرض تحذير الباور") }
    }

    private fun global(action: Int) {
        if (!performGlobalAction(action)) toast("الأمر غير متاح على إصدار النظام الحالي")
    }

    private fun volume(direction: Int) {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        } catch (_: Exception) {
            toast("تعذر التحكم في الصوت")
        }
    }

    private fun toggleFlashlight() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("افتح التطبيق واسمح بصلاحية الكاميرا لتشغيل الكشاف")
            openApp()
            return
        }
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                toast("لم يتم العثور على كشاف متاح")
                return
            }
            manager.setTorchMode(cameraId, !flashlightOn)
            flashlightOn = !flashlightOn
        } catch (_: Exception) {
            toast("الكشاف مشغول أو غير متاح")
        }
    }

    private fun openSystemSettings(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("تعذر فتح الإعدادات") }
    }

    private fun openApp() {
        try { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("تعذر فتح التطبيق") }
    }

    private fun menuText(content: String, size: Float = 14f, color: Int = TouchStyle.WHITE, strong: Boolean = false): TextView =
        TextView(this).apply {
            text = content
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER_VERTICAL
            if (strong) typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

    private fun showMenu() = showMenuPage(0)

    private fun showMenuPage(page: Int) {
        if (menuView != null) return
        val accent = prefs().getInt(TouchPrefs.ACCENT, TouchStyle.BLUE)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = background(0xFF101A2A.toInt(), 28, 0xFF345075.toInt())
            elevation = dp(16).toFloat()
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = menuText(if (page == 0) "QUICK CONTROLS" else "MORE TOOLS", 14f, TouchStyle.MUTED, true)
        header.addView(title, LinearLayout.LayoutParams(0, dp(34), 1f))
        val dismiss = menuText("×", 29f).apply {
            gravity = Gravity.CENTER
            contentDescription = "Close quick controls"
            setOnClickListener { closeMenu() }
        }
        header.addView(dismiss, LinearLayout.LayoutParams(dp(35), dp(35)))
        panel.addView(header)

        val shortcuts = if (page == 0) listOf(
            Triple("⏣", "Lock", "LOCK"), Triple("＋", "Vol up", "VOLUME_UP"),
            Triple("−", "Vol down", "VOLUME_DOWN"), Triple("▣", "Screenshot", "SCREENSHOT"),
            Triple("☼", "Torch", "FLASHLIGHT"), Triple("⏻", "Power", "POWER")
        ) else listOf(
            Triple("◉", "Wi-Fi", "WIFI"), Triple("ᛒ", "Bluetooth", "BLUETOOTH"),
            Triple("♧", "Notices", "NOTIFICATIONS"), Triple("☷", "Quick panel", "QUICK_SETTINGS"),
            Triple("◐", "Display", "DISPLAY"), Triple("♫", "Sound", "SOUND"),
            Triple("☼", "Torch", "FLASHLIGHT"), Triple("⚙", "App", "APP"),
            Triple("✕", "Hide", "HIDE")
        )
        for (r in shortcuts.chunked(3)) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for ((symbol, label, action) in r) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = background(if (action == "LOCK") 0xFF2B65EF.toInt() else 0xFF19283E.toInt(), 17)
                    isClickable = true
                    isFocusable = true
                    contentDescription = label
                    setOnClickListener { execute(action) }
                }
                val glyph = menuText(symbol, 25f, if (action == "LOCK") TouchStyle.WHITE else accent, true).apply {
                    gravity = Gravity.CENTER
                    background = background(if (action == "LOCK") 0xFF457FF4.toInt() else 0xFF243B59.toInt(), 40)
                }
                cell.addView(glyph, LinearLayout.LayoutParams(dp(43), dp(43)))
                val caption = menuText(label, 12f, TouchStyle.WHITE, true).apply { gravity = Gravity.CENTER }
                cell.addView(caption, LinearLayout.LayoutParams(-1, dp(22)).apply { topMargin = dp(2) })
                line.addView(cell, LinearLayout.LayoutParams(0, dp(91), 1f).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
            }
            panel.addView(line)
        }
        if (page == 0) {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            val audioCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = background(0xFF19283E.toInt(), 17)
                setPadding(dp(14), dp(9), dp(14), dp(3))
            }
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            head.addView(menuText("Media volume", 14f, TouchStyle.WHITE, true), LinearLayout.LayoutParams(0, dp(27), 1f))
            val valLabel = menuText("", 12f, TouchStyle.MUTED, true)
            head.addView(valLabel, LinearLayout.LayoutParams(dp(45), dp(27)))
            audioCard.addView(head)
            val seek = SeekBar(this).apply {
                max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                progress = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                progressTintList = ColorStateList.valueOf(accent)
                thumbTintList = ColorStateList.valueOf(TouchStyle.WHITE)
                progressBackgroundTintList = ColorStateList.valueOf(0xFF354A68.toInt())
            }
            valLabel.text = "${(100f * seek.progress / seek.max).toInt()}%"
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, v: Int, fromUser: Boolean) {
                    if (fromUser) {
                        try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0) }
                        catch (_: Exception) { }
                        valLabel.text = "${(100f * v / seek.max).toInt()}%"
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
            audioCard.addView(seek, LinearLayout.LayoutParams(-1, dp(38)))
            panel.addView(audioCard, LinearLayout.LayoutParams(-1, dp(79)).apply { topMargin = dp(10) })

            val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val navigate = listOf(Triple("↶", "Back", "BACK"),Triple("⌂", "Home", "HOME"),Triple("▤", "Recent", "RECENTS"))
            for ((icon,label,action) in navigate) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = background(TouchStyle.SURFACE, 16)
                    isClickable = true
                    setOnClickListener { execute(action) }
                }
                val iconText = menuText(icon, 25f, TouchStyle.MINT, true).apply { gravity = Gravity.CENTER }
                cell.addView(iconText, LinearLayout.LayoutParams(-1, dp(32)))
                val textLabel = menuText(label, 11f).apply { gravity = Gravity.CENTER }
                cell.addView(textLabel, LinearLayout.LayoutParams(-1, dp(21)))
                nav.addView(cell, LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            panel.addView(nav, LinearLayout.LayoutParams(-1, dp(75)).apply { topMargin = dp(7) })
        }
        val more = menuText(if (page == 0) "More tools   ›" else "‹   Back to quick controls", 12f, accent, true).apply {
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { closeMenu(); showMenuPage(if (page == 0) 1 else 0) }
        }
        panel.addView(more, LinearLayout.LayoutParams(-1, dp(35)).apply { topMargin = dp(7) })

        val screen = resources.displayMetrics.widthPixels
        val width = minOf(screen - dp(24), dp(380))
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try { windowManager.addView(panel, params); menuView = panel }
        catch (_: Exception) { toast("تعذر فتح القائمة") }
    }

    private fun closeMenu() {
        val view = menuView ?: return
        try { windowManager.removeView(view) } catch (_: Exception) { }
        menuView = null
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

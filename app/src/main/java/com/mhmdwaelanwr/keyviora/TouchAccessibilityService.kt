package com.mhmdwaelanwr.keyviora

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
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
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import android.widget.ScrollView
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
    private var torchCameraId: String? = null
    private var torchCallbackRegistered = false
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) flashlightOn = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) flashlightOn = false
        }
    }

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // The usable display dimensions can change after rotation and multi-window changes.
        bubbleParams?.let { params ->
            clampBubblePosition(params)
            bubbleView?.let { view ->
                try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
            }
            persistBubblePosition(params)
        }
        closeMenu()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        if (torchCallbackRegistered) {
            try {
                (getSystemService(CAMERA_SERVICE) as CameraManager).unregisterTorchCallback(torchCallback)
            } catch (_: Exception) { }
            torchCallbackRegistered = false
        }
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

    private fun clampBubblePosition(params: WindowManager.LayoutParams) {
        val metrics = resources.displayMetrics
        params.x = BubbleBounds.clamp(params.x, metrics.widthPixels, params.width)
        params.y = BubbleBounds.clampWithinInsets(
            params.y, metrics.heightPixels, params.height, dp(28), dp(44)
        )
    }

    private fun persistBubblePosition(params: WindowManager.LayoutParams) {
        prefs().edit().putInt(TouchPrefs.X, params.x).putInt(TouchPrefs.Y, params.y).apply()
    }

    private fun createBubble() {
        if (bubbleView != null) return
        val diameter = dp(prefs().getInt(TouchPrefs.SIZE, 60).coerceIn(48, 88))
        val button = FrameLayout(this).apply {
            background = TouchStyle.oval(this@TouchAccessibilityService, 0xFF1B3355.toInt(), 0xFF345783.toInt())
            elevation = dp(8).toFloat()
            contentDescription = "KEYVIORA floating button. Double tap to activate the configured single-tap action."
            isClickable = true
            isFocusable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            // TalkBack invokes accessibility ACTION_CLICK rather than the raw touch gestures.
            setOnClickListener { runConfigured(TouchPrefs.SINGLE, "MENU") }
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
            x = BubbleBounds.clamp(prefs().getInt(TouchPrefs.X, defaultX), metrics.widthPixels, diameter)
            y = BubbleBounds.clampWithinInsets(
                prefs().getInt(TouchPrefs.Y, defaultY),
                metrics.heightPixels, diameter, dp(28), dp(44)
            )
        }
        bubbleParams = layout
        bubbleView = button
        var touchStartRawX = 0f
        var touchStartRawY = 0f
        var bubbleStartX = layout.x
        var bubbleStartY = layout.y
        var wasDragged = false
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
                wasDragged = true
                val p = bubbleParams ?: return false
                // Absolute displacement avoids losing sub-pixel drags to per-frame rounding.
                p.x = (bubbleStartX + e2.rawX - touchStartRawX).roundToInt()
                p.y = (bubbleStartY + e2.rawY - touchStartRawY).roundToInt()
                clampBubblePosition(p)
                try { windowManager.updateViewLayout(button, p) } catch (_: Exception) { }
                return true
            }
        })
        button.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                bubbleStartX = bubbleParams?.x ?: 0
                bubbleStartY = bubbleParams?.y ?: 0
                touchStartRawX = event.rawX
                touchStartRawY = event.rawY
                wasDragged = false
            }
            detector.onTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                bubbleParams?.let { params ->
                    if (wasDragged && prefs().getBoolean(TouchPrefs.SNAP_TO_EDGE, true)) {
                        params.x = BubbleBounds.snapToEdge(
                            params.x, resources.displayMetrics.widthPixels, params.width, dp(10)
                        )
                        try { windowManager.updateViewLayout(button, params) } catch (_: Exception) { }
                    }
                    persistBubblePosition(params)
                }
                wasDragged = false
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
        val size = dp(prefs().getInt(TouchPrefs.SIZE, 60).coerceIn(48, 88))
        params.width = size
        params.height = size
        clampBubblePosition(params)
        persistBubblePosition(params)
        val center = (view as? FrameLayout)?.getChildAt(0)
        center?.layoutParams = FrameLayout.LayoutParams(size * 4 / 5, size * 4 / 5, Gravity.CENTER)
        (center as? FrameLayout)?.getChildAt(0)?.let { dot ->
            dot.layoutParams = FrameLayout.LayoutParams(size * 2 / 5, size * 2 / 5, Gravity.CENTER)
            dot.background = TouchStyle.oval(this, prefs().getInt(TouchPrefs.ACCENT, TouchStyle.BLUE))
        }
        view.alpha = prefs().getInt(TouchPrefs.OPACITY, 85).coerceIn(35, 100) / 100f
        try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
    }

    /** Recovery action from Settings when the bubble was dragged to an inconvenient edge. */
    fun resetBubblePosition() {
        if (bubbleParams == null) createBubble()
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        params.x = metrics.widthPixels - params.width - dp(12)
        params.y = metrics.heightPixels / 4
        clampBubblePosition(params)
        persistBubblePosition(params)
        bubbleView?.let { view ->
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) { }
        }
        setBubbleVisible(true)
    }

    /** Actual overlay state, not merely the last value saved in preferences. */
    fun isBubbleVisible(): Boolean = bubbleView?.visibility == View.VISIBLE

    fun setBubbleVisible(visible: Boolean) {
        prefs().edit().putBoolean(TouchPrefs.BUBBLE_VISIBLE, visible).apply()
        if (visible && bubbleView == null) createBubble()
        bubbleView?.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) closeMenu()
    }

    private fun hapticFeedback(view: View?) {
        if (prefs().getBoolean(TouchPrefs.HAPTIC_ENABLED, true)) {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun runConfigured(key: String, defaultAction: String) {
        val saved = prefs().getString(key, defaultAction)
        val action = TouchActionCatalog.resolve(saved, defaultAction).key
        if (action != "NONE") hapticFeedback(bubbleView)
        execute(action)
    }

    /** Allow app dashboard cards to execute the same actions as the overlay. */
    fun performNamedAction(action: String) { execute(action) }

    private fun execute(action: String) {
        // An explicitly disabled gesture must not close an already-open panel.
        if (action == "NONE") return
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
            toast("اسمح بصلاحية الكاميرا، ثم اضغط على الكشاف مرة أخرى")
            openApp(requestCameraPermission = true)
            return
        }
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            // Prefer a rear camera with a torch, not a front/auxiliary camera.
            val available = manager.cameraIdList.filter {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            val cameraId = torchCameraId?.takeIf { it in available }
                ?: available.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK
                } ?: available.firstOrNull()
            if (cameraId == null) {
                toast("لم يتم العثور على كشاف متاح")
                return
            }
            torchCameraId = cameraId
            if (!torchCallbackRegistered) {
                manager.registerTorchCallback(torchCallback, handler)
                torchCallbackRegistered = true
            }
            manager.setTorchMode(cameraId, !flashlightOn)
            flashlightOn = !flashlightOn // Callback keeps state synced with external toggles.
        } catch (_: Exception) {
            toast("الكشاف مشغول أو غير متاح")
        }
    }

    private fun openSystemSettings(action: String) {
        try { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("تعذر فتح الإعدادات") }
    }

    private fun openApp(requestCameraPermission: Boolean = false) {
        try {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_REQUEST_CAMERA_PERMISSION, requestCameraPermission)
            startActivity(intent)
        } catch (_: Exception) { toast("تعذر فتح التطبيق") }
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
            isClickable = true
            isFocusable = true
            contentDescription = "Close quick controls"
            setOnClickListener { closeMenu() }
        }
        header.addView(dismiss, LinearLayout.LayoutParams(dp(48), dp(48)))
        panel.addView(header)

        val favorites = TouchPrefs.favoriteKeys.mapIndexed { index, preference ->
            val fallback = TouchPrefs.favoriteDefaults[index]
            val action = TouchActionCatalog.resolve(prefs().getString(preference, fallback), fallback)
            Triple(action.glyph, action.shortLabel, action.key)
        }
        val quickShortcuts = TouchPrefs.quickKeys.mapIndexed { index, preference ->
            val fallback = TouchPrefs.quickDefaults[index]
            val action = TouchActionCatalog.resolve(prefs().getString(preference, fallback), fallback)
            Triple(action.glyph, action.shortLabel, action.key)
        }
        val shortcuts = if (page == 0) quickShortcuts else favorites + listOf(
            Triple("ᛒ", "Bluetooth", "BLUETOOTH"), Triple("☷", "Quick panel", "QUICK_SETTINGS"),
            Triple("◐", "Display", "DISPLAY"), Triple("♫", "Sound", "SOUND"),
            Triple("☼", "Torch", "FLASHLIGHT"), Triple("✕", "Hide", "HIDE")
        )
        if (page == 1) {
            panel.addView(menuText("PINNED SHORTCUTS", 11f, TouchStyle.MUTED, true))
        }
        for ((rowIndex, r) in shortcuts.chunked(3).withIndex()) {
            if (page == 1 && rowIndex == 1) {
                panel.addView(menuText("MORE ACTIONS", 11f, TouchStyle.MUTED, true))
            }
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for ((symbol, label, action) in r) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = background(if (action == "LOCK") 0xFF2B65EF.toInt() else 0xFF19283E.toInt(), 17)
                    isClickable = true
                    isFocusable = true
                    contentDescription = label
                    setOnClickListener {
                        hapticFeedback(it)
                        execute(action)
                    }
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
            audioCard.addView(seek, LinearLayout.LayoutParams(-1, dp(48)))
            panel.addView(audioCard, LinearLayout.LayoutParams(-1, dp(96)).apply { topMargin = dp(10) })

            val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val navigate = listOf(Triple("↶", "Back", "BACK"),Triple("⌂", "Home", "HOME"),Triple("▤", "Recent", "RECENTS"))
            for ((icon,label,action) in navigate) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = background(TouchStyle.SURFACE, 16)
                    isClickable = true
                    setOnClickListener { hapticFeedback(it); execute(action) }
                }
                val iconText = menuText(icon, 25f, TouchStyle.MINT, true).apply { gravity = Gravity.CENTER }
                cell.addView(iconText, LinearLayout.LayoutParams(-1, dp(32)))
                val textLabel = menuText(label, 11f).apply { gravity = Gravity.CENTER }
                cell.addView(textLabel, LinearLayout.LayoutParams(-1, dp(21)))
                nav.addView(cell, LinearLayout.LayoutParams(0, dp(68), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            panel.addView(nav, LinearLayout.LayoutParams(-1, dp(75)).apply { topMargin = dp(7) })
        }
        val more = menuText(if (page == 0) "More tools   ›" else "‹   Back to quick controls", 14f, accent, true).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = if (page == 0) "Show more tools" else "Back to quick controls"
            setOnClickListener { closeMenu(); showMenuPage(if (page == 0) 1 else 0) }
        }
        panel.addView(more, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) })

        // Keep the full menu reachable with larger fonts or limited landscape height.
        val maxHeight = (resources.displayMetrics.heightPixels - dp(40)).coerceAtLeast(dp(160))
        val scroll = object : ScrollView(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(
                    widthMeasureSpec,
                    View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
                )
            }
        }.apply {
            isVerticalScrollBarEnabled = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(panel)
        }
        val screen = resources.displayMetrics.widthPixels
        val width = minOf((screen - dp(24)).coerceAtLeast(dp(160)), dp(380))
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try { windowManager.addView(scroll, params); menuView = scroll }
        catch (_: Exception) { toast("تعذر فتح القائمة") }
    }

    private fun closeMenu() {
        val view = menuView ?: return
        try { windowManager.removeView(view) } catch (_: Exception) { }
        menuView = null
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

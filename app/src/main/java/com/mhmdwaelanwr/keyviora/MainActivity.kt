package com.mhmdwaelanwr.keyviora

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/** Native Android Views implementation of the Figma KEYVIORA screen system. */
class MainActivity : Activity() {
    companion object {
        const val EXTRA_REQUEST_CAMERA_PERMISSION = "com.mhmdwaelanwr.keyviora.REQUEST_CAMERA"
    }

    private var tab = 0
    private lateinit var page: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private val prefs get() = TouchPrefs.prefs(this)
    private fun dp(n: Int) = TouchStyle.dp(this, n)
    private val svc get() = TouchAccessibilityService.instance
    private val audio by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private fun color() = prefs.getInt(TouchPrefs.ACCENT, TouchStyle.BLUE)
    private fun accentTextColor() = TouchStyle.onAccent(color())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getInt("tab", 0) ?: 0
        window.statusBarColor = TouchStyle.BG
        window.navigationBarColor = TouchStyle.BG
        window.decorView.systemUiVisibility = 0
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePermissionIntent(intent)
    }

    private fun handlePermissionIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_REQUEST_CAMERA_PERMISSION, false) == true) {
            intent.removeExtra(EXTRA_REQUEST_CAMERA_PERMISSION)
            requestCamera()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tab", tab)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::page.isInitialized) render()
        handlePermissionIntent(intent)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun text(label: String, size: Float = 15f, tint: Int = TouchStyle.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = label
            textSize = size
            setTextColor(tint)
            typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            includeFontPadding = true
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private fun pad(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom))
    }
    private fun add(parent: LinearLayout, child: View, top: Int = 0,
                    width: Int = ViewGroup.LayoutParams.MATCH_PARENT, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        parent.addView(child, LinearLayout.LayoutParams(width, height).apply { topMargin = dp(top) })
    }
    private fun space(parent: LinearLayout, h: Int) = add(parent, View(this), 0, -1, dp(h))
    private fun card(radius: Int = 24, background: Int = TouchStyle.SURFACE): LinearLayout = column().apply {
        this.background = TouchStyle.bg(this@MainActivity, background, radius, TouchStyle.BORDER)
        pad(this, 18, 17, 18, 18)
    }
    private fun section(title: String) {
        add(page, text(title, 11f, TouchStyle.MUTED, true), 25)
        space(page, 9)
    }
    private fun labelRow(title: String, subtitle: String, right: String? = null): LinearLayout {
        val r = row()
        val labels = column()
        add(labels, text(title, 15f, TouchStyle.WHITE, true))
        add(labels, text(subtitle, 12f, TouchStyle.MUTED), 5)
        r.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        if (right != null) add(r, text(right, 13f, color(), true), 0, -2)
        return r
    }
    private fun roundedButton(label: String, primary: Boolean = true, handler: () -> Unit): TextView =
        text(label, 14f, if (primary) accentTextColor() else TouchStyle.MINT, true).apply {
            gravity = Gravity.CENTER
            background = TouchStyle.bg(this@MainActivity, if (primary) color() else 0xFF17443C.toInt(), 16)
            minimumHeight = dp(46)
            isClickable = true
            isFocusable = true
            setOnClickListener { handler() }
        }
    private fun chip(label: String, bg: Int, fg: Int): TextView =
        text(label, 10f, fg, true).apply {
            gravity = Gravity.CENTER
            background = TouchStyle.bg(this@MainActivity, bg, 40)
            pad(this, 10, 7, 10, 7)
        }

    private fun render() {
        val shell = column().apply { setBackgroundColor(TouchStyle.BG) }
        val scroller = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(TouchStyle.BG)
        }
        page = column().apply { pad(this, 20, 14, 20, 25) }
        scroller.addView(page)
        shell.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        bottomNav = row().apply {
            setBackgroundColor(TouchStyle.SURFACE)
            pad(this, 9, 7, 9, 7)
        }
        shell.addView(bottomNav, LinearLayout.LayoutParams(-1, dp(66)))
        setContentView(shell)
        when (tab) {
            0 -> home()
            1 -> appearance()
            2 -> gestures()
            else -> setup()
        }
        renderNav()
    }

    private fun renderNav() {
        bottomNav.removeAllViews()
        val items = arrayOf("⌂" to "Home", "◈" to "Style", "◎" to "Gestures", "⚙" to "Setup")
        for ((i, item) in items.withIndex()) {
            val selected = i == tab
            val cell = column().apply {
                gravity = Gravity.CENTER
                background = TouchStyle.bg(this@MainActivity, if (selected) 0xFF203D69.toInt() else TouchStyle.SURFACE, 17)
                isClickable = true
                isFocusable = true
                contentDescription = item.second
                setOnClickListener { tab = i; render() }
            }
            add(cell, text(item.first, 22f, if (selected) color() else TouchStyle.MUTED, true).apply { gravity = Gravity.CENTER }, 0)
            add(cell, text(item.second, 10f, if (selected) TouchStyle.WHITE else TouchStyle.MUTED).apply { gravity = Gravity.CENTER })
            bottomNav.addView(cell, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(3); rightMargin = dp(3) })
        }
    }

    private fun runAction(action: String) {
        val service = svc
        if (service == null) {
            AlertDialog.Builder(this)
                .setTitle("Enable KEYVIORA")
                .setMessage("Accessibility is needed for on-screen Lock, Power and navigation controls. Enable KEYVIORA Controls in system settings.")
                .setPositiveButton("Open settings") { _, _ -> openAccessibility() }
                .setNegativeButton("Cancel", null).show()
        } else service.performNamedAction(action)
    }
    private fun openAccessibility() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun home() {
        val brand = row()
        add(brand, text("✦", 29f, color(), true), 0, dp(36))
        brand.addView(text("KEYVIORA", 16f, TouchStyle.WHITE, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val active = svc != null
        add(brand, chip(if (active) "● ACTIVE" else "● SET UP", if (active) 0xFF123F3A.toInt() else 0xFF463628.toInt(), if (active) TouchStyle.MINT else TouchStyle.AMBER), 0, -2)
        add(page, brand)
        add(page, text("One touch.", 34f, TouchStyle.WHITE, true), 23)
        add(page, text("Total control.", 34f, color(), true))
        add(page, text("بديل أزرار الباور والصوت على HONOR 50", 14f, TouchStyle.MUTED), 6)

        val hero = FrameLayout(this).apply { background = TouchStyle.bg(this@MainActivity, TouchStyle.SURFACE, 26, TouchStyle.BORDER) }
        val heroText = column().apply { pad(this, 17, 17, 0, 0) }
        add(heroText, text("Floating control", 18f, TouchStyle.WHITE, true))
        add(heroText, text("Ready anywhere on screen", 12f, TouchStyle.MUTED), 7)
        add(heroText, chip(if (active) "● SERVICE RUNNING" else "● ENABLE SERVICE", if (active) 0xFF17443C.toInt() else 0xFF453829.toInt(), if (active) TouchStyle.MINT else TouchStyle.AMBER), 24, -2)
        hero.addView(heroText, FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.TOP))
        val halo = FrameLayout(this).apply { background = TouchStyle.oval(this@MainActivity, 0xFF1B3E72.toInt()) }
        val ring = FrameLayout(this).apply { background = TouchStyle.oval(this@MainActivity, 0xFF2F5591.toInt()) }
        val middle = FrameLayout(this).apply { background = TouchStyle.oval(this@MainActivity, color()) }
        ring.addView(middle, FrameLayout.LayoutParams(dp(36), dp(36), Gravity.CENTER))
        halo.addView(ring, FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER))
        halo.contentDescription = if (active) "Open floating quick controls" else "Enable KEYVIORA accessibility"
        halo.isFocusable = true
        halo.isClickable = true
        halo.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (active) runAction("MENU") else openAccessibility()
        }
        hero.addView(halo, FrameLayout.LayoutParams(dp(90), dp(90), Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = dp(15) })
        add(page, hero, 25, -1, dp(145))
        if (!active) {
            val start = card()
            add(start, text("Get your on-screen buttons working", 17f, TouchStyle.WHITE, true))
            add(start, text("1. Enable KEYVIORA Controls in Accessibility.  2. Come back here.  3. Tap the floating button to open the controls.", 13f, TouchStyle.MUTED), 8)
            add(start, roundedButton("Enable accessibility →") { openAccessibility() }, 14)
            add(page, start, 14)
        } else {
            val ready = row().apply {
                background = TouchStyle.bg(this@MainActivity, 0xFF123F3A.toInt(), 16)
                pad(this, 14, 12, 14, 12)
            }
            ready.addView(text("✓  Controls ready", 13f, TouchStyle.MINT, true),
                LinearLayout.LayoutParams(0, -2, 1f))
            val action = roundedButton(
                if (prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true)) "Hide button" else "Show button",
                false
            ) {
                svc?.setBubbleVisible(!prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true))
                render()
            }
            ready.addView(action, LinearLayout.LayoutParams(dp(120), dp(48)))
            add(page, ready, 14)
        }

        section("YOUR ESSENTIALS")
        val grid = column()
        val data = listOf(
            listOf(Triple("⏣", "Lock screen", "LOCK"), Triple("⏻", "Power menu", "POWER")),
            listOf(Triple("＋", "Volume up", "VOLUME_UP"), Triple("−", "Volume down", "VOLUME_DOWN"))
        )
        data.forEachIndexed { ri, entries ->
            val r = row()
            entries.forEachIndexed { idx, a ->
                val selected = ri == 0 && idx == 0
                val item = column().apply {
                    pad(this, 13, 10, 12, 8)
                    background = TouchStyle.bg(this@MainActivity, if (selected) TouchStyle.BLUE_DARK else TouchStyle.TILE, 20, if (selected) null else TouchStyle.BORDER)
                    isClickable = true
                    isFocusable = true
                    contentDescription = a.second
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        runAction(a.third)
                    }
                }
                val glyph = text(a.first, 23f, if (selected) TouchStyle.WHITE else color(), true).apply {
                    gravity = Gravity.CENTER
                    background = TouchStyle.oval(this@MainActivity, if (selected) 0xFF457FF4.toInt() else 0xFF243B59.toInt())
                }
                add(item, glyph, 0, dp(40), dp(40))
                add(item, text(a.second, 13f, TouchStyle.WHITE, true), 12)
                if (ri == 0) add(item, text(if (idx == 0) "Most used" else "System dialog", 11f, if (selected) 0xFFD5E2FB.toInt() else TouchStyle.MUTED))
                r.addView(item, LinearLayout.LayoutParams(0, dp(if (ri == 0) 108 else 89), 1f).apply {
                    if (idx > 0) leftMargin = dp(11)
                })
            }
            add(grid, r, if (ri > 0) 11 else 0)
        }
        add(page, grid)
        val card = card()
        val volumeHeader = labelRow("◖))   Media volume", "Drag to adjust media sound", "${volumePercent()}%")
        add(card, volumeHeader)
        val volumeLabel = volumeHeader.getChildAt(volumeHeader.childCount - 1) as TextView
        val bar = volumeSeekBar { volumeLabel.text = "${volumePercent()}%" }
        add(card, bar, 8, -1, dp(32))
        add(page, card, 17)
        if (active && !prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true)) {
            add(page, roundedButton("Restore floating button") {
                svc?.setBubbleVisible(true)
                render()
            }, 16)
        }
        add(page, text("Tip: Drag the floating button to move it. Set single, double and long taps in Gestures.", 12f, TouchStyle.MUTED), 16)
    }

    private fun volumePercent(): Int {
        val maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return (100f * audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maximum).roundToInt()
    }
    private fun volumeSeekBar(onVolumeChanged: (() -> Unit)? = null): SeekBar = SeekBar(this).apply {
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        max = maxVolume
        progress = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        progressTintList = ColorStateList.valueOf(color())
        thumbTintList = ColorStateList.valueOf(TouchStyle.WHITE)
        progressBackgroundTintList = ColorStateList.valueOf(0xFF354A68.toInt())
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) {
                    try {
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                        onVolumeChanged?.invoke()
                    } catch (_: Exception) { toast("Could not adjust media volume") }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun appearance() {
        add(page, text("Appearance", 30f, TouchStyle.WHITE, true), 12)
        add(page, text("Make it feel like yours.", 14f, TouchStyle.MUTED), 5)
        val preview = column().apply {
            gravity = Gravity.CENTER
            background = TouchStyle.bg(this@MainActivity, TouchStyle.SURFACE, 25, TouchStyle.BORDER)
        }
        add(preview, text("LIVE PREVIEW", 11f, TouchStyle.MUTED, true).apply { gravity = Gravity.CENTER })
        val previewSize = prefs.getInt(TouchPrefs.SIZE, 60).coerceIn(40, 88)
        val outer = FrameLayout(this).apply {
            background = TouchStyle.oval(this@MainActivity, 0xFF203B66.toInt())
            alpha = prefs.getInt(TouchPrefs.OPACITY, 85).coerceIn(35, 100) / 100f
        }
        val mid = FrameLayout(this).apply { background = TouchStyle.oval(this@MainActivity, 0xFF38639E.toInt()) }
        val inner = View(this).apply { background = TouchStyle.oval(this@MainActivity, color()) }
        mid.addView(inner, FrameLayout.LayoutParams(dp(previewSize * 2 / 5), dp(previewSize * 2 / 5), Gravity.CENTER))
        outer.addView(mid, FrameLayout.LayoutParams(dp(previewSize * 4 / 5), dp(previewSize * 4 / 5), Gravity.CENTER))
        add(preview, outer, 14, dp(previewSize), dp(previewSize))
        add(preview, text("Preview of your floating control", 12f, TouchStyle.MUTED).apply { gravity = Gravity.CENTER }, 9)
        add(preview, chip("CUSTOM ACCENT", 0xFF253A5C.toInt(), TouchStyle.WHITE), 11, -2)
        add(page, preview, 25, -1, dp(221))

        val controls = card()
        add(controls, text("Floating button", 18f, TouchStyle.WHITE, true))
        add(controls, labelRow("Size", "Button diameter", "${prefs.getInt(TouchPrefs.SIZE,60)} dp"), 17)
        slider(controls, TouchPrefs.SIZE, 48, 88, 60)
        add(controls, labelRow("Opacity", "How visible the orb is", "${prefs.getInt(TouchPrefs.OPACITY,85)}%"), 12)
        slider(controls, TouchPrefs.OPACITY, 35, 100, 85)
        add(controls, roundedButton(
            if (prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true)) "Hide floating control" else "Show floating control",
            false
        ) { toggleBubble() }, 14)
        add(controls, roundedButton("Reset bubble position", false) {
            if (svc == null) openAccessibility()
            else {
                svc?.resetBubblePosition()
                toast("Floating button moved to the right edge")
                render()
            }
        }, 10)
        add(page, controls, 18)
        section("ACCENT COLORS")
        val colors = row().apply { gravity = Gravity.CENTER }
        TouchStyle.accentColors.forEachIndexed { i, c ->
            val selected = c == color()
            val v = text(if (selected) "✓" else "", 18f, TouchStyle.onAccent(c), true).apply {
                gravity = Gravity.CENTER
                contentDescription = "Color ${i + 1}"
                background = TouchStyle.oval(this@MainActivity, c, if (selected) TouchStyle.WHITE else null)
                isClickable = true
                setOnClickListener {
                    prefs.edit().putInt(TouchPrefs.ACCENT, c).apply()
                    svc?.refreshAppearance()
                    render()
                }
            }
            colors.addView(v, LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                leftMargin = dp(5); rightMargin = dp(5)
            })
        }
        add(page, colors, 5)
        val helpers = card()
        add(helpers, text("Comfort & accessibility", 16f, TouchStyle.WHITE, true))
        add(helpers, text("The floating control uses no decorative animations. Keep size at 56 dp or larger for an easier touch target.", 13f, TouchStyle.MUTED), 9)
        add(helpers, text("Tip: place the bubble near your thumb's reach. Use Reset bubble position if you lose it.", 13f, TouchStyle.MUTED), 12)
        add(page, helpers, 22)
    }

    private fun slider(parent: LinearLayout, key: String, min: Int, max: Int, initial: Int) {
        val seek = SeekBar(this).apply {
            this.max = max - min
            progress = prefs.getInt(key, initial).coerceIn(min,max) - min
            progressTintList = ColorStateList.valueOf(color())
            thumbTintList = ColorStateList.valueOf(TouchStyle.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(0xFF354A68.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        prefs.edit().putInt(key, progress + min).apply()
                        svc?.refreshAppearance()
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) { render() }
            })
        }
        add(parent, seek, 3, -1, dp(34))
    }

    private fun gestures() {
        add(page, text("Touch gestures", 30f, TouchStyle.WHITE, true), 12)
        add(page, text("Choose exactly what each tap on the floating control does.", 14f, TouchStyle.MUTED), 4)
        val hint = card()
        add(hint, text("Recommended setup", 15f, TouchStyle.WHITE, true))
        add(hint, text("Single tap → Quick menu   •   Double tap → Screenshot   •   Hold → Lock screen", 13f, TouchStyle.MUTED), 8)
        add(hint, text("Tip: A double tap waits briefly to distinguish it from a single tap.", 12f, TouchStyle.MUTED), 8)
        add(page, hint, 19)
        val actions = card()
        val taps = arrayOf(
            Triple("◎", "Single tap", TouchPrefs.SINGLE),
            Triple("◉", "Double tap", TouchPrefs.DOUBLE),
            Triple("◌", "Long press", TouchPrefs.LONG)
        )
        taps.forEachIndexed { index, item ->
            val key = item.third
            val fallback = when (index) { 0 -> "MENU"; 1 -> "SCREENSHOT"; else -> "LOCK" }
            val currentAction = prefs.getString(key, fallback) ?: fallback
            val label = TouchPrefs.actionKeys.indexOf(currentAction).takeIf { it >= 0 }?.let { TouchPrefs.actionLabels[it] } ?: currentAction
            val r = row().apply {
                minimumHeight = dp(86)
                isClickable = true
                isFocusable = true
                contentDescription = "${item.second}, current action: $label. Double tap to change"
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    selectAction(key, fallback)
                }
            }
            add(r, text(item.first, 22f, color(), true).apply {
                gravity = Gravity.CENTER
                background = TouchStyle.oval(this@MainActivity, 0xFF213B5D.toInt())
            }, 0, dp(48), dp(48))
            val copy = column().apply { pad(this, 14, 0, 0, 0) }
            add(copy, text(item.second, 16f, TouchStyle.WHITE, true))
            add(copy, text(label, 13f, TouchStyle.MUTED), 6)
            r.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
            add(r, text("›", 23f, TouchStyle.MUTED), 0, dp(20))
            add(actions, r)
            if (index < 2) add(actions, View(this).apply { setBackgroundColor(TouchStyle.BORDER) }, 2, -1, dp(1))
        }
        add(page, actions, 18)
        add(page, roundedButton("Restore recommended gestures", false) {
            AlertDialog.Builder(this)
                .setTitle("Restore default gestures?")
                .setMessage("Single tap opens the menu, double tap takes a screenshot and long press locks the screen. Your appearance settings will stay unchanged.")
                .setPositiveButton("Restore") { _, _ ->
                    prefs.edit()
                        .putString(TouchPrefs.SINGLE, "MENU")
                        .putString(TouchPrefs.DOUBLE, "SCREENSHOT")
                        .putString(TouchPrefs.LONG, "LOCK")
                        .apply()
                    render()
                    toast("Recommended gestures restored")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }, 12)
        section("TRY A QUICK ACTION")
        val grid = column()
        listOf(
            Triple("⏣", "Screen lock", "LOCK"), Triple("▣", "Screenshot", "SCREENSHOT"),
            Triple("＋", "Volume up", "VOLUME_UP"), Triple("−", "Volume down", "VOLUME_DOWN"),
            Triple("☼", "Flashlight", "FLASHLIGHT"), Triple("⏻", "Power menu", "POWER")
        ).chunked(2).forEachIndexed { ri, items ->
            val r = row()
            items.forEachIndexed { ci, item ->
                val tile = row().apply {
                    pad(this, 12, 8, 12, 8)
                    background = TouchStyle.bg(this@MainActivity, TouchStyle.TILE, 17, TouchStyle.BORDER)
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        runAction(item.third)
                    }
                    isClickable = true
                    isFocusable = true
                    contentDescription = item.second
                }
                add(tile, text(item.first, 22f, color(), true), 0, dp(31))
                tile.addView(text(item.second, 12f, TouchStyle.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
                r.addView(tile, LinearLayout.LayoutParams(0, dp(75), 1f).apply { if (ci > 0) leftMargin = dp(10) })
            }
            add(grid, r, if (ri > 0) 10 else 0)
        }
        add(page, grid)
    }

    private fun toggleBubble() {
        if (svc == null) { openAccessibility(); return }
        val next = !prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true)
        svc?.setBubbleVisible(next)
        toast(if (next) "Floating control shown" else "Floating control hidden — open KEYVIORA to restore it")
        render()
    }

    private fun selectAction(key: String, fallback: String) {
        val current = prefs.getString(key, fallback)
        val selected = TouchPrefs.actionKeys.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Choose an action  •  اختر الوظيفة")
            .setSingleChoiceItems(TouchPrefs.actionLabels, selected) { dialog, index ->
                val chosen = TouchPrefs.actionKeys[index]
                dialog.dismiss()
                if (chosen == "HIDE") {
                    AlertDialog.Builder(this)
                        .setTitle("Hide floating button?")
                        .setMessage("Using this gesture will hide the button. Open KEYVIORA again to restore it. Choose this only if you have another way to open the app.")
                        .setPositiveButton("Use Hide") { _, _ -> saveGesture(key, chosen) }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    saveGesture(key, chosen)
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun saveGesture(key: String, action: String) {
        prefs.edit().putString(key, action).apply()
        toast("Shortcut updated")
        render()
    }

    private fun setup() {
        add(page, text("Safety & setup", 30f, TouchStyle.WHITE, true), 12)
        add(page, text("See what's ready, enable what's missing.", 13f, TouchStyle.MUTED), 4)
        val ready = svc != null
        val visible = prefs.getBoolean(TouchPrefs.BUBBLE_VISIBLE, true)
        val cameraReady = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val status = card()
        add(status, text(if (ready && visible) "✓ KEYVIORA is ready" else "Finish setting up KEYVIORA", 18f, if (ready && visible) TouchStyle.MINT else TouchStyle.AMBER, true))
        add(status, labelRow("Accessibility service", "Required for floating controls", if (ready) "READY" else "NEEDED"), 13)
        add(status, labelRow("Floating button", "Restore it here whenever you hide it", if (!ready) "WAITING" else if (visible) "VISIBLE" else "HIDDEN"), 11)
        add(status, labelRow("Flashlight access", "Optional: only required for Torch", if (cameraReady) "ALLOWED" else "OPTIONAL"), 11)
        add(page, status, 22)
        val serviceCard = card()
        add(serviceCard, labelRow("Accessibility service", "Required for lock and gestures", if (svc != null) "ON" else "OFF"))
        add(serviceCard, roundedButton(if (svc == null) "Enable accessibility" else "Accessibility settings") { openAccessibility() }, 17)
        if (ready) add(serviceCard, roundedButton(if (visible) "Hide floating button" else "Show floating button", false) { toggleBubble() }, 10)
        add(page, serviceCard, 26)
        section("GET STARTED")
        val guide = card()
        val steps = listOf(
            Triple("1", "Enable Accessibility", "Allow KEYVIORA Controls"),
            Triple("2", "Allow camera access", "Only needed for flashlight"),
            Triple("3", "Set up screen wake", "Enable double tap or raise to wake on HONOR")
        )
        steps.forEachIndexed { index, entry ->
            val item = row().apply {
                minimumHeight = dp(68)
                setOnClickListener {
                    when (index) {
                        0 -> openAccessibility()
                        1 -> requestCamera()
                        else -> startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                    }
                }
                isClickable = true
            }
            item.isFocusable = true
            item.contentDescription = "${entry.second}. ${entry.third}"
            add(item, text(if (index == 0 && ready || index == 1 && cameraReady) "✓" else entry.first, 16f, TouchStyle.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = TouchStyle.oval(this@MainActivity, 0xFF254679.toInt())
            }, 0, dp(40), dp(40))
            val pair = column().apply { pad(this, 14, 0, 0, 0) }
            add(pair, text(entry.second, 15f, TouchStyle.WHITE, true))
            add(pair, text(entry.third, 12f, TouchStyle.MUTED), 4)
            item.addView(pair, LinearLayout.LayoutParams(0, -2, 1f))
            add(guide, item, if (index > 0) 8 else 0)
        }
        add(page, guide)
        add(page, text("If your HONOR automatically closes the service, review battery optimization for KEYVIORA in Android App info.", 12f, TouchStyle.MUTED), 16)
        add(page, roundedButton("Open battery / app settings", false) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, 18)
        val warning = column().apply {
            background = TouchStyle.bg(this@MainActivity, TouchStyle.WARN_BG, 22, 0xFF725063.toInt())
            pad(this, 17, 15, 17, 17)
        }
        add(warning, text("!  IMPORTANT", 14f, TouchStyle.AMBER, true))
        add(warning, text("A fully powered-off phone can't be restarted by this app.", 15f, TouchStyle.WHITE, true), 12)
        add(warning, text("Repair the physical power button when possible.", 12f, TouchStyle.MUTED), 7)
        add(page, warning, 20)
        add(page, text("Offline • No ads • No content reading", 12f, TouchStyle.MINT).apply { gravity = Gravity.CENTER }, 25)
    }

    private fun requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            toast("Camera permission already granted")
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            render()
            toast(
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                    "Camera enabled. Tap Torch again to turn it on."
                else "Camera permission denied. Flashlight cannot be used."
            )
        }
    }
}

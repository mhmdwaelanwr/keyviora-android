package com.mhmdwaelanwr.keyviora

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/** Shared palette mirrored from the editable Figma KEYVIORA design. */
object TouchStyle {
    val BG = 0xFF080E1A.toInt()
    val SURFACE = 0xFF121D2E.toInt()
    val TILE = 0xFF19283E.toInt()
    val BORDER = 0xFF2B3E56.toInt()
    val BLUE = 0xFF5B93FF.toInt()
    val BLUE_DARK = 0xFF2B65EF.toInt()
    val MINT = 0xFF58DEBE.toInt()
    val WHITE = 0xFFF5F8FF.toInt()
    val MUTED = 0xFF9BAFC9.toInt()
    val AMBER = 0xFFFFCF85.toInt()
    val WARN_BG = 0xFF2C2430.toInt()
    val accentColors = intArrayOf(BLUE, MINT, Color.rgb(167,139,250), AMBER,
        Color.rgb(251,113,133), WHITE)

    /** Choose readable foreground text for light accent colors (mint, amber, white). */
    fun onAccent(color: Int): Int {
        val perceivedBrightness = (Color.red(color) * 299 +
            Color.green(color) * 587 + Color.blue(color) * 114) / 1000
        return if (perceivedBrightness > 160) BG else WHITE
    }

    fun dp(context: Context, number: Int): Int = (number * context.resources.displayMetrics.density + 0.5f).toInt()

    fun bg(context: Context, color: Int, radius: Int = 20, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radius).toFloat()
            if (stroke != null) setStroke(dp(context, 1), stroke)
        }
    fun oval(context: Context, color: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (stroke != null) setStroke(dp(context, 2), stroke)
        }
}

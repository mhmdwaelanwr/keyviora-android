package com.mhmdwaelanwr.keyviora

import android.content.Context

object TouchPrefs {
    private const val NAME = "keyviora_preferences"
    const val ACCENT = "accent_color"
    const val SIZE = "bubble_size"
    const val OPACITY = "bubble_opacity"
    const val BUBBLE_VISIBLE = "bubble_visible"
    const val X = "bubble_x"
    const val Y = "bubble_y"
    const val SINGLE = "single_action"
    const val DOUBLE = "double_action"
    const val LONG = "long_action"
    const val SNAP_TO_EDGE = "snap_to_edge"

    // Three user-configurable tiles at the top of the More tools panel.
    val favoriteKeys = arrayOf("favorite_1", "favorite_2", "favorite_3")
    val favoriteDefaults = arrayOf("WIFI", "NOTIFICATIONS", "APP")

    val actionKeys = TouchActionCatalog.actions.map { it.key }.toTypedArray()
    val actionLabels = TouchActionCatalog.actions.map { it.label }.toTypedArray()

    fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}

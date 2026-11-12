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

    val actionKeys = arrayOf(
        "MENU", "LOCK", "POWER", "SCREENSHOT", "VOLUME_UP", "VOLUME_DOWN",
        "HOME", "BACK", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS", "FLASHLIGHT", "HIDE"
    )
    val actionLabels = arrayOf(
        "فتح القائمة", "قفل الشاشة", "قائمة الباور", "لقطة شاشة", "رفع الصوت", "خفض الصوت",
        "الرئيسية", "رجوع", "التطبيقات الأخيرة", "الإشعارات", "الإعدادات السريعة", "الكشاف", "إخفاء الزر"
    )

    fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}

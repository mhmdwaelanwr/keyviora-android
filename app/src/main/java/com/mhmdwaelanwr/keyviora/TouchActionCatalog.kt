package com.mhmdwaelanwr.keyviora

/** Single source of truth for quick controls and user-configurable shortcuts. */
internal data class TouchAction(
    val key: String,
    val label: String,
    val glyph: String,
    val shortLabel: String
)

internal object TouchActionCatalog {
    val actions: List<TouchAction> = listOf(
        TouchAction("MENU", "فتح القائمة", "☷", "Menu"),
        TouchAction("LOCK", "قفل الشاشة", "⏣", "Lock"),
        TouchAction("POWER", "قائمة الباور", "⏻", "Power"),
        TouchAction("SCREENSHOT", "لقطة شاشة", "▣", "Screenshot"),
        TouchAction("VOLUME_UP", "رفع الصوت", "＋", "Vol up"),
        TouchAction("VOLUME_DOWN", "خفض الصوت", "−", "Vol down"),
        TouchAction("HOME", "الرئيسية", "⌂", "Home"),
        TouchAction("BACK", "رجوع", "↶", "Back"),
        TouchAction("RECENTS", "التطبيقات الأخيرة", "▤", "Recent"),
        TouchAction("NOTIFICATIONS", "الإشعارات", "♧", "Notices"),
        TouchAction("QUICK_SETTINGS", "الإعدادات السريعة", "☷", "Quick panel"),
        TouchAction("FLASHLIGHT", "الكشاف", "☼", "Torch"),
        TouchAction("WIFI", "إعدادات Wi-Fi", "◉", "Wi-Fi"),
        TouchAction("BLUETOOTH", "إعدادات Bluetooth", "ᛒ", "Bluetooth"),
        TouchAction("DISPLAY", "إعدادات الشاشة", "◐", "Display"),
        TouchAction("SOUND", "إعدادات الصوت", "♫", "Sound"),
        TouchAction("APP", "فتح KEYVIORA", "⚙", "App"),
        TouchAction("HIDE", "إخفاء الزر", "✕", "Hide"),
        TouchAction("NONE", "بدون إجراء", "○", "None")
    )

    private val byKey = actions.associateBy { it.key }

    fun resolve(saved: String?, fallback: String): TouchAction =
        byKey[saved] ?: byKey[fallback] ?: requireNotNull(byKey["MENU"])
}

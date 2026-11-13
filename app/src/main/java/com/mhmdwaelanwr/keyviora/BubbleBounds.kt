package com.mhmdwaelanwr.keyviora

/**
 * Bounds an accessibility overlay along one axis.
 * The button remains recoverable after drag, screen rotation or size changes.
 */
internal object BubbleBounds {
    fun clamp(position: Int, displaySize: Int, bubbleSize: Int): Int {
        val maximum = (displaySize.toLong() - bubbleSize.toLong()).coerceAtLeast(0)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return position.coerceIn(0, maximum)
    }

    /** Respect status/navigation-bar comfort zones, even on tiny displays. */
    fun clampWithinInsets(
        position: Int, displaySize: Int, bubbleSize: Int, startInset: Int, endInset: Int
    ): Int {
        val maximum = clamp(Int.MAX_VALUE, displaySize, bubbleSize)
        val first = startInset.coerceAtLeast(0).coerceAtMost(maximum)
        val last = (maximum - endInset.coerceAtLeast(0)).coerceAtLeast(first)
        return position.coerceIn(first, last)
    }

    /** Choose the nearest side without moving the control outside the display. */
    fun snapToEdge(position: Int, displaySize: Int, bubbleSize: Int, inset: Int): Int {
        val end = clamp(Int.MAX_VALUE, displaySize, bubbleSize)
        val margin = inset.coerceAtLeast(0).coerceAtMost(end / 2)
        val left = margin
        val right = end - margin
        val current = clamp(position, displaySize, bubbleSize)
        return if (current.toLong() - left <= right.toLong() - current) left else right
    }
}

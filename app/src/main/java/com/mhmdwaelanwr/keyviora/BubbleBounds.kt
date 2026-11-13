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
}

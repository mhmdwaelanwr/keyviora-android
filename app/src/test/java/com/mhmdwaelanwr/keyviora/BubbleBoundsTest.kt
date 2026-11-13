package com.mhmdwaelanwr.keyviora

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleBoundsTest {
    @Test fun preservesValidPositions() {
        assertEquals(120, BubbleBounds.clamp(120, 1080, 80))
    }

    @Test fun clampsNegativePositionsToZero() {
        assertEquals(0, BubbleBounds.clamp(-85, 1080, 80))
    }

    @Test fun clampsPositionBeyondScreen() {
        assertEquals(1000, BubbleBounds.clamp(1080, 1080, 80))
    }

    @Test fun handlesButtonLargerThanDisplay() {
        assertEquals(0, BubbleBounds.clamp(70, 30, 88))
    }

    @Test fun edgeSnapChoosesNearestSide() {
        assertEquals(12, BubbleBounds.snapToEdge(25, 1080, 80, 12))
        assertEquals(988, BubbleBounds.snapToEdge(800, 1080, 80, 12))
    }

    @Test fun edgeSnapHandlesSmallScreensAndMargins() {
        assertEquals(0, BubbleBounds.snapToEdge(100, 30, 88, 12))
        assertEquals(10, BubbleBounds.snapToEdge(10, 100, 80, 50))
    }

    @Test fun handlesZeroWidthAndExtremeValues() {
        assertEquals(0, BubbleBounds.clamp(Int.MAX_VALUE, 0, 0))
        assertEquals(0, BubbleBounds.clamp(Int.MIN_VALUE, 300, 80))
    }
}

package org.pixelduo

import org.junit.Assert.*
import org.junit.Test

class FoldMotionTest {
    @Test fun hingeAndPowerPolicy() {
        assertNull(FoldMotion.normalize(Float.NaN))
        assertNull(FoldMotion.normalize(Float.POSITIVE_INFINITY))
        assertEquals(0f, FoldMotion.normalize(-10f)!!, 0f)
        assertEquals(180f, FoldMotion.normalize(200f)!!, 0f)
        assertEquals(0f, FoldMotion.strength(180f, true), 0f)
        assertEquals(1f, FoldMotion.strength(90f, true), 0f)
        assertEquals(0f, FoldMotion.strength(0f, false), 0f)
        assertEquals(1f, FoldMotion.strength(90f, false), 0f)
        for (degrees in 0..180) for (inner in listOf(false, true)) {
            assertTrue(FoldMotion.strength(degrees.toFloat(), inner) in 0f..1f)
        }
        var at60 = 90f
        var at120 = 90f
        repeat(12) { at60 = FoldMotion.smooth(at60, 172f, 1000f / 60f) }
        repeat(24) { at120 = FoldMotion.smooth(at120, 172f, 1000f / 120f) }
        assertEquals(at60, at120, 0.1f)
        for (mask in 0..15) {
            assertEquals(mask == 11, FoldMotion.shouldAnimate(mask and 1 != 0,
                mask and 2 != 0, mask and 4 != 0, mask and 8 != 0))
        }
    }
}

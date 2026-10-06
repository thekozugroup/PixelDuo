package org.pixelduo

import kotlin.math.abs
import kotlin.math.exp

/** Public sensor angles are degrees. Endpoint tolerance accommodates hinge calibration. */
internal object FoldMotion {
    const val OPEN = 172f
    const val CLOSED = 2f
    const val IDLE_MS = 1200L
    fun normalize(value: Float): Float? = if (value.isFinite()) value.coerceIn(0f, 180f) else null
    fun strength(angle: Float, inner: Boolean): Float =
        (if (inner) (OPEN - angle) / (OPEN - 90f) else angle / 90f).coerceIn(0f, 1f)

    // Adapted from Duo Fold Live's FrameSmoothing: independent of refresh rate.
    fun smooth(current: Float, target: Float, elapsedMs: Float): Float {
        val next = current + (target - current) * (1f - exp(-elapsedMs.coerceIn(0f, 50f) / 24f))
        return if (abs(next - target) < 0.05f) target else next
    }
    fun shouldAnimate(enabled: Boolean, interactive: Boolean, powerSave: Boolean, animations: Boolean) =
        enabled && interactive && !powerSave && animations
}

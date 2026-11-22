package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopSmoothWheelScrollTest {

    @Test
    fun computeSmoothWheelScrollStep_decaysSmoothlyAndSnapsNearZero() {
        val first = computeSmoothWheelScrollStep(remainingPx = 200f, deltaSeconds = 1f / 60f)
        assertTrue(first.consumedDeltaPx > 0f && first.consumedDeltaPx < 200f)
        assertEquals(200f, first.consumedDeltaPx + first.remainingPx, absoluteTolerance = 0.01f)

        val tiny = computeSmoothWheelScrollStep(remainingPx = 0.4f, deltaSeconds = 1f / 60f)
        assertEquals(0.4f, tiny.consumedDeltaPx, absoluteTolerance = 0.001f)
        assertEquals(0f, tiny.remainingPx, absoluteTolerance = 0.001f)
    }

    @Test
    fun computeSmoothWheelScrollStep_handlesNegativeAndNonFiniteValuesSafely() {
        val upward = computeSmoothWheelScrollStep(remainingPx = -150f, deltaSeconds = 1f / 120f)
        assertTrue(upward.consumedDeltaPx < 0f && upward.remainingPx < 0f)
        assertEquals(-150f, upward.consumedDeltaPx + upward.remainingPx, absoluteTolerance = 0.01f)

        val nan = computeSmoothWheelScrollStep(remainingPx = Float.NaN, deltaSeconds = Float.NaN)
        assertEquals(0f, nan.consumedDeltaPx)
        assertEquals(0f, nan.remainingPx)
    }

    @Test
    fun accumulateSmoothWheelTarget_addsSameDirectionAndCancelsOpposingDirection() {
        assertEquals(192f, accumulateSmoothWheelTarget(96f, 96f))
        assertEquals(-96f, accumulateSmoothWheelTarget(180f, -96f))
        assertEquals(96f, accumulateSmoothWheelTarget(-180f, 96f))
    }

    @Test
    fun accumulateSmoothWheelTarget_clampsPendingBurstToMaxPending() {
        assertEquals(2_400f, accumulateSmoothWheelTarget(2_350f, 300f))
        assertEquals(-2_400f, accumulateSmoothWheelTarget(-2_350f, -300f))
        assertEquals(100f, accumulateSmoothWheelTarget(100f, Float.NaN))
    }
}

package com.nuvio.app.features.details.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DetailSeriesContentWheelScrollTest {

    @Test
    fun midRowVerticalWheelScrollsHorizontallyAndConsumes() {
        val forward = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = 1.5f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 1.5f, consume = true), forward)

        val backward = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = -1.5f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = -1.5f, consume = true), backward)
    }

    @Test
    fun lastEpisodeEdgeLocksAndConsumesForwardWheelWithoutDispatching() {
        val result = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = 2f,
            isRtl = false,
            canScrollForward = false,
            canScrollBackward = true,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 0f, consume = true), result)
    }

    @Test
    fun firstEpisodeEdgeLocksAndConsumesBackwardWheelWithoutDispatching() {
        val result = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = -2f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = false,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 0f, consume = true), result)
    }

    @Test
    fun edgeStillAllowsScrollingBackIntoOverflowingRow() {
        val backFromEnd = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = -1f,
            isRtl = false,
            canScrollForward = false,
            canScrollBackward = true,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = -1f, consume = true), backFromEnd)

        val forwardFromStart = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = 1f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = false,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 1f, consume = true), forwardFromStart)
    }

    @Test
    fun nonOverflowingRowYieldsToOuterVerticalScroll() {
        val result = resolveDesktopWheelHorizontalScroll(
            deltaX = 0f,
            deltaY = 1.5f,
            isRtl = false,
            canScrollForward = false,
            canScrollBackward = false,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false), result)
    }

    @Test
    fun rtlHorizontalTrackpadFlipsDirectionAndLocksAtBoundaries() {
        val midRow = resolveDesktopWheelHorizontalScroll(
            deltaX = 2f,
            deltaY = 0.2f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = true,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = -2f, consume = true), midRow)

        val lockedAtStart = resolveDesktopWheelHorizontalScroll(
            deltaX = 2f,
            deltaY = 0.2f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = false,
        )
        assertEquals(DesktopWheelScrollDecision(dispatchDelta = 0f, consume = true), lockedAtStart)
    }

    @Test
    fun zeroAndNonFiniteDeltasNeitherDispatchNorConsume() {
        assertEquals(
            DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false),
            resolveDesktopWheelHorizontalScroll(0f, 0f, isRtl = false, canScrollForward = true, canScrollBackward = true),
        )
        assertEquals(
            DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false),
            resolveDesktopWheelHorizontalScroll(Float.NaN, 1f, isRtl = false, canScrollForward = true, canScrollBackward = true),
        )
        assertEquals(
            DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false),
            resolveDesktopWheelHorizontalScroll(0f, Float.POSITIVE_INFINITY, isRtl = false, canScrollForward = true, canScrollBackward = true),
        )
        assertEquals(
            DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false),
            resolveDesktopWheelHorizontalScroll(0f, Float.NEGATIVE_INFINITY, isRtl = true, canScrollForward = true, canScrollBackward = true),
        )
    }
}

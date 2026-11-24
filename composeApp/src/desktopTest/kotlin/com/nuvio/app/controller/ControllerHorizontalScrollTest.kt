package com.nuvio.app.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ControllerHorizontalScrollTest {

    @Test
    fun routeRestoreDoesNotScrollDuringPointerNavigation() {
        assertFalse(shouldTriggerRouteRestoreScroll(isNavigationActive = false))
        assertTrue(shouldTriggerRouteRestoreScroll(isNavigationActive = true))
    }

    @Test
    fun rtlFirstCardAtRestNeverScrollsOnRouteRestore() {
        // 1920px window with 72px right sidebar -> LazyRow occupies [0, 1848] with 32px contentPadding
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1671f,
            cardRight = 1816f,
            rowLeft = 0f,
            rowRight = 1848f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = false,
            direction = null,
            isStartMostItem = true,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
        assertEquals(HorizontalRowScrollAction.None, action)
    }

    @Test
    fun rtlCardClippedPastRightEdgeScrollsBackwardInsteadOfForward() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1727f,
            cardRight = 1872f,
            rowLeft = 0f,
            rowRight = 1848f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = true,
            direction = null,
            isStartMostItem = false,
            firstVisibleItemIndex = 1,
            firstVisibleItemScrollOffset = 56,
        )
        val scroll = assertIs<HorizontalRowScrollAction.ScrollBy>(action)
        assertEquals(-80f, scroll.deltaPx)
    }

    @Test
    fun rtlStartMostCardClippedPastRightEdgeSnapsToStart() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1727f,
            cardRight = 1872f,
            rowLeft = 0f,
            rowRight = 1848f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = true,
            direction = null,
            isStartMostItem = true,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 56,
        )
        assertEquals(HorizontalRowScrollAction.ScrollToStart, action)
    }

    @Test
    fun rtlCardClippedPastRightEdgeAtZeroScrollDoesNotScrollForward() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1700f,
            cardRight = 1845f,
            rowLeft = 0f,
            rowRight = 1848f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = false,
            direction = null,
            isStartMostItem = true,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
        assertEquals(HorizontalRowScrollAction.None, action)
    }

    @Test
    fun rtlCardClippedPastLeftEdgeScrollsForwardToReveal() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = -20f,
            cardRight = 125f,
            rowLeft = 0f,
            rowRight = 1848f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = true,
            canScrollForward = true,
            canScrollBackward = false,
            direction = ControllerDirection.Left,
            isStartMostItem = false,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
        val scroll = assertIs<HorizontalRowScrollAction.ScrollBy>(action)
        assertEquals(76f, scroll.deltaPx)
    }

    @Test
    fun ltrCardFlushWithRightEdgeAndLeftSidebarStaysStationary() {
        // 1920px window with 72px left sidebar -> LazyRow occupies [72, 1920] with 32px contentPadding
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1743f,
            cardRight = 1888f,
            rowLeft = 72f,
            rowRight = 1920f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = false,
            direction = null,
            isStartMostItem = false,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
        assertEquals(HorizontalRowScrollAction.None, action)
    }

    @Test
    fun ltrCardClippedPastRightEdgeScrollsForwardToReveal() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = 1795f,
            cardRight = 1940f,
            rowLeft = 72f,
            rowRight = 1920f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = false,
            direction = ControllerDirection.Right,
            isStartMostItem = false,
            firstVisibleItemIndex = 0,
            firstVisibleItemScrollOffset = 0,
        )
        val scroll = assertIs<HorizontalRowScrollAction.ScrollBy>(action)
        assertEquals(76f, scroll.deltaPx)
    }

    @Test
    fun ltrCardClippedPastLeftEdgeScrollsBackwardToReveal() {
        val action = computeHorizontalRowScrollAction(
            cardLeft = 60f,
            cardRight = 205f,
            rowLeft = 72f,
            rowRight = 1920f,
            beforeContentPadding = 32f,
            afterContentPadding = 32f,
            isRtl = false,
            canScrollForward = true,
            canScrollBackward = true,
            direction = ControllerDirection.Left,
            isStartMostItem = false,
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffset = 44,
        )
        val scroll = assertIs<HorizontalRowScrollAction.ScrollBy>(action)
        assertEquals(-68f, scroll.deltaPx)
    }
}

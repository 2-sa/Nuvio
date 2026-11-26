package com.nuvio.app.core.ui

import com.nuvio.app.AppScreenTab
import com.nuvio.app.navigation.DetailRoute
import com.nuvio.app.navigation.PlayerRoute
import com.nuvio.app.navigation.StreamRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopNavigationHistoryTest {
    @Test
    fun goBackOnInitialHomeReturnsNull() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)

        assertFalse(history.canGoBack)
        assertFalse(history.canGoForward)
        assertNull(history.goBack())
        assertEquals(DesktopNavEntry(AppScreenTab.Home, null), history.current)
    }

    @Test
    fun tabSwitchesRecordHistoryAndTraverseBackAndForward() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)

        history.recordTabChange(AppScreenTab.Search)
        history.recordTabChange(AppScreenTab.Library)

        assertTrue(history.canGoBack)
        assertFalse(history.canGoForward)
        assertEquals(DesktopNavEntry(AppScreenTab.Library, null), history.current)

        assertEquals(DesktopNavEntry(AppScreenTab.Search, null), history.goBack())
        assertTrue(history.canGoBack)
        assertTrue(history.canGoForward)

        assertEquals(DesktopNavEntry(AppScreenTab.Home, null), history.goBack())
        assertFalse(history.canGoBack)
        assertTrue(history.canGoForward)

        assertEquals(DesktopNavEntry(AppScreenTab.Search, null), history.goForward())
        assertEquals(DesktopNavEntry(AppScreenTab.Library, null), history.goForward())
        assertFalse(history.canGoForward)
    }

    @Test
    fun playerAndStreamRoutesAreIgnoredAndNeverAppearInForwardStack() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)
        val detail = DetailRoute(type = "movie", id = "tt123")

        history.recordRoutePush(detail, AppScreenTab.Home)
        history.recordRoutePush(StreamRoute(launchId = 1L), AppScreenTab.Home)
        history.recordRoutePush(PlayerRoute(launchId = 2L), AppScreenTab.Home)

        assertEquals(DesktopNavEntry(AppScreenTab.Home, detail), history.current)

        assertEquals(DesktopNavEntry(AppScreenTab.Home, null), history.goBack())
        assertEquals(DesktopNavEntry(AppScreenTab.Home, detail), history.goForward())
        assertFalse(history.canGoForward)
    }

    @Test
    fun newNavigationAfterGoBackClearsForwardStack() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)
        val detailA = DetailRoute(type = "movie", id = "tt1")
        val detailB = DetailRoute(type = "series", id = "tt2")

        history.recordTabChange(AppScreenTab.Search)
        history.recordRoutePush(detailA, AppScreenTab.Search)
        assertEquals(DesktopNavEntry(AppScreenTab.Search, null), history.goBack())
        assertTrue(history.canGoForward)

        history.recordRoutePush(detailB, AppScreenTab.Search)
        assertFalse(history.canGoForward)
        assertNull(history.goForward())
        assertEquals(DesktopNavEntry(AppScreenTab.Search, detailB), history.current)
    }

    @Test
    fun syncRoutePopPushesPoppedRouteToForwardStack() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)
        val detail = DetailRoute(type = "movie", id = "tt42")

        history.recordRoutePush(detail, AppScreenTab.Home)
        history.syncRoutePop(newRoute = null, activeTab = AppScreenTab.Home)

        assertEquals(DesktopNavEntry(AppScreenTab.Home, null), history.current)
        assertFalse(history.canGoBack)
        assertTrue(history.canGoForward)
        assertEquals(DesktopNavEntry(AppScreenTab.Home, detail), history.goForward())
    }

    @Test
    fun syncRoutePopIgnoresPlayerAndStreamRoutes() {
        val history = DesktopNavigationHistory(AppScreenTab.Home)
        val detail = DetailRoute(type = "movie", id = "tt99")

        history.recordRoutePush(detail, AppScreenTab.Home)
        history.syncRoutePop(newRoute = StreamRoute(launchId = 1L), activeTab = AppScreenTab.Home)
        history.syncRoutePop(newRoute = PlayerRoute(launchId = 2L), activeTab = AppScreenTab.Home)

        assertEquals(DesktopNavEntry(AppScreenTab.Home, detail), history.current)
        assertTrue(history.canGoBack)
        assertFalse(history.canGoForward)
    }
}

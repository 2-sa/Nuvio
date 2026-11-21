package com.nuvio.app.core.ui

import androidx.compose.ui.unit.dp
import com.nuvio.app.DesktopSidebarCollapsedWidth
import com.nuvio.app.DesktopSidebarExpandedWidth
import com.nuvio.app.desktopSidebarActiveIndicatorAlpha
import com.nuvio.app.desktopSidebarIsExpanded
import com.nuvio.app.desktopSidebarWidthForState
import com.nuvio.app.features.settings.DesktopNavigationLayout
import com.nuvio.app.features.settings.NavBarStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopNavigationBarTest {
    @Test
    fun desktopLayoutPreferencesKeepTopBarAndSidebar() {
        assertEquals(DesktopNavigationLayout.TopBar, DesktopNavigationLayout.fromName("TopBar"))
        assertEquals(DesktopNavigationLayout.TopBar, DesktopNavigationLayout.fromName("topbar"))
        assertEquals(DesktopNavigationLayout.Sidebar, DesktopNavigationLayout.fromName("Sidebar"))
        assertEquals(DesktopNavigationLayout.Default, DesktopNavigationLayout.fromName(null))
    }

    @Test
    fun adaptiveHomeKeepsLabelsStableAcrossHeroHoverPopupAndScroll() {
        assertEquals(1f, labels(hero = true))
        assertEquals(1f, labels(hero = true, hovered = true))
        assertEquals(1f, labels(hero = true, popup = true))
        assertEquals(1f, labels(hero = true, scrolled = true))
    }

    @Test
    fun adaptiveHomeWithoutHeroKeepsLabelsStableWhileScrolling() {
        assertEquals(1f, labels())
        assertEquals(1f, labels(scrolled = true))
        assertEquals(1f, labels(scrolled = true, hovered = true))
    }

    @Test
    fun otherTabsKeepTheirLabelsWhileScrolling() {
        assertEquals(1f, labels(home = false, scrolled = true))
    }

    @Test
    fun explicitStylesOverrideHoverAndScrolling() {
        assertEquals(0f, labels(style = NavBarStyle.COMPACT, hovered = true, popup = true))
        assertEquals(1f, labels(style = NavBarStyle.EXPANDED, hero = true, scrolled = true))
    }

    @Test
    fun desktopSidebarWidthForStateReturnsCollapsedAndExpandedWidths() {
        assertEquals(72.dp, DesktopSidebarCollapsedWidth)
        assertEquals(224.dp, DesktopSidebarExpandedWidth)
        assertEquals(DesktopSidebarCollapsedWidth, desktopSidebarWidthForState(expanded = false))
        assertEquals(DesktopSidebarExpandedWidth, desktopSidebarWidthForState(expanded = true))
    }

    @Test
    fun desktopSidebarExpandedStyleAlwaysExpands() {
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.EXPANDED, isHovered = false, profileStackVisible = false))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.EXPANDED, isHovered = true, profileStackVisible = false))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.EXPANDED, isHovered = false, profileStackVisible = true))
    }

    @Test
    fun desktopSidebarCompactStyleOnlyExpandsForProfileStack() {
        assertFalse(desktopSidebarIsExpanded(NavBarStyle.COMPACT, isHovered = false, profileStackVisible = false))
        assertFalse(desktopSidebarIsExpanded(NavBarStyle.COMPACT, isHovered = true, profileStackVisible = false))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.COMPACT, isHovered = false, profileStackVisible = true))
    }

    @Test
    fun desktopSidebarAdaptiveStyleExpandsOnHoverOrProfileStack() {
        assertFalse(desktopSidebarIsExpanded(NavBarStyle.ADAPTIVE, isHovered = false, profileStackVisible = false))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.ADAPTIVE, isHovered = true, profileStackVisible = false))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.ADAPTIVE, isHovered = false, profileStackVisible = true))
        assertTrue(desktopSidebarIsExpanded(NavBarStyle.ADAPTIVE, isHovered = true, profileStackVisible = true))
    }

    @Test
    fun desktopSidebarActiveIndicatorAlphaReflectsSelection() {
        assertEquals(1f, desktopSidebarActiveIndicatorAlpha(selected = true))
        assertEquals(0f, desktopSidebarActiveIndicatorAlpha(selected = false))
    }

    private fun labels(
        style: NavBarStyle = NavBarStyle.ADAPTIVE,
        home: Boolean = true,
        hero: Boolean = false,
        hovered: Boolean = false,
        popup: Boolean = false,
        scrolled: Boolean = false,
    ): Float = desktopNavigationLabelFraction(style, home, hero, hovered, popup, scrolled)
}

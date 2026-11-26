package com.nuvio.app.core.ui

import com.nuvio.app.AppScreenTab
import com.nuvio.app.navigation.AppRoute
import com.nuvio.app.navigation.PlayerRoute
import com.nuvio.app.navigation.StreamRoute
import com.nuvio.app.navigation.TabsRoute

internal data class DesktopNavEntry(
    val tab: AppScreenTab,
    val route: AppRoute?,
)

/**
 * Pure-Kotlin unified Back/Forward navigation history for desktop.
 * Tracks both top-level tab switches (represented with `route == null`) and detail routes,
 * while ignoring transient playback/stream selection routes.
 */
internal class DesktopNavigationHistory(
    initialTab: AppScreenTab = AppScreenTab.Home,
) {
    var current: DesktopNavEntry = DesktopNavEntry(tab = initialTab, route = null)
        private set

    private val backStack = ArrayDeque<DesktopNavEntry>()
    private val forwardStack = ArrayDeque<DesktopNavEntry>()

    val canGoBack: Boolean
        get() = backStack.isNotEmpty()

    val canGoForward: Boolean
        get() = forwardStack.isNotEmpty()

    fun recordTabChange(tab: AppScreenTab) {
        val target = DesktopNavEntry(tab = tab, route = null)
        if (current == target) return
        backStack.addLast(current)
        current = target
        forwardStack.clear()
    }

    fun recordRoutePush(route: AppRoute, activeTab: AppScreenTab) {
        if (route is PlayerRoute || route is StreamRoute) return
        if (route is TabsRoute) {
            recordTabChange(activeTab)
            return
        }
        if (current.route == route) return
        backStack.addLast(current)
        current = DesktopNavEntry(tab = activeTab, route = route)
        forwardStack.clear()
    }

    fun syncRoutePop(newRoute: AppRoute?, activeTab: AppScreenTab) {
        if (newRoute is PlayerRoute || newRoute is StreamRoute) return
        val normalizedNewRoute = if (newRoute is TabsRoute) null else newRoute
        if (current.route != normalizedNewRoute) {
            val currentRoute = current.route
            if (currentRoute != null && currentRoute !is PlayerRoute && currentRoute !is StreamRoute) {
                forwardStack.addLast(current)
            }
            val target = DesktopNavEntry(tab = activeTab, route = normalizedNewRoute)
            if (backStack.lastOrNull() == target) {
                backStack.removeLast()
            }
            current = target
        }
    }

    fun goBack(): DesktopNavEntry? {
        if (backStack.isEmpty()) return null
        val currentRoute = current.route
        if (currentRoute !is PlayerRoute && currentRoute !is StreamRoute) {
            forwardStack.addLast(current)
        }
        current = backStack.removeLast()
        return current
    }

    fun goForward(): DesktopNavEntry? {
        if (forwardStack.isEmpty()) return null
        backStack.addLast(current)
        current = forwardStack.removeLast()
        return current
    }
}

package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSettingsNavigationTest {

    @Test
    fun consolidatedSidebarHasFourGroupsAndElevenUniqueWorkspaces() {
        val sections = buildDesktopSettingsNavSections()

        assertEquals(
            listOf(
                DesktopSettingsGroup.Look,
                DesktopSettingsGroup.Playback,
                DesktopSettingsGroup.Services,
                DesktopSettingsGroup.AccountSystem,
            ),
            sections.map { it.group },
        )

        val allItems = sections.flatMap { it.items }
        assertEquals(11, allItems.size)
        assertEquals(allItems.size, allItems.map { it.id }.distinct().size)
        assertEquals(
            DesktopSettingsWorkspace.entries,
            allItems.map { it.workspace },
        )
    }

    @Test
    fun resolveDesktopInitialSettingsPageRedirectsRootToAppearance() {
        assertEquals(SettingsPage.Appearance, resolveDesktopInitialSettingsPage(SettingsPage.Root))
        SettingsPage.entries
            .filter { it != SettingsPage.Root }
            .forEach { page ->
                assertEquals(page, resolveDesktopInitialSettingsPage(page))
            }
    }

    @Test
    fun resolveDesktopWorkspaceRouteCoversEverySettingsPageWithValidSidebarDestination() {
        val sidebarPages = buildDesktopSettingsNavSections()
            .flatMap { it.items }
            .mapNotNull { (it.destination as? DesktopSettingsDestination.Page)?.page }
            .toSet()

        SettingsPage.entries.forEach { page ->
            val route = resolveDesktopWorkspaceRoute(page)
            val resolvedPrimary = resolveDesktopSidebarSelectedPage(page)
            assertEquals(route.workspace.primaryPage, resolvedPrimary)
            assertTrue(
                resolvedPrimary in sidebarPages,
                "Expected $page -> $resolvedPrimary to exist in desktop sidebar pages",
            )
            val tabs = desktopWorkspaceTabs(
                workspace = route.workspace,
                showPlugins = true,
                showNotifications = true,
                showSentry = true,
            )
            if (route.selectedTab != null) {
                assertTrue(
                    route.selectedTab in tabs,
                    "Expected $page tab ${route.selectedTab} to belong to ${route.workspace}",
                )
            }
        }
    }

    @Test
    fun mergedPagesRouteToExpectedConsolidatedWorkspaceAndTab() {
        // Layout merges into Appearance & Nav
        assertEquals(
            DesktopSettingsWorkspace.AppearanceAndNav,
            resolveDesktopWorkspaceRoute(SettingsPage.Layout).workspace,
        )

        // HoverPreview + PosterCustomization merge into Posters & Preview tab of Home & Cards
        val hoverRoute = resolveDesktopWorkspaceRoute(SettingsPage.HoverPreview)
        assertEquals(DesktopSettingsWorkspace.HomeAndCards, hoverRoute.workspace)
        assertEquals(DesktopSettingsWorkspaceTab.PostersAndPreview, hoverRoute.selectedTab)

        // Streams merges into Playback & Streams -> Streams & Badges tab
        val streamsRoute = resolveDesktopWorkspaceRoute(SettingsPage.Streams)
        assertEquals(DesktopSettingsWorkspace.PlaybackAndStreams, streamsRoute.workspace)
        assertEquals(DesktopSettingsWorkspaceTab.StreamsAndBadges, streamsRoute.selectedTab)

        // MDBList Ratings merges into Metadata & Ratings -> MDBList tab
        val mdbRoute = resolveDesktopWorkspaceRoute(SettingsPage.MdbListRatings)
        assertEquals(DesktopSettingsWorkspace.MetadataAndRatings, mdbRoute.workspace)
        assertEquals(DesktopSettingsWorkspaceTab.MdbList, mdbRoute.selectedTab)

        // Plugins merges into Addons & Plugins -> Plugins tab
        val pluginsRoute = resolveDesktopWorkspaceRoute(SettingsPage.Plugins)
        assertEquals(DesktopSettingsWorkspace.AddonsAndPlugins, pluginsRoute.workspace)
        assertEquals(DesktopSettingsWorkspaceTab.Plugins, pluginsRoute.selectedTab)

        // Notifications merges into System & Performance -> Notifications tab
        val notificationsRoute = resolveDesktopWorkspaceRoute(SettingsPage.Notifications)
        assertEquals(DesktopSettingsWorkspace.SystemAndPerformance, notificationsRoute.workspace)
        assertEquals(DesktopSettingsWorkspaceTab.Notifications, notificationsRoute.selectedTab)
    }

    @Test
    fun desktopWorkspaceTabsRespectFeatureFlags() {
        val addonsNoPlugins = desktopWorkspaceTabs(
            workspace = DesktopSettingsWorkspace.AddonsAndPlugins,
            showPlugins = false,
            showNotifications = false,
            showSentry = false,
        )
        assertEquals(listOf(DesktopSettingsWorkspaceTab.Addons), addonsNoPlugins)

        val systemMinimal = desktopWorkspaceTabs(
            workspace = DesktopSettingsWorkspace.SystemAndPerformance,
            showPlugins = false,
            showNotifications = false,
            showSentry = false,
        )
        assertEquals(listOf(DesktopSettingsWorkspaceTab.PerformanceAndGraphics), systemMinimal)
    }

    @Test
    fun resolveDesktopParentPageOnlyReturnsParentForStandaloneSubDetails() {
        assertEquals(SettingsPage.MediaServers, resolveDesktopParentPage(SettingsPage.MediaServer))
        assertEquals(SettingsPage.About, resolveDesktopParentPage(SettingsPage.SupportersContributors))
        assertEquals(SettingsPage.About, resolveDesktopParentPage(SettingsPage.LicensesAttributions))

        listOf(
            SettingsPage.Appearance,
            SettingsPage.Homescreen,
            SettingsPage.Playback,
            SettingsPage.PlaybackAudio,
            SettingsPage.Addons,
            SettingsPage.TmdbEnrichment,
            SettingsPage.MdbListRatings,
            SettingsPage.About,
        ).forEach { tabbedOrRootPage ->
            assertNull(resolveDesktopParentPage(tabbedOrRootPage))
        }
    }

    @Test
    fun desktopSettingsSubtitleResProvidesSubtitleForAllNonRootPages() {
        assertNull(desktopSettingsSubtitleRes(SettingsPage.Root))
        SettingsPage.entries
            .filter { it != SettingsPage.Root }
            .forEach { page ->
                assertNotNull(desktopSettingsSubtitleRes(page), "Expected subtitle resource for $page")
            }
    }
}

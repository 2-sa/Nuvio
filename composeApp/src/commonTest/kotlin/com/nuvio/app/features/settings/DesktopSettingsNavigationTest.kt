package com.nuvio.app.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSettingsNavigationTest {
    @Test
    fun sectionsAppearInExactOrderWithExpectedDestinations() {
        val sections = buildDesktopSettingsNavSections(
            showPlugins = true,
            showNotifications = true,
            showSwitchProfile = true,
            showWatchParty = true,
        )

        assertEquals(
            listOf(
                SettingsCategory.You,
                SettingsCategory.Look,
                SettingsCategory.Watch,
                SettingsCategory.Services,
                SettingsCategory.System,
            ),
            sections.map { it.category },
        )

        val allIds = sections.flatMap { section -> section.items.map { it.id } }
        assertEquals(allIds.size, allIds.distinct().size)

        assertEquals(
            listOf(
                DesktopSettingsDestination.Page(SettingsPage.Account),
                DesktopSettingsDestination.SwitchProfile,
                DesktopSettingsDestination.WatchParty,
            ),
            sections[0].items.map { it.destination },
        )
        assertEquals(
            listOf(
                DesktopSettingsDestination.Page(SettingsPage.Appearance),
                DesktopSettingsDestination.Page(SettingsPage.Homescreen),
                DesktopSettingsDestination.Page(SettingsPage.Layout),
                DesktopSettingsDestination.Page(SettingsPage.PosterCustomization),
                DesktopSettingsDestination.Page(SettingsPage.MetaScreen),
                DesktopSettingsDestination.Page(SettingsPage.ContinueWatching),
                DesktopSettingsDestination.Page(SettingsPage.HoverPreview),
            ),
            sections[1].items.map { it.destination },
        )
        assertEquals(
            listOf(
                DesktopSettingsDestination.Page(SettingsPage.Playback),
                DesktopSettingsDestination.Page(SettingsPage.Streams),
                DesktopSettingsDestination.Page(SettingsPage.Addons),
                DesktopSettingsDestination.Page(SettingsPage.Plugins),
                DesktopSettingsDestination.Page(SettingsPage.MediaServers),
                DesktopSettingsDestination.Collections,
            ),
            sections[2].items.map { it.destination },
        )
        assertEquals(
            listOf(
                DesktopSettingsDestination.Page(SettingsPage.TraktAuthentication),
                DesktopSettingsDestination.Page(SettingsPage.TmdbEnrichment),
                DesktopSettingsDestination.Page(SettingsPage.MdbListRatings),
                DesktopSettingsDestination.Page(SettingsPage.Debrid),
            ),
            sections[3].items.map { it.destination },
        )
        assertEquals(
            listOf(
                DesktopSettingsDestination.Page(SettingsPage.Notifications),
                DesktopSettingsDestination.Page(SettingsPage.Advanced),
                DesktopSettingsDestination.Page(SettingsPage.About),
            ),
            sections[4].items.map { it.destination },
        )
    }

    @Test
    fun featureFlagsIncludeAndExcludeOptionalItemsAccurately() {
        val minimal = buildDesktopSettingsNavSections(
            showPlugins = false,
            showNotifications = false,
            showSwitchProfile = false,
            showWatchParty = false,
        )
        val minimalDestinations = minimal.flatMap { section -> section.items.map { it.destination } }.toSet()

        assertFalse(DesktopSettingsDestination.SwitchProfile in minimalDestinations)
        assertFalse(DesktopSettingsDestination.WatchParty in minimalDestinations)
        assertFalse(DesktopSettingsDestination.Page(SettingsPage.Plugins) in minimalDestinations)
        assertFalse(DesktopSettingsDestination.Page(SettingsPage.Notifications) in minimalDestinations)
        assertTrue(DesktopSettingsDestination.Page(SettingsPage.Account) in minimalDestinations)
        assertTrue(DesktopSettingsDestination.Collections in minimalDestinations)
        assertTrue(DesktopSettingsDestination.Page(SettingsPage.About) in minimalDestinations)

        val withPluginsOnly = buildDesktopSettingsNavSections(
            showPlugins = true,
            showNotifications = false,
            showSwitchProfile = false,
            showWatchParty = false,
        ).flatMap { it.items }.map { it.destination }.toSet()
        assertTrue(DesktopSettingsDestination.Page(SettingsPage.Plugins) in withPluginsOnly)
        assertFalse(DesktopSettingsDestination.Page(SettingsPage.Notifications) in withPluginsOnly)

        val withNotificationsOnly = buildDesktopSettingsNavSections(
            showPlugins = false,
            showNotifications = true,
            showSwitchProfile = false,
            showWatchParty = false,
        ).flatMap { it.items }.map { it.destination }.toSet()
        assertTrue(DesktopSettingsDestination.Page(SettingsPage.Notifications) in withNotificationsOnly)
        assertFalse(DesktopSettingsDestination.Page(SettingsPage.Plugins) in withNotificationsOnly)
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
    fun resolveDesktopSidebarSelectedPageMapsEveryEntryToExistingSidebarItem() {
        val sidebarPages = buildDesktopSettingsNavSections(
            showPlugins = true,
            showNotifications = true,
            showSwitchProfile = true,
            showWatchParty = true,
        )
            .flatMap { it.items }
            .mapNotNull { (it.destination as? DesktopSettingsDestination.Page)?.page }
            .toSet()

        SettingsPage.entries.forEach { page ->
            val resolved = resolveDesktopSidebarSelectedPage(page)
            assertTrue(
                resolved in sidebarPages,
                "Expected $page -> $resolved to exist in desktop sidebar pages",
            )
        }

        assertEquals(SettingsPage.Appearance, resolveDesktopSidebarSelectedPage(SettingsPage.Root))
        assertEquals(SettingsPage.Playback, resolveDesktopSidebarSelectedPage(SettingsPage.PlaybackAudio))
        assertEquals(SettingsPage.Playback, resolveDesktopSidebarSelectedPage(SettingsPage.PlaybackSubtitles))
        assertEquals(SettingsPage.MediaServers, resolveDesktopSidebarSelectedPage(SettingsPage.MediaServer))
        assertEquals(SettingsPage.Addons, resolveDesktopSidebarSelectedPage(SettingsPage.ContentDiscovery))
        assertEquals(SettingsPage.TmdbEnrichment, resolveDesktopSidebarSelectedPage(SettingsPage.Integrations))
        assertEquals(SettingsPage.About, resolveDesktopSidebarSelectedPage(SettingsPage.SupportersContributors))
        assertEquals(SettingsPage.About, resolveDesktopSidebarSelectedPage(SettingsPage.LicensesAttributions))
    }

    @Test
    fun resolveDesktopParentPageOnlyReturnsParentForSubChildPages() {
        assertEquals(SettingsPage.Playback, resolveDesktopParentPage(SettingsPage.PlaybackAudio))
        assertEquals(SettingsPage.Playback, resolveDesktopParentPage(SettingsPage.PlaybackPlayer))
        assertEquals(SettingsPage.MediaServers, resolveDesktopParentPage(SettingsPage.MediaServer))
        assertEquals(SettingsPage.About, resolveDesktopParentPage(SettingsPage.SupportersContributors))
        assertEquals(SettingsPage.About, resolveDesktopParentPage(SettingsPage.LicensesAttributions))

        listOf(
            SettingsPage.Appearance,
            SettingsPage.Homescreen,
            SettingsPage.Layout,
            SettingsPage.Playback,
            SettingsPage.Addons,
            SettingsPage.TmdbEnrichment,
            SettingsPage.About,
        ).forEach { directPage ->
            assertNull(resolveDesktopParentPage(directPage))
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

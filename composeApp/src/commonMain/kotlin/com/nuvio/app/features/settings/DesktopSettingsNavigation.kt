package com.nuvio.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.ui.graphics.vector.ImageVector
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.about_licenses_attributions_subtitle
import nuvio.composeapp.generated.resources.about_supporters_contributors_subtitle
import nuvio.composeapp.generated.resources.collections_header
import nuvio.composeapp.generated.resources.compose_settings_root_about_description
import nuvio.composeapp.generated.resources.compose_settings_root_account_description
import nuvio.composeapp.generated.resources.compose_settings_root_advanced_description
import nuvio.composeapp.generated.resources.compose_settings_root_content_discovery_description
import nuvio.composeapp.generated.resources.compose_settings_root_integrations_description
import nuvio.composeapp.generated.resources.compose_settings_root_layout_description
import nuvio.composeapp.generated.resources.compose_settings_root_notifications_description
import nuvio.composeapp.generated.resources.compose_settings_root_streams_description
import nuvio.composeapp.generated.resources.compose_settings_root_switch_profile_title
import nuvio.composeapp.generated.resources.compose_settings_root_tracking_description
import nuvio.composeapp.generated.resources.party_title
import nuvio.composeapp.generated.resources.servers_libraries_description
import nuvio.composeapp.generated.resources.settings_appearance_continue_watching_description
import nuvio.composeapp.generated.resources.settings_appearance_description
import nuvio.composeapp.generated.resources.settings_appearance_hover_preview_description
import nuvio.composeapp.generated.resources.settings_appearance_poster_customization_description
import nuvio.composeapp.generated.resources.settings_content_discovery_addons_description
import nuvio.composeapp.generated.resources.settings_content_discovery_homescreen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_media_servers_description
import nuvio.composeapp.generated.resources.settings_content_discovery_meta_screen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_plugins_description
import nuvio.composeapp.generated.resources.settings_integrations_debrid_description
import nuvio.composeapp.generated.resources.settings_integrations_mdblist_description
import nuvio.composeapp.generated.resources.settings_integrations_tmdb_description
import nuvio.composeapp.generated.resources.settings_playback_page_audio_description
import nuvio.composeapp.generated.resources.settings_playback_page_buffer_description
import nuvio.composeapp.generated.resources.settings_playback_page_interface_description
import nuvio.composeapp.generated.resources.settings_playback_page_p2p_description
import nuvio.composeapp.generated.resources.settings_playback_page_player_description
import nuvio.composeapp.generated.resources.settings_playback_page_skip_description
import nuvio.composeapp.generated.resources.settings_playback_page_stream_selection_description
import nuvio.composeapp.generated.resources.settings_playback_page_subtitles_description
import nuvio.composeapp.generated.resources.settings_playback_page_up_next_description
import nuvio.composeapp.generated.resources.settings_playback_page_video_description
import nuvio.composeapp.generated.resources.settings_playback_subtitle
import org.jetbrains.compose.resources.StringResource

internal sealed interface DesktopSettingsDestination {
    data class Page(val page: SettingsPage) : DesktopSettingsDestination
    data object SwitchProfile : DesktopSettingsDestination
    data object WatchParty : DesktopSettingsDestination
    data object Collections : DesktopSettingsDestination
}

internal data class DesktopSettingsNavItem(
    val id: String,
    val destination: DesktopSettingsDestination,
    val titleRes: StringResource,
    val icon: ImageVector,
)

internal data class DesktopSettingsNavSection(
    val category: SettingsCategory,
    val items: List<DesktopSettingsNavItem>,
)

private fun SettingsPage.toDesktopNavItem(): DesktopSettingsNavItem = DesktopSettingsNavItem(
    id = name,
    destination = DesktopSettingsDestination.Page(this),
    titleRes = titleRes,
    icon = icon,
)

internal fun buildDesktopSettingsNavSections(
    showPlugins: Boolean,
    showNotifications: Boolean,
    showSwitchProfile: Boolean,
    showWatchParty: Boolean,
): List<DesktopSettingsNavSection> = listOf(
    DesktopSettingsNavSection(
        category = SettingsCategory.You,
        items = buildList {
            add(SettingsPage.Account.toDesktopNavItem())
            if (showSwitchProfile) {
                add(
                    DesktopSettingsNavItem(
                        id = "switch-profile",
                        destination = DesktopSettingsDestination.SwitchProfile,
                        titleRes = Res.string.compose_settings_root_switch_profile_title,
                        icon = Icons.Default.People,
                    ),
                )
            }
            if (showWatchParty) {
                add(
                    DesktopSettingsNavItem(
                        id = "watch-party",
                        destination = DesktopSettingsDestination.WatchParty,
                        titleRes = Res.string.party_title,
                        icon = Icons.Rounded.Groups,
                    ),
                )
            }
        },
    ),
    DesktopSettingsNavSection(
        category = SettingsCategory.Look,
        items = listOf(
            SettingsPage.Appearance.toDesktopNavItem(),
            SettingsPage.Homescreen.toDesktopNavItem(),
            SettingsPage.Layout.toDesktopNavItem(),
            SettingsPage.PosterCustomization.toDesktopNavItem(),
            SettingsPage.MetaScreen.toDesktopNavItem(),
            SettingsPage.ContinueWatching.toDesktopNavItem(),
            SettingsPage.HoverPreview.toDesktopNavItem(),
        ),
    ),
    DesktopSettingsNavSection(
        category = SettingsCategory.Watch,
        items = buildList {
            add(SettingsPage.Playback.toDesktopNavItem())
            add(SettingsPage.Streams.toDesktopNavItem())
            add(SettingsPage.Addons.toDesktopNavItem())
            if (showPlugins) {
                add(SettingsPage.Plugins.toDesktopNavItem())
            }
            add(SettingsPage.MediaServers.toDesktopNavItem())
            add(
                DesktopSettingsNavItem(
                    id = "collections",
                    destination = DesktopSettingsDestination.Collections,
                    titleRes = Res.string.collections_header,
                    icon = Icons.Default.CollectionsBookmark,
                ),
            )
        },
    ),
    DesktopSettingsNavSection(
        category = SettingsCategory.Services,
        items = listOf(
            SettingsPage.TraktAuthentication.toDesktopNavItem(),
            SettingsPage.TmdbEnrichment.toDesktopNavItem(),
            SettingsPage.MdbListRatings.toDesktopNavItem(),
            SettingsPage.Debrid.toDesktopNavItem(),
        ),
    ),
    DesktopSettingsNavSection(
        category = SettingsCategory.System,
        items = buildList {
            if (showNotifications) {
                add(SettingsPage.Notifications.toDesktopNavItem())
            }
            add(SettingsPage.Advanced.toDesktopNavItem())
            add(SettingsPage.About.toDesktopNavItem())
        },
    ),
)

internal fun resolveDesktopInitialSettingsPage(requestedPage: SettingsPage): SettingsPage =
    if (requestedPage == SettingsPage.Root) SettingsPage.Appearance else requestedPage

internal fun resolveDesktopSidebarSelectedPage(currentPage: SettingsPage): SettingsPage = when (currentPage) {
    SettingsPage.Root -> SettingsPage.Appearance
    SettingsPage.PlaybackPlayer,
    SettingsPage.PlaybackStreamSelection,
    SettingsPage.PlaybackUpNext,
    SettingsPage.PlaybackSkipSegments,
    SettingsPage.PlaybackInterface,
    SettingsPage.PlaybackAudio,
    SettingsPage.PlaybackSubtitles,
    SettingsPage.PlaybackVideo,
    SettingsPage.PlaybackBuffer,
    SettingsPage.PlaybackP2p -> SettingsPage.Playback
    SettingsPage.MediaServer -> SettingsPage.MediaServers
    SettingsPage.ContentDiscovery -> SettingsPage.Addons
    SettingsPage.Integrations -> SettingsPage.TmdbEnrichment
    SettingsPage.SupportersContributors,
    SettingsPage.LicensesAttributions -> SettingsPage.About
    else -> currentPage
}

internal fun resolveDesktopParentPage(currentPage: SettingsPage): SettingsPage? = when (currentPage) {
    SettingsPage.PlaybackPlayer,
    SettingsPage.PlaybackStreamSelection,
    SettingsPage.PlaybackUpNext,
    SettingsPage.PlaybackSkipSegments,
    SettingsPage.PlaybackInterface,
    SettingsPage.PlaybackAudio,
    SettingsPage.PlaybackSubtitles,
    SettingsPage.PlaybackVideo,
    SettingsPage.PlaybackBuffer,
    SettingsPage.PlaybackP2p -> SettingsPage.Playback
    SettingsPage.MediaServer -> SettingsPage.MediaServers
    SettingsPage.SupportersContributors,
    SettingsPage.LicensesAttributions -> SettingsPage.About
    else -> null
}

internal fun desktopSettingsSubtitleRes(page: SettingsPage): StringResource? = when (page) {
    SettingsPage.Root -> null
    SettingsPage.Account -> Res.string.compose_settings_root_account_description
    SettingsPage.Appearance -> Res.string.settings_appearance_description
    SettingsPage.Layout -> Res.string.compose_settings_root_layout_description
    SettingsPage.Homescreen -> Res.string.settings_content_discovery_homescreen_description
    SettingsPage.PosterCustomization -> Res.string.settings_appearance_poster_customization_description
    SettingsPage.MetaScreen -> Res.string.settings_content_discovery_meta_screen_description
    SettingsPage.ContinueWatching -> Res.string.settings_appearance_continue_watching_description
    SettingsPage.HoverPreview -> Res.string.settings_appearance_hover_preview_description
    SettingsPage.Streams -> Res.string.compose_settings_root_streams_description
    SettingsPage.Playback -> Res.string.settings_playback_subtitle
    SettingsPage.PlaybackPlayer -> Res.string.settings_playback_page_player_description
    SettingsPage.PlaybackStreamSelection -> Res.string.settings_playback_page_stream_selection_description
    SettingsPage.PlaybackUpNext -> Res.string.settings_playback_page_up_next_description
    SettingsPage.PlaybackSkipSegments -> Res.string.settings_playback_page_skip_description
    SettingsPage.PlaybackInterface -> Res.string.settings_playback_page_interface_description
    SettingsPage.PlaybackAudio -> Res.string.settings_playback_page_audio_description
    SettingsPage.PlaybackSubtitles -> Res.string.settings_playback_page_subtitles_description
    SettingsPage.PlaybackVideo -> Res.string.settings_playback_page_video_description
    SettingsPage.PlaybackBuffer -> Res.string.settings_playback_page_buffer_description
    SettingsPage.PlaybackP2p -> Res.string.settings_playback_page_p2p_description
    SettingsPage.ContentDiscovery -> Res.string.compose_settings_root_content_discovery_description
    SettingsPage.Addons -> Res.string.settings_content_discovery_addons_description
    SettingsPage.Plugins -> Res.string.settings_content_discovery_plugins_description
    SettingsPage.MediaServers -> Res.string.settings_content_discovery_media_servers_description
    SettingsPage.MediaServer -> Res.string.servers_libraries_description
    SettingsPage.Integrations -> Res.string.compose_settings_root_integrations_description
    SettingsPage.TmdbEnrichment -> Res.string.settings_integrations_tmdb_description
    SettingsPage.MdbListRatings -> Res.string.settings_integrations_mdblist_description
    SettingsPage.Debrid -> Res.string.settings_integrations_debrid_description
    SettingsPage.TraktAuthentication -> Res.string.compose_settings_root_tracking_description
    SettingsPage.Notifications -> Res.string.compose_settings_root_notifications_description
    SettingsPage.Advanced -> Res.string.compose_settings_root_advanced_description
    SettingsPage.About -> Res.string.compose_settings_root_about_description
    SettingsPage.SupportersContributors -> Res.string.about_supporters_contributors_subtitle
    SettingsPage.LicensesAttributions -> Res.string.about_licenses_attributions_subtitle
}

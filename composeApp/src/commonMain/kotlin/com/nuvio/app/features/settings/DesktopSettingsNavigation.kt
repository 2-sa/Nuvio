package com.nuvio.app.features.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.about_licenses_attributions_subtitle
import nuvio.composeapp.generated.resources.about_supporters_contributors_subtitle
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import nuvio.composeapp.generated.resources.compose_settings_root_about_description
import nuvio.composeapp.generated.resources.compose_settings_root_account_description
import nuvio.composeapp.generated.resources.compose_settings_root_advanced_description
import nuvio.composeapp.generated.resources.compose_settings_root_content_discovery_description
import nuvio.composeapp.generated.resources.compose_settings_root_integrations_description
import nuvio.composeapp.generated.resources.compose_settings_root_layout_description
import nuvio.composeapp.generated.resources.compose_settings_root_notifications_description
import nuvio.composeapp.generated.resources.compose_settings_root_streams_description
import nuvio.composeapp.generated.resources.compose_settings_root_tracking_description
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
import nuvio.composeapp.generated.resources.settings_desktop_group_account
import nuvio.composeapp.generated.resources.settings_desktop_group_look
import nuvio.composeapp.generated.resources.settings_desktop_group_playback
import nuvio.composeapp.generated.resources.settings_desktop_group_services
import nuvio.composeapp.generated.resources.settings_desktop_tab_audio_subtitles
import nuvio.composeapp.generated.resources.settings_desktop_tab_continue_watching
import nuvio.composeapp.generated.resources.settings_desktop_tab_details
import nuvio.composeapp.generated.resources.settings_desktop_tab_home
import nuvio.composeapp.generated.resources.settings_desktop_tab_interface_network
import nuvio.composeapp.generated.resources.settings_desktop_tab_mdblist
import nuvio.composeapp.generated.resources.settings_desktop_tab_notifications
import nuvio.composeapp.generated.resources.settings_desktop_tab_performance
import nuvio.composeapp.generated.resources.settings_desktop_tab_player_video
import nuvio.composeapp.generated.resources.settings_desktop_tab_posters_preview
import nuvio.composeapp.generated.resources.settings_desktop_tab_privacy
import nuvio.composeapp.generated.resources.settings_desktop_tab_streams_badges
import nuvio.composeapp.generated.resources.settings_desktop_tab_tmdb
import nuvio.composeapp.generated.resources.settings_desktop_tab_up_next_skip
import nuvio.composeapp.generated.resources.settings_desktop_ws_about
import nuvio.composeapp.generated.resources.settings_desktop_ws_account
import nuvio.composeapp.generated.resources.settings_desktop_ws_account_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_addons
import nuvio.composeapp.generated.resources.settings_desktop_ws_addons_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_apis
import nuvio.composeapp.generated.resources.settings_desktop_ws_apis_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_appearance
import nuvio.composeapp.generated.resources.settings_desktop_ws_appearance_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_debrid
import nuvio.composeapp.generated.resources.settings_desktop_ws_home_cards
import nuvio.composeapp.generated.resources.settings_desktop_ws_home_cards_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_playback
import nuvio.composeapp.generated.resources.settings_desktop_ws_playback_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_servers
import nuvio.composeapp.generated.resources.settings_desktop_ws_servers_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_system
import nuvio.composeapp.generated.resources.settings_desktop_ws_system_subtitle
import nuvio.composeapp.generated.resources.settings_desktop_ws_tracking
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

internal enum class DesktopSettingsGroup(
    val labelRes: StringResource,
) {
    Look(Res.string.settings_desktop_group_look),
    Playback(Res.string.settings_desktop_group_playback),
    Services(Res.string.settings_desktop_group_services),
    AccountSystem(Res.string.settings_desktop_group_account),
}

internal enum class DesktopSettingsWorkspace(
    val group: DesktopSettingsGroup,
    val primaryPage: SettingsPage,
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val icon: ImageVector,
) {
    AppearanceAndNav(
        group = DesktopSettingsGroup.Look,
        primaryPage = SettingsPage.Appearance,
        titleRes = Res.string.settings_desktop_ws_appearance,
        subtitleRes = Res.string.settings_desktop_ws_appearance_subtitle,
        icon = Icons.Default.Palette,
    ),
    HomeAndCards(
        group = DesktopSettingsGroup.Look,
        primaryPage = SettingsPage.Homescreen,
        titleRes = Res.string.settings_desktop_ws_home_cards,
        subtitleRes = Res.string.settings_desktop_ws_home_cards_subtitle,
        icon = Icons.Default.Dashboard,
    ),
    PlaybackAndStreams(
        group = DesktopSettingsGroup.Playback,
        primaryPage = SettingsPage.Playback,
        titleRes = Res.string.settings_desktop_ws_playback,
        subtitleRes = Res.string.settings_desktop_ws_playback_subtitle,
        icon = Icons.Rounded.PlayArrow,
    ),
    AddonsAndPlugins(
        group = DesktopSettingsGroup.Playback,
        primaryPage = SettingsPage.Addons,
        titleRes = Res.string.settings_desktop_ws_addons,
        subtitleRes = Res.string.settings_desktop_ws_addons_subtitle,
        icon = Icons.Default.Extension,
    ),
    ServersAndCollections(
        group = DesktopSettingsGroup.Playback,
        primaryPage = SettingsPage.MediaServers,
        titleRes = Res.string.settings_desktop_ws_servers,
        subtitleRes = Res.string.settings_desktop_ws_servers_subtitle,
        icon = Icons.Default.Dns,
    ),
    TrackingAndSync(
        group = DesktopSettingsGroup.Services,
        primaryPage = SettingsPage.TraktAuthentication,
        titleRes = Res.string.settings_desktop_ws_tracking,
        subtitleRes = Res.string.compose_settings_root_tracking_description,
        icon = Icons.Default.Sync,
    ),
    MetadataAndRatings(
        group = DesktopSettingsGroup.Services,
        primaryPage = SettingsPage.TmdbEnrichment,
        titleRes = Res.string.settings_desktop_ws_apis,
        subtitleRes = Res.string.settings_desktop_ws_apis_subtitle,
        icon = Icons.Default.Link,
    ),
    DebridServices(
        group = DesktopSettingsGroup.Services,
        primaryPage = SettingsPage.Debrid,
        titleRes = Res.string.settings_desktop_ws_debrid,
        subtitleRes = Res.string.settings_integrations_debrid_description,
        icon = Icons.Rounded.Bolt,
    ),
    AccountAndProfiles(
        group = DesktopSettingsGroup.AccountSystem,
        primaryPage = SettingsPage.Account,
        titleRes = Res.string.settings_desktop_ws_account,
        subtitleRes = Res.string.settings_desktop_ws_account_subtitle,
        icon = Icons.Default.Person,
    ),
    SystemAndPerformance(
        group = DesktopSettingsGroup.AccountSystem,
        primaryPage = SettingsPage.Advanced,
        titleRes = Res.string.settings_desktop_ws_system,
        subtitleRes = Res.string.settings_desktop_ws_system_subtitle,
        icon = Icons.Default.Build,
    ),
    AboutAndUpdates(
        group = DesktopSettingsGroup.AccountSystem,
        primaryPage = SettingsPage.About,
        titleRes = Res.string.settings_desktop_ws_about,
        subtitleRes = Res.string.compose_settings_root_about_description,
        icon = Icons.Default.Info,
    ),
}

internal enum class DesktopSettingsWorkspaceTab(
    val id: String,
    val labelRes: StringResource,
    val targetPage: SettingsPage,
) {
    // Home & Cards
    HomeCatalogs("home-catalogs", Res.string.settings_desktop_tab_home, SettingsPage.Homescreen),
    ContinueWatching("continue-watching", Res.string.settings_desktop_tab_continue_watching, SettingsPage.ContinueWatching),
    PostersAndPreview("posters-preview", Res.string.settings_desktop_tab_posters_preview, SettingsPage.PosterCustomization),
    DetailsPage("details-page", Res.string.settings_desktop_tab_details, SettingsPage.MetaScreen),

    // Playback & Streams
    PlayerAndVideo("player-video", Res.string.settings_desktop_tab_player_video, SettingsPage.PlaybackPlayer),
    AudioAndSubtitles("audio-subtitles", Res.string.settings_desktop_tab_audio_subtitles, SettingsPage.PlaybackAudio),
    StreamsAndBadges("streams-badges", Res.string.settings_desktop_tab_streams_badges, SettingsPage.PlaybackStreamSelection),
    UpNextAndSkip("up-next-skip", Res.string.settings_desktop_tab_up_next_skip, SettingsPage.PlaybackUpNext),
    InterfaceAndNetwork("interface-network", Res.string.settings_desktop_tab_interface_network, SettingsPage.PlaybackInterface),

    // Addons & Plugins
    Addons("addons", Res.string.compose_settings_page_addons, SettingsPage.Addons),
    Plugins("plugins", Res.string.compose_settings_page_plugins, SettingsPage.Plugins),

    // Metadata & Ratings (APIs)
    Tmdb("tmdb", Res.string.settings_desktop_tab_tmdb, SettingsPage.TmdbEnrichment),
    MdbList("mdblist", Res.string.settings_desktop_tab_mdblist, SettingsPage.MdbListRatings),

    // System & Performance
    PerformanceAndGraphics("system-performance", Res.string.settings_desktop_tab_performance, SettingsPage.Advanced),
    PrivacyAndDiagnostics("system-privacy", Res.string.settings_desktop_tab_privacy, SettingsPage.Advanced),
    Notifications("system-notifications", Res.string.settings_desktop_tab_notifications, SettingsPage.Notifications),
}

internal data class DesktopSettingsWorkspaceRoute(
    val workspace: DesktopSettingsWorkspace,
    val selectedTab: DesktopSettingsWorkspaceTab?,
    val subDetailPage: SettingsPage? = null,
)

internal sealed interface DesktopSettingsDestination {
    data class Page(val page: SettingsPage) : DesktopSettingsDestination
    data object SwitchProfile : DesktopSettingsDestination
    data object WatchParty : DesktopSettingsDestination
    data object Collections : DesktopSettingsDestination
}

internal data class DesktopSettingsNavItem(
    val id: String,
    val workspace: DesktopSettingsWorkspace,
    val destination: DesktopSettingsDestination,
    val titleRes: StringResource,
    val icon: ImageVector,
)

internal data class DesktopSettingsNavSection(
    val group: DesktopSettingsGroup,
    val items: List<DesktopSettingsNavItem>,
)

internal fun buildDesktopSettingsNavSections(): List<DesktopSettingsNavSection> =
    DesktopSettingsGroup.entries.map { group ->
        DesktopSettingsNavSection(
            group = group,
            items = DesktopSettingsWorkspace.entries
                .filter { it.group == group }
                .map { workspace ->
                    DesktopSettingsNavItem(
                        id = workspace.name,
                        workspace = workspace,
                        destination = DesktopSettingsDestination.Page(workspace.primaryPage),
                        titleRes = workspace.titleRes,
                        icon = workspace.icon,
                    )
                },
        )
    }

internal fun desktopWorkspaceTabs(
    workspace: DesktopSettingsWorkspace,
    showPlugins: Boolean,
    showNotifications: Boolean,
    showSentry: Boolean,
): List<DesktopSettingsWorkspaceTab> = when (workspace) {
    DesktopSettingsWorkspace.HomeAndCards -> listOf(
        DesktopSettingsWorkspaceTab.HomeCatalogs,
        DesktopSettingsWorkspaceTab.ContinueWatching,
        DesktopSettingsWorkspaceTab.PostersAndPreview,
        DesktopSettingsWorkspaceTab.DetailsPage,
    )
    DesktopSettingsWorkspace.PlaybackAndStreams -> listOf(
        DesktopSettingsWorkspaceTab.PlayerAndVideo,
        DesktopSettingsWorkspaceTab.AudioAndSubtitles,
        DesktopSettingsWorkspaceTab.StreamsAndBadges,
        DesktopSettingsWorkspaceTab.UpNextAndSkip,
        DesktopSettingsWorkspaceTab.InterfaceAndNetwork,
    )
    DesktopSettingsWorkspace.AddonsAndPlugins -> buildList {
        add(DesktopSettingsWorkspaceTab.Addons)
        if (showPlugins) add(DesktopSettingsWorkspaceTab.Plugins)
    }
    DesktopSettingsWorkspace.MetadataAndRatings -> listOf(
        DesktopSettingsWorkspaceTab.Tmdb,
        DesktopSettingsWorkspaceTab.MdbList,
    )
    DesktopSettingsWorkspace.SystemAndPerformance -> buildList {
        add(DesktopSettingsWorkspaceTab.PerformanceAndGraphics)
        if (showSentry) add(DesktopSettingsWorkspaceTab.PrivacyAndDiagnostics)
        if (showNotifications) add(DesktopSettingsWorkspaceTab.Notifications)
    }
    DesktopSettingsWorkspace.AppearanceAndNav,
    DesktopSettingsWorkspace.ServersAndCollections,
    DesktopSettingsWorkspace.TrackingAndSync,
    DesktopSettingsWorkspace.DebridServices,
    DesktopSettingsWorkspace.AccountAndProfiles,
    DesktopSettingsWorkspace.AboutAndUpdates,
    -> emptyList()
}

internal fun resolveDesktopWorkspaceRoute(page: SettingsPage): DesktopSettingsWorkspaceRoute = when (page) {
    SettingsPage.Root,
    SettingsPage.Appearance,
    SettingsPage.Layout,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AppearanceAndNav,
        selectedTab = null,
    )

    SettingsPage.Homescreen -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.HomeAndCards,
        selectedTab = DesktopSettingsWorkspaceTab.HomeCatalogs,
    )
    SettingsPage.ContinueWatching -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.HomeAndCards,
        selectedTab = DesktopSettingsWorkspaceTab.ContinueWatching,
    )
    SettingsPage.PosterCustomization,
    SettingsPage.HoverPreview,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.HomeAndCards,
        selectedTab = DesktopSettingsWorkspaceTab.PostersAndPreview,
    )
    SettingsPage.MetaScreen -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.HomeAndCards,
        selectedTab = DesktopSettingsWorkspaceTab.DetailsPage,
    )

    SettingsPage.Playback,
    SettingsPage.PlaybackPlayer,
    SettingsPage.PlaybackVideo,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.PlaybackAndStreams,
        selectedTab = DesktopSettingsWorkspaceTab.PlayerAndVideo,
    )
    SettingsPage.PlaybackAudio,
    SettingsPage.PlaybackSubtitles,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.PlaybackAndStreams,
        selectedTab = DesktopSettingsWorkspaceTab.AudioAndSubtitles,
    )
    SettingsPage.PlaybackStreamSelection,
    SettingsPage.Streams,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.PlaybackAndStreams,
        selectedTab = DesktopSettingsWorkspaceTab.StreamsAndBadges,
    )
    SettingsPage.PlaybackUpNext,
    SettingsPage.PlaybackSkipSegments,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.PlaybackAndStreams,
        selectedTab = DesktopSettingsWorkspaceTab.UpNextAndSkip,
    )
    SettingsPage.PlaybackInterface,
    SettingsPage.PlaybackBuffer,
    SettingsPage.PlaybackP2p,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.PlaybackAndStreams,
        selectedTab = DesktopSettingsWorkspaceTab.InterfaceAndNetwork,
    )

    SettingsPage.ContentDiscovery,
    SettingsPage.Addons,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AddonsAndPlugins,
        selectedTab = DesktopSettingsWorkspaceTab.Addons,
    )
    SettingsPage.Plugins -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AddonsAndPlugins,
        selectedTab = DesktopSettingsWorkspaceTab.Plugins,
    )

    SettingsPage.MediaServers -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.ServersAndCollections,
        selectedTab = null,
    )
    SettingsPage.MediaServer -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.ServersAndCollections,
        selectedTab = null,
        subDetailPage = SettingsPage.MediaServer,
    )

    SettingsPage.TraktAuthentication -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.TrackingAndSync,
        selectedTab = null,
    )

    SettingsPage.Integrations,
    SettingsPage.TmdbEnrichment,
    -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.MetadataAndRatings,
        selectedTab = DesktopSettingsWorkspaceTab.Tmdb,
    )
    SettingsPage.MdbListRatings -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.MetadataAndRatings,
        selectedTab = DesktopSettingsWorkspaceTab.MdbList,
    )

    SettingsPage.Debrid -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.DebridServices,
        selectedTab = null,
    )

    SettingsPage.Account -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AccountAndProfiles,
        selectedTab = null,
    )

    SettingsPage.Advanced -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.SystemAndPerformance,
        selectedTab = DesktopSettingsWorkspaceTab.PerformanceAndGraphics,
    )
    SettingsPage.Notifications -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.SystemAndPerformance,
        selectedTab = DesktopSettingsWorkspaceTab.Notifications,
    )

    SettingsPage.About -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AboutAndUpdates,
        selectedTab = null,
    )
    SettingsPage.SupportersContributors -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AboutAndUpdates,
        selectedTab = null,
        subDetailPage = SettingsPage.SupportersContributors,
    )
    SettingsPage.LicensesAttributions -> DesktopSettingsWorkspaceRoute(
        workspace = DesktopSettingsWorkspace.AboutAndUpdates,
        selectedTab = null,
        subDetailPage = SettingsPage.LicensesAttributions,
    )
}

internal fun resolveDesktopInitialSettingsPage(requestedPage: SettingsPage): SettingsPage =
    if (requestedPage == SettingsPage.Root) SettingsPage.Appearance else requestedPage

internal fun resolveDesktopSidebarSelectedPage(currentPage: SettingsPage): SettingsPage =
    resolveDesktopWorkspaceRoute(currentPage).workspace.primaryPage

internal fun resolveDesktopParentPage(currentPage: SettingsPage): SettingsPage? = when (currentPage) {
    SettingsPage.MediaServer -> SettingsPage.MediaServers
    SettingsPage.SupportersContributors,
    SettingsPage.LicensesAttributions,
    -> SettingsPage.About
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

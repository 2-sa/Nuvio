package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.features.search.SearchHistoryRepository
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_addons
import nuvio.composeapp.generated.resources.compose_settings_page_media_servers
import nuvio.composeapp.generated.resources.compose_settings_page_plugins
import nuvio.composeapp.generated.resources.settings_content_discovery_addons_description
import nuvio.composeapp.generated.resources.settings_content_discovery_addons_description_appstore
import nuvio.composeapp.generated.resources.settings_content_discovery_media_servers_description
import nuvio.composeapp.generated.resources.settings_content_discovery_plugins_description
import nuvio.composeapp.generated.resources.settings_content_discovery_section_sources
import nuvio.composeapp.generated.resources.settings_content_discovery_section_search
import nuvio.composeapp.generated.resources.settings_content_discovery_recent_searches
import nuvio.composeapp.generated.resources.settings_content_discovery_recent_searches_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.contentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onMediaServersClick: () -> Unit,
) {
    item {
        val recentSearchesEnabled by remember {
            SearchHistoryRepository.ensureLoaded()
            SearchHistoryRepository.enabled
        }.collectAsStateWithLifecycle()

        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_search),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_content_discovery_recent_searches),
                    description = stringResource(Res.string.settings_content_discovery_recent_searches_description),
                    checked = recentSearchesEnabled,
                    isTablet = isTablet,
                    onCheckedChange = SearchHistoryRepository::setEnabled,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_sources),
            isTablet = isTablet,
        ) {
            val addonsDescription = stringResource(
                if (AppFeaturePolicy.personalMediaAddonCopyEnabled) {
                    Res.string.settings_content_discovery_addons_description_appstore
                } else {
                    Res.string.settings_content_discovery_addons_description
                },
            )
            val addonsTitle = stringResource(Res.string.compose_settings_page_addons)
            val mediaServersTitle = stringResource(Res.string.compose_settings_page_media_servers)
            val mediaServersDescription = stringResource(Res.string.settings_content_discovery_media_servers_description)
            val pluginsTitle = stringResource(Res.string.compose_settings_page_plugins)
            val pluginsDescription = stringResource(Res.string.settings_content_discovery_plugins_description)

            if (isDesktop) {
                SettingsBentoGrid(
                    items = buildList {
                        add(
                            SettingsBentoItem(
                                id = "addons",
                                title = addonsTitle,
                                description = addonsDescription,
                                icon = SettingsPage.Addons.icon,
                                onClick = onAddonsClick,
                            ),
                        )
                        add(
                            SettingsBentoItem(
                                id = "media-servers",
                                title = mediaServersTitle,
                                description = mediaServersDescription,
                                icon = SettingsPage.MediaServers.icon,
                                onClick = onMediaServersClick,
                            ),
                        )
                        if (showPluginsEntry) {
                            add(
                                SettingsBentoItem(
                                    id = "plugins",
                                    title = pluginsTitle,
                                    description = pluginsDescription,
                                    icon = SettingsPage.Plugins.icon,
                                    onClick = onPluginsClick,
                                ),
                            )
                        }
                    },
                    isTablet = isTablet,
                )
            } else {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = addonsTitle,
                        description = addonsDescription,
                        isTablet = isTablet,
                        onClick = onAddonsClick,
                    )
                    SettingsNavigationRow(
                        title = mediaServersTitle,
                        description = mediaServersDescription,
                        isTablet = isTablet,
                        onClick = onMediaServersClick,
                    )
                    if (showPluginsEntry) {
                        SettingsNavigationRow(
                            title = pluginsTitle,
                            description = pluginsDescription,
                            isTablet = isTablet,
                            onClick = onPluginsClick,
                        )
                    }
                }
            }
        }
    }
}

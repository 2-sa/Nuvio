package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.compose_settings_page_debrid
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import nuvio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import nuvio.composeapp.generated.resources.settings_integrations_mdblist_description
import nuvio.composeapp.generated.resources.settings_integrations_debrid_description
import nuvio.composeapp.generated.resources.settings_integrations_section_title
import nuvio.composeapp.generated.resources.settings_integrations_tmdb_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_title),
            isTablet = isTablet,
        ) {
            val tmdbTitle = stringResource(Res.string.compose_settings_page_tmdb_enrichment)
            val tmdbDescription = stringResource(Res.string.settings_integrations_tmdb_description)
            val tmdbPainter = integrationLogoPainter(IntegrationLogo.Tmdb)
            val mdbListTitle = stringResource(Res.string.compose_settings_page_mdblist_ratings)
            val mdbListDescription = stringResource(Res.string.settings_integrations_mdblist_description)
            val mdbListPainter = integrationLogoPainter(IntegrationLogo.MdbList)
            val debridTitle = stringResource(Res.string.compose_settings_page_debrid)
            val debridDescription = stringResource(Res.string.settings_integrations_debrid_description)

            if (isDesktop) {
                SettingsBentoGrid(
                    items = listOf(
                        SettingsBentoItem(
                            id = "tmdb",
                            title = tmdbTitle,
                            description = tmdbDescription,
                            icon = SettingsPage.TmdbEnrichment.icon,
                            iconPainter = tmdbPainter,
                            onClick = onTmdbClick,
                        ),
                        SettingsBentoItem(
                            id = "mdblist",
                            title = mdbListTitle,
                            description = mdbListDescription,
                            icon = SettingsPage.MdbListRatings.icon,
                            iconPainter = mdbListPainter,
                            onClick = onMdbListClick,
                        ),
                        SettingsBentoItem(
                            id = "debrid",
                            title = debridTitle,
                            description = debridDescription,
                            icon = SettingsPage.Debrid.icon,
                            onClick = onDebridClick,
                        ),
                    ),
                    isTablet = isTablet,
                )
            } else {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = tmdbTitle,
                        description = tmdbDescription,
                        iconPainter = tmdbPainter,
                        isTablet = isTablet,
                        onClick = onTmdbClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = mdbListTitle,
                        description = mdbListDescription,
                        iconPainter = mdbListPainter,
                        isTablet = isTablet,
                        onClick = onMdbListClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = debridTitle,
                        description = debridDescription,
                        isTablet = isTablet,
                        onClick = onDebridClick,
                    )
                }
            }
        }
    }
}

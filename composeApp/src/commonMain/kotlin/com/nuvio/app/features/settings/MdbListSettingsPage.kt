package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListSettings
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_api_key_status_account
import nuvio.composeapp.generated.resources.settings_api_key_status_not_set
import nuvio.composeapp.generated.resources.settings_api_key_status_personal
import nuvio.composeapp.generated.resources.settings_mdb_api_key_label
import nuvio.composeapp.generated.resources.settings_mdb_api_key_title
import nuvio.composeapp.generated.resources.settings_mdb_enable_ratings
import nuvio.composeapp.generated.resources.settings_mdb_enable_ratings_description
import nuvio.composeapp.generated.resources.settings_mdb_ratings_connected_account
import nuvio.composeapp.generated.resources.settings_mdb_ratings_credentials_required
import nuvio.composeapp.generated.resources.settings_mdb_ratings_key_override_description
import nuvio.composeapp.generated.resources.settings_mdb_section_api_key
import nuvio.composeapp.generated.resources.settings_mdb_section_rating_providers
import nuvio.composeapp.generated.resources.settings_mdb_section_title
import nuvio.composeapp.generated.resources.settings_mdb_show_on_hero
import nuvio.composeapp.generated.resources.settings_mdb_show_on_hero_description
import nuvio.composeapp.generated.resources.settings_modules_disable_all
import nuvio.composeapp.generated.resources.settings_modules_enable_all
import nuvio.composeapp.generated.resources.source_audience_score
import nuvio.composeapp.generated.resources.source_imdb
import nuvio.composeapp.generated.resources.source_letterboxd
import nuvio.composeapp.generated.resources.source_mal
import nuvio.composeapp.generated.resources.source_metacritic
import nuvio.composeapp.generated.resources.source_rotten_tomatoes
import nuvio.composeapp.generated.resources.source_tmdb
import nuvio.composeapp.generated.resources.source_trakt
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.mdbListSettingsContent(
    isTablet: Boolean,
    settings: MdbListSettings,
) {
    val providerControlsEnabled = settings.enabled

    item {
        val controlsContent: @Composable () -> Unit = {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_mdb_enable_ratings),
                    description = stringResource(Res.string.settings_mdb_enable_ratings_description),
                    checked = settings.enabled,
                    isTablet = isTablet,
                    onCheckedChange = MdbListSettingsRepository::setEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_mdb_show_on_hero),
                    description = stringResource(Res.string.settings_mdb_show_on_hero_description),
                    checked = settings.showOnHero,
                    enabled = providerControlsEnabled,
                    isTablet = isTablet,
                    onCheckedChange = MdbListSettingsRepository::setShowOnHero,
                )
                if (!settings.hasCredentials) {
                    SettingsGroupDivider(isTablet = isTablet)
                    MdbListInfoRow(
                        isTablet = isTablet,
                        text = stringResource(Res.string.settings_mdb_ratings_credentials_required),
                    )
                }
            }
        }
        if (isDesktop) {
            controlsContent()
        } else {
            SettingsSection(
                title = stringResource(Res.string.settings_mdb_section_title),
                isTablet = isTablet,
            ) {
                controlsContent()
            }
        }
    }

    item {
        val usingConnectedAccount = settings.accountScope != null && !settings.hasApiKey
        val statusLabelRes = when {
            settings.hasApiKey -> Res.string.settings_api_key_status_personal
            usingConnectedAccount -> Res.string.settings_api_key_status_account
            else -> Res.string.settings_api_key_status_not_set
        }
        val statusTone = when {
            settings.hasApiKey || usingConnectedAccount -> SettingsStatusTone.Success
            else -> SettingsStatusTone.Warning
        }

        SettingsSection(
            title = stringResource(Res.string.settings_mdb_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsApiKeyCard(
                    title = stringResource(Res.string.settings_mdb_api_key_title),
                    description = stringResource(Res.string.settings_mdb_ratings_key_override_description),
                    placeholder = stringResource(Res.string.settings_mdb_api_key_label),
                    savedValue = settings.apiKey,
                    statusLabel = stringResource(statusLabelRes),
                    statusTone = statusTone,
                    isTablet = isTablet,
                    supportsDefaultFallback = false,
                    supportingNote = if (usingConnectedAccount) {
                        stringResource(Res.string.settings_mdb_ratings_connected_account)
                    } else {
                        null
                    },
                    onSave = MdbListSettingsRepository::setApiKey,
                    onTestKey = { candidate -> validateMdbListApiKey(candidate) },
                )
            }
        }
    }

    item {
        val providers = listOf(
            MdbListMetadataService.PROVIDER_IMDB to Res.string.source_imdb,
            MdbListMetadataService.PROVIDER_TMDB to Res.string.source_tmdb,
            MdbListMetadataService.PROVIDER_TOMATOES to Res.string.source_rotten_tomatoes,
            MdbListMetadataService.PROVIDER_METACRITIC to Res.string.source_metacritic,
            MdbListMetadataService.PROVIDER_TRAKT to Res.string.source_trakt,
            MdbListMetadataService.PROVIDER_LETTERBOXD to Res.string.source_letterboxd,
            MdbListMetadataService.PROVIDER_AUDIENCE to Res.string.source_audience_score,
            MdbListMetadataService.PROVIDER_MAL to Res.string.source_mal,
        )
        val allProvidersEnabled = providers.all { (id, _) -> settings.isProviderEnabled(id) }

        SettingsSection(
            title = stringResource(Res.string.settings_mdb_section_rating_providers),
            isTablet = isTablet,
            actions = {
                if (providerControlsEnabled) {
                    NuvioActionLabel(
                        text = stringResource(
                            if (allProvidersEnabled) {
                                Res.string.settings_modules_disable_all
                            } else {
                                Res.string.settings_modules_enable_all
                            },
                        ),
                        onClick = {
                            val target = !allProvidersEnabled
                            providers.forEach { (id, _) ->
                                MdbListSettingsRepository.setProviderEnabled(id, target)
                            }
                        },
                    )
                }
            },
        ) {
            SettingsToggleGrid(
                items = providers.map { (providerId, providerLabelRes) ->
                    SettingsToggleGridItem(
                        id = providerId,
                        title = stringResource(providerLabelRes),
                        checked = settings.isProviderEnabled(providerId),
                        enabled = providerControlsEnabled,
                        onCheckedChange = { checked ->
                            MdbListSettingsRepository.setProviderEnabled(providerId, checked)
                        },
                    )
                },
                isTablet = isTablet,
            )
        }
    }
}

@Composable
private fun MdbListInfoRow(
    isTablet: Boolean,
    text: String,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 14.dp else 12.dp

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

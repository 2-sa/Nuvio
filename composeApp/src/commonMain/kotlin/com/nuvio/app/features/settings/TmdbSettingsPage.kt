package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.tmdb.TmdbSettings
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.normalizeLanguage
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.settings_api_key_status_default
import nuvio.composeapp.generated.resources.settings_api_key_status_personal
import nuvio.composeapp.generated.resources.settings_modules_disable_all
import nuvio.composeapp.generated.resources.settings_modules_enable_all
import nuvio.composeapp.generated.resources.settings_tmdb_api_key_label
import nuvio.composeapp.generated.resources.settings_tmdb_api_key_override_description
import nuvio.composeapp.generated.resources.settings_tmdb_enable_enrichment
import nuvio.composeapp.generated.resources.settings_tmdb_enable_enrichment_description
import nuvio.composeapp.generated.resources.settings_tmdb_language_code_label
import nuvio.composeapp.generated.resources.settings_tmdb_module_artwork
import nuvio.composeapp.generated.resources.settings_tmdb_module_artwork_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_basic_info
import nuvio.composeapp.generated.resources.settings_tmdb_module_basic_info_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_collections
import nuvio.composeapp.generated.resources.settings_tmdb_module_collections_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_credits
import nuvio.composeapp.generated.resources.settings_tmdb_module_credits_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_details
import nuvio.composeapp.generated.resources.settings_tmdb_module_details_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_episodes
import nuvio.composeapp.generated.resources.settings_tmdb_module_episodes_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_more_like_this
import nuvio.composeapp.generated.resources.settings_tmdb_module_more_like_this_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_networks
import nuvio.composeapp.generated.resources.settings_tmdb_module_networks_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_production_companies
import nuvio.composeapp.generated.resources.settings_tmdb_module_production_companies_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_season_posters
import nuvio.composeapp.generated.resources.settings_tmdb_module_season_posters_description
import nuvio.composeapp.generated.resources.settings_tmdb_module_trailers
import nuvio.composeapp.generated.resources.settings_tmdb_module_trailers_description
import nuvio.composeapp.generated.resources.settings_tmdb_personal_api_key
import nuvio.composeapp.generated.resources.settings_tmdb_preferred_language
import nuvio.composeapp.generated.resources.settings_tmdb_preferred_language_description
import nuvio.composeapp.generated.resources.settings_tmdb_section_localization
import nuvio.composeapp.generated.resources.settings_tmdb_section_modules
import nuvio.composeapp.generated.resources.settings_tmdb_section_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.tmdbSettingsContent(
    isTablet: Boolean,
    settings: TmdbSettings,
) {
    val enrichmentControlsEnabled = settings.enabled

    item {
        val coreContent: @Composable () -> Unit = {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_tmdb_enable_enrichment),
                    description = stringResource(Res.string.settings_tmdb_enable_enrichment_description),
                    checked = settings.enabled,
                    isTablet = isTablet,
                    onCheckedChange = TmdbSettingsRepository::setEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsApiKeyCard(
                    title = stringResource(Res.string.settings_tmdb_personal_api_key),
                    description = stringResource(Res.string.settings_tmdb_api_key_override_description),
                    placeholder = stringResource(Res.string.settings_tmdb_api_key_label),
                    savedValue = settings.apiKey,
                    statusLabel = stringResource(
                        if (settings.apiKey.isNotBlank()) {
                            Res.string.settings_api_key_status_personal
                        } else {
                            Res.string.settings_api_key_status_default
                        },
                    ),
                    statusTone = if (settings.apiKey.isNotBlank()) {
                        SettingsStatusTone.Success
                    } else {
                        SettingsStatusTone.Neutral
                    },
                    isTablet = isTablet,
                    supportsDefaultFallback = true,
                    onSave = TmdbSettingsRepository::setApiKey,
                    onTestKey = { candidate ->
                        validateTmdbApiKey(
                            apiKey = candidate,
                            fallbackDefaultKey = TmdbSettingsRepository.effectiveApiKey(),
                        )
                    },
                )
            }
        }
        if (isDesktop) {
            coreContent()
        } else {
            SettingsSection(
                title = stringResource(Res.string.settings_tmdb_section_title),
                isTablet = isTablet,
            ) {
                coreContent()
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_localization),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                TmdbLanguageRow(
                    isTablet = isTablet,
                    value = settings.language,
                    enabled = true,
                    onLanguageCommitted = TmdbSettingsRepository::setLanguage,
                )
            }
        }
    }

    item {
        val allModulesEnabled = settings.useTrailers &&
            settings.useArtwork &&
            settings.useBasicInfo &&
            settings.useDetails &&
            settings.useCredits &&
            settings.useProductions &&
            settings.useNetworks &&
            settings.useEpisodes &&
            settings.useSeasonPosters &&
            settings.useMoreLikeThis &&
            settings.useCollections
        val setAllModules: (Boolean) -> Unit = { target ->
            TmdbSettingsRepository.setUseTrailers(target)
            TmdbSettingsRepository.setUseArtwork(target)
            TmdbSettingsRepository.setUseBasicInfo(target)
            TmdbSettingsRepository.setUseDetails(target)
            TmdbSettingsRepository.setUseCredits(target)
            TmdbSettingsRepository.setUseProductions(target)
            TmdbSettingsRepository.setUseNetworks(target)
            TmdbSettingsRepository.setUseEpisodes(target)
            TmdbSettingsRepository.setUseSeasonPosters(target)
            TmdbSettingsRepository.setUseMoreLikeThis(target)
            TmdbSettingsRepository.setUseCollections(target)
        }

        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_modules),
            isTablet = isTablet,
            actions = {
                if (enrichmentControlsEnabled) {
                    NuvioActionLabel(
                        text = stringResource(
                            if (allModulesEnabled) {
                                Res.string.settings_modules_disable_all
                            } else {
                                Res.string.settings_modules_enable_all
                            },
                        ),
                        onClick = { setAllModules(!allModulesEnabled) },
                    )
                }
            },
        ) {
            SettingsToggleGrid(
                items = listOf(
                    SettingsToggleGridItem(
                        id = "trailers",
                        title = stringResource(Res.string.settings_tmdb_module_trailers),
                        description = stringResource(Res.string.settings_tmdb_module_trailers_description),
                        checked = settings.useTrailers,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseTrailers,
                    ),
                    SettingsToggleGridItem(
                        id = "artwork",
                        title = stringResource(Res.string.settings_tmdb_module_artwork),
                        description = stringResource(Res.string.settings_tmdb_module_artwork_description),
                        checked = settings.useArtwork,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseArtwork,
                    ),
                    SettingsToggleGridItem(
                        id = "basic-info",
                        title = stringResource(Res.string.settings_tmdb_module_basic_info),
                        description = stringResource(Res.string.settings_tmdb_module_basic_info_description),
                        checked = settings.useBasicInfo,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseBasicInfo,
                    ),
                    SettingsToggleGridItem(
                        id = "details",
                        title = stringResource(Res.string.settings_tmdb_module_details),
                        description = stringResource(Res.string.settings_tmdb_module_details_description),
                        checked = settings.useDetails,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseDetails,
                    ),
                    SettingsToggleGridItem(
                        id = "credits",
                        title = stringResource(Res.string.settings_tmdb_module_credits),
                        description = stringResource(Res.string.settings_tmdb_module_credits_description),
                        checked = settings.useCredits,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseCredits,
                    ),
                    SettingsToggleGridItem(
                        id = "productions",
                        title = stringResource(Res.string.settings_tmdb_module_production_companies),
                        description = stringResource(Res.string.settings_tmdb_module_production_companies_description),
                        checked = settings.useProductions,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseProductions,
                    ),
                    SettingsToggleGridItem(
                        id = "networks",
                        title = stringResource(Res.string.settings_tmdb_module_networks),
                        description = stringResource(Res.string.settings_tmdb_module_networks_description),
                        checked = settings.useNetworks,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseNetworks,
                    ),
                    SettingsToggleGridItem(
                        id = "episodes",
                        title = stringResource(Res.string.settings_tmdb_module_episodes),
                        description = stringResource(Res.string.settings_tmdb_module_episodes_description),
                        checked = settings.useEpisodes,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseEpisodes,
                    ),
                    SettingsToggleGridItem(
                        id = "season-posters",
                        title = stringResource(Res.string.settings_tmdb_module_season_posters),
                        description = stringResource(Res.string.settings_tmdb_module_season_posters_description),
                        checked = settings.useSeasonPosters,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseSeasonPosters,
                    ),
                    SettingsToggleGridItem(
                        id = "more-like-this",
                        title = stringResource(Res.string.settings_tmdb_module_more_like_this),
                        description = stringResource(Res.string.settings_tmdb_module_more_like_this_description),
                        checked = settings.useMoreLikeThis,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseMoreLikeThis,
                    ),
                    SettingsToggleGridItem(
                        id = "collections",
                        title = stringResource(Res.string.settings_tmdb_module_collections),
                        description = stringResource(Res.string.settings_tmdb_module_collections_description),
                        checked = settings.useCollections,
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setUseCollections,
                    ),
                ),
                isTablet = isTablet,
            )
        }
    }
}

@Composable
private fun TmdbLanguageRow(
    isTablet: Boolean,
    value: String,
    enabled: Boolean,
    onLanguageCommitted: (String) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val normalizedDraft = normalizeLanguage(draft)
    val canSave = enabled && normalizedDraft != value
    val commitLanguage: () -> Unit = {
        if (canSave) {
            draft = normalizedDraft
            onLanguageCommitted(normalizedDraft)
        }
    }

    if (isDesktop) {
        var isFocused by remember { mutableStateOf(false) }
        val pillShape = RoundedCornerShape(8.dp)
        DesktopSettingRow(
            title = stringResource(Res.string.settings_tmdb_preferred_language),
            description = stringResource(Res.string.settings_tmdb_preferred_language_description),
            enabled = enabled,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        modifier = Modifier
                            .width(132.dp)
                            .height(34.dp),
                        shape = pillShape,
                        color = Color.White.copy(alpha = 0.055f),
                        border = BorderStroke(
                            tokens.borders.thin,
                            if (isFocused) tokens.colors.accent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.13f),
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.settings_tmdb_language_code_label),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = tokens.colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                enabled = enabled,
                                singleLine = true,
                                cursorBrush = SolidColor(tokens.colors.accent),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted,
                                    textDirection = TextDirection.Ltr,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitLanguage() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isFocused = it.isFocused },
                            )
                        }
                    }
                }
                Button(
                    onClick = commitLanguage,
                    enabled = canSave,
                    shape = pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        disabledContentColor = tokens.colors.textMuted,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .then(if (canSave) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                ) {
                    Text(
                        text = stringResource(Res.string.action_save),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16,
                    vertical = if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.settings_tmdb_preferred_language),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(Res.string.settings_tmdb_preferred_language_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    enabled = enabled,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(stringResource(Res.string.settings_tmdb_language_code_label)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.accent.copy(alpha = 0.78f),
                        unfocusedBorderColor = tokens.colors.borderSubtle,
                        focusedContainerColor = tokens.colors.surfaceElevated,
                        unfocusedContainerColor = tokens.colors.surfaceElevated,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        cursorColor = tokens.colors.accent,
                    ),
                )
                Button(
                    onClick = commitLanguage,
                    enabled = canSave,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        disabledContentColor = tokens.colors.textMuted,
                    ),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(stringResource(Res.string.action_save))
                }
            }
        }
    }
}

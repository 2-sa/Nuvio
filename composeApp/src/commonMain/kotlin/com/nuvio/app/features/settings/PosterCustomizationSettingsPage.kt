package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.poster.CustomPosterScreen
import com.nuvio.app.core.poster.CustomPosterUrlRepository
import com.nuvio.app.core.ui.AnimatedArtworkMode
import com.nuvio.app.core.ui.CardDepthStyleRepository
import com.nuvio.app.core.ui.CardDepthStyleUiState
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.core.ui.DefaultCardDepthEdgeCoverage
import com.nuvio.app.core.ui.DefaultCardDepthEdgeStrength
import com.nuvio.app.core.ui.DefaultCardDepthSheenStrength
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.Slider
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.cardDepthVisual
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.isDesktop
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.settings_card_depth_apply_to
import nuvio.composeapp.generated.resources.settings_card_depth_description
import nuvio.composeapp.generated.resources.settings_card_depth_edge
import nuvio.composeapp.generated.resources.settings_card_depth_edge_balanced
import nuvio.composeapp.generated.resources.settings_card_depth_edge_bold
import nuvio.composeapp.generated.resources.settings_card_depth_edge_subtle
import nuvio.composeapp.generated.resources.settings_card_depth_edge_value
import nuvio.composeapp.generated.resources.settings_card_depth_edge_coverage
import nuvio.composeapp.generated.resources.settings_card_depth_coverage_full
import nuvio.composeapp.generated.resources.settings_card_depth_coverage_half
import nuvio.composeapp.generated.resources.settings_card_depth_coverage_top
import nuvio.composeapp.generated.resources.settings_card_depth_coverage_value
import nuvio.composeapp.generated.resources.settings_card_depth_enabled
import nuvio.composeapp.generated.resources.settings_card_depth_fine_tune
import nuvio.composeapp.generated.resources.settings_card_depth_fine_tune_hint
import nuvio.composeapp.generated.resources.settings_card_depth_fine_tune_title
import nuvio.composeapp.generated.resources.settings_card_depth_pad_edge_axis
import nuvio.composeapp.generated.resources.settings_card_depth_pad_sheen_axis
import nuvio.composeapp.generated.resources.settings_card_depth_preview_meta
import nuvio.composeapp.generated.resources.settings_card_depth_preview_title
import nuvio.composeapp.generated.resources.settings_card_depth_sheen
import nuvio.composeapp.generated.resources.settings_card_depth_sheen_value
import nuvio.composeapp.generated.resources.settings_card_depth_sheen_bright
import nuvio.composeapp.generated.resources.settings_card_depth_sheen_off
import nuvio.composeapp.generated.resources.settings_card_depth_sheen_soft
import nuvio.composeapp.generated.resources.settings_card_depth_surface_cast
import nuvio.composeapp.generated.resources.settings_card_depth_surface_continue_watching
import nuvio.composeapp.generated.resources.settings_card_depth_surface_episodes
import nuvio.composeapp.generated.resources.settings_card_depth_surface_posters
import nuvio.composeapp.generated.resources.settings_card_depth_surface_trailers
import nuvio.composeapp.generated.resources.settings_card_depth_title
import nuvio.composeapp.generated.resources.settings_poster_card_radius
import nuvio.composeapp.generated.resources.settings_poster_card_style
import nuvio.composeapp.generated.resources.settings_poster_card_width
import nuvio.composeapp.generated.resources.settings_poster_custom
import nuvio.composeapp.generated.resources.settings_poster_description
import nuvio.composeapp.generated.resources.settings_poster_animated_artwork
import nuvio.composeapp.generated.resources.settings_poster_animated_artwork_always
import nuvio.composeapp.generated.resources.settings_poster_animated_artwork_off
import nuvio.composeapp.generated.resources.settings_poster_animated_artwork_on_hover
import nuvio.composeapp.generated.resources.settings_poster_hide_labels
import nuvio.composeapp.generated.resources.settings_poster_always_show_landscape_clearlogo
import nuvio.composeapp.generated.resources.settings_poster_landscape_mode
import nuvio.composeapp.generated.resources.settings_poster_live_preview
import nuvio.composeapp.generated.resources.settings_poster_option_with_value
import nuvio.composeapp.generated.resources.settings_poster_preview_corner_radius
import nuvio.composeapp.generated.resources.settings_poster_preview_height
import nuvio.composeapp.generated.resources.settings_poster_preview_width
import nuvio.composeapp.generated.resources.settings_poster_radius_classic
import nuvio.composeapp.generated.resources.settings_poster_radius_pill
import nuvio.composeapp.generated.resources.settings_poster_radius_rounded
import nuvio.composeapp.generated.resources.settings_poster_radius_sharp
import nuvio.composeapp.generated.resources.settings_poster_radius_subtle
import nuvio.composeapp.generated.resources.settings_poster_width_balanced
import nuvio.composeapp.generated.resources.settings_poster_width_comfort
import nuvio.composeapp.generated.resources.settings_poster_width_compact
import nuvio.composeapp.generated.resources.settings_poster_width_dense
import nuvio.composeapp.generated.resources.settings_poster_width_large
import nuvio.composeapp.generated.resources.settings_poster_width_standard
import nuvio.composeapp.generated.resources.settings_custom_poster_title
import nuvio.composeapp.generated.resources.settings_custom_poster_description
import nuvio.composeapp.generated.resources.settings_custom_poster_placeholder
import nuvio.composeapp.generated.resources.settings_custom_poster_active
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.settings_api_key_testing
import nuvio.composeapp.generated.resources.settings_custom_poster_save
import nuvio.composeapp.generated.resources.settings_custom_poster_clear
import nuvio.composeapp.generated.resources.settings_custom_poster_test
import nuvio.composeapp.generated.resources.settings_custom_poster_test_failed
import nuvio.composeapp.generated.resources.settings_custom_poster_test_invalid_pattern
import nuvio.composeapp.generated.resources.settings_custom_poster_test_success
import nuvio.composeapp.generated.resources.settings_custom_poster_apply_to
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_home
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_continue_watching
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_collections
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_library
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_search
import nuvio.composeapp.generated.resources.settings_custom_poster_screen_details
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.posterCustomizationSettingsContent(
    isTablet: Boolean,
    uiState: PosterCardStyleUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_poster_card_style),
            isTablet = isTablet,
            actions = {
                NuvioActionLabel(
                    text = stringResource(Res.string.action_reset),
                    onClick = PosterCardStyleRepository::resetToDefaults,
                )
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                PosterCardStyleControls(
                    isTablet = isTablet,
                    widthDp = uiState.widthDp,
                    cornerRadiusDp = uiState.cornerRadiusDp,
                    catalogLandscapeModeEnabled = uiState.catalogLandscapeModeEnabled,
                    hideLabelsEnabled = uiState.hideLabelsEnabled,
                    alwaysShowLandscapeClearlogo = uiState.alwaysShowLandscapeClearlogo,
                    animatedArtworkMode = uiState.animatedArtworkMode,
                    onWidthSelected = PosterCardStyleRepository::setWidthDp,
                    onCornerRadiusSelected = PosterCardStyleRepository::setCornerRadiusDp,
                    onCatalogLandscapeModeChange = PosterCardStyleRepository::setCatalogLandscapeModeEnabled,
                    onHideLabelsChange = PosterCardStyleRepository::setHideLabelsEnabled,
                    onAlwaysShowLandscapeClearlogoChange = PosterCardStyleRepository::setAlwaysShowLandscapeClearlogo,
                    onAnimatedArtworkModeSelected = PosterCardStyleRepository::setAnimatedArtworkMode,
                )
            }
        }
    }
    item {
        CustomPosterUrlRepository.ensureLoaded()
        val currentPattern by CustomPosterUrlRepository.pattern.collectAsState()
        var editingPattern by rememberSaveable(currentPattern) { mutableStateOf(currentPattern) }
        val isActive = currentPattern.isNotBlank()

        SettingsSection(
            title = stringResource(Res.string.settings_custom_poster_title),
            isTablet = isTablet,
            actions = {
                if (isActive) {
                    NuvioActionLabel(
                        text = stringResource(Res.string.settings_custom_poster_clear),
                        onClick = {
                            editingPattern = ""
                            CustomPosterUrlRepository.clearPattern()
                        },
                    )
                }
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                val tokens = MaterialTheme.nuvio
                val horizontalPadding = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16
                val verticalPadding = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16
                val scope = rememberCoroutineScope()
                var urlTestResult by remember(currentPattern) { mutableStateOf<ApiKeyTestResult>(ApiKeyTestResult.Idle) }
                Column(
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_custom_poster_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                    OutlinedTextField(
                        value = editingPattern,
                        onValueChange = {
                            editingPattern = it
                            if (urlTestResult !is ApiKeyTestResult.Idle) {
                                urlTestResult = ApiKeyTestResult.Idle
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(NuvioTokens.Radius.lg),
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.settings_custom_poster_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = tokens.colors.textPrimary,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.colors.accent,
                            unfocusedBorderColor = tokens.colors.borderDefault,
                            focusedContainerColor = tokens.colors.surfaceElevated,
                            unfocusedContainerColor = tokens.colors.surfaceElevated,
                            focusedPlaceholderColor = tokens.colors.textMuted,
                            unfocusedPlaceholderColor = tokens.colors.textMuted,
                            cursorColor = tokens.colors.accent,
                        ),
                        singleLine = false,
                        maxLines = 4,
                    )
                    val isTestingUrl = urlTestResult is ApiKeyTestResult.Testing
                    val canTestUrl = !isTestingUrl && editingPattern.trim().isNotBlank()
                    val canSave = editingPattern.trim() != currentPattern
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!canTestUrl) return@OutlinedButton
                                urlTestResult = ApiKeyTestResult.Testing
                                scope.launch {
                                    urlTestResult = validateCustomPosterUrlPattern(editingPattern)
                                }
                            },
                            enabled = canTestUrl,
                            shape = RoundedCornerShape(NuvioTokens.Radius.lg),
                            border = BorderStroke(
                                width = tokens.borders.thin,
                                color = if (canTestUrl) tokens.colors.accent.copy(alpha = 0.45f) else tokens.colors.borderSubtle,
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = tokens.colors.accent.copy(alpha = 0.10f),
                                contentColor = tokens.colors.textPrimary,
                                disabledContainerColor = Color.Transparent,
                                disabledContentColor = tokens.colors.textDisabled,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .then(if (isDesktop && canTestUrl) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                        ) {
                            if (isTestingUrl) {
                                NuvioLoadingIndicator(
                                    color = tokens.colors.accent,
                                    modifier = Modifier.size(15.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = if (canTestUrl) tokens.colors.accent else tokens.colors.textDisabled,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (isTestingUrl) Res.string.settings_api_key_testing else Res.string.settings_custom_poster_test,
                                ),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Button(
                            onClick = {
                                CustomPosterUrlRepository.setPattern(editingPattern)
                            },
                            enabled = canSave,
                            modifier = Modifier.then(
                                if (isDesktop && canSave) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier,
                            ),
                            shape = RoundedCornerShape(NuvioTokens.Radius.lg),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.colors.accent,
                                contentColor = tokens.colors.onAccent,
                                disabledContainerColor = tokens.colors.surfaceElevated,
                                disabledContentColor = tokens.colors.textDisabled,
                            ),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_custom_poster_save),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (isActive) {
                            SettingsStatusPill(
                                label = stringResource(Res.string.settings_custom_poster_active),
                                tone = SettingsStatusTone.Success,
                            )
                        }
                    }
                    AnimatedVisibility(visible = urlTestResult is ApiKeyTestResult.Success || urlTestResult is ApiKeyTestResult.Failure) {
                        val failureOverride = when (val failure = urlTestResult as? ApiKeyTestResult.Failure) {
                            null -> null
                            else -> when (failure.error) {
                                ApiKeyTestError.InvalidUrlPattern ->
                                    stringResource(Res.string.settings_custom_poster_test_invalid_pattern)
                                ApiKeyTestError.NetworkError ->
                                    stringResource(Res.string.settings_custom_poster_test_failed)
                                else -> null
                            }
                        }
                        ApiKeyTestResultBanner(
                            result = urlTestResult,
                            successOverrideText = stringResource(Res.string.settings_custom_poster_test_success),
                            failureOverrideText = failureOverride,
                        )
                    }
                    if (isActive) {
                        HorizontalDivider(
                            thickness = tokens.borders.hairline,
                            color = tokens.colors.borderSubtle,
                        )
                        Text(
                            text = stringResource(Res.string.settings_custom_poster_apply_to),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val enabledScreens by CustomPosterUrlRepository.enabledScreens.collectAsState()
                        val screenEntries = listOf(
                            CustomPosterScreen.HOME to stringResource(Res.string.settings_custom_poster_screen_home),
                            CustomPosterScreen.CONTINUE_WATCHING to stringResource(Res.string.settings_custom_poster_screen_continue_watching),
                            CustomPosterScreen.COLLECTIONS to stringResource(Res.string.settings_custom_poster_screen_collections),
                            CustomPosterScreen.LIBRARY to stringResource(Res.string.settings_custom_poster_screen_library),
                            CustomPosterScreen.SEARCH to stringResource(Res.string.settings_custom_poster_screen_search),
                            CustomPosterScreen.DETAILS to stringResource(Res.string.settings_custom_poster_screen_details),
                        )
                        screenEntries.forEachIndexed { index, (screen, label) ->
                            if (index > 0) {
                                HorizontalDivider(
                                    thickness = tokens.borders.hairline,
                                    color = tokens.colors.borderSubtle,
                                )
                            }
                            PosterToggleRow(
                                title = label,
                                checked = screen in enabledScreens,
                                onCheckedChange = { enabled ->
                                    CustomPosterUrlRepository.setScreenEnabled(screen, enabled)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    item {
        CardDepthStyleRepository.ensureLoaded()
        val cardDepthState by CardDepthStyleRepository.uiState.collectAsState()
        SettingsSection(
            title = stringResource(Res.string.settings_card_depth_title),
            isTablet = isTablet,
            actions = {
                NuvioActionLabel(
                    text = stringResource(Res.string.action_reset),
                    onClick = CardDepthStyleRepository::resetToDefaults,
                )
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                CardDepthStyleControls(
                    isTablet = isTablet,
                    uiState = cardDepthState,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDepthStyleControls(
    isTablet: Boolean,
    uiState: CardDepthStyleUiState,
) {
    var showFineTune by remember { mutableStateOf(false) }
    val edgeOptions = listOf(
        PresetOption(stringResource(Res.string.settings_card_depth_edge_subtle), 28),
        PresetOption(stringResource(Res.string.settings_card_depth_edge_balanced), 42),
        PresetOption(stringResource(Res.string.settings_card_depth_edge_bold), 56),
    )
    val sheenOptions = listOf(
        PresetOption(stringResource(Res.string.settings_card_depth_sheen_off), 0),
        PresetOption(stringResource(Res.string.settings_card_depth_sheen_soft), 10),
        PresetOption(stringResource(Res.string.settings_card_depth_sheen_bright), 16),
    )
    val coverageOptions = listOf(
        PresetOption(stringResource(Res.string.settings_card_depth_coverage_top), 0),
        PresetOption(stringResource(Res.string.settings_card_depth_coverage_half), 50),
        PresetOption(stringResource(Res.string.settings_card_depth_coverage_full), 100),
    )
    val surfaceRows = listOf(
        stringResource(Res.string.settings_card_depth_surface_posters) to NuvioCardDepthSurface.Posters,
        stringResource(Res.string.settings_card_depth_surface_continue_watching) to NuvioCardDepthSurface.ContinueWatching,
        stringResource(Res.string.settings_card_depth_surface_episodes) to NuvioCardDepthSurface.EpisodeCards,
        stringResource(Res.string.settings_card_depth_surface_cast) to NuvioCardDepthSurface.Cast,
        stringResource(Res.string.settings_card_depth_surface_trailers) to NuvioCardDepthSurface.Trailers,
    )

    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16,
                vertical = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
    ) {
        Text(
            text = stringResource(Res.string.settings_card_depth_description),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
        PosterToggleRow(
            title = stringResource(Res.string.settings_card_depth_enabled),
            checked = uiState.enabled,
            onCheckedChange = CardDepthStyleRepository::setEnabled,
        )
        if (uiState.enabled) {
            PosterStyleOptionRow(
                title = stringResource(Res.string.settings_card_depth_edge),
                selectedValue = uiState.edgeStrength,
                options = edgeOptions,
                onSelected = CardDepthStyleRepository::setEdgeStrength,
            )
            PosterStyleOptionRow(
                title = stringResource(Res.string.settings_card_depth_sheen),
                selectedValue = uiState.sheenStrength,
                options = sheenOptions,
                onSelected = CardDepthStyleRepository::setSheenStrength,
            )
            PosterStyleOptionRow(
                title = stringResource(Res.string.settings_card_depth_edge_coverage),
                selectedValue = uiState.edgeCoverage,
                options = coverageOptions,
                onSelected = CardDepthStyleRepository::setEdgeCoverage,
            )
            val fineTuneInteraction = remember { MutableInteractionSource() }
            val isFineTuneHovered by fineTuneInteraction.collectIsHoveredAsState()
            val fineTuneShape = RoundedCornerShape(NuvioTokens.Radius.lg)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(fineTuneShape)
                    .then(
                        if (isDesktop) {
                            Modifier
                                .hoverable(fineTuneInteraction)
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .nuvioFocusBorder(fineTuneShape)
                    .clickable(
                        interactionSource = fineTuneInteraction,
                        indication = null,
                    ) { showFineTune = true },
                shape = fineTuneShape,
                color = if (isDesktop && isFineTuneHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
                border = BorderStroke(
                    tokens.borders.thin,
                    if (isDesktop && isFineTuneHovered) tokens.colors.borderStrong else tokens.colors.borderSubtle,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.settings_card_depth_fine_tune),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = tokens.colors.accent,
                    )
                }
            }
            Text(
                text = stringResource(Res.string.settings_card_depth_apply_to),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            surfaceRows.forEach { (title, surface) ->
                PosterToggleRow(
                    title = title,
                    checked = uiState.isSurfaceEnabled(surface),
                    onCheckedChange = { enabled ->
                        CardDepthStyleRepository.setSurfaceEnabled(surface, enabled)
                    },
                )
            }
        }
    }

    if (showFineTune) {
        CardDepthFineTuneSheet(
            initialEdgeStrength = uiState.edgeStrength,
            initialSheenStrength = uiState.sheenStrength,
            initialEdgeCoverage = uiState.edgeCoverage,
            onDismiss = { showFineTune = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardDepthFineTuneSheet(
    initialEdgeStrength: Int,
    initialSheenStrength: Int,
    initialEdgeCoverage: Int,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftEdge by remember { mutableFloatStateOf(initialEdgeStrength.toFloat()) }
    var draftSheen by remember { mutableFloatStateOf(initialSheenStrength.toFloat()) }
    var draftCoverage by remember { mutableFloatStateOf(initialEdgeCoverage.toFloat()) }

    fun commitDraft() {
        CardDepthStyleRepository.setEdgeStrength(draftEdge.roundToInt())
        CardDepthStyleRepository.setSheenStrength(draftSheen.roundToInt())
        CardDepthStyleRepository.setEdgeCoverage(draftCoverage.roundToInt())
    }

    NuvioModalBottomSheet(
        onDismissRequest = {
            commitDraft()
            onDismiss()
        },
        sheetState = sheetState,
        fullHeight = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.settings_card_depth_fine_tune_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                NuvioActionLabel(
                    text = stringResource(Res.string.action_reset),
                    onClick = {
                        draftEdge = DefaultCardDepthEdgeStrength.toFloat()
                        draftSheen = DefaultCardDepthSheenStrength.toFloat()
                        draftCoverage = DefaultCardDepthEdgeCoverage.toFloat()
                        commitDraft()
                    },
                )
            }
            Text(
                text = stringResource(Res.string.settings_card_depth_fine_tune_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            CardDepthPreviewCard(
                edgeStrength = draftEdge,
                sheenStrength = draftSheen,
                edgeCoverage = draftCoverage,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${stringResource(Res.string.settings_card_depth_edge_value)}: ${formatDepthPercentage(draftEdge)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.textPrimary,
                )
                Text(
                    text = "${stringResource(Res.string.settings_card_depth_sheen_value)}: ${formatDepthPercentage(draftSheen)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.colors.textPrimary,
                )
            }
            CardDepthTuningPad(
                edgeStrength = draftEdge,
                sheenStrength = draftSheen,
                onChange = { edge, sheen ->
                    draftEdge = edge
                    draftSheen = sheen
                },
                onCommit = { edge, sheen ->
                    CardDepthStyleRepository.setEdgeStrength(edge.roundToInt())
                    CardDepthStyleRepository.setSheenStrength(sheen.roundToInt())
                },
            )
            Text(
                text = "${stringResource(Res.string.settings_card_depth_coverage_value)}: ${formatDepthPercentage(draftCoverage)}",
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.textPrimary,
            )
            Slider(
                value = draftCoverage,
                onValueChange = { draftCoverage = it },
                onValueChangeFinished = {
                    CardDepthStyleRepository.setEdgeCoverage(draftCoverage.roundToInt())
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun formatDepthPercentage(value: Float): String {
    val tenths = (value.coerceIn(0f, 100f) * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun CardDepthPreviewCard(
    edgeStrength: Float,
    sheenStrength: Float,
    edgeCoverage: Float,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.lg + NuvioTokens.Space.s2)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tokens.colors.surfaceCard,
                        tokens.colors.surfaceElevated,
                        tokens.colors.background,
                    ),
                ),
            )
            .border(tokens.borders.thin, tokens.colors.borderSubtle, shape)
            .cardDepthVisual(
                shape = shape,
                edgeStrength = edgeStrength,
                sheenStrength = sheenStrength,
                edgeCoverage = edgeCoverage,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to tokens.colors.accent.copy(alpha = 0.12f),
                        0.42f to Color.Transparent,
                        0.68f to Color.Black.copy(alpha = 0.42f),
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(NuvioTokens.Space.s14),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            Text(
                text = stringResource(Res.string.settings_card_depth_preview_meta),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textSecondary,
            )
            Text(
                text = stringResource(Res.string.settings_card_depth_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun CardDepthTuningPad(
    edgeStrength: Float,
    sheenStrength: Float,
    onChange: (Float, Float) -> Unit,
    onCommit: (Float, Float) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val maxEdge = 70f
    val maxSheen = 25f
    val shape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val thumbColor = tokens.colors.accent
    val gridColor = tokens.colors.borderSubtle
    val currentEdge by rememberUpdatedState(edgeStrength)
    val currentSheen by rememberUpdatedState(sheenStrength)
    val currentOnChange by rememberUpdatedState(onChange)
    val currentOnCommit by rememberUpdatedState(onCommit)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(tokens.colors.surfaceElevated)
            .border(
                width = tokens.borders.thin,
                color = tokens.colors.borderDefault,
                shape = shape,
            )
            .pointerInput(Unit) {
                fun valuesAt(position: Offset): Pair<Float, Float> {
                    val x = (position.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (position.y / size.height).coerceIn(0f, 1f)
                    return (x * maxEdge) to (y * maxSheen)
                }
                detectDragGestures(
                    onDragStart = { position ->
                        val (edge, sheen) = valuesAt(position)
                        currentOnChange(edge, sheen)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val (edge, sheen) = valuesAt(change.position)
                        currentOnChange(edge, sheen)
                    },
                    onDragEnd = {
                        currentOnCommit(currentEdge, currentSheen)
                    },
                    onDragCancel = {
                        currentOnCommit(currentEdge, currentSheen)
                    },
                )
            }
            .pointerInput(Unit) {
                fun valuesAt(position: Offset): Pair<Float, Float> {
                    val x = (position.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (position.y / size.height).coerceIn(0f, 1f)
                    return (x * maxEdge) to (y * maxSheen)
                }
                detectTapGestures { position ->
                    val (edge, sheen) = valuesAt(position)
                    currentOnChange(edge, sheen)
                    currentOnCommit(edge, sheen)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (step in 1..3) {
                val x = size.width * step / 4f
                val y = size.height * step / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            val thumbX = size.width * (edgeStrength / maxEdge).coerceIn(0f, 1f)
            val thumbY = size.height * (1f - (sheenStrength / maxSheen).coerceIn(0f, 1f))
            drawLine(
                color = thumbColor.copy(alpha = 0.35f),
                start = Offset(thumbX, 0f),
                end = Offset(thumbX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = thumbColor.copy(alpha = 0.35f),
                start = Offset(0f, thumbY),
                end = Offset(size.width, thumbY),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = thumbColor.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = Offset(thumbX, thumbY),
            )
            drawCircle(
                color = thumbColor,
                radius = 9.dp.toPx(),
                center = Offset(thumbX, thumbY),
            )
            drawCircle(
                color = tokens.colors.onAccent,
                radius = 4.dp.toPx(),
                center = Offset(thumbX, thumbY),
            )
        }
        Text(
            text = stringResource(Res.string.settings_card_depth_pad_sheen_axis),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        )
        Text(
            text = stringResource(Res.string.settings_card_depth_pad_edge_axis),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterCardStyleControls(
    isTablet: Boolean,
    widthDp: Int,
    cornerRadiusDp: Int,
    catalogLandscapeModeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
    alwaysShowLandscapeClearlogo: Boolean,
    animatedArtworkMode: AnimatedArtworkMode,
    onWidthSelected: (Int) -> Unit,
    onCornerRadiusSelected: (Int) -> Unit,
    onCatalogLandscapeModeChange: (Boolean) -> Unit,
    onHideLabelsChange: (Boolean) -> Unit,
    onAlwaysShowLandscapeClearlogoChange: (Boolean) -> Unit,
    onAnimatedArtworkModeSelected: (AnimatedArtworkMode) -> Unit,
) {
    val animatedArtworkOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_animated_artwork_always), AnimatedArtworkMode.Always.ordinal),
        PresetOption(stringResource(Res.string.settings_poster_animated_artwork_on_hover), AnimatedArtworkMode.OnHover.ordinal),
        PresetOption(stringResource(Res.string.settings_poster_animated_artwork_off), AnimatedArtworkMode.Off.ordinal),
    )
    val widthOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_width_compact), 104),
        PresetOption(stringResource(Res.string.settings_poster_width_dense), 112),
        PresetOption(stringResource(Res.string.settings_poster_width_standard), 120),
        PresetOption(stringResource(Res.string.settings_poster_width_balanced), 126),
        PresetOption(stringResource(Res.string.settings_poster_width_comfort), 134),
        PresetOption(stringResource(Res.string.settings_poster_width_large), 140),
    )
    val radiusOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_radius_sharp), 0),
        PresetOption(stringResource(Res.string.settings_poster_radius_subtle), 4),
        PresetOption(stringResource(Res.string.settings_poster_radius_classic), 8),
        PresetOption(stringResource(Res.string.settings_poster_radius_rounded), 12),
        PresetOption(stringResource(Res.string.settings_poster_radius_pill), 16),
    )

    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16,
                vertical = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
    ) {
        Text(
            text = stringResource(Res.string.settings_poster_description),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
        PosterCardLivePreview(
            widthDp = widthDp,
            cornerRadiusDp = cornerRadiusDp,
        )
        PosterStyleOptionRow(
            title = stringResource(Res.string.settings_poster_card_width),
            selectedValue = widthDp,
            options = widthOptions,
            onSelected = onWidthSelected,
        )
        PosterStyleOptionRow(
            title = stringResource(Res.string.settings_poster_card_radius),
            selectedValue = cornerRadiusDp,
            options = radiusOptions,
            onSelected = onCornerRadiusSelected,
        )
        PosterLandscapeModeToggleRow(
            checked = catalogLandscapeModeEnabled,
            onCheckedChange = onCatalogLandscapeModeChange,
        )
        PosterToggleRow(
            title = stringResource(Res.string.settings_poster_hide_labels),
            checked = hideLabelsEnabled,
            onCheckedChange = onHideLabelsChange,
        )
        PosterToggleRow(
            title = stringResource(Res.string.settings_poster_always_show_landscape_clearlogo),
            checked = alwaysShowLandscapeClearlogo,
            onCheckedChange = onAlwaysShowLandscapeClearlogoChange,
        )
        PosterStyleOptionRow(
            title = stringResource(Res.string.settings_poster_animated_artwork),
            selectedValue = animatedArtworkMode.ordinal,
            options = animatedArtworkOptions,
            onSelected = { ordinal ->
                onAnimatedArtworkModeSelected(AnimatedArtworkMode.entries[ordinal])
            },
        )
    }
}

@Composable
private fun PosterLandscapeModeToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    PosterToggleRow(
        title = stringResource(Res.string.settings_poster_landscape_mode),
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun PosterToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowShape = RoundedCornerShape(NuvioTokens.Radius.md)
    val rowBackground by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.overlayHover else Color.Transparent,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "posterToggleBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(rowBackground)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(vertical = NuvioTokens.Space.s4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f).padding(end = NuvioTokens.Space.s16),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.colors.onAccent,
                checkedTrackColor = tokens.colors.accent,
                uncheckedThumbColor = tokens.colors.textSecondary,
                uncheckedTrackColor = tokens.colors.surfaceElevated,
                uncheckedBorderColor = tokens.colors.borderDefault,
            ),
        )
    }
}

@Composable
private fun PosterCardLivePreview(
    widthDp: Int,
    cornerRadiusDp: Int,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val targetHeightDp = (widthDp * 3) / 2
    val previewFrameWidthDp = 140
    val previewFrameHeightDp = 210
    val animatedWidth = animateDpAsState(
        targetValue = widthDp.dp,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewWidth",
    )
    val animatedHeight = animateDpAsState(
        targetValue = targetHeightDp.dp,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewHeight",
    )
    val animatedCornerRadius = animateDpAsState(
        targetValue = cornerRadiusDp.dp,
        animationSpec = tween(durationMillis = 220),
        label = "posterPreviewCornerRadius",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surfaceElevated,
        shape = RoundedCornerShape(NuvioTokens.Radius.xl),
        border = BorderStroke(tokens.borders.thin, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NuvioTokens.Space.s16),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            Text(
                text = stringResource(Res.string.settings_poster_live_preview),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(previewFrameWidthDp.dp)
                        .height(previewFrameHeightDp.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val posterShape = RoundedCornerShape(animatedCornerRadius.value)
                    Box(
                        modifier = Modifier
                            .width(animatedWidth.value)
                            .height(animatedHeight.value)
                            .clip(posterShape)
                            .background(tokens.colors.surfaceCard)
                            .border(
                                width = NuvioTokens.Border.medium,
                                color = tokens.colors.accent.copy(alpha = 0.75f),
                                shape = posterShape,
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(NuvioTokens.Space.s4)
                                .align(Alignment.TopCenter)
                                .background(palette.accentBrush()),
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(NuvioTokens.Space.s10),
                            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(NuvioTokens.Space.s6)
                                    .clip(CircleShape)
                                    .background(tokens.colors.textPrimary.copy(alpha = 0.35f)),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(NuvioTokens.Space.s4)
                                    .clip(CircleShape)
                                    .background(tokens.colors.textMuted.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_poster_preview_width, widthDp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(Res.string.settings_poster_preview_corner_radius, cornerRadiusDp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(Res.string.settings_poster_preview_height, targetHeightDp),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosterStyleOptionRow(
    title: String,
    selectedValue: Int,
    options: List<PresetOption>,
    onSelected: (Int) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label
        ?: stringResource(Res.string.settings_poster_custom)

    Column(
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
    ) {
        Text(
            text = stringResource(Res.string.settings_poster_option_with_value, title, selectedLabel),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            options.forEach { option ->
                Chip(
                    label = option.label,
                    selected = option.value == selectedValue,
                    onClick = { onSelected(option.value) },
                )
            }
        }
    }
}

private data class PresetOption(
    val label: String,
    val value: Int,
)

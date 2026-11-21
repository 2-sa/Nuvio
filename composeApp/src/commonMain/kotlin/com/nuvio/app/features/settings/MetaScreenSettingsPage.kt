package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.core.ui.NuvioActionLabel
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.features.details.MetaEpisodeCardStyle
import com.nuvio.app.features.details.MetaScreenBackgroundMode
import com.nuvio.app.features.details.MetaScreenSectionItem
import com.nuvio.app.features.details.MetaScreenSectionKey
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.details.MetaScreenSettingsUiState
import com.nuvio.app.features.details.desktopHeroOwnedMetaSectionKeys
import com.nuvio.app.features.shuffle.EpisodeShuffleRepository
import com.nuvio.app.isDesktop
import com.nuvio.app.supportsPosterNavigationMotion
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_reorder
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.layout_random_episode_sub
import nuvio.composeapp.generated.resources.random_episode_title
import nuvio.composeapp.generated.resources.settings_homescreen_hidden
import nuvio.composeapp.generated.resources.settings_homescreen_visible
import nuvio.composeapp.generated.resources.settings_meta_actions
import nuvio.composeapp.generated.resources.settings_meta_actions_description
import nuvio.composeapp.generated.resources.settings_meta_cast
import nuvio.composeapp.generated.resources.settings_meta_cast_description
import nuvio.composeapp.generated.resources.settings_meta_collection
import nuvio.composeapp.generated.resources.settings_meta_collection_description
import nuvio.composeapp.generated.resources.settings_meta_comments
import nuvio.composeapp.generated.resources.settings_meta_comments_description
import nuvio.composeapp.generated.resources.settings_meta_details
import nuvio.composeapp.generated.resources.settings_meta_details_description
import nuvio.composeapp.generated.resources.settings_meta_hero_trailer_playback
import nuvio.composeapp.generated.resources.settings_meta_hero_trailer_playback_description
import nuvio.composeapp.generated.resources.settings_meta_episode_cards
import nuvio.composeapp.generated.resources.settings_meta_episode_cards_description
import nuvio.composeapp.generated.resources.settings_meta_episode_style_horizontal
import nuvio.composeapp.generated.resources.settings_meta_episode_style_horizontal_description
import nuvio.composeapp.generated.resources.settings_meta_episode_style_list
import nuvio.composeapp.generated.resources.settings_meta_episode_style_list_description
import nuvio.composeapp.generated.resources.settings_meta_episodes
import nuvio.composeapp.generated.resources.settings_meta_episodes_description
import nuvio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes
import nuvio.composeapp.generated.resources.settings_meta_blur_unwatched_episodes_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode
import nuvio.composeapp.generated.resources.settings_meta_background_mode_cinematic
import nuvio.composeapp.generated.resources.settings_meta_background_mode_cinematic_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_dominant
import nuvio.composeapp.generated.resources.settings_meta_background_mode_dominant_description
import nuvio.composeapp.generated.resources.settings_meta_background_mode_normal
import nuvio.composeapp.generated.resources.settings_meta_background_mode_normal_description
import nuvio.composeapp.generated.resources.settings_meta_group_label
import nuvio.composeapp.generated.resources.settings_meta_more_like_this
import nuvio.composeapp.generated.resources.settings_meta_more_like_this_description
import nuvio.composeapp.generated.resources.settings_meta_none
import nuvio.composeapp.generated.resources.settings_meta_overview
import nuvio.composeapp.generated.resources.settings_meta_overview_description
import nuvio.composeapp.generated.resources.settings_meta_production
import nuvio.composeapp.generated.resources.settings_meta_production_description
import nuvio.composeapp.generated.resources.settings_meta_poster_transition
import nuvio.composeapp.generated.resources.settings_meta_poster_transition_description
import nuvio.composeapp.generated.resources.settings_meta_section_appearance
import nuvio.composeapp.generated.resources.settings_meta_section_sections
import nuvio.composeapp.generated.resources.settings_meta_tab_group_format
import nuvio.composeapp.generated.resources.settings_meta_tab_layout
import nuvio.composeapp.generated.resources.settings_meta_tab_layout_description
import nuvio.composeapp.generated.resources.settings_meta_trailers
import nuvio.composeapp.generated.resources.settings_meta_trailers_description
import nuvio.composeapp.generated.resources.shuffle_save_failed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal fun LazyListScope.metaScreenSettingsContent(
    isTablet: Boolean,
    uiState: MetaScreenSettingsUiState,
) {
    val showHeroTrailerPlaybackSetting = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_meta_section_appearance),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsChipRow(
                    title = stringResource(Res.string.settings_meta_background_mode),
                    description = stringResource(Res.string.settings_meta_background_mode_description),
                    isTablet = isTablet,
                    footer = stringResource(uiState.backgroundMode.descriptionRes),
                ) {
                    MetaScreenBackgroundMode.entries.forEach { mode ->
                        Chip(
                            label = stringResource(mode.labelRes),
                            selected = uiState.backgroundMode == mode,
                            onClick = { MetaScreenSettingsRepository.setBackgroundMode(mode) },
                        )
                    }
                }
                if (supportsPosterNavigationMotion) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_poster_transition),
                        description = stringResource(Res.string.settings_meta_poster_transition_description),
                        checked = uiState.posterTransitionEnabled,
                        isTablet = isTablet,
                        onCheckedChange = MetaScreenSettingsRepository::setPosterTransitionEnabled,
                    )
                }
                if (showHeroTrailerPlaybackSetting) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_playback),
                        description = stringResource(Res.string.settings_meta_hero_trailer_playback_description),
                        checked = uiState.heroTrailerPlayback,
                        isTablet = isTablet,
                        onCheckedChange = { MetaScreenSettingsRepository.setHeroTrailerPlayback(it) },
                    )
                }
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_tab_layout),
                    description = stringResource(Res.string.settings_meta_tab_layout_description),
                    checked = uiState.tabLayout,
                    isTablet = isTablet,
                    onCheckedChange = { MetaScreenSettingsRepository.setTabLayout(it) },
                )
                SettingsGroupDivider(isTablet = isTablet)
                RatingsSettings(isTablet = isTablet, uiState = uiState)
                SettingsGroupDivider(isTablet = isTablet)
                val shuffleProfile by remember {
                    EpisodeShuffleRepository.ensureLoaded()
                    EpisodeShuffleRepository.uiState
                }.collectAsStateWithLifecycle()
                val shuffleSaveFailed = stringResource(Res.string.shuffle_save_failed)
                SettingsSwitchRow(
                    title = stringResource(Res.string.random_episode_title),
                    description = stringResource(Res.string.layout_random_episode_sub),
                    checked = shuffleProfile.available,
                    isTablet = isTablet,
                    onCheckedChange = {
                        if (!EpisodeShuffleRepository.setAvailable(it)) NuvioToastController.show(shuffleSaveFailed)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                MetaEpisodeCardStyleSelector(
                    isTablet = isTablet,
                    selectedStyle = uiState.episodeCardStyle,
                    onStyleSelected = MetaScreenSettingsRepository::setEpisodeCardStyle,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_blur_unwatched_episodes),
                    description = stringResource(Res.string.settings_meta_blur_unwatched_episodes_description),
                    checked = uiState.blurUnwatchedEpisodes,
                    isTablet = isTablet,
                    onCheckedChange = { MetaScreenSettingsRepository.setBlurUnwatchedEpisodes(it) },
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_meta_section_sections),
            isTablet = isTablet,
            actions = {
                NuvioActionLabel(
                    text = stringResource(Res.string.action_reset),
                    onClick = MetaScreenSettingsRepository::resetToDefaults,
                )
            },
        ) {
            SettingsGroup(isTablet = isTablet) {
                val sectionItems = uiState.items.visibleMetaSectionSettingsItems()
                MetaSectionReorderableList(
                    items = sectionItems,
                    isTablet = isTablet,
                    tabLayout = uiState.tabLayout,
                )
            }
        }
    }
}

@Composable
private fun MetaSectionReorderableList(
    items: List<MetaScreenSectionItem>,
    isTablet: Boolean,
    tabLayout: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        val fromKey = items.getOrNull(from.index)?.key
        val toKey = items.getOrNull(to.index)?.key
        if (fromKey != null && toKey != null) {
            MetaScreenSettingsRepository.moveByKey(fromKey, toKey)
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Count members per group for enforcing max 3
    val groupCounts: Map<Int, Int> = if (tabLayout) {
        items.filter { it.tabGroup != null }.groupBy { it.tabGroup!! }.mapValues { it.value.size }
    } else {
        emptyMap()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (isTablet) 820.dp else 640.dp),
        state = lazyListState,
    ) {
        itemsIndexed(items, key = { _, item -> item.key.name }) { index, item ->
            ReorderableItem(reorderableLazyListState, key = item.key.name) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                Surface(
                    shadowElevation = elevation,
                    color = if (isDragging) tokens.colors.surfaceElevated else tokens.colors.surfaceCard,
                ) {
                    Column {
                        if (index > 0) {
                            SettingsGroupDivider(isTablet = isTablet)
                        }
                        MetaSectionRow(
                            item = item,
                            isTablet = isTablet,
                            tabLayout = tabLayout,
                            groupCounts = groupCounts,
                            onEnabledChange = { MetaScreenSettingsRepository.setEnabled(item.key, it) },
                            onTabGroupChange = { MetaScreenSettingsRepository.setTabGroup(item.key, it) },
                            dragHandleScope = this@ReorderableItem,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaSectionRow(
    item: MetaScreenSectionItem,
    isTablet: Boolean,
    tabLayout: Boolean,
    groupCounts: Map<Int, Int>,
    onEnabledChange: (Boolean) -> Unit,
    onTabGroupChange: (Int?) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16
    val verticalPadding = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = NuvioTokens.Space.s12),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
            ) {
                Text(
                    text = stringResource(item.key.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(item.key.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (item.enabled) {
                            stringResource(Res.string.settings_homescreen_visible)
                        } else {
                            stringResource(Res.string.settings_homescreen_hidden)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                    if (tabLayout && item.tabGroup != null) {
                        Box(
                            modifier = Modifier
                                .size(NuvioTokens.Space.s4)
                                .clip(RoundedCornerShape(NuvioTokens.Space.s2))
                                .background(tokens.colors.textMuted.copy(alpha = 0.4f)),
                        )
                        Text(
                            text = stringResource(Res.string.settings_meta_tab_group_format, item.tabGroup ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.accent,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = tokens.colors.textSecondary,
                    uncheckedTrackColor = tokens.colors.surfaceElevated,
                    uncheckedBorderColor = tokens.colors.borderDefault,
                ),
            )
            IconButton(
                modifier = with(dragHandleScope) {
                    Modifier
                        .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .draggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        )
                },
                onClick = {},
            ) {
                Icon(
                    Icons.Rounded.Menu,
                    contentDescription = stringResource(Res.string.action_reorder),
                    tint = tokens.colors.textMuted,
                )
            }
        }

        
        AnimatedVisibility(
            visible = tabLayout && item.enabled && item.key.canBeTabbed,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    label = stringResource(Res.string.settings_meta_none),
                    selected = item.tabGroup == null,
                    onClick = { onTabGroupChange(null) },
                )
                for (groupId in 1..3) {
                    val currentCount = groupCounts[groupId] ?: 0
                    val isSelected = item.tabGroup == groupId
                    val isFull = currentCount >= 3 && !isSelected
                    Chip(
                        label = stringResource(Res.string.settings_meta_group_label, groupId),
                        selected = isSelected,
                        enabled = !isFull,
                        onClick = { onTabGroupChange(groupId) },
                    )
                }
            }
        }
    }
}

private fun List<MetaScreenSectionItem>.visibleMetaSectionSettingsItems(): List<MetaScreenSectionItem> =
    if (!isDesktop) {
        this
    } else {
        filterNot { it.key in desktopHeroOwnedMetaSectionKeys }
    }

private val MetaScreenBackgroundMode.labelRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic
        MetaScreenBackgroundMode.DominantColor -> Res.string.settings_meta_background_mode_dominant
    }

private val MetaScreenBackgroundMode.descriptionRes: StringResource
    get() = when (this) {
        MetaScreenBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal_description
        MetaScreenBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic_description
        MetaScreenBackgroundMode.DominantColor -> Res.string.settings_meta_background_mode_dominant_description
    }

@Composable
private fun MetaEpisodeCardStyleSelector(
    isTablet: Boolean,
    selectedStyle: MetaEpisodeCardStyle,
    onStyleSelected: (MetaEpisodeCardStyle) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16,
                vertical = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
    ) {
        Text(
            text = stringResource(Res.string.settings_meta_episode_cards),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.settings_meta_episode_cards_description),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            MetaEpisodeCardStyle.entries.forEach { style ->
                Box(modifier = Modifier.weight(1f)) {
                    MetaEpisodeCardStyleOption(
                        style = style,
                        selected = selectedStyle == style,
                        isTablet = isTablet,
                        onClick = { onStyleSelected(style) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaEpisodeCardStyleOption(
    style: MetaEpisodeCardStyle,
    selected: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val optionShape = RoundedCornerShape(NuvioTokens.Radius.lg + NuvioTokens.Space.s2)
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "episodeStyleBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            isDesktop && isHovered -> tokens.colors.surfaceElevated
            else -> tokens.colors.background
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "episodeStyleBg",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(optionShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(optionShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        color = containerColor,
        shape = optionShape,
        border = BorderStroke(if (selected) NuvioTokens.Border.medium else tokens.borders.thin, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                contentAlignment = Alignment.Center,
            ) {
                MetaEpisodeCardStylePreview(style = style)
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = tokens.colors.accent,
                        modifier = Modifier.align(Alignment.TopEnd).size(if (isTablet) 24.dp else 18.dp),
                    )
                }
            }
            Text(
                text = stringResource(style.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) tokens.colors.textPrimary else tokens.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(style.descriptionRes),
                style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium,
                color = tokens.colors.textMuted,
            )
        }
    }
}

private val MetaEpisodeCardStyle.labelRes: StringResource
    get() = when (this) {
        MetaEpisodeCardStyle.Horizontal -> Res.string.settings_meta_episode_style_horizontal
        MetaEpisodeCardStyle.List -> Res.string.settings_meta_episode_style_list
    }

private val MetaEpisodeCardStyle.descriptionRes: StringResource
    get() = when (this) {
        MetaEpisodeCardStyle.Horizontal -> Res.string.settings_meta_episode_style_horizontal_description
        MetaEpisodeCardStyle.List -> Res.string.settings_meta_episode_style_list_description
    }

private val MetaScreenSectionKey.titleRes: StringResource
    get() = when (this) {
        MetaScreenSectionKey.ACTIONS -> Res.string.settings_meta_actions
        MetaScreenSectionKey.OVERVIEW -> Res.string.settings_meta_overview
        MetaScreenSectionKey.PRODUCTION -> Res.string.settings_meta_production
        MetaScreenSectionKey.CAST -> Res.string.settings_meta_cast
        MetaScreenSectionKey.COMMENTS -> Res.string.settings_meta_comments
        MetaScreenSectionKey.TRAILERS -> Res.string.settings_meta_trailers
        MetaScreenSectionKey.EPISODES -> Res.string.settings_meta_episodes
        MetaScreenSectionKey.DETAILS -> Res.string.settings_meta_details
        MetaScreenSectionKey.COLLECTION -> Res.string.settings_meta_collection
        MetaScreenSectionKey.MORE_LIKE_THIS -> Res.string.settings_meta_more_like_this
    }

private val MetaScreenSectionKey.descriptionRes: StringResource
    get() = when (this) {
        MetaScreenSectionKey.ACTIONS -> Res.string.settings_meta_actions_description
        MetaScreenSectionKey.OVERVIEW -> Res.string.settings_meta_overview_description
        MetaScreenSectionKey.PRODUCTION -> Res.string.settings_meta_production_description
        MetaScreenSectionKey.CAST -> Res.string.settings_meta_cast_description
        MetaScreenSectionKey.COMMENTS -> Res.string.settings_meta_comments_description
        MetaScreenSectionKey.TRAILERS -> Res.string.settings_meta_trailers_description
        MetaScreenSectionKey.EPISODES -> Res.string.settings_meta_episodes_description
        MetaScreenSectionKey.DETAILS -> Res.string.settings_meta_details_description
        MetaScreenSectionKey.COLLECTION -> Res.string.settings_meta_collection_description
        MetaScreenSectionKey.MORE_LIKE_THIS -> Res.string.settings_meta_more_like_this_description
    }

@Composable
private fun MetaEpisodeCardStylePreview(
    style: MetaEpisodeCardStyle,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(NuvioTokens.Radius.lg))
            .background(tokens.colors.surfaceElevated)
            .border(tokens.borders.thin, tokens.colors.borderSubtle, RoundedCornerShape(NuvioTokens.Radius.lg))
            .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (style) {
            MetaEpisodeCardStyle.Horizontal -> {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(NuvioTokens.Radius.md))
                        .background(tokens.colors.surfaceCard)
                        .border(tokens.borders.hairline, tokens.colors.borderDefault, RoundedCornerShape(NuvioTokens.Radius.md)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .align(Alignment.BottomCenter)
                            .background(tokens.colors.overlayScrim),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 6.dp, top = 6.dp)
                            .width(42.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tokens.colors.textPrimary.copy(alpha = 0.28f)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp)
                            .width(72.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(tokens.colors.textSecondary.copy(alpha = 0.24f)),
                    )
                }
            }

            MetaEpisodeCardStyle.List -> {
                Row(
                    modifier = Modifier
                        .width(132.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(NuvioTokens.Radius.md))
                        .background(tokens.colors.surfaceCard)
                        .border(tokens.borders.hairline, tokens.colors.borderDefault, RoundedCornerShape(NuvioTokens.Radius.md)),
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(78.dp)
                            .background(tokens.colors.borderDefault.copy(alpha = 0.7f)),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(tokens.colors.textPrimary.copy(alpha = 0.24f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(tokens.colors.textMuted.copy(alpha = 0.22f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(tokens.colors.accent.copy(alpha = 0.7f)),
                        )
                    }
                }
            }
        }
    }
}

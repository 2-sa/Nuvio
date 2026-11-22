package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioSectionLabel
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.home.HomeCatalogSettingsItem
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_search_clear
import nuvio.composeapp.generated.resources.settings_back_to_format
import nuvio.composeapp.generated.resources.settings_homescreen_collection_with_addon
import nuvio.composeapp.generated.resources.settings_homescreen_display_name
import nuvio.composeapp.generated.resources.settings_homescreen_hero_source
import nuvio.composeapp.generated.resources.settings_homescreen_hidden
import nuvio.composeapp.generated.resources.settings_homescreen_not_in_hero
import nuvio.composeapp.generated.resources.settings_homescreen_pinned
import nuvio.composeapp.generated.resources.settings_homescreen_pinned_to_top
import nuvio.composeapp.generated.resources.settings_homescreen_reorder
import nuvio.composeapp.generated.resources.settings_homescreen_visible
import nuvio.composeapp.generated.resources.settings_search_placeholder
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
private fun SettingsCard(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(NuvioTokens.Radius.xl)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surfaceCard,
        shape = cardShape,
        border = BorderStroke(
            tokens.borders.thin,
            tokens.colors.borderSubtle,
        ),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroup(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsCard(
        isTablet = isTablet,
        modifier = modifier,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsGroupDivider(isTablet: Boolean) {
    val tokens = MaterialTheme.nuvio
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16),
        thickness = tokens.borders.hairline,
        color = tokens.colors.borderSubtle,
    )
}

@Composable
internal fun TabletPageHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .nuvioConsumePointerEvents(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
        ) {
            if (showBack) {
                NuvioBackButton(
                    onClick = onBack,
                    modifier = Modifier.size(NuvioTokens.Space.s36),
                    shape = tokens.shapes.compactCard,
                    containerColor = tokens.colors.surfaceCard,
                    showContainerOnDesktop = true,
                    contentColor = tokens.colors.textPrimary,
                    buttonSize = NuvioTokens.Space.s36,
                    iconSize = tokens.icons.md,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun SettingsSidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val accent = tokens.colors.accent
    val background by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            isDesktop && isHovered -> tokens.colors.overlayHover
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "sidebarItemBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = 0.35f)
            isDesktop && isHovered -> tokens.colors.borderSubtle
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "sidebarItemBorder",
    )
    val iconChip = if (selected) accent.copy(alpha = tokens.opacity.selected) else Color.Transparent
    val contentColor = when {
        selected -> tokens.colors.textPrimary
        isDesktop && isHovered -> tokens.colors.textSecondary
        else -> tokens.colors.textMuted
    }

    val sidebarItemShape = RoundedCornerShape(NuvioTokens.Radius.lg)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.listGap, vertical = NuvioTokens.Space.s2)
            .clip(sidebarItemShape)
            .background(background, sidebarItemShape)
            .border(tokens.borders.thin, borderColor, sidebarItemShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(sidebarItemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                start = NuvioTokens.Space.s10,
                end = NuvioTokens.Space.s14,
                top = NuvioTokens.Space.s10,
                bottom = NuvioTokens.Space.s10,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(NuvioTokens.Space.s3)
                .height(NuvioTokens.Space.s20)
                .clip(RoundedCornerShape(NuvioTokens.Radius.full))
                .background(if (selected) accent else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s10))
        Surface(
            modifier = Modifier.size(tokens.icons.xl),
            color = iconChip,
            shape = RoundedCornerShape(NuvioTokens.Radius.md),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accent else contentColor,
                    modifier = Modifier.size(tokens.icons.md),
                )
            }
        }
        Spacer(modifier = Modifier.width(tokens.spacing.listGap))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    isTablet: Boolean,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s6),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NuvioSectionLabel(text = title)
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Spacer(modifier = Modifier.height(if (isTablet) tokens.spacing.listGap else NuvioTokens.Space.s10))
        content()
    }
}

@Composable
internal fun SettingsNavigationRow(
    title: String,
    description: String?,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    isTablet: Boolean,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val iconSize = if (isTablet) 40.dp else 36.dp
    val verticalPadding = if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14
    val horizontalPadding = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16
    val rowBackground by animateColorAsState(
        targetValue = if (isDesktop && enabled && isHovered) tokens.colors.overlayHover else Color.Transparent,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "navRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(
                if (isDesktop && enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(RoundedCornerShape(NuvioTokens.Space.s4))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = NuvioTokens.Space.s12)
                .widthIn(max = if (isTablet) 560.dp else Dp.Unspecified),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null || iconPainter != null) {
                Surface(
                    modifier = Modifier.size(iconSize),
                    color = if (isDesktop && enabled && isHovered) {
                        tokens.colors.focusBackground
                    } else {
                        tokens.colors.accent.copy(alpha = tokens.opacity.pressed)
                    },
                    shape = tokens.shapes.compactCard,
                    border = if (isDesktop && enabled && isHovered) {
                        BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.25f))
                    } else {
                        null
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (iconPainter != null) {
                            androidx.compose.foundation.Image(
                                painter = iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(if (isTablet) 26.dp else 24.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tokens.colors.accent,
                                modifier = Modifier.size(tokens.icons.md),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(NuvioTokens.Space.s2))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
        if (trailingContent != null) {
            trailingContent.invoke(this)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDesktop && enabled && isHovered) tokens.colors.accent else tokens.colors.textMuted,
                modifier = Modifier.size(tokens.icons.md),
            )
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    isTablet: Boolean,
    horizontalPadding: Dp = if (isTablet) 20.dp else 16.dp,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val verticalPadding = if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14
    val rowBackground by animateColorAsState(
        targetValue = if (isDesktop && enabled && isHovered) tokens.colors.overlayHover else Color.Transparent,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "switchRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(
                if (isDesktop && enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(RoundedCornerShape(NuvioTokens.Space.s4))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = NuvioTokens.Space.s12)
                .widthIn(max = if (isTablet) 560.dp else Dp.Unspecified)
                .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(start = NuvioTokens.Space.s4),
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.colors.onAccent,
                checkedTrackColor = tokens.colors.accent,
                uncheckedThumbColor = tokens.colors.textSecondary,
                uncheckedTrackColor = tokens.colors.surfaceElevated,
                uncheckedBorderColor = tokens.colors.borderDefault,
                disabledCheckedThumbColor = tokens.colors.onAccent.copy(alpha = tokens.opacity.disabled),
                disabledCheckedTrackColor = tokens.colors.accent.copy(alpha = tokens.opacity.disabled),
                disabledUncheckedThumbColor = tokens.colors.textDisabled,
                disabledUncheckedTrackColor = tokens.colors.surfaceElevated.copy(alpha = tokens.opacity.disabled),
                disabledUncheckedBorderColor = tokens.colors.borderSubtle,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsChipRow(
    title: String,
    description: String,
    isTablet: Boolean,
    footer: String? = null,
    chips: @Composable FlowRowScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = chips,
        )
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
internal fun HomescreenCatalogRow(
    item: HomeCatalogSettingsItem,
    isTablet: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
    onPinnedDragAttempt: () -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val horizontalPadding = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16
    val verticalPadding = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16
    val hapticFeedback = LocalHapticFeedback.current
    val rowBackground by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.overlayHover else Color.Transparent,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "homescreenRowBg",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .then(
                if (isDesktop && !item.isCollection) {
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
            ) { onExpandedChange(!expanded) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .then(if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isCollection) {
                        stringResource(Res.string.settings_homescreen_collection_with_addon, item.addonName)
                    } else {
                        item.addonName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
                Text(
                    text = buildString {
                        append(
                            if (item.enabled) {
                                stringResource(Res.string.settings_homescreen_visible)
                            } else {
                                stringResource(Res.string.settings_homescreen_hidden)
                            },
                        )
                        if (item.isCollection) {
                            if (item.isPinnedToTop) {
                                append(" • ")
                                append(stringResource(Res.string.settings_homescreen_pinned_to_top))
                            }
                        } else {
                            append(" • ")
                            append(
                                if (item.heroSourceEnabled) {
                                    stringResource(Res.string.settings_homescreen_hero_source)
                                } else {
                                    stringResource(Res.string.settings_homescreen_not_in_hero)
                                },
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                if (item.isPinnedToTop) {
                    IconButton(
                        onClick = onPinnedDragAttempt,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = stringResource(Res.string.settings_homescreen_pinned),
                            tint = tokens.colors.textMuted.copy(alpha = tokens.opacity.medium),
                        )
                    }
                } else {
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
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(Res.string.settings_homescreen_reorder),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded && !item.isCollection) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = item.customTitle,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(NuvioTokens.Radius.lg),
                    label = { Text(stringResource(Res.string.settings_homescreen_display_name)) },
                    placeholder = { Text(item.defaultTitle) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = tokens.colors.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.accent,
                        unfocusedBorderColor = tokens.colors.borderDefault,
                        focusedContainerColor = tokens.colors.surfaceElevated,
                        unfocusedContainerColor = tokens.colors.surfaceElevated,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        focusedLabelColor = tokens.colors.accent,
                        unfocusedLabelColor = tokens.colors.textMuted,
                        focusedPlaceholderColor = tokens.colors.textMuted,
                        unfocusedPlaceholderColor = tokens.colors.textMuted,
                        cursorColor = tokens.colors.accent,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun DesktopSettingsSidebarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val focusRequester = remember { FocusRequester() }
    val containerInteraction = remember { MutableInteractionSource() }
    val isHovered by containerInteraction.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    val pillShape = RoundedCornerShape(10.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> tokens.colors.accent
            isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "desktopSidebarSearchBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFocused) tokens.colors.accent else tokens.colors.textMuted,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "desktopSidebarSearchIconTint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(NuvioTokens.Space.s40)
            .clip(pillShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .hoverable(containerInteraction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = containerInteraction,
                indication = null,
            ) {
                focusRequester.requestFocus()
            }
            .padding(horizontal = NuvioTokens.Space.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(NuvioTokens.Icon.sm),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(Res.string.settings_search_placeholder),
                    style = MaterialTheme.nuvioTypeScale.bodySm.copy(fontSize = 13.5.sp),
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.nuvioTypeScale.bodySm.copy(
                    fontSize = 13.5.sp,
                    color = tokens.colors.textPrimary,
                    textDirection = TextDirection.Content,
                ),
                cursorBrush = SolidColor(tokens.colors.accent),
            )
        }
        if (query.isNotEmpty()) {
            val clearInteraction = remember { MutableInteractionSource() }
            val isClearHovered by clearInteraction.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isClearHovered) tokens.colors.surfaceElevated else tokens.colors.overlayHover)
                    .hoverable(clearInteraction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .nuvioFocusBorder(CircleShape)
                    .clickable(
                        interactionSource = clearInteraction,
                        indication = null,
                        role = Role.Button,
                        onClick = { onQueryChange("") },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.compose_search_clear),
                    tint = if (isClearHovered) tokens.colors.textPrimary else tokens.colors.textMuted,
                    modifier = Modifier.size(NuvioTokens.Icon.xs),
                )
            }
        }
    }
}

@Composable
internal fun DesktopSettingsSidebarSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = NuvioTokens.Space.s14,
                bottom = NuvioTokens.Space.s6,
                start = NuvioTokens.Space.s12,
                end = NuvioTokens.Space.s12,
            ),
        style = MaterialTheme.nuvioTypeScale.labelXs.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = tokens.colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun DesktopSettingsSidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val indicatorBrush = remember(palette) { palette.accentBrush() }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val itemShape = RoundedCornerShape(10.dp)
    val accent = tokens.colors.accent

    val background by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            isHovered -> tokens.colors.overlayHover
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "desktopSidebarItemBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> accent.copy(alpha = 0.35f)
            isHovered -> tokens.colors.borderSubtle
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "desktopSidebarItemBorder",
    )
    val contentColor = when {
        selected -> tokens.colors.textPrimary
        isHovered -> tokens.colors.textSecondary
        else -> tokens.colors.textMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(itemShape)
            .background(background, itemShape)
            .border(tokens.borders.thin, borderColor, itemShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .nuvioFocusBorder(itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = NuvioTokens.Space.s10),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(NuvioTokens.Space.s3)
                .height(NuvioTokens.Space.s18)
                .clip(RoundedCornerShape(NuvioTokens.Radius.full))
                .then(
                    if (selected) {
                        Modifier.background(indicatorBrush)
                    } else {
                        Modifier.background(Color.Transparent)
                    },
                ),
        )
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s10))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else contentColor,
            modifier = Modifier.size(NuvioTokens.Space.s18),
        )
        Spacer(modifier = Modifier.width(NuvioTokens.Space.s10))
        Text(
            text = label,
            style = MaterialTheme.nuvioTypeScale.bodySm.copy(fontSize = 13.5.sp),
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DesktopSettingsPageHeader(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    parentTitle: String? = null,
    onParentClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .nuvioConsumePointerEvents(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                if (parentTitle != null && onParentClick != null) {
                    val breadcrumbInteraction = remember { MutableInteractionSource() }
                    val isBreadcrumbHovered by breadcrumbInteraction.collectIsHoveredAsState()
                    val breadcrumbShape = RoundedCornerShape(NuvioTokens.Radius.full)
                    Row(
                        modifier = Modifier
                            .clip(breadcrumbShape)
                            .background(
                                if (isBreadcrumbHovered) {
                                    tokens.colors.surfaceCard
                                } else {
                                    tokens.colors.surfaceElevated
                                },
                            )
                            .border(
                                width = tokens.borders.thin,
                                color = if (isBreadcrumbHovered) {
                                    tokens.colors.accent.copy(alpha = 0.45f)
                                } else {
                                    tokens.colors.borderSubtle
                                },
                                shape = breadcrumbShape,
                            )
                            .hoverable(breadcrumbInteraction)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .nuvioFocusBorder(breadcrumbShape)
                            .clickable(
                                interactionSource = breadcrumbInteraction,
                                indication = null,
                                onClick = onParentClick,
                            )
                            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s5),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = if (isBreadcrumbHovered) tokens.colors.accent else tokens.colors.textSecondary,
                            modifier = Modifier.size(NuvioTokens.Space.s14),
                        )
                        Text(
                            text = stringResource(Res.string.settings_back_to_format, parentTitle),
                            style = MaterialTheme.nuvioTypeScale.labelSm,
                            color = if (isBreadcrumbHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = tokens.colors.focusBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = tokens.borders.thin,
                            color = tokens.colors.accent.copy(alpha = 0.30f),
                        ),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tokens.colors.accent,
                                modifier = Modifier.size(tokens.icons.md),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s3),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.nuvioTypeScale.titleMd.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = tokens.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = tokens.borders.hairline,
            color = tokens.colors.borderSubtle,
        )
    }
}

internal data class SettingsBentoItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String? = null,
    val iconPainter: Painter? = null,
    val onClick: () -> Unit,
)

@Composable
internal fun SettingsBentoGrid(
    items: List<SettingsBentoItem>,
    modifier: Modifier = Modifier,
    isTablet: Boolean = isDesktop,
) {
    if (items.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (isDesktop && maxWidth >= 640.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                    ) {
                        rowItems.forEach { item ->
                            SettingsBentoTile(
                                item = item,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            SettingsGroup(isTablet = isTablet) {
                items.forEachIndexed { index, item ->
                    if (index > 0) SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = item.title,
                        description = item.description,
                        icon = if (item.iconPainter == null) item.icon else null,
                        iconPainter = item.iconPainter,
                        isTablet = isTablet,
                        onClick = item.onClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsBentoTile(
    item: SettingsBentoItem,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tileShape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val containerColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.surfaceElevated
        } else {
            tokens.colors.surfaceCard
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "bentoTileBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent.copy(alpha = 0.65f)
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "bentoTileBorder",
    )
    val chevronTint by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent
        } else {
            tokens.colors.textMuted
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "bentoTileChevron",
    )

    Surface(
        modifier = modifier
            .clip(tileShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(tileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = item.onClick,
            ),
        color = containerColor,
        shape = tileShape,
        border = BorderStroke(tokens.borders.thin, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NuvioTokens.Space.s16),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    modifier = Modifier.size(NuvioTokens.Space.s40),
                    color = tokens.colors.focusBackground,
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(
                        width = tokens.borders.thin,
                        color = tokens.colors.accent.copy(alpha = 0.28f),
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.iconPainter != null) {
                            androidx.compose.foundation.Image(
                                painter = item.iconPainter,
                                contentDescription = null,
                                modifier = Modifier.size(NuvioTokens.Space.s24),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = tokens.colors.accent,
                                modifier = Modifier.size(tokens.icons.md),
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                ) {
                    if (!item.badge.isNullOrBlank()) {
                        Surface(
                            color = tokens.colors.surfaceElevated,
                            shape = RoundedCornerShape(NuvioTokens.Radius.full),
                            border = BorderStroke(tokens.borders.thin, tokens.colors.borderSubtle),
                        ) {
                            Text(
                                text = item.badge,
                                style = MaterialTheme.nuvioTypeScale.labelXs,
                                color = tokens.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(
                                    horizontal = NuvioTokens.Space.s8,
                                    vertical = NuvioTokens.Space.s3,
                                ),
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = chevronTint,
                        modifier = Modifier.size(tokens.icons.md),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                    color = tokens.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioLoadingIndicator
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
import nuvio.composeapp.generated.resources.settings_action_change
import nuvio.composeapp.generated.resources.settings_action_open
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
import nuvio.composeapp.generated.resources.settings_search_empty
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
    if (isDesktop) {
        Column(
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        SettingsCard(
            isTablet = isTablet,
            modifier = modifier,
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SettingsGroupDivider(
    isTablet: Boolean,
    fullWidth: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    HorizontalDivider(
        modifier = if (isDesktop) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.padding(horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16)
        },
        thickness = tokens.borders.hairline,
        color = if (isDesktop) Color.White.copy(alpha = 0.065f) else tokens.colors.borderSubtle,
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isDesktop) 0.dp else NuvioTokens.Space.s6),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isDesktop) {
                Text(
                    text = title,
                    style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tokens.colors.accent,
                )
            } else {
                NuvioSectionLabel(text = title)
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        Spacer(
            modifier = Modifier.height(
                when {
                    isDesktop -> NuvioTokens.Space.s8
                    isTablet -> tokens.spacing.listGap
                    else -> NuvioTokens.Space.s10
                },
            ),
        )
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
    actionLabel: String? = null,
    valueLabel: String? = null,
    showValueChevron: Boolean = true,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    if (isDesktop) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp)
                .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = NuvioTokens.Space.s24),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                if (iconPainter != null) {
                    androidx.compose.foundation.Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tokens.colors.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!description.isNullOrBlank() && (valueLabel == null || description != valueLabel)) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = tokens.colors.textMuted,
                        )
                    }
                }
            }
            if (trailingContent != null) {
                DesktopControlPill(
                    enabled = enabled,
                    onClick = onClick,
                    content = trailingContent,
                )
            } else if (valueLabel != null) {
                DesktopSelectChip(
                    label = valueLabel,
                    enabled = enabled,
                    showChevron = showValueChevron,
                    onClick = onClick,
                )
            } else {
                DesktopSelectChip(
                    label = actionLabel ?: stringResource(Res.string.settings_action_change),
                    enabled = enabled,
                    showChevron = actionLabel == null,
                    onClick = onClick,
                )
            }
        }
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val iconSize = if (isTablet) 40.dp else 36.dp
    val verticalPadding = if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14
    val horizontalPadding = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                .widthIn(max = if (isTablet && !isDesktop) 560.dp else Dp.Unspecified),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null || iconPainter != null) {
                Surface(
                    modifier = Modifier.size(iconSize),
                    color = tokens.colors.accent.copy(alpha = tokens.opacity.pressed),
                    shape = tokens.shapes.compactCard,
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
                tint = tokens.colors.textMuted,
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
    horizontalPadding: Dp = if (isDesktop) 0.dp else if (isTablet) 20.dp else 16.dp,
    constrainWidth: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    if (isDesktop) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = NuvioTokens.Space.s24)
                    .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                        color = tokens.colors.textMuted,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier = Modifier
                    .padding(start = NuvioTokens.Space.s4)
                    .scale(0.84f)
                    .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.colors.onAccent,
                    checkedTrackColor = tokens.colors.accent,
                    uncheckedThumbColor = Color(0xFFE2E8F0),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.18f),
                    disabledCheckedThumbColor = tokens.colors.onAccent.copy(alpha = tokens.opacity.disabled),
                    disabledCheckedTrackColor = tokens.colors.accent.copy(alpha = tokens.opacity.disabled),
                    disabledUncheckedThumbColor = tokens.colors.textDisabled,
                    disabledUncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                    disabledUncheckedBorderColor = Color.White.copy(alpha = 0.08f),
                ),
            )
        }
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val verticalPadding = if (isTablet) NuvioTokens.Space.s16 else NuvioTokens.Space.s14

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                .widthIn(max = if (isTablet && !isDesktop) 560.dp else Dp.Unspecified)
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
                horizontal = if (isDesktop) 0.dp else if (isTablet) 20.dp else 16.dp,
                vertical = if (isDesktop) 14.dp else if (isTablet) 16.dp else 14.dp,
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
    val horizontalPadding = if (isDesktop) 0.dp else if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16
    val verticalPadding = if (isDesktop) 13.dp else if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s16
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                    .then(
                        if (!item.isCollection) {
                            Modifier
                                .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) { onExpandedChange(!expanded) }
                        } else {
                            Modifier
                        },
                    ),
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
                    modifier = Modifier.then(if (isDesktop) Modifier.scale(0.84f) else Modifier),
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
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val tokens = MaterialTheme.nuvio
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
        } else if (!isFocused) {
            Surface(
                color = tokens.colors.surfaceElevated,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
            ) {
                Text(
                    text = "Ctrl+K",
                    style = MaterialTheme.nuvioTypeScale.labelXs.copy(fontSize = 10.sp),
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                top = NuvioTokens.Space.s10,
                bottom = NuvioTokens.Space.s4,
                start = NuvioTokens.Space.s10,
                end = NuvioTokens.Space.s10,
            ),
        style = MaterialTheme.nuvioTypeScale.labelXs.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = tokens.colors.textMuted.copy(alpha = 0.85f),
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
            selected -> accent.copy(alpha = 0.16f)
            isHovered -> Color.White.copy(alpha = 0.04f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "desktopSidebarItemBg",
    )
    val contentColor = when {
        selected -> tokens.colors.textPrimary
        isHovered -> tokens.colors.textPrimary
        else -> tokens.colors.textMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(itemShape)
            .background(background, itemShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
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
                .height(NuvioTokens.Space.s16)
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
            modifier = Modifier.size(17.dp),
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .nuvioConsumePointerEvents(),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
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
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color.White.copy(alpha = 0.04f)
                                },
                            )
                            .border(
                                width = tokens.borders.thin,
                                color = if (isBreadcrumbHovered) {
                                    tokens.colors.accent.copy(alpha = 0.55f)
                                } else {
                                    Color.White.copy(alpha = 0.10f)
                                },
                                shape = breadcrumbShape,
                            )
                            .hoverable(breadcrumbInteraction)
                            .pointerHoverIcon(PointerIcon.Hand)
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tokens.colors.accent,
                            modifier = Modifier.size(22.dp),
                        )
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
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = tokens.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = tokens.borders.hairline,
            color = Color.White.copy(alpha = 0.08f),
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
    if (isDesktop) {
        Column(modifier = modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    SettingsGroupDivider(isTablet = true)
                }
                SettingsNavigationRow(
                    title = item.title,
                    description = item.description,
                    icon = if (item.iconPainter == null) item.icon else null,
                    iconPainter = item.iconPainter,
                    isTablet = true,
                    actionLabel = item.badge ?: stringResource(Res.string.settings_action_open),
                    onClick = item.onClick,
                )
            }
        }
        return
    }
    SettingsGroup(isTablet = isTablet, modifier = modifier) {
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

internal enum class SettingsStatusTone {
    Success,
    Warning,
    Error,
    Neutral,
}

@Composable
internal fun SettingsStatusPill(
    label: String,
    tone: SettingsStatusTone,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    val tint = when (tone) {
        SettingsStatusTone.Success -> Color(0xFF34D399)
        SettingsStatusTone.Warning -> Color(0xFFFBBF24)
        SettingsStatusTone.Error -> Color(0xFFFB7185)
        SettingsStatusTone.Neutral -> tokens.colors.textSecondary
    }
    val container = when (tone) {
        SettingsStatusTone.Neutral -> Color.White.copy(alpha = 0.06f)
        else -> tint.copy(alpha = 0.14f)
    }
    val border = when (tone) {
        SettingsStatusTone.Neutral -> Color.White.copy(alpha = 0.12f)
        else -> tint.copy(alpha = 0.34f)
    }

    Surface(
        modifier = modifier,
        color = container,
        shape = RoundedCornerShape(NuvioTokens.Radius.full),
        border = BorderStroke(tokens.borders.thin, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.nuvioTypeScale.labelXs.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopSettingsTabBar(
    tabs: List<DesktopSettingsWorkspaceTab>,
    selectedTab: DesktopSettingsWorkspaceTab?,
    onTabSelected: (DesktopSettingsWorkspaceTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size <= 1) return
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val underlineBrush = remember(palette) { palette.accentBrush() }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == selectedTab
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val textColor = when {
                    isSelected -> tokens.colors.textPrimary
                    isHovered -> tokens.colors.textPrimary
                    else -> tokens.colors.textMuted
                }

                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) },
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(NuvioTokens.Radius.full))
                            .then(
                                if (isSelected) {
                                    Modifier.background(underlineBrush)
                                } else {
                                    Modifier.background(Color.Transparent)
                                },
                            ),
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = tokens.borders.hairline,
            color = Color.White.copy(alpha = 0.08f),
        )
    }
}

internal data class SettingsToggleGridItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val checked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
internal fun SettingsToggleGrid(
    items: List<SettingsToggleGridItem>,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    SettingsGroup(isTablet = isTablet, modifier = modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) SettingsGroupDivider(isTablet = isTablet)
            SettingsSwitchRow(
                title = item.title,
                description = item.description,
                checked = item.checked,
                enabled = item.enabled,
                isTablet = isTablet,
                horizontalPadding = if (isDesktop) 0.dp else if (isTablet) 20.dp else 16.dp,
                onCheckedChange = item.onCheckedChange,
            )
        }
    }
}

@Composable
internal fun DesktopSettingRow(
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    constrainWidth: Boolean = true,
    trailingControl: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
            .alpha(if (enabled) NuvioTokens.Opacity.visible else tokens.opacity.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = NuvioTokens.Space.s24),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    color = tokens.colors.textMuted,
                )
            }
        }
        trailingControl()
    }
}

internal data class DesktopSegmentedOption<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> DesktopSegmentedControl(
    options: List<DesktopSegmentedOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val activeBrush = remember(palette) { palette.accentBrush() }
    val trackShape = RoundedCornerShape(10.dp)

    FlowRow(
        modifier = modifier
            .clip(trackShape)
            .background(Color.White.copy(alpha = 0.045f), trackShape)
            .border(tokens.borders.thin, Color.White.copy(alpha = 0.09f), trackShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val pillShape = RoundedCornerShape(7.dp)
            val textColor = when {
                isSelected -> tokens.colors.onAccent
                !option.enabled -> tokens.colors.textMuted.copy(alpha = 0.35f)
                isHovered -> tokens.colors.textPrimary
                else -> tokens.colors.textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .then(
                        when {
                            isSelected -> Modifier.background(activeBrush, pillShape)
                            option.enabled && isHovered -> Modifier.background(Color.White.copy(alpha = 0.09f), pillShape)
                            else -> Modifier
                        },
                    )
                    .then(
                        if (option.enabled && !isSelected) {
                            Modifier
                                .hoverable(interactionSource)
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        enabled = option.enabled && !isSelected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.RadioButton,
                        onClick = { onSelected(option.value) },
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun DesktopSelectChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    showChevron: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    DesktopControlPill(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    ) {
        if (isLoading) {
            NuvioLoadingIndicator(
                color = tokens.colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = if (enabled) tokens.colors.textSecondary else tokens.colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun DesktopControlPill(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(8.dp)
    val bgColor = when {
        enabled && isHovered -> Color.White.copy(alpha = 0.11f)
        else -> Color.White.copy(alpha = 0.055f)
    }
    val borderColor = when {
        enabled && isHovered -> tokens.colors.accent.copy(alpha = 0.60f)
        else -> Color.White.copy(alpha = 0.13f)
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor, shape)
            .border(tokens.borders.thin, borderColor, shape)
            .then(
                if (enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

internal data class DesktopDropdownOption<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val enabled: Boolean = true,
)

@Composable
internal fun <T> DesktopDropdownSelect(
    label: String,
    options: List<DesktopDropdownOption<T>>,
    selectedValue: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val showSearch = options.size > 12
    val trimmedQuery = searchQuery.trim()
    val filteredOptions = remember(options, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            options
        } else {
            options.filter { option ->
                option.label.contains(trimmedQuery, ignoreCase = true) ||
                    (option.description?.contains(trimmedQuery, ignoreCase = true) == true)
            }
        }
    }

    Box(modifier = modifier) {
        DesktopSelectChip(
            label = label,
            enabled = enabled,
            showChevron = true,
            onClick = { if (enabled) expanded = true },
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            },
            offset = DpOffset(0.dp, 6.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = tokens.colors.surfacePopover,
            tonalElevation = tokens.elevation.flat,
            shadowElevation = tokens.elevation.overlay,
            border = BorderStroke(tokens.borders.thin, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
                .widthIn(min = 210.dp, max = 340.dp)
                .heightIn(max = 360.dp),
        ) {
            if (showSearch) {
                DesktopDropdownSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
            }
            if (filteredOptions.isEmpty()) {
                DesktopDropdownEmptyState()
            } else {
                filteredOptions.forEach { option ->
                    val isSelected = option.value == selectedValue
                    DesktopDropdownOptionRow(
                        label = option.label,
                        description = option.description,
                        selected = isSelected,
                        enabled = option.enabled,
                        role = Role.RadioButton,
                        onClick = {
                            onSelected(option.value)
                            expanded = false
                            searchQuery = ""
                        },
                        trailing = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = tokens.colors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun <T> DesktopDropdownMultiSelect(
    label: String,
    options: List<DesktopDropdownOption<T>>,
    selectedValues: Set<T>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val showSearch = options.size > 12
    val trimmedQuery = searchQuery.trim()
    val filteredOptions = remember(options, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            options
        } else {
            options.filter { option ->
                option.label.contains(trimmedQuery, ignoreCase = true) ||
                    (option.description?.contains(trimmedQuery, ignoreCase = true) == true)
            }
        }
    }

    Box(modifier = modifier) {
        DesktopSelectChip(
            label = label,
            enabled = enabled,
            showChevron = true,
            onClick = { if (enabled) expanded = true },
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            },
            offset = DpOffset(0.dp, 6.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = tokens.colors.surfacePopover,
            tonalElevation = tokens.elevation.flat,
            shadowElevation = tokens.elevation.overlay,
            border = BorderStroke(tokens.borders.thin, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
                .widthIn(min = 210.dp, max = 340.dp)
                .heightIn(max = 360.dp),
        ) {
            if (showSearch) {
                DesktopDropdownSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
            }
            if (filteredOptions.isEmpty()) {
                DesktopDropdownEmptyState()
            } else {
                filteredOptions.forEach { option ->
                    val isSelected = option.value in selectedValues
                    DesktopDropdownOptionRow(
                        label = option.label,
                        description = option.description,
                        selected = isSelected,
                        enabled = option.enabled,
                        role = Role.Checkbox,
                        onClick = { onToggle(option.value) },
                        trailing = {
                            val checkboxShape = RoundedCornerShape(5.dp)
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(checkboxShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                color = if (option.enabled) {
                                                    tokens.colors.accent
                                                } else {
                                                    tokens.colors.accent.copy(alpha = tokens.opacity.disabled)
                                                },
                                                shape = checkboxShape,
                                            )
                                        } else {
                                            Modifier.border(
                                                width = 1.5.dp,
                                                color = Color.White.copy(alpha = if (option.enabled) 0.28f else 0.12f),
                                                shape = checkboxShape,
                                            )
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (option.enabled) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = tokens.opacity.disabled)
                                        },
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopDropdownSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(8.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.06f), shape)
                .border(tokens.borders.thin, Color.White.copy(alpha = 0.10f), shape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = tokens.colors.textMuted,
                modifier = Modifier.size(14.dp),
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.settings_search_placeholder),
                        style = MaterialTheme.nuvioTypeScale.bodySm.copy(fontSize = 12.5.sp),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.nuvioTypeScale.bodySm.copy(
                        fontSize = 12.5.sp,
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
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isClearHovered) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f))
                        .hoverable(clearInteraction)
                        .pointerHoverIcon(PointerIcon.Hand)
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
                        tint = tokens.colors.textMuted,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = tokens.borders.hairline,
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun DesktopDropdownEmptyState() {
    val tokens = MaterialTheme.nuvio
    Text(
        text = stringResource(Res.string.settings_search_empty),
        style = MaterialTheme.nuvioTypeScale.bodySm.copy(fontSize = 12.5.sp),
        color = tokens.colors.textMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun DesktopDropdownOptionRow(
    label: String,
    description: String?,
    selected: Boolean,
    enabled: Boolean,
    role: Role,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowShape = RoundedCornerShape(8.dp)
    val bgColor = when {
        selected -> tokens.colors.accent.copy(alpha = 0.15f)
        enabled && isHovered -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(rowShape)
            .background(bgColor, rowShape)
            .then(
                if (enabled) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = role,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.nuvioTypeScale.bodySm.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = if (enabled) tokens.colors.textPrimary else tokens.colors.textDisabled,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.nuvioTypeScale.bodySm.copy(fontSize = 11.5.sp),
                    color = if (enabled) tokens.colors.textMuted else tokens.colors.textDisabled,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}



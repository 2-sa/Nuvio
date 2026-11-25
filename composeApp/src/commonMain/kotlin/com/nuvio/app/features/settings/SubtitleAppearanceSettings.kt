package com.nuvio.app.features.settings

import com.nuvio.app.isDesktop
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.features.player.*
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun SubtitleAppearanceSettings(
    style: SubtitleStyleState,
    isTablet: Boolean = false,
    enabled: Boolean = true,
    onStyleChanged: (SubtitleStyleState) -> Unit,
) {
    var showFonts by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<String?>(null) }
    var installedFonts by remember { mutableStateOf(SubtitleFonts.installed()) }
    var fontImportFailed by remember { mutableStateOf(false) }
    val importFont = rememberSubtitleFontImporter { result ->
        fontImportFailed = result.isFailure
        result.getOrNull()?.let {
            installedFonts = SubtitleFonts.installed()
            onStyleChanged(style.copy(appearance = style.appearance.copy(fontId = it.id)))
        }
    }
    val desktopColorSwatches = remember {
        (SubtitleColorSwatches + Color.Black).distinctBy { it.toStorageHexString() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF182A35), Color(0xFF53646B), Color(0xFF1C2934))),
                    RoundedCornerShape(16.dp),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.subtitle_preview), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth().heightIn(min = if (isDesktop) 200.dp else 88.dp), contentAlignment = Alignment.Center) {
                StyledSubtitleText(stringResource(Res.string.subtitle_preview_text), style, scale = if (isDesktop) 2f else 1f)
            }
        }
        Text(
            stringResource(Res.string.subtitle_appearance_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsGroup(isTablet) {
            val fontLabel = subtitleFontLabel(style.appearance.fontId, installedFonts)
            if (isDesktop) {
                val fontOptions = BuiltInSubtitleFonts.map { id ->
                    DesktopDropdownOption(
                        value = id,
                        label = subtitleFontLabel(id, installedFonts),
                    )
                } + installedFonts.map { font ->
                    DesktopDropdownOption(
                        value = font.id,
                        label = font.name,
                    )
                }
                val selectedCustomFont = installedFonts.firstOrNull { it.id == style.appearance.fontId }
                val fontDescription = when {
                    fontImportFailed -> stringResource(Res.string.subtitle_font_import_error)
                    style.appearance.fontId !in BuiltInSubtitleFonts && selectedCustomFont == null ->
                        stringResource(Res.string.subtitle_font_missing)
                    else -> null
                }
                DesktopSettingRow(
                    title = stringResource(Res.string.subtitle_font),
                    description = fontDescription,
                    enabled = enabled,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopDropdownSelect(
                            label = fontLabel,
                            options = fontOptions,
                            selectedValue = style.appearance.fontId,
                            onSelected = { fontId ->
                                fontImportFailed = false
                                onStyleChanged(style.copy(appearance = style.appearance.copy(fontId = fontId)))
                            },
                            enabled = enabled,
                        )
                        if (selectedCustomFont != null) {
                            DesktopControlPill(
                                enabled = enabled,
                                onClick = {
                                    SubtitleFonts.remove(selectedCustomFont.id)
                                    installedFonts = SubtitleFonts.installed()
                                    onStyleChanged(style.copy(appearance = style.appearance.copy(fontId = "default")))
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(Res.string.subtitle_font_remove, selectedCustomFont.name),
                                    tint = MaterialTheme.nuvio.colors.textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        DesktopSelectChip(
                            label = stringResource(Res.string.subtitle_font_import),
                            onClick = importFont,
                            enabled = enabled,
                            showChevron = false,
                        )
                    }
                }
            } else {
                SettingsNavigationRow(
                    title = stringResource(Res.string.subtitle_font),
                    description = fontLabel,
                    enabled = enabled, isTablet = isTablet, valueLabel = fontLabel, onClick = { showFonts = true },
                )
            }
            SettingsGroupDivider(isTablet)
            SettingsSliderRow(
                title = stringResource(Res.string.settings_playback_subtitle_size),
                value = style.fontSizeSp,
                valueText = { stringResource(Res.string.compose_player_font_size_value, it) },
                valueRange = subtitleFontSizeRangeSp, step = 2, isTablet = isTablet, enabled = enabled,
                onValueChange = { onStyleChanged(style.copy(fontSizeSp = it)) },
            )
            SettingsGroupDivider(isTablet)
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_subtitle_bold),
                checked = style.bold, enabled = enabled, isTablet = isTablet,
                onCheckedChange = { onStyleChanged(style.copy(bold = it)) },
            )
            SettingsGroupDivider(isTablet)
            val textColorLabel = subtitleColorLabel(style.textColor)
            if (isDesktop) {
                DesktopColorSwatchRow(
                    title = stringResource(Res.string.settings_playback_subtitle_text_color),
                    selectedColor = style.textColor.copy(alpha = 1f),
                    label = textColorLabel,
                    colors = desktopColorSwatches,
                    enabled = enabled,
                    onColorSelected = { selected ->
                        onStyleChanged(style.copy(textColor = selected.copy(alpha = style.textColor.alpha)))
                    },
                )
            } else {
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_playback_subtitle_text_color),
                    description = textColorLabel,
                    enabled = enabled, isTablet = isTablet, valueLabel = textColorLabel, onClick = { colorTarget = "text" },
                )
            }
            SettingsSliderRow(
                title = stringResource(Res.string.compose_player_text_opacity),
                value = (style.textColor.alpha * 100).roundToInt(), valueText = { "$it%" },
                valueRange = 0..100, step = 5, isTablet = isTablet, enabled = enabled,
                onValueChange = { onStyleChanged(style.copy(textColor = style.textColor.copy(alpha = it / 100f))) },
            )
        }
        SettingsGroup(isTablet) {
            val backgroundColorLabel = subtitleColorLabel(style.backgroundColor.copy(alpha = 1f))
            if (isDesktop) {
                DesktopColorSwatchRow(
                    title = stringResource(Res.string.settings_playback_subtitle_background_color),
                    selectedColor = style.backgroundColor.copy(alpha = 1f),
                    label = backgroundColorLabel,
                    colors = desktopColorSwatches,
                    enabled = enabled,
                    onColorSelected = { selected ->
                        onStyleChanged(style.copy(backgroundColor = selected.copy(alpha = style.backgroundColor.alpha)))
                    },
                )
            } else {
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_playback_subtitle_background_color),
                    description = backgroundColorLabel,
                    enabled = enabled, isTablet = isTablet, valueLabel = backgroundColorLabel, onClick = { colorTarget = "background" },
                )
            }
            SettingsSliderRow(
                title = stringResource(Res.string.subtitle_background_opacity),
                value = (style.backgroundColor.alpha * 100).roundToInt(), valueText = { "$it%" },
                valueRange = 0..100, step = 5, isTablet = isTablet, enabled = enabled,
                onValueChange = { onStyleChanged(style.copy(backgroundColor = style.backgroundColor.copy(alpha = it / 100f))) },
            )
            SettingsSliderRow(
                title = stringResource(Res.string.subtitle_background_roundness),
                value = style.appearance.backgroundRadius, valueText = { it.toString() },
                valueRange = 0..24, step = 2, isTablet = isTablet, enabled = enabled && style.backgroundColor.alpha > 0,
                onValueChange = { onStyleChanged(style.copy(appearance = style.appearance.copy(backgroundRadius = it))) },
            )
        }
        SettingsGroup(isTablet) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_playback_subtitle_outline),
                checked = style.outlineEnabled, enabled = enabled, isTablet = isTablet,
                onCheckedChange = { onStyleChanged(style.copy(outlineEnabled = it)) },
            )
            if (style.outlineEnabled) {
                val outlineColorLabel = subtitleColorLabel(style.outlineColor)
                if (isDesktop) {
                    DesktopColorSwatchRow(
                        title = stringResource(Res.string.settings_playback_subtitle_outline_color),
                        selectedColor = style.outlineColor.copy(alpha = 1f),
                        label = outlineColorLabel,
                        colors = desktopColorSwatches,
                        enabled = enabled,
                        onColorSelected = { selected ->
                            onStyleChanged(style.copy(outlineColor = selected.copy(alpha = style.outlineColor.alpha)))
                        },
                    )
                } else {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_playback_subtitle_outline_color),
                        description = outlineColorLabel,
                        enabled = enabled, isTablet = isTablet, valueLabel = outlineColorLabel, onClick = { colorTarget = "outline" },
                    )
                }
                SettingsSliderRow(
                    title = stringResource(Res.string.subtitle_outline_width),
                    value = style.outlineWidth, valueText = { it.toString() },
                    valueRange = subtitleOutlineWidthRange, step = 1, isTablet = isTablet, enabled = enabled,
                    onValueChange = { onStyleChanged(style.copy(outlineWidth = it)) },
                )
            }
            SettingsGroupDivider(isTablet)
            SettingsSwitchRow(
                title = stringResource(Res.string.subtitle_shadow),
                description = stringResource(Res.string.subtitle_shadow_description),
                checked = style.appearance.shadowEnabled, enabled = enabled, isTablet = isTablet,
                onCheckedChange = { onStyleChanged(style.copy(appearance = style.appearance.copy(shadowEnabled = it))) },
            )
            if (style.appearance.shadowEnabled) {
                SettingsSliderRow(
                    title = stringResource(Res.string.subtitle_shadow_opacity),
                    value = style.appearance.shadowOpacity, valueText = { "$it%" },
                    valueRange = 0..100, step = 5, isTablet = isTablet, enabled = enabled,
                    onValueChange = { onStyleChanged(style.copy(appearance = style.appearance.copy(shadowOpacity = it))) },
                )
                SettingsSliderRow(
                    title = stringResource(Res.string.subtitle_shadow_size),
                    value = style.appearance.shadowSize, valueText = { it.toString() },
                    valueRange = 1..8, step = 1, isTablet = isTablet, enabled = enabled,
                    onValueChange = { onStyleChanged(style.copy(appearance = style.appearance.copy(shadowSize = it))) },
                )
            }
        }
        SettingsGroup(isTablet) {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_playback_subtitle_vertical_offset),
                value = style.bottomOffset, valueText = { it.toString() },
                valueRange = 0..200, step = 5, isTablet = isTablet, enabled = enabled,
                onValueChange = { onStyleChanged(style.copy(bottomOffset = it)) },
            )
        }
        TextButton(onClick = { onStyleChanged(style.resetAppearance()) }, enabled = enabled) {
            Text(stringResource(Res.string.subtitle_reset_appearance))
        }
    }
    if (!isDesktop && showFonts) {
        SubtitleFontPicker(
            selectedId = style.appearance.fontId,
            onSelect = { onStyleChanged(style.copy(appearance = style.appearance.copy(fontId = it))) },
            onDismiss = { showFonts = false },
        )
    }
    if (!isDesktop) {
        colorTarget?.let { target ->
            val color = when (target) {
                "background" -> style.backgroundColor
                "outline" -> style.outlineColor
                else -> style.textColor
            }
            val title = when (target) {
                "background" -> Res.string.settings_playback_subtitle_background_color
                "outline" -> Res.string.settings_playback_subtitle_outline_color
                else -> Res.string.settings_playback_subtitle_text_color
            }
            SubtitleColorDialog(
                title = stringResource(title),
                colors = SubtitleColorSwatches + Color.Black,
                selectedColor = color.copy(alpha = 1f),
                onColorSelected = {
                    val selected = it.copy(alpha = color.alpha)
                    onStyleChanged(when (target) {
                        "background" -> style.copy(backgroundColor = selected)
                        "outline" -> style.copy(outlineColor = selected)
                        else -> style.copy(textColor = selected)
                    })
                    colorTarget = null
                },
                onDismiss = { colorTarget = null },
            )
        }
    }
}

@Composable
private fun DesktopColorSwatchRow(
    title: String,
    selectedColor: Color,
    label: String,
    colors: List<Color>,
    enabled: Boolean,
    onColorSelected: (Color) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    var expanded by remember { mutableStateOf(false) }
    val selectedHex = selectedColor.toStorageHexString()

    DesktopSettingRow(
        title = title,
        enabled = enabled,
    ) {
        Box {
            DesktopControlPill(
                enabled = enabled,
                onClick = { expanded = true },
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(selectedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(4.dp)),
                )
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
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (enabled) tokens.colors.textSecondary else tokens.colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 6.dp),
                containerColor = tokens.colors.surfacePopover,
                tonalElevation = tokens.elevation.flat,
                shadowElevation = tokens.elevation.overlay,
                border = BorderStroke(tokens.borders.thin, tokens.colors.borderDefault),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    colors.chunked(5).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { swatch ->
                                val isSelected = swatch.toStorageHexString() == selectedHex
                                val interactionSource = remember { MutableInteractionSource() }
                                val isHovered by interactionSource.collectIsHoveredAsState()
                                val swatchShape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(swatchShape)
                                        .background(swatch)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> tokens.colors.accent
                                                isHovered -> Color.White.copy(alpha = 0.7f)
                                                else -> Color.White.copy(alpha = 0.22f)
                                            },
                                            shape = swatchShape,
                                        )
                                        .hoverable(interactionSource)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                        ) {
                                            onColorSelected(swatch)
                                            expanded = false
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = subtitleColorLabel(swatch),
                                            tint = if (swatch.luminance() > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

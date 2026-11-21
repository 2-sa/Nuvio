package com.nuvio.app.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.CustomThemeColors
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.labelRes
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.features.membership.MemberAccessRepository
import com.nuvio.app.features.membership.availableAppThemes
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cd_selected
import nuvio.composeapp.generated.resources.community_membership_tier_supporter
import nuvio.composeapp.generated.resources.custom_theme_edit
import org.jetbrains.compose.resources.stringResource

private val SupporterThemes = setOf(
    AppTheme.GOLD,
    AppTheme.JADE,
    AppTheme.ROSE_GOLD,
    AppTheme.ARCTIC_BLUE,
    AppTheme.GRAPHITE,
)

@Composable
internal fun AppearanceThemePicker(
    isTablet: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    val memberAccess by remember {
        MemberAccessRepository.ensureStarted()
        MemberAccessRepository.access
    }.collectAsStateWithLifecycle()
    val customColors by ThemeSettingsRepository.customThemeColors.collectAsStateWithLifecycle()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val profileId = profileState.activeProfile?.profileIndex
    val gradientEnabled = memberAccess.tier != null
    var showEditor by rememberSaveable(profileId, gradientEnabled) { mutableStateOf(false) }
    val availableThemes = availableAppThemes(memberAccess.entitlements)
    val themes = remember(availableThemes) {
        listOfNotNull(availableThemes.firstOrNull { it == AppTheme.CINEMA_OBSIDIAN }) +
            availableThemes.filterNot { it == AppTheme.CINEMA_OBSIDIAN }
    }
    val themeSpacing = if (isTablet) NuvioTokens.Space.s14 else NuvioTokens.Space.s10
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) NuvioTokens.Space.s20 else NuvioTokens.Space.s16,
                vertical = if (isTablet) NuvioTokens.Space.s18 else NuvioTokens.Space.s14,
            ),
    ) {
        val preferredColumns = if (isTablet) 3 else 2
        val minThemeCellWidth = if (isTablet) 172.dp else 136.dp
        val themeColumns = ((maxWidth + themeSpacing) / (minThemeCellWidth + themeSpacing))
            .toInt().coerceIn(1, preferredColumns)
        Column(verticalArrangement = Arrangement.spacedBy(themeSpacing)) {
            themes.chunked(themeColumns).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(themeSpacing),
                ) {
                    rowThemes.forEach { theme ->
                        ThemeSwatchCard(
                            theme = theme,
                            customColors = customColors,
                            isSelected = theme == selectedTheme,
                            isSupporter = theme in SupporterThemes,
                            onClick = {
                                if (theme == AppTheme.CUSTOM) showEditor = true else onThemeSelected(theme)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(themeColumns - rowThemes.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (showEditor) {
        CustomThemeEditor(
            initialColors = customColors,
            allowGradient = gradientEnabled,
            onSave = ThemeSettingsRepository::setCustomTheme,
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
private fun ThemeSwatchCard(
    theme: AppTheme,
    customColors: CustomThemeColors,
    isSelected: Boolean,
    isSupporter: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = remember(theme, customColors) { ThemeColors.getColorPalette(theme, customColors) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val cardShape = RoundedCornerShape(NuvioTokens.Radius.lg + NuvioTokens.Space.s2)
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) NuvioTokens.Border.medium else tokens.borders.thin,
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "themeSwatchBorderWidth",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = tokens.motion.fastMillis),
        label = "themeSwatchBorderColor",
    )

    Surface(
        modifier = modifier
            .clip(cardShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .nuvioFocusBorder(cardShape)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = cardShape,
        color = palette.background,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(NuvioTokens.Space.s12),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(NuvioTokens.Space.s12)
                                    .clip(CircleShape)
                                    .background(palette.accentBrush()),
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = NuvioTokens.Space.s36, height = NuvioTokens.Space.s6)
                                    .clip(CircleShape)
                                    .background(palette.accentBrush()),
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(NuvioTokens.Space.s20)
                                    .clip(CircleShape)
                                    .background(tokens.colors.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.cd_selected),
                                    tint = tokens.colors.onAccent,
                                    modifier = Modifier.size(NuvioTokens.Space.s12),
                                )
                            }
                        } else if (theme == AppTheme.CUSTOM) {
                            Box(
                                modifier = Modifier
                                    .size(NuvioTokens.Space.s20)
                                    .clip(CircleShape)
                                    .background(palette.backgroundElevated)
                                    .border(tokens.borders.thin, palette.borderSubtle, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(Res.string.custom_theme_edit),
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(NuvioTokens.Space.s12),
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(NuvioTokens.Space.s20))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NuvioTokens.Radius.md))
                            .background(palette.backgroundCard)
                            .border(tokens.borders.thin, palette.borderSubtle, RoundedCornerShape(NuvioTokens.Radius.md))
                            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s8),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(NuvioTokens.Space.s6)
                                    .clip(CircleShape)
                                    .background(palette.textPrimary.copy(alpha = 0.72f)),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.46f)
                                    .height(NuvioTokens.Space.s4)
                                    .clip(CircleShape)
                                    .background(palette.textMuted.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.backgroundElevated)
                    .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s8),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(theme.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isSupporter) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.secondary.copy(alpha = 0.16f))
                            .border(
                                tokens.borders.thin,
                                palette.secondary.copy(alpha = 0.35f),
                                CircleShape,
                            )
                            .padding(horizontal = NuvioTokens.Space.s6, vertical = NuvioTokens.Space.s2),
                    ) {
                        Text(
                            text = stringResource(Res.string.community_membership_tier_supporter),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.build.AppVersionPolicy
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.about_licenses_attributions_subtitle
import nuvio.composeapp.generated.resources.about_supporters_contributors_subtitle
import nuvio.composeapp.generated.resources.compose_about_based_on_version_format
import nuvio.composeapp.generated.resources.compose_about_made_with
import nuvio.composeapp.generated.resources.compose_about_version_format
import nuvio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import nuvio.composeapp.generated.resources.compose_settings_page_supporters_contributors
import nuvio.composeapp.generated.resources.compose_settings_root_about_section
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_description
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_title
import nuvio.composeapp.generated.resources.settings_about_copy_version
import nuvio.composeapp.generated.resources.settings_about_version_copied
import nuvio.composeapp.generated.resources.updates_debug_test_description
import nuvio.composeapp.generated.resources.updates_debug_test_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.aboutSettingsContent(
    isTablet: Boolean,
    onSupportersContributorsClick: () -> Unit,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)?,
    onTestUpdateBannerClick: (() -> Unit)?,
    showSupportersContributorsPage: Boolean,
) {
    if (isDesktop) {
        item(key = "desktop-about-hero") {
            DesktopAboutHeroCard(
                onCheckForUpdatesClick = onCheckForUpdatesClick,
            )
        }
        if (onCheckForUpdatesClick != null) {
            item(key = "desktop-about-update-channel") {
                SettingsGroup(isTablet = isTablet) {
                    UpdateChannelSettingsRow(isTablet = isTablet)
                }
            }
        }
        item(key = "desktop-about-bento") {
            val supportersTitle = stringResource(Res.string.compose_settings_page_supporters_contributors)
            val supportersDesc = stringResource(Res.string.about_supporters_contributors_subtitle)
            val licensesTitle = stringResource(Res.string.compose_settings_page_licenses_attributions)
            val licensesDesc = stringResource(Res.string.about_licenses_attributions_subtitle)
            val debugTitle = stringResource(Res.string.updates_debug_test_title)
            val debugDesc = stringResource(Res.string.updates_debug_test_description)

            val bentoItems = buildList {
                if (showSupportersContributorsPage) {
                    add(
                        SettingsBentoItem(
                            id = "supporters-contributors",
                            title = supportersTitle,
                            description = supportersDesc,
                            icon = SettingsPage.SupportersContributors.icon,
                            onClick = onSupportersContributorsClick,
                        ),
                    )
                }
                add(
                    SettingsBentoItem(
                        id = "licenses-attributions",
                        title = licensesTitle,
                        description = licensesDesc,
                        icon = SettingsPage.LicensesAttributions.icon,
                        onClick = onLicensesAttributionsClick,
                    ),
                )
                if (onTestUpdateBannerClick != null) {
                    add(
                        SettingsBentoItem(
                            id = "updates-debug-test",
                            title = debugTitle,
                            description = debugDesc,
                            icon = Icons.Default.BugReport,
                            onClick = onTestUpdateBannerClick,
                        ),
                    )
                }
            }
            SettingsBentoGrid(
                items = bentoItems,
                isTablet = isTablet,
            )
        }
        return
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.compose_settings_root_about_section),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (showSupportersContributorsPage) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_page_supporters_contributors),
                        description = stringResource(Res.string.about_supporters_contributors_subtitle),
                        icon = SettingsPage.SupportersContributors.icon,
                        isTablet = isTablet,
                        onClick = onSupportersContributorsClick,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                }
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_licenses_attributions),
                    description = stringResource(Res.string.about_licenses_attributions_subtitle),
                    icon = SettingsPage.LicensesAttributions.icon,
                    isTablet = isTablet,
                    onClick = onLicensesAttributionsClick,
                )
                if (onCheckForUpdatesClick != null) {
                    SettingsGroupDivider(isTablet = isTablet)
                    UpdateChannelSettingsRow(isTablet = isTablet)
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.compose_settings_root_check_updates_title),
                        description = stringResource(Res.string.compose_settings_root_check_updates_description),
                        icon = SettingsSearchTarget.CheckForUpdates.icon,
                        isTablet = isTablet,
                        onClick = onCheckForUpdatesClick,
                    )
                }
                if (onTestUpdateBannerClick != null) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.updates_debug_test_title),
                        description = stringResource(Res.string.updates_debug_test_description),
                        icon = Icons.Default.BugReport,
                        isTablet = isTablet,
                        onClick = onTestUpdateBannerClick,
                    )
                }
            }
        }
    }
    settingsFooterContent(isTablet)
}

@Composable
private fun DesktopAboutHeroCard(
    onCheckForUpdatesClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val clipboardManager = LocalClipboardManager.current
    val versionLine = stringResource(
        Res.string.compose_about_version_format,
        AppVersionPolicy.displayVersionName,
        AppVersionPolicy.displayVersionCode,
    )
    val basedOnLine = AppVersionPolicy.basedOnVersionName?.let { basedOn ->
        stringResource(Res.string.compose_about_based_on_version_format, basedOn)
    }
    val copiedToast = stringResource(Res.string.settings_about_version_copied)
    val copyPayload = buildString {
        append("Nuvio ")
        append(AppVersionPolicy.displayVersionName)
        append(" (")
        append(AppVersionPolicy.displayVersionCode)
        append(')')
        AppVersionPolicy.basedOnVersionName?.let { basedOn ->
            append(" • Based on Nuvio ")
            append(basedOn)
        }
    }
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        tokens.colors.accent.copy(alpha = 0.16f),
                        tokens.colors.accentStrong.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f),
                    ),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(tokens.colors.accent),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = NuvioTokens.Space.s20, vertical = NuvioTokens.Space.s18),
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s20),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                    ) {
                        MemberBrandWordmark(height = 28.dp)
                        SettingsStatusPill(
                            label = "v${AppVersionPolicy.displayVersionName}",
                            tone = SettingsStatusTone.Neutral,
                            showDot = false,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.compose_about_made_with),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                    Text(
                        text = listOfNotNull(versionLine, basedOnLine).joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.textMuted,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(copyPayload))
                            NuvioToastController.show(copiedToast)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(tokens.borders.thin, Color.White.copy(alpha = 0.16f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            contentColor = tokens.colors.textPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.settings_about_copy_version),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (onCheckForUpdatesClick != null) {
                        Button(
                            onClick = onCheckForUpdatesClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.colors.accent,
                                contentColor = tokens.colors.onAccent,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Icon(
                                imageVector = SettingsSearchTarget.CheckForUpdates.icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.compose_settings_root_check_updates_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.settingsFooterContent(isTablet: Boolean) {
    item {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (isTablet) 20.dp else 16.dp),
        ) {
            MemberBrandWordmark(
                height = if (isTablet) 30.dp else 26.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = Modifier.height(if (isTablet) 10.dp else 8.dp),
            )
            Text(
                text = stringResource(Res.string.compose_about_made_with),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    Res.string.compose_about_version_format,
                    AppVersionPolicy.displayVersionName,
                    AppVersionPolicy.displayVersionCode,
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            AppVersionPolicy.basedOnVersionName?.let { basedOnVersionName ->
                Text(
                    text = stringResource(
                        Res.string.compose_about_based_on_version_format,
                        basedOnVersionName,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun DesktopSettingsSidebarVersionCard(
    onCheckForUpdatesClick: (() -> Unit)?,
    onOpenAboutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(12.dp)
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardHovered by cardInteraction.collectIsHoveredAsState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isCardHovered && onOpenAboutClick != null) {
            tokens.colors.surfacePopover
        } else {
            tokens.colors.surfaceCard
        },
        shape = cardShape,
        border = BorderStroke(tokens.borders.thin, tokens.colors.borderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onOpenAboutClick != null) {
                        Modifier
                            .clip(cardShape)
                            .hoverable(cardInteraction)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .nuvioFocusBorder(cardShape)
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = null,
                                onClick = onOpenAboutClick,
                            )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s3),
            ) {
                MemberBrandWordmark(height = NuvioTokens.Space.s16)
                Text(
                    text = stringResource(
                        Res.string.compose_about_version_format,
                        AppVersionPolicy.displayVersionName,
                        AppVersionPolicy.displayVersionCode,
                    ),
                    style = MaterialTheme.nuvioTypeScale.labelSm,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onCheckForUpdatesClick != null) {
                val updateInteraction = remember { MutableInteractionSource() }
                val isUpdateHovered by updateInteraction.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(NuvioTokens.Space.s32)
                        .clip(CircleShape)
                        .background(
                            if (isUpdateHovered) {
                                tokens.colors.accent.copy(alpha = 0.22f)
                            } else {
                                tokens.colors.focusBackground
                            },
                        )
                        .nuvioFocusBorder(CircleShape)
                        .hoverable(updateInteraction)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = updateInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = onCheckForUpdatesClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SettingsSearchTarget.CheckForUpdates.icon,
                        contentDescription = stringResource(Res.string.compose_settings_root_check_updates_title),
                        tint = tokens.colors.accent,
                        modifier = Modifier.size(NuvioTokens.Icon.sm),
                    )
                }
            }
        }
    }
}

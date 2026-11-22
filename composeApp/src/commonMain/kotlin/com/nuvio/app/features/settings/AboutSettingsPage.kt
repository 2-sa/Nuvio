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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.build.AppVersionPolicy
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTypeScale
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.about_licenses_attributions_subtitle
import nuvio.composeapp.generated.resources.about_supporters_contributors_subtitle
import nuvio.composeapp.generated.resources.compose_about_based_on_version_format
import nuvio.composeapp.generated.resources.compose_about_made_with
import nuvio.composeapp.generated.resources.compose_about_version_format
import nuvio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import nuvio.composeapp.generated.resources.compose_settings_page_privacy_policy
import nuvio.composeapp.generated.resources.compose_settings_page_supporters_contributors
import nuvio.composeapp.generated.resources.compose_settings_root_about_section
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_description
import nuvio.composeapp.generated.resources.compose_settings_root_check_updates_title
import nuvio.composeapp.generated.resources.compose_settings_root_privacy_policy_description
import nuvio.composeapp.generated.resources.updates_debug_test_description
import nuvio.composeapp.generated.resources.updates_debug_test_title
import org.jetbrains.compose.resources.stringResource

private const val PRIVACY_POLICY_URL = "https://nuvio.tv/privacy-policy"

internal fun LazyListScope.aboutSettingsContent(
    isTablet: Boolean,
    onSupportersContributorsClick: () -> Unit,
    onLicensesAttributionsClick: () -> Unit,
    onCheckForUpdatesClick: (() -> Unit)?,
    onTestUpdateBannerClick: (() -> Unit)?,
    showSupportersContributorsPage: Boolean,
) {
    item {
        val uriHandler = LocalUriHandler.current
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
                    title = stringResource(Res.string.compose_settings_page_privacy_policy),
                    description = stringResource(Res.string.compose_settings_root_privacy_policy_description),
                    icon = Icons.Rounded.Policy,
                    isTablet = isTablet,
                    onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                SettingsGroupDivider(isTablet = isTablet)
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

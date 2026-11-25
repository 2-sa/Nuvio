package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsUiState
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_notifications_disabled_in_app
import nuvio.composeapp.generated.resources.settings_notifications_episode_release_alerts
import nuvio.composeapp.generated.resources.settings_notifications_episode_release_alerts_description
import nuvio.composeapp.generated.resources.settings_notifications_permission_disabled
import nuvio.composeapp.generated.resources.settings_notifications_scheduled_count
import nuvio.composeapp.generated.resources.settings_notifications_section_alerts
import nuvio.composeapp.generated.resources.settings_notifications_section_test
import nuvio.composeapp.generated.resources.settings_notifications_send_test
import nuvio.composeapp.generated.resources.settings_notifications_sending_test
import nuvio.composeapp.generated.resources.settings_notifications_test_for_title
import nuvio.composeapp.generated.resources.settings_notifications_test_requires_saved_show
import nuvio.composeapp.generated.resources.settings_notifications_test_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.notificationsSettingsContent(
    isTablet: Boolean,
    uiState: EpisodeReleaseNotificationsUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_notifications_section_alerts),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_notifications_episode_release_alerts),
                    description = stringResource(Res.string.settings_notifications_episode_release_alerts_description),
                    checked = uiState.isEnabled,
                    enabled = !uiState.isLoading,
                    isTablet = isTablet,
                    onCheckedChange = EpisodeReleaseNotificationsRepository::setEnabled,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_notifications_section_test),
            isTablet = isTablet,
        ) {
            NotificationTestCard(
                isTablet = isTablet,
                uiState = uiState,
            )
        }
    }
}

@Composable
private fun NotificationTestCard(
    isTablet: Boolean,
    uiState: EpisodeReleaseNotificationsUiState,
) {
    val tokens = MaterialTheme.nuvio
    val canSend = !uiState.isSendingTest && !uiState.isLoading && uiState.testTargetTitle != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_notifications_test_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.testTargetTitle?.let { title ->
                        stringResource(Res.string.settings_notifications_test_for_title, title)
                    } ?: stringResource(Res.string.settings_notifications_test_requires_saved_show),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (uiState.isEnabled) {
                        stringResource(Res.string.settings_notifications_scheduled_count, uiState.scheduledCount)
                    } else {
                        stringResource(Res.string.settings_notifications_disabled_in_app)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = EpisodeReleaseNotificationsRepository::sendTestNotification,
                enabled = canSend,
                shape = if (isDesktop) RoundedCornerShape(8.dp) else ButtonDefaults.shape,
                contentPadding = if (isDesktop) {
                    PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    ButtonDefaults.ContentPadding
                },
                modifier = if (isDesktop) {
                    Modifier
                        .heightIn(min = 36.dp)
                        .then(if (canSend) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                } else {
                    Modifier.fillMaxWidth()
                },
                colors = if (isDesktop) {
                    ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                        disabledContainerColor = tokens.colors.surfaceElevated,
                        disabledContentColor = tokens.colors.textMuted,
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            ) {
                Text(
                    text = if (uiState.isSendingTest) {
                        stringResource(Res.string.settings_notifications_sending_test)
                    } else {
                        stringResource(Res.string.settings_notifications_send_test)
                    },
                    fontWeight = if (isDesktop) FontWeight.SemiBold else null,
                )
            }

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!uiState.permissionGranted) {
                Text(
                    text = stringResource(Res.string.settings_notifications_permission_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

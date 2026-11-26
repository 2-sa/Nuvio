package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.format.formatLocalDateTime
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.anilist.AniListAuthCallbackParser
import com.nuvio.app.features.anilist.AniListTracker
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.watchprogress.WatchProgressSourceCoordinator
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.settings_anilist_access_token_label
import nuvio.composeapp.generated.resources.settings_anilist_client_id_label
import nuvio.composeapp.generated.resources.settings_anilist_client_id_placeholder
import nuvio.composeapp.generated.resources.settings_anilist_connect_with_token
import nuvio.composeapp.generated.resources.settings_anilist_connected_as
import nuvio.composeapp.generated.resources.settings_anilist_connected_description
import nuvio.composeapp.generated.resources.settings_anilist_description
import nuvio.composeapp.generated.resources.settings_anilist_disconnect
import nuvio.composeapp.generated.resources.settings_anilist_last_synced
import nuvio.composeapp.generated.resources.settings_anilist_sign_in
import nuvio.composeapp.generated.resources.settings_anilist_sync_now
import nuvio.composeapp.generated.resources.settings_anilist_syncing
import nuvio.composeapp.generated.resources.settings_anilist_title
import nuvio.composeapp.generated.resources.settings_anilist_visit
import nuvio.composeapp.generated.resources.settings_tracking_not_connected
import nuvio.composeapp.generated.resources.settings_tracking_status_connected
import nuvio.composeapp.generated.resources.settings_tracking_status_disconnected
import org.jetbrains.compose.resources.stringResource

private val AniListAccent = Color(0xFF02A9FF)
private const val ANILIST_WEBSITE_URL = "https://anilist.co"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AniListProviderCard(modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.nuvio
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val authState by remember {
        AniListTracker.ensureLoaded()
        AniListTracker.authState
    }.collectAsStateWithLifecycle()
    val syncState by AniListTracker.syncState.collectAsStateWithLifecycle()

    var clientIdInput by rememberSaveable(authState.profileId) { mutableStateOf(authState.clientId) }
    var tokenInput by rememberSaveable(authState.profileId) { mutableStateOf("") }

    LaunchedEffect(authState.profileId, authState.clientId) {
        if (authState.clientId.isNotBlank() && clientIdInput != authState.clientId) {
            clientIdInput = authState.clientId
        }
    }

    val isConnected = authState.isAuthenticated
    val statusLabel = if (isConnected) {
        stringResource(Res.string.settings_tracking_status_connected)
    } else {
        stringResource(Res.string.settings_tracking_status_disconnected)
    }
    val statusTone = if (isConnected) SettingsStatusTone.Success else SettingsStatusTone.Neutral

    val titleText = if (isConnected) {
        stringResource(
            Res.string.settings_anilist_connected_as,
            authState.user?.name.orEmpty(),
        )
    } else {
        stringResource(Res.string.settings_anilist_title)
    }
    val subtitleText = if (isConnected) {
        stringResource(Res.string.settings_anilist_connected_description)
    } else {
        stringResource(Res.string.settings_anilist_description)
    }

    val syncStatusText = when {
        syncState.isLoading -> stringResource(Res.string.settings_anilist_syncing)
        isConnected && syncState.snapshot != null -> stringResource(
            Res.string.settings_anilist_last_synced,
            formatLocalDateTime(syncState.snapshot!!.checkedAtEpochMs),
        )
        !isConnected -> stringResource(Res.string.settings_tracking_not_connected)
        else -> null
    }
    val errorText = authState.error?.takeIf { it.isNotBlank() }
        ?: syncState.error?.takeIf { it.isNotBlank() }

    val cardShape = if (isDesktop) RoundedCornerShape(10.dp) else tokens.shapes.card
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AniListAccent.copy(alpha = 0.8f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
        focusedContainerColor = Color.Black.copy(alpha = 0.22f),
        unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
        disabledContainerColor = Color.Black.copy(alpha = 0.12f),
        focusedTextColor = tokens.colors.textPrimary,
        unfocusedTextColor = tokens.colors.textPrimary,
        focusedLabelColor = AniListAccent,
        unfocusedLabelColor = tokens.colors.textSecondary,
    )

    val cardModifier = modifier
        .clip(cardShape)
        .background(
            Brush.horizontalGradient(
                listOf(
                    AniListAccent.copy(alpha = 0.17f),
                    AniListAccent.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.02f),
                ),
            ),
            cardShape,
        )

    Surface(
        modifier = cardModifier,
        color = Color.Transparent,
        shape = cardShape,
        border = if (isDesktop) null else BorderStroke(tokens.borders.hairline, Color.White.copy(alpha = 0.14f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isDesktop) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(AniListAccent, Color(0xFF3B82F6)),
                            ),
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (isDesktop) 16.dp else 18.dp, vertical = if (isDesktop) 14.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            SettingsStatusPill(
                                label = statusLabel,
                                tone = statusTone,
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.textSecondary,
                        )
                    }

                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(ANILIST_WEBSITE_URL) } },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = tokens.colors.accent),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_anilist_visit),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                if (isConnected) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    WatchProgressSourceCoordinator.refreshProviderAndActiveSource(
                                        profileId = authState.profileId,
                                        providerId = TrackingProviderId.ANILIST,
                                        refreshProvider = {
                                            AniListTracker.syncNow(TrackingRefreshIntent.USER_INITIATED)
                                            AniListTracker.syncState.value.error == null
                                        },
                                    )
                                }
                            },
                            enabled = !authState.isBusy && !syncState.isLoading,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(tokens.borders.thin, AniListAccent.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AniListAccent.copy(alpha = 0.14f),
                                contentColor = tokens.colors.textPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            if (syncState.isLoading) {
                                NuvioLoadingIndicator(
                                    color = tokens.colors.textPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.settings_anilist_sync_now),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        TextButton(
                            onClick = { scope.launch { AniListTracker.disconnect() } },
                            enabled = !authState.isBusy && !syncState.isLoading,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFFB7185),
                                disabledContentColor = tokens.colors.textMuted,
                            ),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Text(
                                text = stringResource(Res.string.settings_anilist_disconnect),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = clientIdInput,
                                onValueChange = { value ->
                                    clientIdInput = value
                                    AniListTracker.setClientId(value)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !authState.isBusy,
                                singleLine = true,
                                label = { Text(stringResource(Res.string.settings_anilist_client_id_label)) },
                                placeholder = { Text(stringResource(Res.string.settings_anilist_client_id_placeholder)) },
                                colors = textFieldColors,
                            )
                            Button(
                                onClick = {
                                    AniListTracker.setClientId(clientIdInput)
                                    val url = AniListAuthCallbackParser.buildOAuthAuthorizeUrl(clientIdInput)
                                    runCatching { uriHandler.openUri(url) }
                                },
                                enabled = clientIdInput.isNotBlank() && !authState.isBusy,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AniListAccent,
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Text(
                                    text = stringResource(Res.string.settings_anilist_sign_in),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SettingsSecretTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = stringResource(Res.string.settings_anilist_access_token_label),
                                modifier = Modifier.weight(1f),
                                enabled = !authState.isBusy,
                                colors = textFieldColors,
                                imeAction = ImeAction.Done,
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (tokenInput.isNotBlank() && !authState.isBusy) {
                                            val candidate = tokenInput
                                            scope.launch {
                                                AniListTracker.connectWithToken(candidate)
                                                if (AniListTracker.authState.value.isAuthenticated) {
                                                    tokenInput = ""
                                                }
                                            }
                                        }
                                    },
                                ),
                            )
                            Button(
                                onClick = {
                                    val candidate = tokenInput
                                    scope.launch {
                                        AniListTracker.connectWithToken(candidate)
                                        if (AniListTracker.authState.value.isAuthenticated) {
                                            tokenInput = ""
                                        }
                                    }
                                },
                                enabled = tokenInput.isNotBlank() && !authState.isBusy,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.colors.accent,
                                    contentColor = tokens.colors.onAccent,
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                if (authState.isBusy) {
                                    NuvioLoadingIndicator(
                                        color = tokens.colors.onAccent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = stringResource(Res.string.settings_anilist_connect_with_token),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                syncStatusText?.takeIf { it.isNotBlank() }?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(NuvioTokens.Radius.md),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.colors.textSecondary,
                        )
                    }
                }

                errorText?.let { message ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFDAD6).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(NuvioTokens.Radius.md),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFDAD6),
                        )
                    }
                }
            }
        }
    }
}

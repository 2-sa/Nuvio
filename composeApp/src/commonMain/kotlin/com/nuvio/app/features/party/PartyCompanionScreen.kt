package com.nuvio.app.features.party

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.party_companion_chatting_as
import nuvio.composeapp.generated.resources.party_companion_seat_left
import nuvio.composeapp.generated.resources.party_error_ended
import nuvio.composeapp.generated.resources.party_reconnecting
import nuvio.composeapp.generated.resources.party_status_offline
import nuvio.composeapp.generated.resources.party_status_ready
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PartyCompanionScreen(onBack: () -> Unit) {
    val state by PartyCompanion.state.collectAsStateWithLifecycle()
    val link = state.link
    val error = state.error
    PartyPage(onBack = onBack) {
        when {
            link != null -> LinkedChat(link)
            error != null -> CompanionError(error = error, onBack = onBack)
        }
    }
}

@Composable
private fun LinkedChat(link: PartyCompanionLink) {
    val tokens = MaterialTheme.nuvio
    val seat = link.seat
    val cardShape = RoundedCornerShape(18.dp)
    val statusColor = when {
        link.reconnecting -> tokens.colors.warning
        link.connected -> tokens.colors.success
        else -> tokens.colors.textMuted
    }
    val statusLabel = stringResource(
        when {
            link.reconnecting -> Res.string.party_reconnecting
            link.connected -> Res.string.party_status_ready
            else -> Res.string.party_status_offline
        },
    )
    val seatPresence = when {
        link.reconnecting -> PartyPresenceState.Away
        link.connected -> PartyPresenceState.Watching
        else -> PartyPresenceState.Offline
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(tokens.colors.surfaceCard)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(if (isDesktop) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (seat != null) {
                    PartyAvatar(
                        member = seat,
                        size = 42.dp,
                        presence = seatPresence,
                        ringColor = tokens.colors.surfaceCard,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.party_companion_chatting_as, seat?.displayName.orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (link.reconnecting) stringResource(Res.string.party_reconnecting) else link.media.displayTitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (link.reconnecting) tokens.colors.accent else tokens.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val statusPillShape = RoundedCornerShape(999.dp)
                Row(
                    modifier = Modifier
                        .clip(statusPillShape)
                        .background(tokens.colors.surfaceElevated)
                        .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, statusPillShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                    )
                }
            }

            if (link.members.isNotEmpty()) {
                val membersScrollState = rememberScrollState()
                val chipShape = RoundedCornerShape(999.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(membersScrollState)
                        .horizontalScroll(membersScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    link.members.forEach { member ->
                        val isSelf = member.id == link.memberId
                        val chipBg = if (isSelf) tokens.colors.focusBackground else tokens.colors.surfaceElevated
                        val chipBorder = if (isSelf) tokens.colors.accent.copy(alpha = 0.6f) else tokens.colors.borderDefault
                        Row(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(chipBg)
                                .border(NuvioTokens.Border.thin, chipBorder, chipShape)
                                .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PartyAvatar(
                                member = member,
                                size = 22.dp,
                                ringColor = chipBg,
                            )
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(12.dp))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .clip(cardShape)
                .background(tokens.colors.surfaceElevated)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            PartyChatContent(
                members = link.members,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CompanionError(
    error: PartyCompanionError,
    onBack: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val cardShape = RoundedCornerShape(20.dp)
    val iconShape = RoundedCornerShape(16.dp)
    val ctaShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(tokens.colors.surfaceCard)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(iconShape)
                    .background(tokens.colors.surfaceElevated)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LinkOff,
                    contentDescription = null,
                    tint = tokens.colors.textMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(
                    when (error) {
                        PartyCompanionError.SeatLeft -> Res.string.party_companion_seat_left
                        PartyCompanionError.Ended -> Res.string.party_error_ended
                    },
                ),
                modifier = Modifier.widthIn(max = 360.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(ctaShape)
                    .background(palette.accentBrush())
                    .then(
                        if (isDesktop && isHovered) {
                            Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                        } else {
                            Modifier
                        },
                    )
                    .nuvioFocusBorder(ctaShape)
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
                        role = Role.Button,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.action_back),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.colors.onAccent,
                )
            }
        }
    }
}

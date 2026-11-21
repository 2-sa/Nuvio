package com.nuvio.app.features.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.nuvioTypeScale
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_delete
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.action_resume
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.compose_action_pause
import nuvio.composeapp.generated.resources.cw_action_go_to_details
import nuvio.composeapp.generated.resources.downloads_status_failed
import nuvio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadActionSheet(
    item: DownloadItem?,
    watchStatus: DownloadWatchStatus,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleTransfer: () -> Unit,
    onOpenDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    if (item == null) return
    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    fun dismissAfter(action: () -> Unit) {
        action()
        coroutineScope.launch {
            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(tokens.spacing.screenHorizontal)),
        ) {
            DownloadSheetHeader(item = item, watchStatus = watchStatus)
            NuvioBottomSheetDivider()
            when (item.status) {
                DownloadStatus.Completed -> NuvioBottomSheetActionRow(
                    icon = Icons.Rounded.PlayArrow,
                    title = stringResource(
                        if (watchStatus.progress > 0f) Res.string.action_resume else Res.string.action_play,
                    ),
                    onClick = { dismissAfter(onPlay) },
                )
                DownloadStatus.Downloading -> NuvioBottomSheetActionRow(
                    icon = Icons.Rounded.Pause,
                    title = stringResource(Res.string.compose_action_pause),
                    onClick = { dismissAfter(onToggleTransfer) },
                )
                DownloadStatus.Paused -> NuvioBottomSheetActionRow(
                    icon = Icons.Rounded.Download,
                    title = stringResource(Res.string.action_resume),
                    onClick = { dismissAfter(onToggleTransfer) },
                )
                DownloadStatus.Failed -> NuvioBottomSheetActionRow(
                    icon = Icons.Rounded.Refresh,
                    title = stringResource(Res.string.action_retry),
                    onClick = { dismissAfter(onToggleTransfer) },
                )
            }
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.Info,
                title = stringResource(Res.string.cw_action_go_to_details),
                onClick = { dismissAfter(onOpenDetails) },
            )
            NuvioBottomSheetActionRow(
                icon = Icons.Rounded.DeleteOutline,
                title = stringResource(Res.string.action_delete),
                onClick = { dismissAfter(onDelete) },
            )
        }
    }
}

@Composable
private fun DownloadSheetHeader(
    item: DownloadItem,
    watchStatus: DownloadWatchStatus,
) {
    val tokens = MaterialTheme.nuvio
    val headerCardShape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.screenHorizontal, vertical = NuvioTokens.Space.s12)
            .clip(headerCardShape)
            .background(tokens.colors.surfaceCard)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, headerCardShape)
            .padding(NuvioTokens.Space.s12),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DownloadPoster(item = item)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.episodeLabel()?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val metaLine = listOfNotNull(
                formatBytes(item.sizeOnDiskBytes).takeIf { item.sizeOnDiskBytes > 0L },
                item.providerName.takeIf { it.isNotBlank() },
            ).joinToString(DownloadTextSeparator)
            if (metaLine.isNotBlank()) {
                Text(
                    text = metaLine,
                    style = MaterialTheme.nuvioTypeScale.bodySm,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.status == DownloadStatus.Failed || watchStatus.isWatched) {
                val isFailed = item.status == DownloadStatus.Failed
                val badgeShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .padding(top = NuvioTokens.Space.s2)
                        .clip(badgeShape)
                        .background(if (isFailed) tokens.colors.danger.copy(alpha = 0.16f) else tokens.colors.focusBackground)
                        .border(
                            NuvioTokens.Border.thin,
                            if (isFailed) tokens.colors.danger.copy(alpha = 0.5f) else tokens.colors.accent.copy(alpha = 0.6f),
                            badgeShape,
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (isFailed) Res.string.downloads_status_failed else Res.string.episodes_cd_watched,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFailed) tokens.colors.danger else tokens.colors.accent,
                    )
                }
            }
        }
    }
}

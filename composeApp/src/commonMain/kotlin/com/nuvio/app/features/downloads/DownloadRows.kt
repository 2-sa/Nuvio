package com.nuvio.app.features.downloads

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.gradientMask
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.core.ui.secondaryClickAt
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_resume
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.compose_action_pause
import nuvio.composeapp.generated.resources.details_actions_menu_label
import nuvio.composeapp.generated.resources.downloads_downloading_count
import nuvio.composeapp.generated.resources.downloads_episodes
import nuvio.composeapp.generated.resources.downloads_speed
import nuvio.composeapp.generated.resources.downloads_status_downloading
import nuvio.composeapp.generated.resources.downloads_status_failed
import nuvio.composeapp.generated.resources.downloads_status_paused
import nuvio.composeapp.generated.resources.episodes_cd_watched
import nuvio.composeapp.generated.resources.home_continue_watching_watched
import nuvio.composeapp.generated.resources.media_movie
import nuvio.composeapp.generated.resources.media_series
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ActiveDownloadRow(
    item: DownloadItem,
    speedBytesPerSecond: Long?,
    inShowPage: Boolean,
    onToggle: () -> Unit,
    onOpenActions: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val progress by animateFloatAsState(targetValue = item.progressFraction, label = "downloadProgress")
    DownloadRow(
        artwork = {
            DownloadThumbnail(imageUrl = item.landscapeArtwork())
        },
        title = if (inShowPage) item.episodeDisplayTitle() else item.title,
        subtitle = if (inShowPage) null else item.episodeLabel(),
        onClick = onOpenActions,
        onLongClick = onOpenActions,
        footer = {
            if (item.status == DownloadStatus.Failed) {
                StatusText(
                    text = item.errorMessage ?: stringResource(Res.string.downloads_status_failed),
                    color = tokens.colors.danger,
                )
            } else {
                StatusText(text = activeStatusText(item, speedBytesPerSecond))
            }
            Spacer(modifier = Modifier.height(6.dp))
            DownloadGradientProgressBar(
                item = item,
                progress = progress,
            )
        },
        trailing = {
            TransferButton(item = item, progress = progress, onClick = onToggle)
        },
    )
}

@Composable
private fun DownloadGradientProgressBar(
    item: DownloadItem,
    progress: Float,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val trackShape = RoundedCornerShape(999.dp)
    val isDownloading = item.status == DownloadStatus.Downloading
    val isIndeterminate = isDownloading && !item.hasKnownSize
    val fillBrush = when (item.status) {
        DownloadStatus.Failed -> SolidColor(tokens.colors.danger)
        DownloadStatus.Paused -> SolidColor(tokens.colors.textMuted.copy(alpha = 0.65f))
        DownloadStatus.Downloading, DownloadStatus.Completed -> palette.accentBrush()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(trackShape)
            .background(tokens.colors.surfaceElevated)
            .border(NuvioTokens.Border.hairline, tokens.colors.borderSubtle, trackShape),
    ) {
        if (isIndeterminate) {
            val transition = rememberInfiniteTransition(label = "downloadIndeterminateProgress")
            val travel by transition.animateFloat(
                initialValue = -0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "downloadIndeterminateTravel",
            )
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val segmentFraction = 0.35f
            val travelDp = maxWidth.value
            val offsetFraction = if (isRtl) -travel else travel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(segmentFraction)
                    .graphicsLayer {
                        translationX = offsetFraction * travelDp * density
                    }
                    .clip(trackShape)
                    .background(fillBrush),
            )
        } else {
            val clamped = progress.coerceIn(0f, 1f)
            if (clamped > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clamped)
                        .clip(trackShape)
                        .background(fillBrush),
                )
            }
        }
    }
}

@Composable
internal fun DownloadedMovieRow(
    item: DownloadItem,
    watchStatus: DownloadWatchStatus,
    onPlay: () -> Unit,
    onOpenActions: () -> Unit,
) {
    DownloadRow(
        artwork = {
            DownloadPoster(item = item)
        },
        title = item.title,
        subtitle = listOf(
            stringResource(Res.string.media_movie),
            formatBytes(item.sizeOnDiskBytes),
        ).joinToString(DownloadTextSeparator),
        onClick = onPlay,
        onLongClick = onOpenActions,
        footer = {
            watchStatusText(watchStatus)?.let { StatusText(text = it) }
        },
        trailing = {
            MoreActionsButton(onClick = onOpenActions)
        },
    )
}

@Composable
internal fun DownloadedShowRow(
    cover: DownloadItem,
    episodeCount: Int,
    sizeBytes: Long,
    downloadingCount: Int,
    onOpen: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    DownloadRow(
        artwork = {
            DownloadPoster(item = cover)
        },
        title = cover.title,
        subtitle = listOf(
            stringResource(Res.string.media_series),
            pluralStringResource(Res.plurals.downloads_episodes, episodeCount, episodeCount),
            formatBytes(sizeBytes),
        ).joinToString(DownloadTextSeparator),
        onClick = onOpen,
        onLongClick = null,
        footer = {
            if (downloadingCount > 0) {
                StatusText(
                    text = stringResource(Res.string.downloads_downloading_count, downloadingCount),
                    color = tokens.colors.accent,
                )
            }
        },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens.colors.textMuted,
                modifier = Modifier.padding(horizontal = NuvioTokens.Space.s8),
            )
        },
    )
}

@Composable
internal fun DownloadedEpisodeRow(
    item: DownloadItem,
    watchStatus: DownloadWatchStatus,
    onPlay: () -> Unit,
    onOpenActions: () -> Unit,
) {
    DownloadRow(
        artwork = {
            DownloadThumbnail(
                imageUrl = item.landscapeArtwork(),
                watchProgress = watchStatus.progress,
            )
        },
        title = item.episodeDisplayTitle(),
        subtitle = listOfNotNull(
            formatBytes(item.sizeOnDiskBytes),
            watchStatusText(watchStatus),
        ).joinToString(DownloadTextSeparator),
        onClick = onPlay,
        onLongClick = onOpenActions,
        trailing = {
            MoreActionsButton(onClick = onOpenActions)
        },
    )
}

@Composable
private fun DownloadRow(
    artwork: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    footer: @Composable ColumnScope.() -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowShape = RoundedCornerShape(14.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent.copy(alpha = 0.6f)
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "downloadRowBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.surfaceElevated
        } else {
            tokens.colors.surfaceCard
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "downloadRowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(containerColor)
            .border(NuvioTokens.Border.thin, borderColor, rowShape)
            .nuvioFocusBorder(rowShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (isDesktop) null else onLongClick,
            )
            .secondaryClickAt(
                if (isDesktop && onLongClick != null) {
                    { onLongClick() }
                } else {
                    null
                },
            )
            .padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s12),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        artwork()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s3),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.nuvioTypeScale.bodySm,
                    color = tokens.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            footer()
        }
        trailing()
    }
}

@Composable
private fun TransferButton(
    item: DownloadItem,
    progress: Float,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    IconButton(
        onClick = onClick,
        modifier = if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier,
    ) {
        if (item.status == DownloadStatus.Failed) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(Res.string.action_retry),
                tint = tokens.colors.danger,
            )
        } else {
            val isDownloading = item.status == DownloadStatus.Downloading
            val trackColor = tokens.colors.borderStrong
            val strokeWidth = NuvioTokens.Space.s2
            val ringColor = if (isDownloading) Color.White else tokens.colors.textMuted
            val ringModifier = Modifier
                .size(NuvioTokens.Space.s32)
                .then(
                    if (isDownloading) {
                        Modifier.gradientMask(MaterialTheme.themePalette.accentBrush())
                    } else {
                        Modifier
                    },
                )
            Box(
                modifier = Modifier
                    .size(NuvioTokens.Space.s32)
                    .drawBehind {
                        val strokePx = strokeWidth.toPx()
                        drawCircle(
                            color = trackColor,
                            radius = (size.minDimension - strokePx) / 2f,
                            style = Stroke(width = strokePx),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isDownloading && !item.hasKnownSize) {
                    CircularProgressIndicator(
                        modifier = ringModifier,
                        color = ringColor,
                        strokeWidth = strokeWidth,
                        trackColor = Color.Transparent,
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = ringModifier,
                        color = ringColor,
                        strokeWidth = strokeWidth,
                        trackColor = Color.Transparent,
                        gapSize = NuvioTokens.Space.none,
                    )
                }
                Icon(
                    imageVector = if (isDownloading) Icons.Rounded.Pause else Icons.Rounded.Download,
                    contentDescription = stringResource(
                        if (isDownloading) Res.string.compose_action_pause else Res.string.action_resume,
                    ),
                    tint = tokens.colors.textPrimary,
                    modifier = Modifier.size(NuvioTokens.Icon.sm),
                )
            }
        }
    }
}

@Composable
private fun MoreActionsButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier,
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(Res.string.details_actions_menu_label),
            tint = MaterialTheme.nuvio.colors.textSecondary,
        )
    }
}

@Composable
private fun StatusText(
    text: String,
    color: Color = MaterialTheme.nuvio.colors.textMuted,
) {
    Text(
        text = text,
        style = MaterialTheme.nuvioTypeScale.bodySm,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun activeStatusText(item: DownloadItem, speedBytesPerSecond: Long?): String {
    val total = item.totalBytes?.takeIf { it > 0L }
    val transferred = if (total != null) {
        "${formatBytes(item.downloadedBytes)} / ${formatBytes(total)}"
    } else {
        formatBytes(item.downloadedBytes)
    }
    if (item.status == DownloadStatus.Paused) {
        return stringResource(Res.string.downloads_status_paused, transferred)
    }
    val percent = "${(item.progressFraction * 100f).toInt()}%"
    val speed = speedBytesPerSecond?.takeIf { it > 0L }
    if (speed == null) {
        return if (total != null) {
            listOf(percent, transferred).joinToString(DownloadTextSeparator)
        } else {
            stringResource(Res.string.downloads_status_downloading, transferred)
        }
    }
    val speedText = stringResource(Res.string.downloads_speed, formatBytes(speed))
    if (total == null) {
        return listOf(transferred, speedText).joinToString(DownloadTextSeparator)
    }
    val timeLeft = formatTimeLeft((total - item.downloadedBytes).coerceAtLeast(0L) * 1_000L / speed)
    return listOf(percent, speedText, timeLeft).joinToString(DownloadTextSeparator)
}

@Composable
private fun watchStatusText(status: DownloadWatchStatus): String? = when {
    status.isWatched -> stringResource(Res.string.episodes_cd_watched)
    status.remainingMs > 0L -> formatTimeLeft(status.remainingMs)
    status.progress > 0f -> stringResource(
        Res.string.home_continue_watching_watched,
        "${(status.progress * 100f).toInt()}%",
    )
    else -> null
}

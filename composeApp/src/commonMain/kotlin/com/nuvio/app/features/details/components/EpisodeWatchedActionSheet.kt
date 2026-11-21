package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAddCheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DesktopContextMenuPopup
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterZoomAnchorHolder
import com.nuvio.app.core.ui.PosterZoomOverlayAction
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingRatingState
import com.nuvio.app.isDesktop
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.i18n.localizedEpisodeTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeWatchedActionSheet(
    episode: MetaVideo,
    seasonLabel: String,
    isEpisodeWatched: Boolean,
    canMarkPreviousEpisodes: Boolean,
    arePreviousEpisodesWatched: Boolean,
    isSeasonWatched: Boolean,
    onDismiss: () -> Unit,
    onToggleWatched: () -> Unit,
    onTogglePreviousWatched: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    showPlayManually: Boolean = false,
    onPlayManually: (() -> Unit)? = null,
    ratingMedia: TrackingMediaReference? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val ratingState = ratingMedia?.let { rememberRatingState(it).value } ?: TrackingRatingState()
    var showRating by remember { mutableStateOf(false) }
    val desktopAnchor = remember { if (isDesktop) PosterZoomAnchorHolder.consume() else null }
    if (isDesktop) {
        DisposableEffect(Unit) {
            if (desktopAnchor != null) {
                PosterZoomOverlayCoordinator.show()
            }
            onDispose {
                if (desktopAnchor != null) {
                    PosterZoomOverlayCoordinator.hide()
                }
            }
        }
    }
    val dismiss: () -> Unit = {
        coroutineScope.launch {
            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
        }
    }

    val episodeTitleText = localizedEpisodeTitle(episode.title, episode.episode) ?: episode.title
    val episodeSubtitleText = buildString {
        localizedSeasonEpisodeCode(
            seasonNumber = episode.season,
            episodeNumber = episode.episode,
        )?.let {
            append(it)
            append(" • ")
        }
        append(seasonLabel)
    }

    if (isDesktop && !showRating) {
        val markWatchedTitle = if (isEpisodeWatched) {
            stringResource(Res.string.episode_mark_unwatched)
        } else {
            stringResource(Res.string.episode_mark_watched)
        }
        val markPreviousTitle = if (arePreviousEpisodesWatched) {
            stringResource(Res.string.episode_mark_previous_unwatched)
        } else {
            stringResource(Res.string.episode_mark_previous_watched)
        }
        val markSeasonTitle = if (isSeasonWatched) {
            stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
        } else {
            stringResource(Res.string.episode_mark_season_watched, seasonLabel)
        }
        val rateEpisodeTitle = stringResource(Res.string.rating_rate_episode)
        val playManuallyTitle = stringResource(Res.string.play_manually)
        val rated = ratingState.rating
        val desktopActions = buildList {
            add(
                PosterZoomOverlayAction(
                    icon = Icons.Default.CheckCircle,
                    label = markWatchedTitle,
                    onSelected = onToggleWatched,
                ),
            )
            if (canMarkPreviousEpisodes) {
                add(
                    PosterZoomOverlayAction(
                        icon = Icons.Default.DoneAll,
                        label = markPreviousTitle,
                        onSelected = onTogglePreviousWatched,
                    ),
                )
            }
            add(
                PosterZoomOverlayAction(
                    icon = Icons.Default.PlaylistAddCheckCircle,
                    label = markSeasonTitle,
                    onSelected = onToggleSeasonWatched,
                ),
            )
            if (ratingState.isAvailable && ratingMedia != null) {
                add(
                    PosterZoomOverlayAction(
                        icon = if (rated != null) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        label = rateEpisodeTitle,
                        trailingText = rated?.let(::formatRating),
                        dismissOnSelect = false,
                        onSelected = { showRating = true },
                    ),
                )
            }
            if (showPlayManually && onPlayManually != null) {
                add(
                    PosterZoomOverlayAction(
                        icon = Icons.Default.PlayArrow,
                        label = playManuallyTitle,
                        onSelected = onPlayManually,
                    ),
                )
            }
        }
        val pointerPos = remember(desktopAnchor) {
            desktopAnchor?.pointerPositionInRoot ?: desktopAnchor?.boundsInRoot?.center
        }
        DesktopContextMenuPopup(
            title = episodeTitleText,
            subtitle = episodeSubtitleText,
            pointerPositionInRoot = pointerPos,
            actions = desktopActions,
            onDismissed = onDismiss,
        )
        return
    }

    val tokens = MaterialTheme.nuvio
    NuvioModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        containerColor = tokens.colors.surfaceSheet,
        contentColor = tokens.colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            EpisodeActionSheetHeader(
                episode = episode,
                seasonLabel = seasonLabel,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenHorizontal),
                thickness = NuvioTokens.Border.thin,
                color = tokens.colors.borderSubtle,
            )
            AnimatedContent(
                targetState = showRating && ratingMedia != null,
                transitionSpec = {
                    val forward = targetState
                    val motion = spring<IntOffset>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                    (slideInHorizontally(motion) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(motion) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(120))) using
                        SizeTransform(clip = false)
                },
                label = "episode_sheet_page",
            ) { rating ->
                if (rating && ratingMedia != null) {
                    RatingPanel(
                        media = ratingMedia,
                        state = ratingState,
                        onFinished = dismiss,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                } else {
                    val groupShape = RoundedCornerShape(NuvioTokens.Radius.lg)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s12)
                            .clip(groupShape)
                            .background(tokens.colors.surfaceElevated)
                            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, groupShape)
                            .padding(vertical = NuvioTokens.Space.s4),
                    ) {
                        NuvioBottomSheetActionRow(
                            icon = Icons.Default.CheckCircle,
                            title = if (isEpisodeWatched) {
                                stringResource(Res.string.episode_mark_unwatched)
                            } else {
                                stringResource(Res.string.episode_mark_watched)
                            },
                            selected = isEpisodeWatched,
                            onClick = {
                                onToggleWatched()
                                dismiss()
                            },
                        )
                        if (canMarkPreviousEpisodes) {
                            SheetActionRowDivider()
                            NuvioBottomSheetActionRow(
                                icon = Icons.Default.DoneAll,
                                title = if (arePreviousEpisodesWatched) {
                                    stringResource(Res.string.episode_mark_previous_unwatched)
                                } else {
                                    stringResource(Res.string.episode_mark_previous_watched)
                                },
                                selected = arePreviousEpisodesWatched,
                                onClick = {
                                    onTogglePreviousWatched()
                                    dismiss()
                                },
                            )
                        }
                        SheetActionRowDivider()
                        NuvioBottomSheetActionRow(
                            icon = Icons.Default.PlaylistAddCheckCircle,
                            title = if (isSeasonWatched) {
                                stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                            } else {
                                stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                            },
                            selected = isSeasonWatched,
                            onClick = {
                                onToggleSeasonWatched()
                                dismiss()
                            },
                        )
                        if (ratingState.isAvailable) {
                            val rated = ratingState.rating
                            SheetActionRowDivider()
                            NuvioBottomSheetActionRow(
                                icon = if (rated != null) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                title = stringResource(Res.string.rating_rate_episode),
                                selected = rated != null,
                                onClick = { showRating = true },
                                trailingContent = if (rated != null) {
                                    {
                                        Text(
                                            text = formatRating(rated),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = tokens.colors.accent,
                                        )
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        if (showPlayManually && onPlayManually != null) {
                            SheetActionRowDivider()
                            NuvioBottomSheetActionRow(
                                icon = Icons.Default.PlayArrow,
                                title = stringResource(Res.string.play_manually),
                                onClick = {
                                    onPlayManually()
                                    dismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonWatchedActionSheet(
    seasonLabel: String,
    isSeasonWatched: Boolean,
    canMarkPreviousSeasons: Boolean,
    onDismiss: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onMarkPreviousSeasonsWatched: () -> Unit,
) {
    val desktopAnchor = remember { if (isDesktop) PosterZoomAnchorHolder.consume() else null }
    if (isDesktop) {
        DisposableEffect(Unit) {
            if (desktopAnchor != null) {
                PosterZoomOverlayCoordinator.show()
            }
            onDispose {
                if (desktopAnchor != null) {
                    PosterZoomOverlayCoordinator.hide()
                }
            }
        }
        val markSeasonTitle = if (isSeasonWatched) {
            stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
        } else {
            stringResource(Res.string.episode_mark_season_watched, seasonLabel)
        }
        val markPrevSeasonsTitle = stringResource(Res.string.episode_mark_previous_seasons_watched)
        val actions = buildList {
            add(
                PosterZoomOverlayAction(
                    icon = Icons.Default.PlaylistAddCheckCircle,
                    label = markSeasonTitle,
                    onSelected = onToggleSeasonWatched,
                ),
            )
            if (canMarkPreviousSeasons) {
                add(
                    PosterZoomOverlayAction(
                        icon = Icons.Default.DoneAll,
                        label = markPrevSeasonsTitle,
                        onSelected = onMarkPreviousSeasonsWatched,
                    ),
                )
            }
        }
        val pointerPos = remember(desktopAnchor) {
            desktopAnchor?.pointerPositionInRoot ?: desktopAnchor?.boundsInRoot?.center
        }
        DesktopContextMenuPopup(
            title = seasonLabel,
            subtitle = null,
            pointerPositionInRoot = pointerPos,
            actions = actions,
            onDismissed = onDismiss,
        )
        return
    }

    val tokens = MaterialTheme.nuvio
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
        containerColor = tokens.colors.surfaceSheet,
        contentColor = tokens.colors.textPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            Text(
                text = seasonLabel,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = tokens.spacing.screenHorizontal),
                thickness = NuvioTokens.Border.thin,
                color = tokens.colors.borderSubtle,
            )
            val groupShape = RoundedCornerShape(NuvioTokens.Radius.lg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s12)
                    .clip(groupShape)
                    .background(tokens.colors.surfaceElevated)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, groupShape)
                    .padding(vertical = NuvioTokens.Space.s4),
            ) {
                NuvioBottomSheetActionRow(
                    icon = Icons.Default.PlaylistAddCheckCircle,
                    title = if (isSeasonWatched) {
                        stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                    } else {
                        stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                    },
                    selected = isSeasonWatched,
                    onClick = {
                        onToggleSeasonWatched()
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                )
                if (canMarkPreviousSeasons) {
                    SheetActionRowDivider()
                    NuvioBottomSheetActionRow(
                        icon = Icons.Default.DoneAll,
                        title = stringResource(Res.string.episode_mark_previous_seasons_watched),
                        onClick = {
                            onMarkPreviousSeasonsWatched()
                            coroutineScope.launch {
                                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActionRowDivider() {
    val tokens = MaterialTheme.nuvio
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = NuvioTokens.Space.s12),
        thickness = NuvioTokens.Border.thin,
        color = tokens.colors.borderSubtle,
    )
}

@Composable
private fun EpisodeActionSheetHeader(
    episode: MetaVideo,
    seasonLabel: String,
) {
    val tokens = MaterialTheme.nuvio
    val codeLabel = localizedSeasonEpisodeCode(
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = localizedEpisodeTitle(episode.title, episode.episode) ?: episode.title,
            style = MaterialTheme.typography.titleLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(NuvioTokens.Radius.sm))
                .background(tokens.colors.surfaceElevated)
                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, RoundedCornerShape(NuvioTokens.Radius.sm))
                .padding(horizontal = NuvioTokens.Space.s8, vertical = NuvioTokens.Space.s4),
        ) {
            Text(
                text = buildString {
                    codeLabel?.let {
                        append(it)
                        append(" • ")
                    }
                    append(seasonLabel)
                },
                style = MaterialTheme.typography.labelMedium,
                color = tokens.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

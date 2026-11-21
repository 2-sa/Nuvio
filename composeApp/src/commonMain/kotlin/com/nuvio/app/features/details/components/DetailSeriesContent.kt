package com.nuvio.app.features.details.components

import com.nuvio.app.core.ui.rememberAnimatedArtworkAnimation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import co.touchlab.kermit.Logger
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.core.ui.NuvioAnimatedWatchedBadge
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterZoomAnchor
import com.nuvio.app.core.ui.PosterZoomAnchorHolder
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.captureSecondaryClickRootOffset
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.secondaryClick
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.isDesktop
import com.nuvio.app.features.details.EpisodeRatingsVisibility
import com.nuvio.app.features.details.MetaEpisodeCardStyle
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.SeasonViewMode
import com.nuvio.app.features.details.SeasonViewModeStorage
import com.nuvio.app.features.details.formatRuntimeFromMinutes
import com.nuvio.app.features.details.groupedEpisodesForDisplay
import com.nuvio.app.features.details.preferredEpisodeNumberForSeason
import com.nuvio.app.features.details.seasonSortKey
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.buildPlaybackVideoId
import com.nuvio.app.features.watching.application.WatchingState
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.nuvio.app.core.i18n.localizedEpisodeTitle

private val log = Logger.withTag("SeriesContent")

@Composable
fun DetailSeriesContent(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
    preferredSeasonNumber: Int? = null,
    preferredEpisodeNumber: Int? = null,
    episodeCardStyle: MetaEpisodeCardStyle = MetaEpisodeCardStyle.Horizontal,
    progressByVideoId: Map<String, WatchProgressEntry> = emptyMap(),
    watchedKeys: Set<String> = emptySet(),
    episodeRatings: Map<Pair<Int, Int>, Double> = emptyMap(),
    episodeRatingsVisibility: EpisodeRatingsVisibility = EpisodeRatingsVisibility.SHOW_ALL,
    blurUnwatchedEpisodes: Boolean = false,
    onEpisodeClick: ((MetaVideo) -> Unit)? = null,
    onEpisodeLongPress: ((MetaVideo) -> Unit)? = null,
    onSeasonLongPress: ((Int) -> Unit)? = null,
) {
    val hasVideos = meta.videos.isNotEmpty()
    if (meta.type != "series" && !hasVideos) return

    if (meta.videos.isEmpty()) {
        DetailSection(
            title = stringResource(Res.string.settings_meta_episodes),
            modifier = modifier,
            showHeader = showHeader,
        ) {
            Text(
                text = when {
                    meta.status.equals("Not yet aired", ignoreCase = true) || meta.hasScheduledVideos ->
                        stringResource(Res.string.details_series_unpublished)
                    else ->
                        stringResource(Res.string.details_series_no_metadata)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val groupedEpisodes = remember(meta.videos) {
        log.d { "videos count=${meta.videos.size}, type=${meta.type}" }
        val withSeasonOrEp = meta.videos.filter { it.season != null || it.episode != null }
        log.d { "videos with season/episode=${withSeasonOrEp.size}" }
        if (meta.videos.isNotEmpty() && withSeasonOrEp.isEmpty()) {
            log.w { "All videos lack season/episode fields! First: ${meta.videos.first()}" }
        }
        meta.groupedEpisodesForDisplay()
    }

    if (groupedEpisodes.isEmpty()) {
        if (meta.type == "series") {
            DetailSection(
                title = stringResource(Res.string.settings_meta_episodes),
                modifier = modifier,
                showHeader = showHeader,
            ) {
                Text(
                    text = stringResource(Res.string.details_series_missing_numbers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val seasons = groupedEpisodes.keys.sortedBy(::seasonSortKey)
    val defaultSeason = preferredSeasonNumber
        ?.takeIf { it in groupedEpisodes }
        ?: seasons.first()
    var selectedSeasonOverride by rememberSaveable(meta.id) { mutableStateOf<Int?>(null) }
    val currentSeason = selectedSeasonOverride
        ?.takeIf { it in groupedEpisodes }
        ?: defaultSeason

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sizing = seriesContentSizing(maxWidth.value)
        val containerWidthDp = maxWidth.value

        Column(
            verticalArrangement = Arrangement.spacedBy(if (isDesktop) 12.dp else 16.dp),
        ) {
            SeriesSeasonSelector(
                meta = meta,
                seasons = seasons,
                groupedEpisodes = groupedEpisodes,
                currentSeason = currentSeason,
                sizing = sizing,
                horizontalScrollPadding = horizontalScrollPadding,
                onSelect = { selectedSeasonOverride = it },
                onLongPress = onSeasonLongPress,
            )

            AnimatedContent(
                targetState = currentSeason,
                transitionSpec = {
                    if (isDesktop) {
                        fadeIn(tween(120)).togetherWith(fadeOut(tween(100)))
                    } else {
                        val fromIdx = seasons.indexOf(initialState).takeIf { it >= 0 } ?: 0
                        val toIdx = seasons.indexOf(targetState).takeIf { it >= 0 } ?: 0
                        val dir = if (toIdx >= fromIdx) 1 else -1
                        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { dir * it / 5 })
                            .togetherWith(
                                fadeOut(tween(170)) + slideOutHorizontally(tween(170)) { -dir * it / 5 },
                            )
                    }
                },
                label = "season_episodes",
            ) { seasonForContent ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!isDesktop || seasons.size <= 1) {
                        DetailSectionTitle(
                            title = if (meta.type != "series" && seasonForContent <= 0) {
                                stringResource(Res.string.details_videos)
                            } else {
                                seasonForContent.label()
                            },
                        )
                    }
                    val seasonEpisodes = groupedEpisodes.getValue(seasonForContent)
                    if (episodeCardStyle == MetaEpisodeCardStyle.Horizontal) {
                        EpisodeHorizontalRow(
                            episodes = seasonEpisodes,
                            maxWidthDp = containerWidthDp,
                            horizontalScrollPadding = horizontalScrollPadding,
                            parentMetaId = meta.id,
                            metaType = meta.type,
                            watchedKeys = watchedKeys,
                            fallbackImage = meta.background ?: meta.poster,
                            progressByVideoId = progressByVideoId,
                            episodeRatings = episodeRatings,
                            episodeRatingsVisibility = episodeRatingsVisibility,
                            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                            preferredEpisodeNumber = preferredEpisodeNumberForSeason(
                                displayedSeasonNumber = seasonForContent,
                                preferredSeasonNumber = preferredSeasonNumber,
                                preferredEpisodeNumber = preferredEpisodeNumber,
                            ),
                            onEpisodeClick = onEpisodeClick,
                            onEpisodeLongPress = onEpisodeLongPress,
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(sizing.cardGap),
                        ) {
                            seasonEpisodes.forEach { episode ->
                                val episodeVideoId = buildPlaybackVideoId(
                                    parentMetaId = meta.id,
                                    seasonNumber = episode.season,
                                    episodeNumber = episode.episode,
                                    fallbackVideoId = episode.id,
                                )
                                EpisodeListCard(
                                    video = episode,
                                    fallbackImage = meta.background ?: meta.poster,
                                    progressEntry = progressByVideoId[episodeVideoId],
                                    imdbRating = episode.seasonEpisodeKey()?.let { episodeRatings[it] } ?: episode.rating,
                                    isWatched = progressByVideoId[episodeVideoId]?.isEffectivelyCompleted == true ||
                                        WatchingState.isEpisodeWatched(
                                            watchedKeys = watchedKeys,
                                            metaType = meta.type,
                                            metaId = meta.id,
                                            episode = episode,
                                        ),
                                    episodeRatingsVisibility = episodeRatingsVisibility,
                                    blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                                    sizing = sizing,
                                    onClick = { onEpisodeClick?.invoke(episode) },
                                    onLongPress = { onEpisodeLongPress?.invoke(episode) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailSeriesListHeader(
    meta: MetaDetails,
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    seasons: List<Int>,
    currentSeason: Int,
    horizontalScrollPadding: Dp,
    onSeasonSelect: (Int) -> Unit,
    onSeasonLongPress: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sizing = seriesContentSizing(maxWidth.value)
        Column(verticalArrangement = Arrangement.spacedBy(if (isDesktop) 12.dp else 16.dp)) {
            SeriesSeasonSelector(
                meta = meta,
                seasons = seasons,
                groupedEpisodes = groupedEpisodes,
                currentSeason = currentSeason,
                sizing = sizing,
                horizontalScrollPadding = horizontalScrollPadding,
                onSelect = onSeasonSelect,
                onLongPress = onSeasonLongPress,
            )
            if (seasons.size == 1) {
                DetailSectionTitle(
                    title = if (meta.type != "series" && currentSeason <= 0) {
                        stringResource(Res.string.details_videos)
                    } else {
                        currentSeason.label()
                    },
                )
            }
        }
    }
}

@Composable
internal fun DetailSeriesListEpisode(
    meta: MetaDetails,
    episode: MetaVideo,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    episodeRatingsVisibility: EpisodeRatingsVisibility,
    blurUnwatchedEpisodes: Boolean,
    onEpisodeClick: ((MetaVideo) -> Unit)?,
    onEpisodeLongPress: ((MetaVideo) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sizing = seriesContentSizing(maxWidth.value)
        val episodeVideoId = buildPlaybackVideoId(
            parentMetaId = meta.id,
            seasonNumber = episode.season,
            episodeNumber = episode.episode,
            fallbackVideoId = episode.id,
        )
        EpisodeListCard(
            video = episode,
            fallbackImage = meta.background ?: meta.poster,
            progressEntry = progressByVideoId[episodeVideoId],
            imdbRating = episode.seasonEpisodeKey()?.let { episodeRatings[it] } ?: episode.rating,
            isWatched = progressByVideoId[episodeVideoId]?.isEffectivelyCompleted == true ||
                WatchingState.isEpisodeWatched(
                    watchedKeys = watchedKeys,
                    metaType = meta.type,
                    metaId = meta.id,
                    episode = episode,
                ),
            episodeRatingsVisibility = episodeRatingsVisibility,
            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
            sizing = sizing,
            onClick = { onEpisodeClick?.invoke(episode) },
            onLongPress = { onEpisodeLongPress?.invoke(episode) },
        )
    }
}

@Composable
private fun SeriesSeasonSelector(
    meta: MetaDetails,
    seasons: List<Int>,
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    currentSeason: Int,
    sizing: SeriesContentSizing,
    horizontalScrollPadding: Dp,
    onSelect: (Int) -> Unit,
    onLongPress: ((Int) -> Unit)?,
) {
    if (seasons.size <= 1) return

    var seasonViewMode by remember {
        mutableStateOf(SeasonViewModeStorage.load() ?: SeasonViewMode.Posters)
    }
    val hasSeasonPosters = seasons.any { season ->
        resolveSeasonPoster(
            season = season,
            groupedEpisodes = groupedEpisodes,
            meta = meta,
        ) != null
    }
    Column(
        modifier = Modifier.animateContentSize(animationSpec = tween(280)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isDesktop) {
                Arrangement.spacedBy(12.dp)
            } else {
                Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.details_seasons),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = sizing.seasonHeaderSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (hasSeasonPosters) {
                SeasonViewModeToggle(
                    mode = seasonViewMode,
                    sizing = sizing,
                    onClick = {
                        val next = seasonViewMode.toggled()
                        seasonViewMode = next
                        SeasonViewModeStorage.save(next)
                    },
                )
            }
        }

        if (hasSeasonPosters) {
            Crossfade(
                targetState = seasonViewMode,
                animationSpec = tween(280),
                label = "season_selector_layout",
            ) { mode ->
                when (mode) {
                    SeasonViewMode.Posters -> SeasonPosterScrollRow(
                        seasons = seasons,
                        groupedEpisodes = groupedEpisodes,
                        meta = meta,
                        currentSeason = currentSeason,
                        sizing = sizing,
                        horizontalScrollPadding = horizontalScrollPadding,
                        onSelect = onSelect,
                        onLongPress = onLongPress,
                    )
                    SeasonViewMode.Text -> SeasonTextChipScrollRow(
                        seasons = seasons,
                        currentSeason = currentSeason,
                        sizing = sizing,
                        horizontalScrollPadding = horizontalScrollPadding,
                        onSelect = onSelect,
                        onLongPress = onLongPress,
                    )
                }
            }
        } else {
            SeasonTextChipScrollRow(
                seasons = seasons,
                currentSeason = currentSeason,
                sizing = sizing,
                horizontalScrollPadding = horizontalScrollPadding,
                onSelect = onSelect,
                onLongPress = onLongPress,
            )
        }
    }
}

@Composable
private fun SeasonViewModeToggle(
    mode: SeasonViewMode,
    sizing: SeriesContentSizing,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val isPosters = mode == SeasonViewMode.Posters
    val shape = RoundedCornerShape(NuvioTokens.Radius.md)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val containerColor = if (isDesktop && isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated
    val borderColor = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderDefault
    val textColor = if (isDesktop && isHovered) tokens.colors.textPrimary else tokens.colors.textSecondary

    Box(
        modifier = Modifier
            .nuvioFocusBorder(shape)
            .clip(shape)
            .background(containerColor)
            .border(
                width = NuvioTokens.Border.thin,
                color = borderColor,
                shape = shape,
            )
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isPosters) {
                stringResource(Res.string.details_season_view_posters)
            } else {
                stringResource(Res.string.details_season_view_text)
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = sizing.seasonToggleTextSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = textColor,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeasonTextChipScrollRow(
    seasons: List<Int>,
    currentSeason: Int,
    sizing: SeriesContentSizing,
    horizontalScrollPadding: Dp,
    onSelect: (Int) -> Unit,
    onLongPress: ((Int) -> Unit)?,
) {
    val seasonListState = rememberLazyListState()
    var hasPositionedSeasonRow by remember(seasons) { mutableStateOf(false) }

    LaunchedEffect(seasons, currentSeason) {
        val currentIndex = seasons.indexOf(currentSeason)
        if (currentIndex >= 0) {
            if (hasPositionedSeasonRow) {
                seasonListState.animateScrollToItem(currentIndex)
            } else {
                seasonListState.scrollToItem(currentIndex)
                hasPositionedSeasonRow = true
            }
        }
    }

    LazyRow(
        state = seasonListState,
        modifier = Modifier
            .nuvioHorizontalScrollBleed(horizontalScrollPadding)
            .fillMaxWidth()
            .nuvioDesktopDragScroll(seasonListState)
            .nuvioDesktopWheelHorizontalScroll(seasonListState)
            .let { if (isDesktop) it.padding(bottom = 4.dp) else it },
        contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
        horizontalArrangement = Arrangement.spacedBy(sizing.seasonChipGap),
    ) {
        items(seasons, key = { season -> season }) { season ->
            val tokens = MaterialTheme.nuvio
            val isSelected = season == currentSeason
            val chipShape = RoundedCornerShape(sizing.seasonChipRadius)
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val chipBackground by animateColorAsState(
                targetValue = when {
                    isSelected -> tokens.colors.focusBackground
                    isDesktop && isHovered -> tokens.colors.surfaceCard
                    else -> tokens.colors.surfaceElevated
                },
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "season_chip_bg",
            )
            val chipBorder by animateColorAsState(
                targetValue = when {
                    isSelected -> tokens.colors.accent
                    isDesktop && isHovered -> tokens.colors.borderStrong
                    else -> tokens.colors.borderDefault
                },
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "season_chip_border",
            )
            var chipBounds by remember(season) { mutableStateOf<Rect?>(null) }
            val onSecondaryClick = onLongPress?.let { handler ->
                {
                    val pointerOffset = PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
                    chipBounds?.let { bounds ->
                        PosterZoomAnchorHolder.stash(
                            PosterZoomAnchor(
                                boundsInRoot = bounds,
                                imageUrl = null,
                                cornerRadius = sizing.seasonChipRadius,
                                pointerPositionInRoot = pointerOffset,
                            ),
                        )
                    }
                    handler(season)
                }
            }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        chipBounds = Rect(
                            left = position.x,
                            top = position.y,
                            right = position.x + coordinates.size.width,
                            bottom = position.y + coordinates.size.height,
                        )
                    }
                    .clip(chipShape)
                    .background(chipBackground)
                    .border(
                        width = NuvioTokens.Border.thin,
                        color = chipBorder,
                        shape = chipShape,
                    )
                    .nuvioFocusBorder(chipShape)
                    .then(
                        if (isDesktop) {
                            Modifier
                                .hoverable(interactionSource)
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .captureSecondaryClickRootOffset(enabled = onSecondaryClick != null)
                    .combinedClickable(
                        onClick = {
                            PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
                            onSelect(season)
                        },
                        onLongClick = onSecondaryClick,
                    )
                    .secondaryClick(onSecondaryClick)
                    .padding(
                        horizontal = sizing.seasonChipHorizontalPadding,
                        vertical = sizing.seasonChipVerticalPadding,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = season.label(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = sizing.seasonChipTextSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isSelected || (isDesktop && isHovered)) {
                        tokens.colors.textPrimary
                    } else {
                        tokens.colors.textSecondary
                    },
                )
            }
        }
    }
}

@Composable
private fun SeasonPosterScrollRow(
    seasons: List<Int>,
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    meta: MetaDetails,
    currentSeason: Int,
    sizing: SeriesContentSizing,
    horizontalScrollPadding: Dp,
    onSelect: (Int) -> Unit,
    onLongPress: ((Int) -> Unit)?,
) {
    val seasonListState = rememberLazyListState()
    var hasPositionedSeasonRow by remember(seasons) { mutableStateOf(false) }

    LaunchedEffect(seasons, currentSeason) {
        val currentIndex = seasons.indexOf(currentSeason)
        if (currentIndex >= 0) {
            if (hasPositionedSeasonRow) {
                seasonListState.animateScrollToItem(currentIndex)
            } else {
                seasonListState.scrollToItem(currentIndex)
                hasPositionedSeasonRow = true
            }
        }
    }

    LazyRow(
        state = seasonListState,
        modifier = Modifier
            .nuvioHorizontalScrollBleed(horizontalScrollPadding)
            .fillMaxWidth()
            .nuvioDesktopDragScroll(seasonListState)
            .nuvioDesktopWheelHorizontalScroll(seasonListState)
            .let { if (isDesktop) it.padding(bottom = 4.dp) else it },
        contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
        horizontalArrangement = Arrangement.spacedBy(sizing.seasonChipGap),
    ) {
        items(seasons, key = { season -> season }) { season ->
            SeasonPosterButton(
                label = season.label(),
                imageUrl = resolveSeasonPoster(
                    season = season,
                    groupedEpisodes = groupedEpisodes,
                    meta = meta,
                )
                    ?: meta.poster
                    ?: meta.background,
                isSelected = season == currentSeason,
                sizing = sizing,
                onClick = { onSelect(season) },
                onLongClick = onLongPress?.let { handler -> { handler(season) } },
            )
        }
    }
}

private fun resolveSeasonPoster(
    season: Int,
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    meta: MetaDetails,
): String? = groupedEpisodes[season]
    .orEmpty()
    .firstNotNullOfOrNull { episode -> episode.seasonPoster?.takeIf(String::isNotBlank) }
    ?: meta.seasonPosters[season]?.takeIf(String::isNotBlank)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeasonPosterButton(
    label: String,
    imageUrl: String?,
    isSelected: Boolean,
    sizing: SeriesContentSizing,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val posterShape = RoundedCornerShape(sizing.seasonPosterRadius)
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> tokens.colors.accent
            isDesktop && isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "season_poster_border",
    )

    Column(
        modifier = Modifier
            .width(sizing.seasonPosterWidth)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .nuvioFocusBorder(posterShape)
            .posterCardClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sizing.seasonPosterHeight)
                .clip(posterShape)
                .background(tokens.colors.surfaceCard)
                .border(
                    width = if (isSelected) 2.dp else NuvioTokens.Border.thin,
                    color = borderColor,
                    shape = posterShape,
                ),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.colors.surfaceElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = sizing.seasonChipTextSize,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (isSelected || (isDesktop && isHovered)) {
                tokens.colors.textPrimary
            } else {
                tokens.colors.textSecondary
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeHorizontalRow(
    episodes: List<MetaVideo>,
    maxWidthDp: Float,
    horizontalScrollPadding: Dp,
    parentMetaId: String,
    metaType: String,
    watchedKeys: Set<String>,
    fallbackImage: String?,
    progressByVideoId: Map<String, WatchProgressEntry>,
    episodeRatings: Map<Pair<Int, Int>, Double>,
    episodeRatingsVisibility: EpisodeRatingsVisibility,
    blurUnwatchedEpisodes: Boolean,
    preferredEpisodeNumber: Int? = null,
    onEpisodeClick: ((MetaVideo) -> Unit)?,
    onEpisodeLongPress: ((MetaVideo) -> Unit)?,
) {
    val rowMetrics = rememberEpisodeHorizontalCardMetrics(maxWidthDp)
    val listState = rememberLazyListState()
    var hasPositioned by remember(episodes) { mutableStateOf(false) }

    LaunchedEffect(episodes, preferredEpisodeNumber) {
        val targetIndex = if (preferredEpisodeNumber != null) {
            episodes.indexOfFirst { it.episode == preferredEpisodeNumber }
        } else {
            -1
        }
        if (targetIndex >= 0) {
            if (hasPositioned) {
                listState.animateScrollToItem(targetIndex)
            } else {
                listState.scrollToItem(targetIndex)
                hasPositioned = true
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .nuvioHorizontalScrollBleed(horizontalScrollPadding)
            .fillMaxWidth()
            .nuvioDesktopDragScroll(listState)
            .nuvioDesktopWheelHorizontalScroll(listState),
        contentPadding = PaddingValues(
            horizontal = horizontalScrollPadding + rowMetrics.rowHorizontalPadding,
            vertical = rowMetrics.rowVerticalPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(rowMetrics.itemSpacing),
    ) {
        itemsIndexed(
            items = episodes,
            key = { index, episode -> "${episode.season}:${episode.episode}:${episode.id}#$index" },
        ) { _, episode ->
            val episodeVideoId = buildPlaybackVideoId(
                parentMetaId = parentMetaId,
                seasonNumber = episode.season,
                episodeNumber = episode.episode,
                fallbackVideoId = episode.id,
            )
            EpisodeHorizontalCard(
                video = episode,
                fallbackImage = fallbackImage,
                progressEntry = progressByVideoId[episodeVideoId],
                imdbRating = episode.seasonEpisodeKey()?.let { episodeRatings[it] } ?: episode.rating,
                isWatched = progressByVideoId[episodeVideoId]?.isEffectivelyCompleted == true ||
                    WatchingState.isEpisodeWatched(
                        watchedKeys = watchedKeys,
                        metaType = metaType,
                        metaId = parentMetaId,
                        episode = episode,
                    ),
                episodeRatingsVisibility = episodeRatingsVisibility,
                blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                metrics = rowMetrics,
                onClick = { onEpisodeClick?.invoke(episode) },
                onLongPress = { onEpisodeLongPress?.invoke(episode) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeHorizontalCard(
    video: MetaVideo,
    fallbackImage: String?,
    progressEntry: WatchProgressEntry?,
    imdbRating: Double?,
    isWatched: Boolean,
    episodeRatingsVisibility: EpisodeRatingsVisibility,
    blurUnwatchedEpisodes: Boolean,
    metrics: EpisodeHorizontalCardMetrics,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val cornerRadius = if (isDesktop) 14.dp else metrics.cornerRadius
    val cardShape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent.copy(alpha = 0.65f)
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "episode_horizontal_card_border",
    )
    val ratingLabel = remember(imdbRating, episodeRatingsVisibility, isWatched) {
        imdbRating?.takeIf { it > 0.0 && episodeRatingsVisibility.showRating(isWatched) }
            ?.let(::formatEpisodeRating)
    }
    val formattedDate = remember(video.released) { video.released?.let { formatReleaseDateForDisplay(it) } }
    val runtimeLabel = remember(video.runtime) { video.runtime?.takeIf { it > 0 }?.let(::formatEpisodeRuntime) }
    val imageUrl = video.thumbnail ?: fallbackImage
    val visibleProgressEntry = progressEntry?.takeIf { it.durationMs > 0L && !it.isCompleted }
    val progressBarHeight = 4.dp
    val progressBarContentSpacing = 6.dp
    val progressBarBottomSpacing = 8.dp
    val contentBottomPadding = if (visibleProgressEntry != null) {
        progressBarContentSpacing + progressBarHeight + progressBarBottomSpacing
    } else {
        metrics.contentBottomPadding
    }
    Box(
        modifier = Modifier
            .width(metrics.cardWidth)
            .height(metrics.cardHeight)
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .nuvioCardDepth(
                shape = cardShape,
                surface = NuvioCardDepthSurface.EpisodeCards,
                fallbackBorderAlpha = 0f,
            )
            .border(
                width = NuvioTokens.Border.thin,
                color = borderColor,
                shape = cardShape,
            )
            .nuvioFocusBorder(cardShape)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .posterCardClickable(
                onClick = onClick,
                onLongClick = onLongPress,
                zoomImageUrl = imageUrl,
                zoomCornerRadius = cornerRadius,
            ),
    ) {
        val shouldBlurArtwork = blurUnwatchedEpisodes && !isWatched
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (shouldBlurArtwork) Modifier.blur(18.dp) else Modifier),
                contentScale = ContentScale.Crop,
                desktopAnimation = rememberAnimatedArtworkAnimation(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.56f to tokens.colors.background.copy(alpha = 0.28f),
                        0.72f to tokens.colors.background.copy(alpha = 0.62f),
                        0.86f to tokens.colors.background.copy(alpha = 0.84f),
                        1f to tokens.colors.background.copy(alpha = 0.96f),
                    ),
                ),
        )

        NuvioAnimatedWatchedBadge(
            isVisible = isWatched,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(metrics.contentPadding),
        )

        EpisodeAvailabilityBadge(
            video = video,
            modifier = Modifier
                .align(if (isWatched) Alignment.TopStart else Alignment.TopEnd)
                .padding(metrics.contentPadding),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = metrics.contentPadding,
                    end = metrics.contentPadding,
                    top = metrics.contentPadding,
                    bottom = contentBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EpisodeCodeBadge(
                text = video.episodeBadge(),
                textSize = metrics.badgeTextSize,
                radius = metrics.badgeRadius,
                horizontalPadding = metrics.badgeHorizontalPadding,
                verticalPadding = metrics.badgeVerticalPadding,
            )

            Text(
                text = localizedEpisodeTitle(video.title, video.episode) ?: video.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = metrics.titleTextSize,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = metrics.titleLineHeight,
                ),
                color = tokens.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!video.overview.isNullOrBlank()) {
                Text(
                    text = video.overview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = metrics.bodyTextSize,
                        lineHeight = metrics.bodyLineHeight,
                    ),
                    color = tokens.colors.textSecondary.copy(alpha = 0.90f),
                    maxLines = metrics.overviewMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (runtimeLabel != null || ratingLabel != null || formattedDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    runtimeLabel?.let { runtime ->
                        Text(
                            text = runtime,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = metrics.metaTextSize),
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                        )
                    }
                    ratingLabel?.let { rating ->
                        ImdbEpisodeRatingBadge(
                            rating = rating,
                            logoWidth = metrics.imdbLogoWidth,
                            logoHeight = metrics.imdbLogoHeight,
                            textSize = metrics.metaTextSize,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    formattedDate?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = metrics.metaTextSize),
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        visibleProgressEntry?.let { entry ->
            EpisodeGradientProgressBar(
                progress = entry.progressFraction,
                height = progressBarHeight,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = metrics.contentPadding,
                        end = metrics.contentPadding,
                        bottom = progressBarBottomSpacing,
                    ),
            )
        }
    }
}

private data class EpisodeHorizontalCardMetrics(
    val rowHorizontalPadding: Dp,
    val rowVerticalPadding: Dp,
    val itemSpacing: Dp,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val contentBottomPadding: Dp,
    val titleTextSize: androidx.compose.ui.unit.TextUnit,
    val titleLineHeight: androidx.compose.ui.unit.TextUnit,
    val bodyTextSize: androidx.compose.ui.unit.TextUnit,
    val bodyLineHeight: androidx.compose.ui.unit.TextUnit,
    val overviewMaxLines: Int,
    val metaTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeRadius: Dp,
    val badgeHorizontalPadding: Dp,
    val badgeVerticalPadding: Dp,
    val imdbLogoWidth: Dp,
    val imdbLogoHeight: Dp,
)

@Composable
private fun rememberEpisodeHorizontalCardMetrics(maxWidthDp: Float): EpisodeHorizontalCardMetrics {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val userCornerRadius = posterCardStyle.cornerRadiusDp.dp
    return remember(maxWidthDp, userCornerRadius) {
        when {
            maxWidthDp >= 1300f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 18.dp,
                cardWidth = 420.dp,
                cardHeight = 256.dp,
                cornerRadius = userCornerRadius,
                contentPadding = 16.dp,
                contentBottomPadding = 18.dp,
                titleTextSize = 18.sp,
                titleLineHeight = 24.sp,
                bodyTextSize = 14.sp,
                bodyLineHeight = 20.sp,
                overviewMaxLines = 3,
                metaTextSize = 12.sp,
                badgeTextSize = 11.sp,
                badgeRadius = 8.dp,
                badgeHorizontalPadding = 10.dp,
                badgeVerticalPadding = 5.dp,
                imdbLogoWidth = 28.dp,
                imdbLogoHeight = 14.dp,
            )

            maxWidthDp >= 1000f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 16.dp,
                cardWidth = 384.dp,
                cardHeight = 236.dp,
                cornerRadius = userCornerRadius,
                contentPadding = 14.dp,
                contentBottomPadding = 16.dp,
                titleTextSize = 17.sp,
                titleLineHeight = 22.sp,
                bodyTextSize = 13.sp,
                bodyLineHeight = 18.sp,
                overviewMaxLines = 3,
                metaTextSize = 12.sp,
                badgeTextSize = 10.sp,
                badgeRadius = 7.dp,
                badgeHorizontalPadding = 9.dp,
                badgeVerticalPadding = 4.dp,
                imdbLogoWidth = 26.dp,
                imdbLogoHeight = 13.dp,
            )

            maxWidthDp >= 760f -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 14.dp,
                cardWidth = 340.dp,
                cardHeight = 212.dp,
                cornerRadius = userCornerRadius,
                contentPadding = 12.dp,
                contentBottomPadding = 14.dp,
                titleTextSize = 16.sp,
                titleLineHeight = 21.sp,
                bodyTextSize = 12.sp,
                bodyLineHeight = 17.sp,
                overviewMaxLines = 2,
                metaTextSize = 11.sp,
                badgeTextSize = 10.sp,
                badgeRadius = 6.dp,
                badgeHorizontalPadding = 8.dp,
                badgeVerticalPadding = 4.dp,
                imdbLogoWidth = 24.dp,
                imdbLogoHeight = 12.dp,
            )

            else -> EpisodeHorizontalCardMetrics(
                rowHorizontalPadding = 0.dp,
                rowVerticalPadding = 0.dp,
                itemSpacing = 12.dp,
                cardWidth = 296.dp,
                cardHeight = 184.dp,
                cornerRadius = userCornerRadius,
                contentPadding = 10.dp,
                contentBottomPadding = 12.dp,
                titleTextSize = 14.sp,
                titleLineHeight = 19.sp,
                bodyTextSize = 11.sp,
                bodyLineHeight = 15.sp,
                overviewMaxLines = 2,
                metaTextSize = 10.sp,
                badgeTextSize = 9.sp,
                badgeRadius = 5.dp,
                badgeHorizontalPadding = 7.dp,
                badgeVerticalPadding = 3.dp,
                imdbLogoWidth = 22.dp,
                imdbLogoHeight = 11.dp,
            )
        }
    }
}

private fun formatEpisodeRuntime(runtimeMinutes: Int): String {
    return formatRuntimeFromMinutes(runtimeMinutes)
}

@Composable
private fun EpisodeCodeBadge(
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    radius: Dp,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    backgroundAlpha: Float = 0.84f,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.colors.surfaceElevated.copy(alpha = backgroundAlpha))
            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = tokens.colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun EpisodeGradientProgressBar(
    progress: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(tokens.shapes.chip)
            .background(tokens.colors.textPrimary.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight()
                .clip(tokens.shapes.chip)
                .background(palette.accentBrush()),
        )
    }
}

@Composable
private fun ImdbEpisodeRatingBadge(
    rating: String,
    logoWidth: Dp,
    logoHeight: Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
) {
    val tokens = MaterialTheme.nuvio
    val badgeShape = RoundedCornerShape(NuvioTokens.Radius.sm)
    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(tokens.colors.surfaceElevated.copy(alpha = 0.78f))
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, badgeShape)
            .padding(horizontal = NuvioTokens.Space.s6, vertical = NuvioTokens.Space.s2),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (AppFeaturePolicy.imdbRatingLogoEnabled) {
            Image(
                painter = painterResource(Res.drawable.rating_imdb),
                contentDescription = stringResource(Res.string.source_imdb),
                modifier = Modifier
                    .width(logoWidth)
                    .height(logoHeight),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = stringResource(Res.string.source_imdb),
                tint = ImdbYellow,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = rating,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
            ),
            color = ImdbYellow,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeListCard(
    video: MetaVideo,
    fallbackImage: String?,
    progressEntry: WatchProgressEntry?,
    imdbRating: Double?,
    isWatched: Boolean,
    episodeRatingsVisibility: EpisodeRatingsVisibility,
    blurUnwatchedEpisodes: Boolean,
    sizing: SeriesContentSizing,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val cornerRadius = if (isDesktop) 14.dp else rememberPosterCardStyleUiState().cornerRadiusDp.dp
    val cardShape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent.copy(alpha = 0.65f)
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "episode_list_card_border",
    )
    val ratingLabel = remember(imdbRating, episodeRatingsVisibility, isWatched) {
        imdbRating?.takeIf { it > 0.0 && episodeRatingsVisibility.showRating(isWatched) }
            ?.let(::formatEpisodeRating)
    }
    val formattedDate = remember(video.released) { video.released?.let { formatReleaseDateForDisplay(it) } }
    val imageUrl = video.thumbnail ?: fallbackImage
    var cardBounds by remember { mutableStateOf<Rect?>(null) }
    val handleLongPress = onLongPress?.let { handler ->
        {
            val pointerOffset = PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
            cardBounds?.let { bounds ->
                PosterZoomAnchorHolder.stash(
                    PosterZoomAnchor(
                        boundsInRoot = bounds,
                        imageUrl = imageUrl,
                        cornerRadius = cornerRadius,
                        pointerPositionInRoot = pointerOffset,
                    ),
                )
            }
            handler()
        }
    }
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                cardBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + coordinates.size.width,
                    bottom = position.y + coordinates.size.height,
                )
            }
            .fillMaxWidth()
            .height(sizing.cardHeight)
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .border(
                width = NuvioTokens.Border.thin,
                color = borderColor,
                shape = cardShape,
            )
            .nuvioFocusBorder(cardShape)
            .then(
                if (isDesktop && (onClick != null || onLongPress != null)) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .captureSecondaryClickRootOffset(enabled = handleLongPress != null)
            .combinedClickable(
                enabled = onClick != null || onLongPress != null,
                onClick = {
                    PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
                    onClick?.invoke()
                },
                onLongClick = handleLongPress,
            )
            .secondaryClick(handleLongPress),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Image area - fixed width matching card height per spec
            Box(
                modifier = Modifier
                    .width(sizing.imageWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius)),
            ) {
                val shouldBlurArtwork = blurUnwatchedEpisodes && !isWatched
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = video.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (shouldBlurArtwork) Modifier.blur(18.dp) else Modifier),
                        contentScale = ContentScale.Crop,
                        desktopAnimation = rememberAnimatedArtworkAnimation(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tokens.colors.surfaceElevated),
                    )
                }

                EpisodeCodeBadge(
                    text = video.episodeBadge(),
                    textSize = sizing.badgeTextSize,
                    radius = sizing.badgeRadius,
                    horizontalPadding = sizing.badgeHorizontalPadding,
                    verticalPadding = sizing.badgeVerticalPadding,
                    backgroundAlpha = 0.88f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 8.dp),
                )

                NuvioAnimatedWatchedBadge(
                    isVisible = isWatched,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )

                EpisodeAvailabilityBadge(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = if (progressEntry != null && progressEntry.durationMs > 0L && !progressEntry.isCompleted) {
                                24.dp
                            } else {
                                8.dp
                            },
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(
                        start = sizing.contentHorizontalPadding,
                        end = sizing.contentHorizontalPadding,
                        top = sizing.contentVerticalPadding,
                        bottom = sizing.contentVerticalPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(sizing.contentSpacing),
            ) {
                Text(
                    text = localizedEpisodeTitle(video.title, video.episode) ?: video.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = sizing.titleTextSize,
                        fontWeight = FontWeight.Bold,
                        lineHeight = sizing.titleLineHeight,
                        letterSpacing = 0.sp,
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = sizing.titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                if (formattedDate != null || ratingLabel != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        formattedDate?.let { date ->
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = sizing.metaTextSize,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = tokens.colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ratingLabel?.let { rating ->
                            ImdbEpisodeRatingBadge(
                                rating = rating,
                                logoWidth = 24.dp,
                                logoHeight = 12.dp,
                                textSize = sizing.metaTextSize,
                            )
                        }
                    }
                }

                if (!video.overview.isNullOrBlank()) {
                    Text(
                        text = video.overview,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = sizing.bodyTextSize,
                            lineHeight = sizing.bodyLineHeight,
                        ),
                        color = tokens.colors.textSecondary,
                        maxLines = sizing.overviewMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        progressEntry
            ?.takeIf { it.durationMs > 0L && !it.isCompleted }
            ?.let { entry ->
                EpisodeGradientProgressBar(
                    progress = entry.progressFraction,
                    height = 5.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(sizing.imageWidth - 24.dp)
                        .padding(start = 12.dp, bottom = 10.dp),
                )
            }
    }
}

private data class SeriesContentSizing(
    val seasonHeaderSize: androidx.compose.ui.unit.TextUnit,
    val seasonToggleTextSize: androidx.compose.ui.unit.TextUnit,
    val seasonChipGap: Dp,
    val seasonChipRadius: Dp,
    val seasonChipHorizontalPadding: Dp,
    val seasonChipVerticalPadding: Dp,
    val seasonChipTextSize: androidx.compose.ui.unit.TextUnit,
    val seasonPosterWidth: Dp,
    val seasonPosterHeight: Dp,
    val seasonPosterRadius: Dp,
    val cardHeight: Dp,
    val imageWidth: Dp,
    val cardGap: Dp,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val contentSpacing: Dp,
    val titleTextSize: androidx.compose.ui.unit.TextUnit,
    val titleLineHeight: androidx.compose.ui.unit.TextUnit,
    val titleMaxLines: Int,
    val bodyTextSize: androidx.compose.ui.unit.TextUnit,
    val bodyLineHeight: androidx.compose.ui.unit.TextUnit,
    val overviewMaxLines: Int,
    val metaTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeTextSize: androidx.compose.ui.unit.TextUnit,
    val badgeRadius: Dp,
    val badgeHorizontalPadding: Dp,
    val badgeVerticalPadding: Dp,
)

private fun seriesContentSizing(maxWidthDp: Float): SeriesContentSizing =
    when {
        maxWidthDp >= 1440f -> SeriesContentSizing(
            seasonHeaderSize = 28.sp,
            seasonToggleTextSize = 16.sp,
            seasonChipGap = 20.dp,
            seasonChipRadius = 16.dp,
            seasonChipHorizontalPadding = 20.dp,
            seasonChipVerticalPadding = 16.dp,
            seasonChipTextSize = 16.sp,
            seasonPosterWidth = 140.dp,
            seasonPosterHeight = 210.dp,
            seasonPosterRadius = 16.dp,
            cardHeight = 200.dp,
            imageWidth = 200.dp,
            cardGap = 20.dp,
            contentHorizontalPadding = 20.dp,
            contentVerticalPadding = 18.dp,
            contentSpacing = 8.dp,
            titleTextSize = 18.sp,
            titleLineHeight = 24.sp,
            titleMaxLines = 3,
            bodyTextSize = 15.sp,
            bodyLineHeight = 22.sp,
            overviewMaxLines = 4,
            metaTextSize = 13.sp,
            badgeTextSize = 13.sp,
            badgeRadius = 6.dp,
            badgeHorizontalPadding = 8.dp,
            badgeVerticalPadding = 4.dp,
        )
        maxWidthDp >= 1024f -> SeriesContentSizing(
            seasonHeaderSize = 26.sp,
            seasonToggleTextSize = 15.sp,
            seasonChipGap = 18.dp,
            seasonChipRadius = 14.dp,
            seasonChipHorizontalPadding = 18.dp,
            seasonChipVerticalPadding = 14.dp,
            seasonChipTextSize = 15.sp,
            seasonPosterWidth = 130.dp,
            seasonPosterHeight = 195.dp,
            seasonPosterRadius = 14.dp,
            cardHeight = 180.dp,
            imageWidth = 180.dp,
            cardGap = 18.dp,
            contentHorizontalPadding = 18.dp,
            contentVerticalPadding = 16.dp,
            contentSpacing = 8.dp,
            titleTextSize = 17.sp,
            titleLineHeight = 22.sp,
            titleMaxLines = 3,
            bodyTextSize = 14.sp,
            bodyLineHeight = 20.sp,
            overviewMaxLines = 4,
            metaTextSize = 12.sp,
            badgeTextSize = 12.sp,
            badgeRadius = 5.dp,
            badgeHorizontalPadding = 7.dp,
            badgeVerticalPadding = 3.dp,
        )
        maxWidthDp >= 768f -> SeriesContentSizing(
            seasonHeaderSize = 24.sp,
            seasonToggleTextSize = 14.sp,
            seasonChipGap = 16.dp,
            seasonChipRadius = 12.dp,
            seasonChipHorizontalPadding = 16.dp,
            seasonChipVerticalPadding = 12.dp,
            seasonChipTextSize = 17.sp,
            seasonPosterWidth = 120.dp,
            seasonPosterHeight = 180.dp,
            seasonPosterRadius = 12.dp,
            cardHeight = 160.dp,
            imageWidth = 160.dp,
            cardGap = 16.dp,
            contentHorizontalPadding = 16.dp,
            contentVerticalPadding = 14.dp,
            contentSpacing = 6.dp,
            titleTextSize = 16.sp,
            titleLineHeight = 20.sp,
            titleMaxLines = 3,
            bodyTextSize = 14.sp,
            bodyLineHeight = 20.sp,
            overviewMaxLines = 3,
            metaTextSize = 12.sp,
            badgeTextSize = 11.sp,
            badgeRadius = 4.dp,
            badgeHorizontalPadding = 6.dp,
            badgeVerticalPadding = 2.dp,
        )
        else -> SeriesContentSizing(
            seasonHeaderSize = 18.sp,
            seasonToggleTextSize = 12.sp,
            seasonChipGap = 16.dp,
            seasonChipRadius = 12.dp,
            seasonChipHorizontalPadding = 16.dp,
            seasonChipVerticalPadding = 12.dp,
            seasonChipTextSize = 15.sp,
            seasonPosterWidth = 100.dp,
            seasonPosterHeight = 150.dp,
            seasonPosterRadius = 8.dp,
            cardHeight = 120.dp,
            imageWidth = 120.dp,
            cardGap = 16.dp,
            contentHorizontalPadding = 12.dp,
            contentVerticalPadding = 12.dp,
            contentSpacing = 4.dp,
            titleTextSize = 15.sp,
            titleLineHeight = 18.sp,
            titleMaxLines = 2,
            bodyTextSize = 13.sp,
            bodyLineHeight = 18.sp,
            overviewMaxLines = 2,
            metaTextSize = 12.sp,
            badgeTextSize = 11.sp,
            badgeRadius = 4.dp,
            badgeHorizontalPadding = 6.dp,
            badgeVerticalPadding = 2.dp,
        )
    }

private fun Int.label(): String =
    if (this <= 0) {
        runBlocking { getString(Res.string.episodes_specials) }
    } else {
        runBlocking { getString(Res.string.episodes_season, this@label) }
    }

private fun MetaVideo.episodeBadge(): String =
    when {
        episode != null || season != null ->
            localizedSeasonEpisodeCode(seasonNumber = season, episodeNumber = episode).orEmpty()
        else -> runBlocking { getString(Res.string.details_episode_badge_file) }
    }

private fun MetaVideo.seasonEpisodeKey(): Pair<Int, Int>? {
    val seasonNumber = season ?: return null
    val episodeNumber = episode ?: return null
    return seasonNumber to episodeNumber
}

private fun formatEpisodeRating(rating: Double): String {
    val roundedTenths = (rating * 10.0).roundToInt()
    val whole = roundedTenths / 10
    val tenth = (roundedTenths % 10).absoluteValue
    return "$whole.$tenth"
}

internal data class DesktopWheelScrollDecision(
    val dispatchDelta: Float,
    val consume: Boolean,
)

internal fun resolveDesktopWheelHorizontalScroll(
    deltaX: Float,
    deltaY: Float,
    isRtl: Boolean,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): DesktopWheelScrollDecision {
    if (!deltaX.isFinite() || !deltaY.isFinite()) {
        return DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false)
    }
    val rawDelta = if (deltaY.absoluteValue >= deltaX.absoluteValue) {
        deltaY
    } else if (isRtl) {
        -deltaX
    } else {
        deltaX
    }
    val isOverflowing = canScrollForward || canScrollBackward
    if (rawDelta == 0f || !isOverflowing) {
        return DesktopWheelScrollDecision(dispatchDelta = 0f, consume = false)
    }
    val canAdvance = (rawDelta > 0f && canScrollForward) || (rawDelta < 0f && canScrollBackward)
    return DesktopWheelScrollDecision(
        dispatchDelta = if (canAdvance) rawDelta else 0f,
        consume = true,
    )
}

/**
 * Maps vertical mouse wheel rotation (Scroll Up / Scroll Down) while hovering over
 * an overflowing seasons or episodes horizontal row into horizontal scrolling, and
 * consumes wheel events at the row boundaries so the outer vertical detail screen
 * does not jump vertically. Yields cleanly when the row fits without overflow.
 */
@Composable
private fun Modifier.nuvioDesktopWheelHorizontalScroll(
    state: ScrollableState,
): Modifier {
    if (!isDesktop) return this

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    return pointerInput(state, isRtl) {
        awaitEachGesture {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            if (event.type == PointerEventType.Scroll && event.changes.none { it.isConsumed }) {
                val (totalDeltaX, totalDeltaY) = event.changes.fold(0f to 0f) { (accX, accY), change ->
                    (accX + change.scrollDelta.x) to (accY + change.scrollDelta.y)
                }
                val decision = resolveDesktopWheelHorizontalScroll(
                    deltaX = totalDeltaX,
                    deltaY = totalDeltaY,
                    isRtl = isRtl,
                    canScrollForward = state.canScrollForward,
                    canScrollBackward = state.canScrollBackward,
                )
                if (decision.dispatchDelta != 0f) {
                    val stepPx = 96.dp.toPx()
                    state.dispatchRawDelta(decision.dispatchDelta * stepPx)
                }
                if (decision.consume) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}


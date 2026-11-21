package com.nuvio.app.features.streams

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmpalette.extensions.painter.rememberPainterDominantColorState
import com.nuvio.app.core.i18n.localizedEpisodeTitle
import com.nuvio.app.core.ui.DesktopBackdropVerticalBias
import com.nuvio.app.core.ui.LocalOnBackdrop
import com.nuvio.app.core.ui.LocalVerticalBarPadding
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioDropdownChip
import com.nuvio.app.core.ui.NuvioDropdownOption
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.StandardDesktopViewportAspectRatio
import com.nuvio.app.core.ui.StreamResultsWideArtworkAspectRatio
import com.nuvio.app.core.ui.dominantBackdropBlendColor
import com.nuvio.app.core.ui.expandingWideArtworkWidthDp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.platformPhysicalTopInset
import com.nuvio.app.core.ui.shimmer
import com.nuvio.app.isDesktop
import com.nuvio.app.isIos
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TabletStreamsLayout(
    isEpisode: Boolean,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    episodeThumbnail: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    dominantColorEnabled: Boolean,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    onStreamSecondaryClick: (StreamItem, Offset) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isDesktop) {
        LegacyTabletStreamsLayout(
            isEpisode = isEpisode,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            episodeThumbnail = episodeThumbnail,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            uiState = uiState,
            debridEnabled = debridEnabled,
            appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
            resumePositionMs = resumePositionMs,
            resumeProgressFraction = resumeProgressFraction,
            onStreamSelected = onStreamSelected,
            onStreamLongPress = onStreamLongPress,
            onStreamSecondaryClick = onStreamSecondaryClick,
            onRefresh = onRefresh,
            modifier = modifier,
        )
        return
    }

    val tokens = MaterialTheme.nuvio
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val hazeState = rememberHazeState()
    val opacity = NuvioTokens.Opacity
    val tabletBackdrop = remember(background, poster) {
        background ?: poster
    }
    val colorScheme = MaterialTheme.colorScheme
    var dominantBackdropPainter by remember(tabletBackdrop) { mutableStateOf<Painter?>(null) }
    val dominantColorState = rememberPainterDominantColorState(
        defaultColor = tokens.colors.background,
        defaultOnColor = tokens.colors.textPrimary,
    )
    LaunchedEffect(dominantColorEnabled, dominantBackdropPainter) {
        dominantBackdropPainter?.takeIf { dominantColorEnabled }?.let { painter ->
            runCatching { dominantColorState.updateFrom(painter) }
        }
    }
    val wideFadeColor by animateColorAsState(
        targetValue = if (dominantColorEnabled) {
            dominantBackdropBlendColor(dominantColorState.color, tokens.colors.background)
        } else {
            tokens.colors.background
        },
        animationSpec = tween(durationMillis = 320),
        label = "stream_dominant_backdrop_color",
    )
    var resolutionFilterKey by rememberSaveable { mutableStateOf(StreamsResolutionFilter.ALL.name) }
    var sortOrderKey by rememberSaveable { mutableStateOf(StreamsSortOrder.DEFAULT.name) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val aspectRatio = if (maxHeight.value > 0f) {
            maxWidth.value / maxHeight.value
        } else {
            StandardDesktopViewportAspectRatio
        }
        val isWideLayout = aspectRatio >= 2f
        val useWideArtworkFrame = aspectRatio > StandardDesktopViewportAspectRatio
        val cropWideArtwork = aspectRatio > StreamResultsWideArtworkAspectRatio
        val baseArtworkWidth = (maxHeight.value * StandardDesktopViewportAspectRatio).dp.coerceAtMost(maxWidth)
        val artworkWidth = expandingWideArtworkWidthDp(
            maxWidth.value,
            maxHeight.value,
            StreamResultsWideArtworkAspectRatio,
        ).dp
        val resultsWidth = (baseArtworkWidth * 0.6f).coerceAtMost(maxWidth)
        val showFramedArtwork = maxHeight >= 520.dp
        val artworkModifier = if (useWideArtworkFrame) {
            Modifier
                .align(Alignment.CenterStart)
                .width(artworkWidth)
                .fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (useWideArtworkFrame) wideFadeColor else tokens.colors.background)
                .hazeSource(state = hazeState),
        ) {
            if (tabletBackdrop != null) {
                AsyncImage(
                    model = tabletBackdrop,
                    contentDescription = null,
                    modifier = artworkModifier,
                    alignment = BiasAlignment(if (isRtl) 0.05f else -0.05f, DesktopBackdropVerticalBias),
                    contentScale = if (useWideArtworkFrame && !cropWideArtwork) ContentScale.Fit else ContentScale.Crop,
                    onSuccess = { state -> dominantBackdropPainter = state.painter },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.colors.background),
                )
            }

            val wideHorizontalStops = remember(wideFadeColor, opacity, isRtl) {
                val ltrStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.14f to wideFadeColor.copy(alpha = opacity.subtle),
                    0.38f to wideFadeColor.copy(alpha = opacity.overlayLight),
                    0.66f to wideFadeColor.copy(alpha = opacity.overlayHeavy),
                    0.88f to wideFadeColor.copy(alpha = 0.98f),
                    1.00f to wideFadeColor,
                )
                if (isRtl) {
                    ltrStops.reversed().map { (stop, color) -> (1f - stop) to color }.toTypedArray()
                } else {
                    ltrStops
                }
            }
            Box(
                modifier = (if (useWideArtworkFrame) artworkModifier else Modifier.fillMaxSize())
                    .background(
                        if (useWideArtworkFrame) {
                            Brush.horizontalGradient(colorStops = wideHorizontalStops)
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    tokens.colors.background.copy(alpha = 0.28f),
                                    tokens.colors.background.copy(alpha = 0.56f),
                                    tokens.colors.background.copy(alpha = 0.84f),
                                ),
                            )
                        },
                    ),
            )
            // Subtle Cinema Obsidian vertical vignette for crisp summary readability.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to tokens.colors.background.copy(alpha = 0.25f),
                                0.55f to Color.Transparent,
                                1.0f to tokens.colors.background.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(if (isWideLayout) artworkWidth else maxWidth * 0.4f)
                .fillMaxHeight()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isEpisode && seasonNumber != null && episodeNumber != null) {
                TabletEpisodeInfoPanel(
                    logo = logo,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    showTitle = title,
                    artworkUrl = if (showFramedArtwork) episodeThumbnail?.takeIf { it.isNotBlank() } ?: poster else null,
                    isEpisodeArtwork = !episodeThumbnail.isNullOrBlank(),
                    isDesktopLayout = true,
                )
            } else {
                TabletMovieInfoPanel(
                    title = title,
                    logo = logo,
                    posterUrl = if (showFramedArtwork) poster else null,
                    isDesktopLayout = true,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(if (isWideLayout) resultsWidth else maxWidth * 0.6f)
                .fillMaxHeight()
                .padding(
                    top = if (isIos) 20.dp else 44.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        ) {
            val paneShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(paneShape)
                    .then(
                        if (isDesktop) {
                            Modifier
                                .background(tokens.colors.surfaceElevated.copy(alpha = 0.94f))
                                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, paneShape)
                        } else {
                            Modifier
                                .hazeEffect(state = hazeState) {
                                    inputScale = HazeInputScale.Fixed(0.66f)
                                    blurRadius = 56.dp
                                }
                                .background(colorScheme.background.copy(alpha = 0.36f))
                        },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    if ((resumePositionMs != null && resumePositionMs > 0L) || (resumeProgressFraction != null && resumeProgressFraction > 0f)) {
                        ResumeBanner(
                            positionMs = resumePositionMs,
                            progressFraction = resumeProgressFraction,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }

                    ProviderFilterRow(
                        groups = uiState.groups,
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { addonId -> StreamsRepository.selectFilter(addonId) },
                        onRefresh = onRefresh,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        spacing = 8.dp,
                        refreshChip = { isLoading, onClick ->
                            CinemaRefreshPill(isLoading = isLoading, onClick = onClick)
                        },
                        filterChip = { group, isSelected, onClick ->
                            CinemaFilterPill(
                                label = group?.addonName ?: stringResource(Res.string.collections_tab_all),
                                selected = isSelected,
                                onClick = onClick,
                            )
                        },
                    )

                    DesktopStreamsFilterToolbar(
                        selectedResolutionKey = resolutionFilterKey,
                        onResolutionSelected = { resolutionFilterKey = it },
                        selectedSortKey = sortOrderKey,
                        onSortSelected = { sortOrderKey = it },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )

                    ActiveScrapersStatusBlock(
                        groups = uiState.groups,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    StreamList(
                        uiState = uiState,
                        debridEnabled = debridEnabled,
                        appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                        onStreamSelected = onStreamSelected,
                        onStreamLongPress = onStreamLongPress,
                        onStreamSecondaryClick = onStreamSecondaryClick,
                        resumePositionMs = resumePositionMs,
                        resumeProgressFraction = resumeProgressFraction,
                        modifier = Modifier.weight(1f),
                        resolutionFilterKey = resolutionFilterKey,
                        sortOrderKey = sortOrderKey,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegacyTabletStreamsLayout(
    isEpisode: Boolean,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    episodeThumbnail: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    uiState: StreamsUiState,
    debridEnabled: Boolean,
    appendInstantServiceToDefaultName: Boolean,
    resumePositionMs: Long?,
    resumeProgressFraction: Float?,
    onStreamSelected: (stream: StreamItem, resumePositionMs: Long?, resumeProgressFraction: Float?) -> Unit,
    onStreamLongPress: (StreamItem) -> Unit,
    onStreamSecondaryClick: (StreamItem, Offset) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()
    val tabletBackdrop = remember(background, poster) {
        background ?: poster
    }
    var backdropVisible by remember(tabletBackdrop) { mutableStateOf(false) }

    LaunchedEffect(tabletBackdrop) {
        backdropVisible = tabletBackdrop == null
        if (tabletBackdrop != null) {
            backdropVisible = true
        }
    }

    val backdropAlpha by animateFloatAsState(
        targetValue = if (backdropVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "tablet_backdrop_alpha",
    )
    val backdropScale by animateFloatAsState(
        targetValue = if (backdropVisible) 1f else 1.05f,
        animationSpec = tween(durationMillis = 620),
        label = "tablet_backdrop_scale",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
        ) {
            if (tabletBackdrop != null) {
                AsyncImage(
                    model = tabletBackdrop,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = backdropAlpha
                            scaleX = backdropScale
                            scaleY = backdropScale
                        },
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.6f),
                            ),
                        ),
                    ),
            )
        }

        val verticalBar = LocalVerticalBarPadding.current
        val layoutDirection = LocalLayoutDirection.current
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .padding(start = verticalBar.calculateStartPadding(layoutDirection))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isEpisode && seasonNumber != null && episodeNumber != null) {
                    TabletEpisodeInfoPanel(
                        logo = logo,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        showTitle = title,
                    )
                } else {
                    TabletMovieInfoPanel(
                        title = title,
                        logo = logo,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(end = verticalBar.calculateEndPadding(layoutDirection))
                    .padding(
                        top = if (isIos) platformPhysicalTopInset() + 60.dp else 60.dp,
                        end = 12.dp,
                        bottom = 12.dp,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .hazeEffect(state = hazeState) {
                            inputScale = HazeInputScale.Fixed(0.66f)
                            blurRadius = 56.dp
                        }
                        .background(Color.Black.copy(alpha = 0.36f)),
                ) {
                    CompositionLocalProvider(LocalOnBackdrop provides true) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        ) {
                            if ((resumePositionMs != null && resumePositionMs > 0L) || (resumeProgressFraction != null && resumeProgressFraction > 0f)) {
                                ResumeBanner(
                                    positionMs = resumePositionMs,
                                    progressFraction = resumeProgressFraction,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }

                            ProviderFilterRow(
                                groups = uiState.groups,
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = { addonId -> StreamsRepository.selectFilter(addonId) },
                                onRefresh = onRefresh,
                            )

                            ActiveScrapersStatusBlock(
                                groups = uiState.groups,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )

                            StreamList(
                                uiState = uiState,
                                debridEnabled = debridEnabled,
                                appendInstantServiceToDefaultName = appendInstantServiceToDefaultName,
                                onStreamSelected = onStreamSelected,
                                onStreamLongPress = onStreamLongPress,
                                onStreamSecondaryClick = onStreamSecondaryClick,
                                resumePositionMs = resumePositionMs,
                                resumeProgressFraction = resumeProgressFraction,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaRefreshPill(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val rotation = rememberRefreshRotation(isLoading)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val pillShape = RoundedCornerShape(10.dp)
    val bgColor by animateColorAsState(
        targetValue = if (isHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cinemaRefreshBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cinemaRefreshBorder",
    )

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(pillShape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .nuvioFocusBorder(pillShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(Res.string.streams_refresh),
            tint = if (isLoading) tokens.colors.accent else tokens.colors.textPrimary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation.value },
        )
    }
}

@Composable
private fun CinemaFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val pillShape = RoundedCornerShape(10.dp)
    val bgColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.focusBackground
            isHovered -> tokens.colors.surfaceCard
            else -> tokens.colors.surfaceElevated
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cinemaFilterBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> tokens.colors.accent
            isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cinemaFilterBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected || isHovered) tokens.colors.textPrimary else tokens.colors.textSecondary,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "cinemaFilterText",
    )

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(pillShape)
            .background(bgColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .nuvioFocusBorder(pillShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesktopStreamsFilterToolbar(
    selectedResolutionKey: String,
    onResolutionSelected: (String) -> Unit,
    selectedSortKey: String,
    onSortSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allLabel = stringResource(Res.string.collections_tab_all)
    val resolutionOptions = remember(allLabel) {
        listOf(
            StreamsResolutionFilter.ALL.name to allLabel,
            StreamsResolutionFilter.UHD_4K.name to "4K",
            StreamsResolutionFilter.FHD_1080P.name to "1080p",
            StreamsResolutionFilter.HD_720P.name to "720p",
        )
    }
    val sortTitle = stringResource(Res.string.settings_debrid_sort_results)
    val sortOptions = listOf(
        NuvioDropdownOption(StreamsSortOrder.DEFAULT.name, stringResource(Res.string.settings_debrid_sort_original)),
        NuvioDropdownOption(StreamsSortOrder.QUALITY.name, stringResource(Res.string.settings_debrid_sort_best_quality)),
        NuvioDropdownOption(StreamsSortOrder.SIZE_DESC.name, stringResource(Res.string.settings_debrid_sort_largest)),
        NuvioDropdownOption(StreamsSortOrder.SIZE_ASC.name, stringResource(Res.string.settings_debrid_sort_smallest)),
    )
    val selectedSortLabel = sortOptions.firstOrNull { it.key == selectedSortKey }?.label ?: sortTitle
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .nuvioDesktopDragScroll(scrollState)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            resolutionOptions.forEach { (key, label) ->
                CinemaFilterPill(
                    label = label,
                    selected = selectedResolutionKey == key,
                    onClick = { onResolutionSelected(key) },
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        NuvioDropdownChip(
            title = sortTitle,
            label = selectedSortLabel,
            selectedKey = selectedSortKey,
            options = sortOptions,
            onSelected = { option -> onSortSelected(option.key) },
        )
    }
}

@Composable
private fun TabletMovieInfoPanel(
    title: String,
    logo: String?,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    isDesktopLayout: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier.fillMaxWidth(0.84f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isDesktopLayout && !posterUrl.isNullOrBlank()) {
            val frameShape = RoundedCornerShape(16.dp)
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                modifier = Modifier
                    .width(150.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(16.dp, frameShape)
                    .clip(frameShape)
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, frameShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isDesktopLayout) 104.dp else 120.dp),
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f,
                    ),
                ),
                color = tokens.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = stringResource(Res.string.streams_no_metadata),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                ),
                color = tokens.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }

        if (isDesktopLayout && title.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            SummaryMetadataPill(
                text = stringResource(Res.string.media_movie),
                accented = false,
            )
        }
    }
}

@Composable
private fun TabletEpisodeInfoPanel(
    logo: String?,
    seasonNumber: Int,
    episodeNumber: Int,
    episodeTitle: String?,
    showTitle: String,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    isEpisodeArtwork: Boolean = false,
    isDesktopLayout: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    var logoLoadError by remember(logo) { mutableStateOf(false) }
    val logoUrl = logo?.takeIf { it.isNotBlank() }
    val textShadow = Shadow(
        color = Color.Black,
        offset = Offset(0f, 0f),
        blurRadius = 4f,
    )

    Column(
        modifier = modifier.fillMaxWidth(0.84f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isDesktopLayout && !artworkUrl.isNullOrBlank()) {
            val frameShape = RoundedCornerShape(16.dp)
            AsyncImage(
                model = artworkUrl,
                contentDescription = showTitle,
                modifier = if (isEpisodeArtwork) {
                    Modifier
                        .width(230.dp)
                        .aspectRatio(16f / 9f)
                } else {
                    Modifier
                        .width(142.dp)
                        .aspectRatio(2f / 3f)
                }
                    .shadow(16.dp, frameShape)
                    .clip(frameShape)
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, frameShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = showTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isDesktopLayout) 96.dp else 120.dp),
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = showTitle,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    shadow = textShadow,
                ),
                color = tokens.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (isDesktopLayout) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryMetadataPill(
                    text = stringResource(Res.string.streams_episode_badge, seasonNumber, episodeNumber),
                    accented = true,
                )
                SummaryMetadataPill(
                    text = stringResource(Res.string.media_series),
                    accented = false,
                )
            }
            val resolvedEpisodeTitle = localizedEpisodeTitle(episodeTitle, episodeNumber)
                ?: episodeTitle?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.streams_episode_fallback_title)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resolvedEpisodeTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    shadow = textShadow,
                ),
                color = tokens.colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = stringResource(
                    Res.string.streams_episode_title_with_name,
                    seasonNumber,
                    episodeNumber,
                    localizedEpisodeTitle(episodeTitle, episodeNumber)
                        ?: episodeTitle?.takeIf { it.isNotBlank() }
                        ?: stringResource(Res.string.streams_episode_fallback_title),
                ),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    shadow = textShadow,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SummaryMetadataPill(
    text: String,
    accented: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(pillShape)
            .background(if (accented) tokens.colors.focusBackground else tokens.colors.surfaceElevated.copy(alpha = 0.85f))
            .border(
                NuvioTokens.Border.thin,
                if (accented) tokens.colors.accent.copy(alpha = 0.7f) else tokens.colors.borderDefault,
                pillShape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (accented) tokens.colors.accent else tokens.colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActiveScrapersStatusBlock(
    groups: List<AddonStreamGroup>,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val activeScrapers = remember(groups) {
        groups.filter { it.isLoading }.map { it.addonName }.distinct()
    }
    if (activeScrapers.isEmpty()) return
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(Res.string.streams_active_scrapers),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.colors.textMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nuvioDesktopDragScroll(scrollState)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            activeScrapers.forEach { addonName ->
                val pillShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(tokens.colors.surfaceCard)
                        .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, pillShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = addonName,
                        modifier = Modifier.shimmer(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = tokens.colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

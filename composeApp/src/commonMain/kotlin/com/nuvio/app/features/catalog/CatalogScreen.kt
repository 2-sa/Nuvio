package com.nuvio.app.features.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.nuvio.app.controller.RegisterControllerVerticalScroll
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.i18n.localizedMediaTypeLabel
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.poster.CustomPosterFallbackInterceptor
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.SkeletonPoster
import com.nuvio.app.core.ui.catalogPosterBaseWidthDp
import com.nuvio.app.core.ui.desktopPageHorizontalPaddingForWidth
import com.nuvio.app.core.ui.desktopPosterHoverScale
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioConsumePointerEvents
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.posterGridColumnCountForViewport
import com.nuvio.app.core.ui.rememberAnimatedArtworkAnimation
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.skeleton
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomePosterHoverPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.isDesktop
import com.nuvio.app.navigation.LocalUseNativeNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.catalog_empty_message
import nuvio.composeapp.generated.resources.catalog_empty_title
import nuvio.composeapp.generated.resources.catalog_load_failed_title
import nuvio.composeapp.generated.resources.collections_trakt_list_items_count
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScreen(
    title: String,
    subtitle: String,
    target: CatalogTarget,
    onBack: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by CatalogRepository.uiState.collectAsStateWithLifecycle()
    val homeCatalogSettingsUiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    val posterCardStyle = rememberPosterCardStyleUiState()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val initialScrollPosition = remember(
        target,
        homeCatalogSettingsUiState.hideUnreleasedContent,
    ) {
        CatalogRepository.scrollPosition(
            target = target,
        )
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialScrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialScrollPosition.firstVisibleItemScrollOffset,
    )

    // Lets the controller spatial engine scroll this grid when focus reaches the bottom edge.
    val scrollViewportModifier = RegisterControllerVerticalScroll(lazyGridState = gridState)
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var observedOfflineState by remember { mutableStateOf(false) }

    LaunchedEffect(target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        CatalogRepository.load(
            target = target,
        )
    }

    LaunchedEffect(gridState, target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                CatalogRepository.saveScrollPosition(
                    target = target,
                    firstVisibleItemIndex = index,
                    firstVisibleItemScrollOffset = offset,
                )
            }
    }

    LaunchedEffect(gridState, uiState.canLoadMore, uiState.isLoading) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it && uiState.canLoadMore && !uiState.isLoading }
            .collect {
                CatalogRepository.loadMore()
            }
    }

    LaunchedEffect(networkStatusUiState.condition, target) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                CatalogRepository.load(
                    target = target,
                    force = true,
                )
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    val tokens = MaterialTheme.nuvio

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background),
    ) {
        val pageHorizontalPadding = if (isDesktop) {
            desktopPageHorizontalPaddingForWidth(maxWidth.value)
        } else {
            16.dp
        }
        val columns = remember(maxWidth, maxHeight, posterCardStyle.widthDp, isDesktop) {
            if (isDesktop) {
                posterGridColumnCountForViewport(maxWidth, maxHeight, posterCardStyle.widthDp)
            } else {
                catalogGridColumnsForWidth(maxWidth)
            }
        }
        val basePosterWidthDp = catalogPosterBaseWidthDp(posterCardStyle.widthDp)
        val gridCells = if (isDesktop) {
            GridCells.FixedSize(
                if (posterCardStyle.catalogLandscapeModeEnabled) {
                    landscapePosterWidth(basePosterWidthDp)
                } else {
                    basePosterWidthDp.dp
                },
            )
        } else {
            GridCells.Fixed(columns)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = gridCells,
                state = gridState,
                modifier = Modifier.fillMaxSize().then(scrollViewportModifier),
                contentPadding = PaddingValues(
                    start = pageHorizontalPadding,
                    top = with(LocalDensity.current) { headerHeightPx.toDp() } + 16.dp,
                    end = pageHorizontalPadding,
                    bottom = nuvioSafeBottomPadding(28.dp),
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isDesktop) 14.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (uiState.items.isEmpty() && uiState.isLoading) {
                    items(columns * 3) {
                        if (isDesktop) {
                            CatalogSkeletonTile(
                                cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                                aspectRatio = if (posterCardStyle.catalogLandscapeModeEnabled) {
                                    PosterLandscapeAspectRatio
                                } else {
                                    0.68f
                                },
                            )
                        } else {
                            SkeletonPoster(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                                showLabels = !posterCardStyle.hideLabelsEnabled,
                            )
                        }
                    }
                } else if (uiState.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogEmptyState(
                            errorMessage = uiState.errorMessage,
                            networkCondition = networkStatusUiState.condition,
                            onRetry = {
                                NetworkStatusRepository.requestRefresh(force = true)
                                CatalogRepository.load(
                                    target = target,
                                    force = true,
                                )
                            },
                        )
                    }
                } else {
                    items(
                        items = uiState.items.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                        key = { item -> item.lazyKey },
                    ) { keyedItem ->
                        val item = keyedItem.value
                        val isWatched = WatchingState.isPosterWatched(
                            watchedKeys = watchedUiState.watchedKeys,
                            item = item,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        )
                        if (isDesktop) {
                            HomePosterCard(
                                item = item,
                                useLandscapeBackdropMode = posterCardStyle.catalogLandscapeModeEnabled,
                                isWatched = isWatched,
                                onClick = onPosterClick?.let { { it(item) } },
                                onLongClick = onPosterLongClick?.let { { it(item) } },
                            )
                        } else {
                            CatalogPosterTile(
                                item = item,
                                cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                                hideLabels = posterCardStyle.hideLabelsEnabled,
                                isWatched = isWatched,
                                onClick = onPosterClick?.let { { it(item) } },
                                onLongClick = onPosterLongClick?.let { { it(item) } },
                            )
                        }
                    }
                    if (uiState.isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogLoadingFooter()
                        }
                    }
                }
            }
            NuvioDesktopVerticalScrollbar(
                state = gridState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
            )

            CatalogHeader(
                title = title,
                subtitle = subtitle,
                target = target,
                itemCount = uiState.items.size,
                pageHorizontalPadding = pageHorizontalPadding,
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                onBack = onBack,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogHeader(
    title: String,
    subtitle: String,
    target: CatalogTarget,
    itemCount: Int,
    pageHorizontalPadding: Dp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalUseNativeNavigation.current) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(44.dp),
        )
        return
    }

    val tokens = MaterialTheme.nuvio
    val activeGenre = (target as? CatalogTarget.Addon)?.genre?.takeIf { it.isNotBlank() }
    val contentTypeLabel = target.contentType.takeIf { it.isNotBlank() }?.let { localizedMediaTypeLabel(it) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.colors.background)
            .nuvioConsumePointerEvents(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isDesktop) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
                .padding(horizontal = pageHorizontalPadding)
                .padding(top = if (isDesktop) 24.dp else 52.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
            ) {
                NuvioBackButton(
                    onClick = onBack,
                    modifier = Modifier.size(if (isDesktop) 42.dp else 40.dp),
                    containerColor = tokens.colors.surfaceElevated,
                    showContainerOnDesktop = true,
                    contentColor = tokens.colors.textPrimary,
                    buttonSize = if (isDesktop) 42.dp else 40.dp,
                    iconSize = 20.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        color = tokens.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = tokens.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (contentTypeLabel != null || activeGenre != null || itemCount > 0) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                    verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                ) {
                    if (contentTypeLabel != null) {
                        CatalogHeaderPill(
                            text = contentTypeLabel,
                            highlighted = false,
                        )
                    }
                    if (activeGenre != null) {
                        CatalogHeaderPill(
                            text = activeGenre,
                            highlighted = true,
                        )
                    }
                    if (itemCount > 0) {
                        CatalogHeaderPill(
                            text = stringResource(Res.string.collections_trakt_list_items_count, itemCount),
                            highlighted = false,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = NuvioTokens.Border.thin,
            color = tokens.colors.borderSubtle,
        )
    }
}

@Composable
private fun CatalogHeaderPill(
    text: String,
    highlighted: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        shape = tokens.shapes.chip,
        color = if (highlighted) tokens.colors.focusBackground else tokens.colors.surfaceElevated,
        border = BorderStroke(
            width = NuvioTokens.Border.thin,
            color = if (highlighted) tokens.colors.accent else tokens.colors.borderDefault,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s4),
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) tokens.colors.textPrimary else tokens.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CatalogPosterTile(
    item: MetaPreview,
    cornerRadiusDp: Int,
    hideLabels: Boolean,
    isWatched: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .desktopPosterHoverScale()
                .then(it),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shape = RoundedCornerShape(cornerRadiusDp.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(item.posterShape.catalogAspectRatio())
                    .clip(shape)
                    .background(tokens.colors.surfaceCard)
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, shape)
                    .nuvioCardDepth(
                        shape = shape,
                        surface = NuvioCardDepthSurface.Posters,
                    )
                    .posterCardClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        zoomImageUrl = item.poster,
                        zoomCornerRadius = cornerRadiusDp.dp,
                        hoverScaleEnabled = false,
                    ),
            ) {
                if (item.poster != null) {
                    val platformContext = LocalPlatformContext.current
                    val hasFallback = !item.rawPosterUrl.isNullOrBlank() && item.rawPosterUrl != item.poster
                    val imageModel = remember(item.poster, item.rawPosterUrl, platformContext) {
                        if (hasFallback) {
                            ImageRequest.Builder(platformContext)
                                .data(item.poster)
                                .memoryCacheKeyExtras(
                                    mapOf(CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to item.rawPosterUrl!!),
                                )
                                .build()
                        } else {
                            item.poster
                        }
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        desktopAnimation = rememberAnimatedArtworkAnimation(),
                    )
                }
                NuvioPosterWatchedOverlay(isWatched = isWatched)
            }
            if (!hideLabels) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CatalogSkeletonTile(
    cornerRadiusDp: Int,
    aspectRatio: Float,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(cornerRadiusDp.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(tokens.colors.surfaceCard, shape)
            .skeleton(shape)
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, shape),
    )
}

private fun PosterShape.catalogAspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 0.68f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 1.78f
    }

private fun catalogGridColumnsForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }

@Composable
private fun CatalogEmptyState(
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
) {
    if (
        !errorMessage.isNullOrBlank() &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            onRetry = onRetry,
        )
        return
    }

    val loadFailed = !errorMessage.isNullOrBlank()
    HomeEmptyStateCard(
        title = stringResource(
            if (loadFailed) Res.string.catalog_load_failed_title else Res.string.catalog_empty_title,
        ),
        message = errorMessage ?: stringResource(Res.string.catalog_empty_message),
        actionLabel = if (loadFailed) stringResource(Res.string.action_retry) else null,
        onActionClick = if (loadFailed) onRetry else null,
    )
}

@Composable
private fun CatalogLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
        )
    }
}

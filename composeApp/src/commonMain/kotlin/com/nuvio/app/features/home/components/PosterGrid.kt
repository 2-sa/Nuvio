package com.nuvio.app.features.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.poster.CustomPosterFallbackInterceptor
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.desktopPosterHoverScale
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberAnimatedArtworkAnimation
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.skeleton
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.isDesktop

internal fun posterGridColumnCountForWidth(screenWidth: Dp): Int =
    com.nuvio.app.core.ui.posterGridColumnCountForWidth(screenWidth)

@Composable
internal fun PosterGridRow(
    items: List<MetaPreview>,
    columns: Int,
    modifier: Modifier = Modifier,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        items.forEach { item ->
            PosterGridTile(
                item = item,
                cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                hideLabels = posterCardStyle.hideLabelsEnabled,
                modifier = Modifier.weight(1f),
                isWatched = WatchingState.isPosterWatched(
                    watchedKeys = watchedKeys,
                    item = item,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                ),
                onClick = onPosterClick?.let { { it(item) } },
                onLongClick = onPosterLongClick?.let { { it(item) } },
            )
        }
        repeat(columns - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun PosterGridSkeletonRow(
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    val cornerRadius = resolvedPosterCornerRadius(posterCardStyle.cornerRadiusDp)
    val cardShape = RoundedCornerShape(cornerRadius)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(columns) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.68f)
                        .clip(cardShape)
                        .background(tokens.colors.surfaceCard, cardShape)
                        .skeleton(cardShape)
                        .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape),
                )
                if (!posterCardStyle.hideLabelsEnabled) {
                    val labelShape = RoundedCornerShape(NuvioTokens.Radius.sm)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(16.dp)
                            .background(tokens.colors.surfaceElevated, labelShape)
                            .skeleton(labelShape),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.46f)
                            .height(12.dp)
                            .background(tokens.colors.surfaceElevated, labelShape)
                            .skeleton(labelShape),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosterGridTile(
    item: MetaPreview,
    cornerRadiusDp: Int,
    hideLabels: Boolean,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val resolvedCornerRadius = resolvedPosterCornerRadius(cornerRadiusDp)
    val cardShape = RoundedCornerShape(resolvedCornerRadius)
    val ratingText = remember(item.imdbRating) { normalizedPosterRating(item.imdbRating) }

    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) { hoverModifier ->
        val cardInteractionSource = remember { MutableInteractionSource() }
        val isHovered by cardInteractionSource.collectIsHoveredAsState()
        val borderColor by animateColorAsState(
            targetValue = if (isDesktop && isHovered) {
                tokens.colors.accent.copy(alpha = 0.70f)
            } else {
                tokens.colors.borderSubtle
            },
            animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
            label = "poster_grid_tile_border",
        )

        Column(
            modifier = Modifier
                .desktopPosterHoverScale()
                .fillMaxWidth()
                .then(hoverModifier)
                .then(if (isDesktop) Modifier.hoverable(cardInteractionSource) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(item.posterShape.posterGridAspectRatio())
                    .clip(cardShape)
                    .background(tokens.colors.surfaceCard)
                    .nuvioCardDepth(
                        shape = cardShape,
                        surface = NuvioCardDepthSurface.Posters,
                    )
                    .border(width = NuvioTokens.Border.thin, color = borderColor, shape = cardShape)
                    .posterCardClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        zoomImageUrl = item.poster,
                        zoomCornerRadius = resolvedCornerRadius,
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
                                    mapOf(CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to item.rawPosterUrl!!)
                                )
                                .build()
                        } else {
                            item.poster
                        }
                    }
                    if (isDesktop) {
                        NuvioAsyncImage(
                            model = imageModel,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            desktopAnimation = rememberAnimatedArtworkAnimation(),
                        )
                    } else {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                if (ratingText != null) {
                    HomePosterRatingPill(rating = ratingText)
                }
                NuvioPosterWatchedOverlay(isWatched = isWatched)
            }
            if (!hideLabels) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp,
                        lineHeight = 20.sp,
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            letterSpacing = 0.sp,
                            lineHeight = 18.sp,
                        ),
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

private fun PosterShape.posterGridAspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 0.68f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 1.78f
    }

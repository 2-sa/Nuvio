package com.nuvio.app.features.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.poster.CustomPosterFallbackInterceptor
import com.nuvio.app.core.ui.DefaultPosterCardCornerRadiusDp
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioDesktopAnimation
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.desktopPosterHoverScale
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.skeleton
import com.nuvio.app.core.ui.toDesktopAnimation
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.poster_logo_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showLandscapeOverlay: Boolean = true,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled
    val effectiveLandscapePoster = if (posterCardStyle.alwaysShowLandscapeClearlogo) null else item.landscapePoster
    val imageUrl = if (isLandscapeMode) {
        effectiveLandscapePoster ?: item.banner ?: item.poster
    } else {
        item.poster
    }
    val fallbackImageUrl = if (isLandscapeMode && !effectiveLandscapePoster.isNullOrBlank()) {
        // Landscape custom poster -> fall back to original backdrop, then portrait
        item.banner ?: item.rawPosterUrl
    } else {
        item.rawPosterUrl
    }

    val effectiveShape = if (isLandscapeMode) PosterShape.Landscape else item.posterShape
    val effectiveBaseWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    val cardWidth = effectiveShape.cardWidth(effectiveBaseWidthDp)
    val resolvedCornerRadius = resolvedPosterCornerRadius(posterCardStyle.cornerRadiusDp)
    val cardShape = RoundedCornerShape(resolvedCornerRadius)
    val shouldShowTitleBelow = !posterCardStyle.hideLabelsEnabled
    val detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) {
        null
    } else {
        item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
    }
    val bottomLeftLogoUrl = if (isLandscapeMode && showLandscapeOverlay && effectiveLandscapePoster.isNullOrBlank()) {
        item.logo
    } else {
        null
    }
    val bottomLeftText = if (
        isLandscapeMode &&
        showLandscapeOverlay &&
        effectiveLandscapePoster.isNullOrBlank() &&
        item.logo.isNullOrBlank() &&
        !posterCardStyle.hideLabelsEnabled
    ) {
        item.name
    } else {
        null
    }
    val overlaySize = homePosterLogoOverlaySize(effectiveBaseWidthDp, effectiveShape)
    val ratingText = remember(item.imdbRating) { normalizedPosterRating(item.imdbRating) }

    HomePosterHoverPreview(
        item = item,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
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
            label = "home_poster_card_border",
        )

        Column(
            modifier = Modifier
                .desktopPosterHoverScale()
                .then(modifier)
                .then(hoverModifier)
                .then(if (isDesktop) Modifier.hoverable(cardInteractionSource) else Modifier)
                .width(cardWidth),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(effectiveShape.posterAspectRatio())
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
                        zoomImageUrl = imageUrl,
                        zoomCornerRadius = resolvedCornerRadius,
                        hoverScaleEnabled = false,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PosterCardImage(
                    imageUrl = imageUrl,
                    fallbackImageUrl = fallbackImageUrl,
                    title = item.name,
                    cardShape = cardShape,
                    desktopImageScaling = if (isLandscapeMode) {
                        NuvioDesktopImageScaling.Auto
                    } else {
                        NuvioDesktopImageScaling.Poster
                    },
                    desktopAnimation = posterCardStyle.animatedArtworkMode.toDesktopAnimation(),
                )

                if (!bottomLeftLogoUrl.isNullOrBlank() || !bottomLeftText.isNullOrBlank()) {
                    val scrimBase = tokens.colors.background
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithCache {
                                val gradient = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.56f to Color.Transparent,
                                        1.0f to scrimBase.copy(alpha = 0.84f),
                                    ),
                                )
                                onDrawBehind { drawRect(gradient) }
                            },
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s10),
                    ) {
                        if (!bottomLeftLogoUrl.isNullOrBlank()) {
                            NuvioAsyncImage(
                                model = bottomLeftLogoUrl,
                                contentDescription = stringResource(Res.string.poster_logo_content_description, item.name),
                                modifier = Modifier
                                    .width(overlaySize.width)
                                    .height(overlaySize.height),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                            )
                        } else {
                            Text(
                                text = bottomLeftText.orEmpty(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 0.sp,
                                    lineHeight = 16.sp,
                                ),
                                color = tokens.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = overlaySize.textMaxWidth),
                            )
                        }
                    }
                }

                if (ratingText != null) {
                    HomePosterRatingPill(rating = ratingText)
                }

                NuvioPosterWatchedOverlay(isWatched = isWatched)
            }

            if (shouldShowTitleBelow) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp,
                        lineHeight = 19.sp,
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!detailLine.isNullOrBlank()) {
                    Text(
                        text = detailLine,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.sp,
                            lineHeight = 15.sp,
                        ),
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Box(modifier = Modifier.height(NuvioTokens.Space.none))
                }
            } else {
                Box(modifier = Modifier.height(NuvioTokens.Space.none))
            }
        }
    }
}

private enum class PosterLoadState {
    Loading,
    Success,
    Error,
}

@Composable
internal fun BoxScope.PosterCardImage(
    imageUrl: String?,
    fallbackImageUrl: String?,
    title: String,
    cardShape: Shape,
    desktopImageScaling: NuvioDesktopImageScaling,
    desktopAnimation: NuvioDesktopAnimation,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        var loadState by remember(imageUrl) { mutableStateOf(PosterLoadState.Loading) }
        var retryAttempt by remember(imageUrl) { mutableStateOf(0) }

        LaunchedEffect(imageUrl, loadState, retryAttempt) {
            if (loadState == PosterLoadState.Error && retryAttempt < 1) {
                delay(2_200L)
                loadState = PosterLoadState.Loading
                retryAttempt += 1
            }
        }

        val platformContext = LocalPlatformContext.current
        val hasFallback = !fallbackImageUrl.isNullOrBlank() && fallbackImageUrl != imageUrl
        val imageModel = remember(imageUrl, fallbackImageUrl, platformContext, retryAttempt) {
            if (!hasFallback && retryAttempt == 0) {
                imageUrl
            } else {
                val extras = buildMap {
                    if (hasFallback) {
                        put(CustomPosterFallbackInterceptor.FALLBACK_URL_KEY, fallbackImageUrl!!)
                    }
                    if (retryAttempt > 0) {
                        put("nuvio_poster_retry", retryAttempt.toString())
                    }
                }
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .memoryCacheKeyExtras(extras)
                    .apply {
                        if (retryAttempt > 0) {
                            memoryCachePolicy(CachePolicy.WRITE_ONLY)
                        }
                    }
                    .build()
            }
        }

        if (loadState == PosterLoadState.Loading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .skeleton(shape = cardShape),
            )
        }
        NuvioAsyncImage(
            model = imageModel,
            contentDescription = title,
            modifier = modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onLoading = { loadState = PosterLoadState.Loading },
            onSuccess = { loadState = PosterLoadState.Success },
            onError = { loadState = PosterLoadState.Error },
            desktopImageScaling = desktopImageScaling,
            desktopAnimation = desktopAnimation,
        )
        if (loadState == PosterLoadState.Error && retryAttempt >= 1) {
            PosterTitleFallback(
                title = title,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        PosterTitleFallback(
            title = title,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun PosterTitleFallback(
    title: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = title,
        modifier = modifier.padding(horizontal = NuvioTokens.Space.s14),
        style = MaterialTheme.typography.titleMedium.copy(
            letterSpacing = 0.sp,
            lineHeight = 22.sp,
        ),
        color = tokens.colors.textMuted,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun BoxScope.HomePosterRatingPill(
    rating: String,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(NuvioTokens.Radius.full)
    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .padding(NuvioTokens.Space.s6)
            .clip(pillShape)
            .background(tokens.colors.surfaceElevated.copy(alpha = 0.84f))
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, pillShape)
            .padding(horizontal = NuvioTokens.Space.s6, vertical = NuvioTokens.Space.s2),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = tokens.colors.warning,
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = rating,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = tokens.colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}

internal fun resolvedPosterCornerRadius(cornerRadiusDp: Int): Dp =
    if (cornerRadiusDp == DefaultPosterCardCornerRadiusDp) 14.dp else cornerRadiusDp.dp

internal fun normalizedPosterRating(rawRating: String?): String? =
    rawRating
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" && !it.equals("N/A", ignoreCase = true) }

private fun PosterShape.posterAspectRatio(): Float = when (this) {
    PosterShape.Poster -> 0.675f
    PosterShape.Square -> 1f
    PosterShape.Landscape -> PosterLandscapeAspectRatio
}

private fun PosterShape.cardWidth(basePosterWidthDp: Int): Dp = when (this) {
    PosterShape.Poster -> basePosterWidthDp.dp
    PosterShape.Square -> basePosterWidthDp.dp
    PosterShape.Landscape -> landscapePosterWidth(basePosterWidthDp)
}

private data class HomePosterLogoOverlaySize(
    val width: Dp,
    val height: Dp,
    val textMaxWidth: Dp,
)

private fun homePosterLogoOverlaySize(
    basePosterWidthDp: Int,
    shape: PosterShape,
): HomePosterLogoOverlaySize = if (shape == PosterShape.Landscape) {
    when {
        basePosterWidthDp <= 108 -> HomePosterLogoOverlaySize(width = 92.dp, height = 24.dp, textMaxWidth = 120.dp)
        basePosterWidthDp <= 120 -> HomePosterLogoOverlaySize(width = 104.dp, height = 28.dp, textMaxWidth = 132.dp)
        basePosterWidthDp <= 132 -> HomePosterLogoOverlaySize(width = 116.dp, height = 30.dp, textMaxWidth = 144.dp)
        else -> HomePosterLogoOverlaySize(width = 128.dp, height = 34.dp, textMaxWidth = 156.dp)
    }
} else {
    when {
        basePosterWidthDp <= 108 -> HomePosterLogoOverlaySize(width = 72.dp, height = 18.dp, textMaxWidth = 92.dp)
        basePosterWidthDp <= 120 -> HomePosterLogoOverlaySize(width = 80.dp, height = 20.dp, textMaxWidth = 104.dp)
        basePosterWidthDp <= 132 -> HomePosterLogoOverlaySize(width = 88.dp, height = 22.dp, textMaxWidth = 112.dp)
        else -> HomePosterLogoOverlaySize(width = 96.dp, height = 24.dp, textMaxWidth = 124.dp)
    }
}

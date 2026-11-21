package com.nuvio.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.nuvio.app.core.ui.NuvioDesktopAnimation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nuvio.app.isDesktop
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.DesktopBackdropVerticalBias
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.heroStretchZoom
import com.nuvio.app.core.ui.ultrawideViewportProgress
import com.nuvio.app.core.ui.centeredVerticalBarPadding
import com.nuvio.app.core.ui.withVerticalBar
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.tmdb.originalTmdbImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val HERO_BACKGROUND_PARALLAX = 0.055f
private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_CONTENT_PARALLAX = 0.18f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val HERO_AUTO_SCROLL_INTERVAL_MS = 8_000L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f
private const val DESKTOP_HERO_MIN_HEIGHT_DP = 440f
private const val DESKTOP_HERO_MAX_HEIGHT_DP = 640f
private const val DESKTOP_HERO_VIEWPORT_RATIO = 0.60f
private const val DESKTOP_HERO_WIDTH_FALLBACK_RATIO = 0.44f
private const val DESKTOP_HERO_CONTENT_MAX_WIDTH_DP = 560f
private const val DESKTOP_HERO_LOGO_MAX_HEIGHT_DP = 96f
private const val ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO = 1f
private const val DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP = 120f
private const val DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP = 192f
private const val DESKTOP_HERO_TOP_FADE_HEIGHT_DP = 160f
private const val DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP = 300f

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentContainerMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val topFadeHeight: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
    val backgroundMotionStrength: Float,
)

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    sectionPadding: Dp? = null,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    showImdbRating: Boolean = true,
    showMdbListRatings: Boolean = false,
    onItemClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val pagerState = key(items.size) {
        rememberPagerState(
            initialPage = if (items.size > 1) {
                val middle = Int.MAX_VALUE / 2
                middle - middle % items.size
            } else {
                0
            },
            pageCount = { if (items.size > 1) Int.MAX_VALUE else items.size },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var pagerDragActive by remember { mutableStateOf(false) }
    val autoScrollPage = pagerState.settledPage

    LaunchedEffect(pagerState) {
        pagerState.scrollToPage(pagerState.currentPage)
    }

    ScreenActivityEffect(pagerState) { active ->
        if (!active) {
            pagerState.stopScroll(MutatePriority.PreventUserInput)
            pagerState.scrollToPage(pagerState.currentPage)
        }
    }

    ScreenActivityEffect(autoScrollPage, items.size) { active ->
        if (isDesktop || !active || items.size <= 1) return@ScreenActivityEffect
        delay(HERO_AUTO_SCROLL_INTERVAL_MS)
        while (pagerState.isScrollInProgress) {
            delay(100L)
        }

        val nextPage = pagerState.currentPage + 1
        pagerState.animateScrollToPage(nextPage)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .homeHeroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
                onDragActiveChange = { pagerDragActive = it },
            )
            .then(
                if (isDesktop) {
                    Modifier.graphicsLayer { clip = true }
                } else {
                    Modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                },
            ),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = isDesktop,
        )
        val heroWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heroHeightPx = with(LocalDensity.current) { layout.heroHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heroStretchHeight(layout.heroHeight, stretchPx),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.01f },
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }

            if (isDesktop) {
                DesktopHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    contentHorizontalPadding = maxOf(
                        sectionPadding ?: layout.contentHorizontalPadding,
                        layout.contentHorizontalPadding,
                    ),
                    coroutineScope = coroutineScope,
                    showImdbRating = showImdbRating,
                    showMdbListRatings = showMdbListRatings,
                    onItemClick = onItemClick,
                )
            } else {
                DefaultHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    coroutineScope = coroutineScope,
                    showImdbRating = showImdbRating,
                    showMdbListRatings = showMdbListRatings,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun HeroBackgroundLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    desktopFrame: Boolean = false,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    val backgroundMotionStrength = if (desktopFrame) layout.backgroundMotionStrength else 1f
    layerPages.forEach { page ->
        val item = items[page % items.size]
        val imageUrl = item.banner ?: item.poster
        val backgroundModifier = if (desktopFrame) {
            Modifier
                .fillMaxSize()
                .heroStretchZoom { stretchPx() * backgroundMotionStrength }
        } else {
            Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .heroStretchZoom(stretchPx)
        }
        AsyncImage(
            model = if (desktopFrame) originalTmdbImageUrl(imageUrl) else imageUrl,
            contentDescription = item.name,
            desktopAnimation = NuvioDesktopAnimation.Always,
            modifier = backgroundModifier
                .graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)
                    alpha = heroPageVisibility(pageOffset)
                    if (desktopFrame) {
                        translationX = 0f
                        translationY = 0f
                        scaleX = 1f
                        scaleY = 1f
                    } else {
                        val scrollOffsetPx = heroScrollOffsetPx(listState, heroHeightPx)
                        val scrollScale = heroBackgroundScrollScale(scrollOffsetPx)
                        translationX = -pageOffset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                        translationY = heroBackgroundScrollTranslationY(scrollOffsetPx)
                        scaleX = HERO_BACKGROUND_SCALE * scrollScale
                        scaleY = HERO_BACKGROUND_SCALE * scrollScale
                    }
                },
            alignment = when {
                desktopFrame -> BiasAlignment(
                    horizontalBias = 0f,
                    verticalBias = DesktopBackdropVerticalBias,
                )
                layout.isTablet -> Alignment.TopCenter
                else -> Alignment.Center
            },
            contentScale = ContentScale.Crop,
            desktopImageScaling = NuvioDesktopImageScaling.Disabled,
        )
    }
}

@Composable
private fun HeroContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    layerPages.forEach { page ->
        val item = items[page % items.size]
        key(item.stableKey()) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)

                    alpha = heroPageVisibility(pageOffset)
                    translationX = -pageOffset * heroWidthPx * HERO_CONTENT_PARALLAX
                },
            ) {
                HeroContentBlock(
                    item = item,
                    layout = layout,
                    showImdbRating = showImdbRating,
                    showMdbListRatings = showMdbListRatings,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun rememberHeroLayerPages(
    pagerState: PagerState,
    itemCount: Int,
    includePagerNeighbors: Boolean,
): List<Int> {
    if (itemCount <= 0) return emptyList()

    val currentPage = pagerState.currentPage
    val includeNeighbors = includePagerNeighbors || pagerState.isScrollInProgress
    return remember(currentPage, includeNeighbors, itemCount) {
        heroLayerPages(
            currentPage = currentPage,
            pageCount = pagerState.pageCount,
            includeNeighbors = includeNeighbors,
        )
    }
}

private fun heroLayerPages(
    currentPage: Int,
    pageCount: Int,
    includeNeighbors: Boolean,
): List<Int> {
    if (!includeNeighbors || pageCount == 1) return listOf(currentPage)

    val neighbors = listOf(currentPage - 1, currentPage + 1)
        .map { page -> page.coerceIn(0, pageCount - 1) }
        .filter { page -> page != currentPage }
        .distinct()
    return neighbors + currentPage
}

@Composable
private fun HeroDesktopContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    layout: HomeHeroLayout,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    layerPages.forEach { page ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)

                    alpha = heroPageVisibility(pageOffset)
                    translationX = 0f
                },
        ) {
            DesktopHeroContentBlock(
                item = items[page % items.size],
                layout = layout,
                showImdbRating = showImdbRating,
                showMdbListRatings = showMdbListRatings,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun DefaultHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    coroutineScope: CoroutineScope,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    if (layout.isTablet) {
                        PaddingValues(horizontal = layout.contentHorizontalPadding).withVerticalBar()
                    } else {
                        centeredVerticalBarPadding(layout.contentHorizontalPadding)
                    },
                )
                .padding(vertical = layout.contentVerticalPadding),
            horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = Alignment.BottomStart,
            ) {
                HeroContentLayers(
                    items = items,
                    pagerState = pagerState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    showImdbRating = showImdbRating,
                    showMdbListRatings = showMdbListRatings,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (!layout.isTablet) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .nuvioFocusBorder(RoundedCornerShape(40.dp))
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(currentHeroItem(items, pagerState))
                        },
                    color = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(40.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.home_view_details),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            HeroPageIndicatorRow(
                itemCount = items.size,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                modifier = Modifier.padding(top = if (layout.isTablet) 14.dp else 12.dp),
            )
        }
    }
}

@Composable
private fun DesktopHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    contentHorizontalPadding: Dp,
    coroutineScope: CoroutineScope,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val space = NuvioTokens.Space
    val backgroundColor = tokens.colors.background
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val heroInteractionSource = remember { MutableInteractionSource() }
    val isHeroHovered by heroInteractionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(if (isDesktop) Modifier.hoverable(heroInteractionSource) else Modifier),
    ) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
            desktopFrame = true,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.topFadeHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to backgroundColor.copy(alpha = 0.68f),
                            0.45f to backgroundColor.copy(alpha = 0.24f),
                            1.00f to Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = heroDirectionalScrimStops(
                            scrimColor = backgroundColor,
                            isRtl = isRtl,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.42f to backgroundColor.copy(alpha = 0.42f),
                            0.76f to backgroundColor.copy(alpha = 0.86f),
                            1.00f to backgroundColor,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = layout.contentContainerMaxWidth)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = contentHorizontalPadding,
                        end = space.s32,
                        bottom = layout.contentVerticalPadding,
                    )
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = Alignment.BottomStart,
            ) {
                HeroDesktopContentLayers(
                    items = items,
                    pagerState = pagerState,
                    layout = layout,
                    showImdbRating = showImdbRating,
                    showMdbListRatings = showMdbListRatings,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (isFullscreenActionSupported) {
                FullscreenActionButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(
                            top = space.s24,
                            end = contentHorizontalPadding,
                        ),
                    buttonSize = 44.dp,
                    iconSize = 22.dp,
                    containerColor = tokens.colors.surfaceElevated.copy(alpha = 0.78f),
                    contentColor = tokens.colors.textPrimary,
                )
            }

            HeroPageIndicatorRow(
                itemCount = items.size,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = contentHorizontalPadding,
                        bottom = layout.contentVerticalPadding,
                    ),
            )
        }

        if (isDesktop && items.size > 1) {
            AnimatedVisibility(
                visible = isHeroHovered && pagerState.currentPage > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = space.s16)
                    .zIndex(2f),
            ) {
                HeroCarouselArrowButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.action_previous),
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                )
            }

            AnimatedVisibility(
                visible = isHeroHovered && pagerState.currentPage < pagerState.pageCount - 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = space.s16)
                    .zIndex(2f),
            ) {
                HeroCarouselArrowButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.action_next),
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount - 1))
                        }
                    },
                )
            }
        }
    }
}

private fun heroDirectionalScrimStops(
    scrimColor: Color,
    isRtl: Boolean,
): Array<Pair<Float, Color>> {
    val ltrStops = arrayOf(
        0.00f to scrimColor.copy(alpha = 0.88f),
        0.22f to scrimColor.copy(alpha = 0.50f),
        0.42f to scrimColor.copy(alpha = 0.15f),
        0.58f to Color.Transparent,
    )
    return if (!isRtl) {
        ltrStops
    } else {
        ltrStops.reversed().map { (stop, color) -> (1f - stop) to color }.toTypedArray()
    }
}

@Composable
private fun HeroCarouselArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(40.dp)
            .nuvioFocusBorder(CircleShape)
            .clip(CircleShape)
            .background(tokens.colors.surfaceElevated.copy(alpha = if (hovered) 0.92f else 0.78f))
            .border(
                width = NuvioTokens.Border.thin,
                color = if (hovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                shape = CircleShape,
            )
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tokens.colors.textPrimary,
            modifier = Modifier.size(NuvioTokens.Icon.md),
        )
    }
}

@Composable
private fun HeroPageIndicatorRow(
    itemCount: Int,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 1) return

    val tokens = MaterialTheme.nuvio
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isDesktop) NuvioTokens.Space.s6 else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(itemCount) { index ->
            val page = heroPageForItem(pagerState.currentPage, index, itemCount)
            val activeFraction = heroPageVisibility(pagerState, page)
            val dotModifier = Modifier
                .nuvioFocusBorder(CircleShape, borderWidth = 2.dp)
                .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .clickable {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(heroPageForItem(pagerState.currentPage, index, itemCount))
                    }
                }
                .clip(CircleShape)

            if (isDesktop) {
                Box(
                    modifier = dotModifier
                        .background(lerpColor(Color.White.copy(alpha = 0.30f), tokens.colors.accent, activeFraction))
                        .width(6.dp + (18.dp * activeFraction))
                        .height(5.dp),
                )
            } else {
                Box(
                    modifier = dotModifier
                        .background(MaterialTheme.colorScheme.onBackground)
                        .graphicsLayer {
                            alpha = 0.35f + (0.57f * activeFraction)
                        }
                        .width(8.dp + (24.dp * activeFraction))
                        .height(8.dp),
                )
            }
        }
    }
}

internal fun heroPageForItem(currentPage: Int, itemIndex: Int, itemCount: Int): Int {
    val page = currentPage.toLong() - currentPage % itemCount + itemIndex
    return listOf(page - itemCount, page, page + itemCount)
        .filter { it in 0L until Int.MAX_VALUE.toLong() }
        .minBy { abs(it - currentPage) }
        .toInt()
}

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun heroPageVisibility(
    pagerState: PagerState,
    page: Int,
): Float = heroPageVisibility(heroPageOffset(pagerState, page))

private fun heroPageVisibility(pageOffset: Float): Float = (1f - abs(pageOffset)).coerceIn(0f, 1f)

private fun currentHeroItem(
    items: List<MetaPreview>,
    pagerState: PagerState,
): MetaPreview {
    val currentPage = pagerState.currentPage
    val currentVisiblePages = heroLayerPages(
        currentPage = currentPage,
        pageCount = pagerState.pageCount,
        includeNeighbors = true,
    )
    val selectedPage = currentVisiblePages.maxBy { page ->
        heroPageVisibility(pagerState, page)
    }
    return items[selectedPage % items.size]
}

@Composable
fun HomeHeroReservedSpace(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = isDesktop,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight),
        )
    }
}

@Composable
private fun HeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth(layout.logoWidthFraction)
                    .aspectRatio(2.6f)
                    .nuvioFocusBorder(RoundedCornerShape(NuvioTokens.Radius.md))
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                alignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .nuvioFocusBorder(RoundedCornerShape(NuvioTokens.Radius.md))
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                style = if (layout.isTablet) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HomeHeroMetaRows(
            item = item,
            isTablet = layout.isTablet,
            showImdbRating = showImdbRating,
            showMdbListRatings = showMdbListRatings,
        )
    }
}

@Composable
private fun DesktopHeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    showImdbRating: Boolean,
    showMdbListRatings: Boolean,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val typeScale = MaterialTheme.nuvioTypeScale
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }
    val itemClickModifier = if (onItemClick != null) {
        Modifier
            .nuvioFocusBorder(RoundedCornerShape(NuvioTokens.Radius.md))
            .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onItemClick(item) },
            )
    } else {
        Modifier
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        if (logoUrl != null && !logoLoadError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = DESKTOP_HERO_LOGO_MAX_HEIGHT_DP.dp)
                    .then(itemClickModifier),
                contentAlignment = Alignment.CenterStart,
            ) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth(desktopHeroLogoWidthFraction(layout))
                        .heightIn(max = DESKTOP_HERO_LOGO_MAX_HEIGHT_DP.dp),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    clipToBounds = false,
                    onError = { logoLoadError = true },
                )
            }
        } else {
            val titleStyle = if (layout.heroHeight >= 540.dp) {
                typeScale.displayMd
            } else {
                typeScale.displaySm
            }
            Text(
                text = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(itemClickModifier),
                style = titleStyle.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                    shadow = Shadow(
                        color = tokens.colors.overlayScrim,
                        offset = Offset(0f, 2f),
                        blurRadius = 10f,
                    ),
                ),
                color = tokens.colors.textPrimary,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))
        HomeHeroMetaRows(
            item = item,
            isTablet = true,
            showImdbRating = showImdbRating,
            showMdbListRatings = showMdbListRatings,
        )

        item.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = NuvioTokens.Type.bodyApp,
                    lineHeight = 22.sp,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = tokens.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onItemClick != null) {
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s20))
            Row(
                horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopHeroPrimaryCta(
                    label = stringResource(Res.string.home_view_details),
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@Composable
private fun DesktopHeroPrimaryCta(
    label: String,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val ctaShape = RoundedCornerShape(NuvioTokens.Radius.lg)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktop_hero_primary_cta_scale",
    )

    Box(
        modifier = Modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .nuvioFocusBorder(ctaShape)
            .clip(ctaShape)
            .background(palette.accentBrush())
            .then(
                if (isHovered) {
                    Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                } else {
                    Modifier
                },
            )
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = NuvioTokens.Space.s20),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = tokens.colors.onAccent,
                modifier = Modifier.size(NuvioTokens.Icon.md),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.colors.onAccent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun desktopHeroLogoWidthFraction(layout: HomeHeroLayout): Float =
    when {
        layout.contentMaxWidth >= 520.dp -> 0.74f
        else -> 0.80f
    }

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
    preferDesktopLayout: Boolean = false,
): HomeHeroLayout {
    if (preferDesktopLayout) {
        val safeMaxWidthDp = if (maxWidthDp.isFinite() && maxWidthDp > 0f) maxWidthDp else 1024f
        val safeViewportHeightDp = viewportHeightDp?.takeIf { it.isFinite() && it > 0f }
        val heroHeight = desktopHeroHeight(
            maxWidthDp = safeMaxWidthDp,
            viewportHeightDp = safeViewportHeightDp,
        )
        val ultrawideProgress = ultrawideViewportProgress(
            widthDp = safeMaxWidthDp,
            heightDp = safeViewportHeightDp,
        )

        val standardHorizontalPadding = homeSectionHorizontalPaddingForWidth(safeMaxWidthDp).value

        return HomeHeroLayout(
            isTablet = true,
            heroHeight = heroHeight,
            contentMaxWidth = DESKTOP_HERO_CONTENT_MAX_WIDTH_DP.dp,
            contentContainerMaxWidth = safeMaxWidthDp.dp,
            contentWidthFraction = 0.58f,
            contentHorizontalPadding = lerp(
                start = standardHorizontalPadding,
                stop = DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            contentVerticalPadding = lerp(
                start = 40f,
                stop = DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            topFadeHeight = DESKTOP_HERO_TOP_FADE_HEIGHT_DP.dp,
            bottomFadeHeight = DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP.dp,
            logoWidthFraction = 0.74f,
            backgroundMotionStrength = 1f - ultrawideProgress,
        )
    }

    return when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.42f).dp.coerceIn(360.dp, 440.dp),
            contentMaxWidth = 640.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.56f,
            contentHorizontalPadding = 56.dp,
            contentVerticalPadding = 22.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 190.dp,
            logoWidthFraction = 0.58f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.46f).dp.coerceIn(340.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.62f,
            contentHorizontalPadding = 40.dp,
            contentVerticalPadding = 20.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 180.dp,
            logoWidthFraction = 0.56f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.58f).dp.coerceIn(320.dp, 380.dp),
            contentMaxWidth = 520.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.72f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 18.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 170.dp,
            logoWidthFraction = 0.54f,
            backgroundMotionStrength = 1f,
        )
        else -> HomeHeroLayout(
            isTablet = false,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 16.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 220.dp,
            logoWidthFraction = 0.62f,
            backgroundMotionStrength = 1f,
        )
    }
}

private fun desktopHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
): Dp {
    val widthFallbackHeight = (maxWidthDp * DESKTOP_HERO_WIDTH_FALLBACK_RATIO).dp
        .coerceIn(DESKTOP_HERO_MIN_HEIGHT_DP.dp, DESKTOP_HERO_MAX_HEIGHT_DP.dp)
    val viewportHeight = viewportHeightDp ?: return widthFallbackHeight
    val baselineHeight = (viewportHeight * DESKTOP_HERO_VIEWPORT_RATIO).dp
        .coerceIn(DESKTOP_HERO_MIN_HEIGHT_DP.dp, DESKTOP_HERO_MAX_HEIGHT_DP.dp)
    val ultrawideProgress = ultrawideViewportProgress(
        widthDp = maxWidthDp,
        heightDp = viewportHeight,
    )
    if (ultrawideProgress <= 0f) return baselineHeight

    val ultrawideHeight = (viewportHeight * ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO).dp
        .coerceAtLeast(baselineHeight)
    return (
        baselineHeight.value +
            (ultrawideHeight.value - baselineHeight.value) * ultrawideProgress
        ).dp
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 1.16f).dp
    val baseHeight = if (mobileBelowSectionHeightHintDp == null) {
        viewportDrivenHeight?.coerceAtMost(widthFallbackHeight) ?: widthFallbackHeight
    } else {
        viewportDrivenHeight ?: widthFallbackHeight
    }

    val cappedHeight = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        val maxAllowedFromViewport = (viewportHeightDp - mobileBelowSectionHeightHintDp).dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }

    return if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        cappedHeight.coerceIn(0.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    } else {
        cappedHeight.coerceIn(MOBILE_HERO_MIN_HEIGHT_DP.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    }
}

private fun heroScrollOffsetPx(
    listState: LazyListState?,
    heroHeightPx: Float,
): Float = when {
    listState == null -> 0f
    listState.firstVisibleItemIndex > 0 -> heroHeightPx
    else -> listState.firstVisibleItemScrollOffset.toFloat()
}

private fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

private fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}

private fun Modifier.homeHeroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
    onDragActiveChange: (Boolean) -> Unit,
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false
            var settleAnimationStarted = false

            try {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (!change.pressed) {
                        if (dragging) {
                            val targetPage = resolveHeroTargetPage(
                                startPage = startPage,
                                pageCount = pagerState.pageCount,
                                totalDx = totalDx,
                                velocityX = velocityTracker.calculateVelocity().x,
                                widthPx = widthPx,
                            )
                            settleAnimationStarted = true
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(targetPage)
                                } finally {
                                    onDragActiveChange(false)
                                }
                            }
                        }
                        break
                    }

                    val delta = change.position - change.previousPosition
                    totalDx += delta.x
                    totalDy += delta.y

                    if (!dragging) {
                        val horizontalDrag =
                            abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                        val verticalDrag =
                            abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                        when {
                            verticalDrag -> break
                            horizontalDrag -> {
                                dragging = true
                                onDragActiveChange(true)
                            }
                            else -> continue
                        }
                    }

                    pagerState.dispatchRawDelta(-delta.x)
                    change.consume()
                }
            } finally {
                if (dragging && !settleAnimationStarted) {
                    onDragActiveChange(false)
                }
            }
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    pageCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, pageCount - 1)
    return when {
        totalDx > 0f -> (currentPage - 1).coerceAtLeast(0)
        totalDx < 0f -> (currentPage + 1).coerceAtMost(pageCount - 1)
        else -> currentPage
    }
}

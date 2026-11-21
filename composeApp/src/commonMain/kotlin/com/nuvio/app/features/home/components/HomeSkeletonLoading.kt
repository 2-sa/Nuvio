package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.landscapePosterHeightForWidth
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.skeleton
import com.nuvio.app.isDesktop

@Composable
fun HomeSkeletonHero(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    sectionPadding: Dp? = null,
) {
    val tokens = MaterialTheme.nuvio
    val backgroundColor = tokens.colors.background

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isDesktop) {
                    Modifier
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
        val containerWidth = maxWidth
        val contentHorizontalPadding = sectionPadding ?: layout.contentHorizontalPadding

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .background(tokens.colors.surfaceElevated)
                .skeleton(RectangleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.02f),
                                backgroundColor.copy(alpha = 0.14f),
                                backgroundColor.copy(alpha = 0.40f),
                                backgroundColor.copy(alpha = 0.82f),
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
                                backgroundColor.copy(alpha = 0f),
                                backgroundColor,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .then(
                        if (isDesktop) {
                            Modifier
                                .widthIn(max = layout.contentContainerMaxWidth)
                                .fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                    .padding(
                        horizontal = contentHorizontalPadding,
                        vertical = layout.contentVerticalPadding,
                    ),
                horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(layout.contentWidthFraction)
                        .widthIn(max = layout.contentMaxWidth),
                    horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
                ) {
                    val logoWidth = containerWidth
                        .times(layout.contentWidthFraction * layout.logoWidthFraction)
                        .coerceAtMost(layout.contentMaxWidth * layout.logoWidthFraction)
                    val logoShape = RoundedCornerShape(NuvioTokens.Radius.md)
                    val metaShape = RoundedCornerShape(NuvioTokens.Radius.sm)

                    Box(
                        modifier = Modifier.width(logoWidth).height(logoWidth / 2.6f),
                        contentAlignment = if (layout.isTablet) Alignment.BottomStart else Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(logoWidth)
                                .height(32.dp)
                                .background(tokens.colors.surfaceCard, logoShape)
                                .skeleton(logoShape),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(156.dp)
                            .height(10.dp)
                            .background(tokens.colors.surfaceCard, metaShape)
                            .skeleton(metaShape),
                    )
                }
                if (!layout.isTablet) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val ctaShape = RoundedCornerShape(40.dp)
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(48.dp)
                            .background(tokens.colors.surfaceCard, ctaShape)
                            .skeleton(ctaShape)
                            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, ctaShape),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun HomeSkeletonRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp? = null,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    val basePosterWidthDp = if (isDesktop) {
        desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    } else {
        posterCardStyle.widthDp
    }
    val skeletonWidth = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterWidth(basePosterWidthDp)
    } else {
        basePosterWidthDp.dp
    }
    val skeletonHeight = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterHeightForWidth(skeletonWidth)
    } else {
        if (isDesktop) {
            ((posterCardStyle.heightDp * basePosterWidthDp.toFloat()) / posterCardStyle.widthDp).toInt().dp
        } else {
            posterCardStyle.heightDp.dp
        }
    }
    val cornerRadius = resolvedPosterCornerRadius(posterCardStyle.cornerRadiusDp)
    val cardShape = RoundedCornerShape(cornerRadius)
    val labelShape = RoundedCornerShape(NuvioTokens.Radius.sm)
    val itemSpacing = if (isDesktop) tokens.spacing.railGap else 10.dp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sectionPadding = horizontalPadding ?: homeSectionHorizontalPaddingForWidth(maxWidth.value)
        val count = ((maxWidth - sectionPadding) / (skeletonWidth + itemSpacing)).toInt().coerceAtLeast(0) + 2

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.padding(horizontal = sectionPadding).height(32.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(128.dp)
                        .height(16.dp)
                        .background(tokens.colors.surfaceElevated, labelShape)
                        .skeleton(labelShape),
                )
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = sectionPadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                userScrollEnabled = false,
            ) {
                items(count) {
                    Column(
                        modifier = Modifier.width(skeletonWidth),
                        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(skeletonWidth / skeletonHeight)
                                .clip(cardShape)
                                .background(tokens.colors.surfaceCard, cardShape)
                                .skeleton(cardShape)
                                .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, cardShape),
                        )
                        if (!posterCardStyle.hideLabelsEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(16.dp)
                                    .background(tokens.colors.surfaceElevated, labelShape)
                                    .skeleton(labelShape),
                            )
                            if (!posterCardStyle.catalogLandscapeModeEnabled) {
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
        }
    }
}

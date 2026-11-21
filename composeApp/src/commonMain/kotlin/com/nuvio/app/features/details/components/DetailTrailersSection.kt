package com.nuvio.app.features.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.Menu
import com.nuvio.app.core.ui.MenuItem
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.isDesktop
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.detail_tab_trailer
import nuvio.composeapp.generated.resources.detail_trailer_category_count
import nuvio.composeapp.generated.resources.detail_trailers_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailTrailersSection(
    trailers: List<MetaTrailer>,
    onTrailerClick: (MetaTrailer) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    if (trailers.isEmpty()) return

    val tokens = MaterialTheme.nuvio
    val trailerLabel = stringResource(Res.string.detail_tab_trailer)
    val grouped = remember(trailers) {
        linkedMapOf<String, MutableList<MetaTrailer>>().apply {
            trailers.forEach { trailer ->
                val category = trailer.type.ifBlank { trailerLabel }
                getOrPut(category) { mutableListOf() }.add(trailer)
            }
        }
    }

    if (grouped.isEmpty()) return

    val initialCategory = remember(grouped) {
        grouped.keys.firstOrNull { it.equals(trailerLabel, ignoreCase = true) }
            ?: grouped.keys.first()
    }
    var selectedCategory by remember(grouped) { mutableStateOf(initialCategory) }
    var menuExpanded by remember { mutableStateOf(false) }

    val selectedTrailers = grouped[selectedCategory].orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizing = trailerSectionSizing(maxWidth.value)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showHeader) {
                    DetailSectionTitle(
                        title = stringResource(Res.string.detail_trailers_title),
                        fullWidth = false,
                    )
                }

                Box {
                    val selectorShape = RoundedCornerShape(sizing.selectorRadius)
                    val selectorInteraction = remember { MutableInteractionSource() }
                    val isSelectorHovered by selectorInteraction.collectIsHoveredAsState()
                    Surface(
                        shape = selectorShape,
                        color = if (isDesktop && isSelectorHovered) tokens.colors.surfaceCard else tokens.colors.surfaceElevated,
                        border = BorderStroke(
                            NuvioTokens.Border.thin,
                            if (isDesktop && isSelectorHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                        ),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .nuvioFocusBorder(selectorShape)
                            .clip(selectorShape)
                            .then(
                                if (isDesktop) {
                                    Modifier
                                        .hoverable(selectorInteraction)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { menuExpanded = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = sizing.selectorHorizontalPadding,
                                vertical = sizing.selectorVerticalPadding,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = selectedCategory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = tokens.colors.textPrimary,
                            )
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = tokens.colors.textSecondary,
                                modifier = Modifier.size(sizing.selectorIconSize),
                            )
                        }
                    }

                    Menu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        grouped.keys.forEach { category ->
                            val count = grouped[category]?.size ?: 0
                            MenuItem(
                                text = stringResource(Res.string.detail_trailer_category_count, category, count),
                                selected = category == selectedCategory,
                                onClick = {
                                    selectedCategory = category
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizing = trailerSectionSizing(maxWidth.value)
            val rowState = rememberLazyListState()
            LazyRow(
                state = rowState,
                modifier = Modifier
                    .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                    .fillMaxWidth()
                    .nuvioDesktopDragScroll(rowState),
                contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                horizontalArrangement = Arrangement.spacedBy(sizing.cardSpacing),
            ) {
                itemsIndexed(
                    items = selectedTrailers,
                    key = { index, trailer -> "${trailer.type}-${trailer.id}-${trailer.seasonNumber ?: 0}#$index" },
                ) { _, trailer ->
                    TrailerCard(
                        trailer = trailer,
                        cardWidth = sizing.cardWidth,
                        cornerRadius = sizing.cardRadius,
                        titleFontSize = sizing.titleFontSize,
                        metaFontSize = sizing.metaFontSize,
                        onClick = { onTrailerClick(trailer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailerCard(
    trailer: MetaTrailer,
    cardWidth: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    metaFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
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
        label = "trailer_card_border",
    )
    val playScale by animateFloatAsState(
        targetValue = if (isDesktop && isHovered) 1.08f else 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "trailer_play_emblem_scale",
    )

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(cardShape)
            .background(tokens.colors.surfaceCard)
            .nuvioCardDepth(
                shape = cardShape,
                surface = NuvioCardDepthSurface.Trailers,
                fallbackBorderAlpha = 0f,
            )
            .border(NuvioTokens.Border.thin, borderColor, cardShape)
            .nuvioFocusBorder(cardShape)
            .then(
                if (isDesktop) {
                    Modifier
                        .hoverable(interactionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(tokens.colors.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "https://img.youtube.com/vi/${trailer.key}/hqdefault.jpg",
                contentDescription = trailer.displayName ?: trailer.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.60f to tokens.colors.background.copy(alpha = 0.16f),
                            1f to tokens.colors.surfaceCard.copy(alpha = 0.72f),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = playScale
                        scaleY = playScale
                    }
                    .clip(CircleShape)
                    .background(
                        tokens.colors.surfaceElevated.copy(
                            alpha = if (isDesktop && isHovered) 0.90f else 0.76f,
                        ),
                    )
                    .border(
                        width = NuvioTokens.Border.thin,
                        color = if (isDesktop && isHovered) {
                            tokens.colors.accent.copy(alpha = 0.70f)
                        } else {
                            tokens.colors.borderDefault
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = tokens.colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTokens.Space.s12, vertical = NuvioTokens.Space.s10),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
        ) {
            Text(
                text = trailer.displayName ?: trailer.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = titleFontSize,
                ),
                color = tokens.colors.textPrimary,
            )

            val year = trailer.publishedAt?.take(4).orEmpty()
            val subtitle = listOfNotNull(
                trailer.type.takeIf { it.isNotBlank() },
                year.takeIf { it.isNotBlank() },
            ).joinToString(" • ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = metaFontSize),
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class TrailerSectionSizing(
    val cardWidth: androidx.compose.ui.unit.Dp,
    val cardSpacing: androidx.compose.ui.unit.Dp,
    val cardRadius: androidx.compose.ui.unit.Dp,
    val selectorRadius: androidx.compose.ui.unit.Dp,
    val selectorHorizontalPadding: androidx.compose.ui.unit.Dp,
    val selectorVerticalPadding: androidx.compose.ui.unit.Dp,
    val selectorIconSize: androidx.compose.ui.unit.Dp,
    val titleFontSize: androidx.compose.ui.unit.TextUnit,
    val metaFontSize: androidx.compose.ui.unit.TextUnit,
)

private fun trailerSectionSizing(maxWidthDp: Float): TrailerSectionSizing =
    when {
        maxWidthDp >= 1200f -> TrailerSectionSizing(
            cardWidth = 280.dp,
            cardSpacing = 16.dp,
            cardRadius = 14.dp,
            selectorRadius = 10.dp,
            selectorHorizontalPadding = 12.dp,
            selectorVerticalPadding = 7.dp,
            selectorIconSize = 20.dp,
            titleFontSize = 15.sp,
            metaFontSize = 12.sp,
        )

        maxWidthDp >= 1024f -> TrailerSectionSizing(
            cardWidth = 260.dp,
            cardSpacing = 14.dp,
            cardRadius = 14.dp,
            selectorRadius = 10.dp,
            selectorHorizontalPadding = 12.dp,
            selectorVerticalPadding = 6.dp,
            selectorIconSize = 18.dp,
            titleFontSize = 14.sp,
            metaFontSize = 12.sp,
        )

        maxWidthDp >= 768f -> TrailerSectionSizing(
            cardWidth = 240.dp,
            cardSpacing = 12.dp,
            cardRadius = 14.dp,
            selectorRadius = 10.dp,
            selectorHorizontalPadding = 10.dp,
            selectorVerticalPadding = 5.dp,
            selectorIconSize = 18.dp,
            titleFontSize = 14.sp,
            metaFontSize = 12.sp,
        )

        else -> TrailerSectionSizing(
            cardWidth = 208.dp,
            cardSpacing = 12.dp,
            cardRadius = 14.dp,
            selectorRadius = 10.dp,
            selectorHorizontalPadding = 10.dp,
            selectorVerticalPadding = 5.dp,
            selectorIconSize = 18.dp,
            titleFontSize = 13.sp,
            metaFontSize = 11.sp,
        )
    }

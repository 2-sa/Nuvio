package com.nuvio.app.features.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.LocalScreenActive
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.desktopCatalogShelfPosterBaseWidthDp
import com.nuvio.app.core.ui.desktopPosterHoverScale
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionFolder
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.isDesktop

@Composable
fun HomeCollectionRowSection(
    collection: Collection,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    animateGifs: Boolean = true,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
) {
    if (collection.folders.isEmpty()) return

    if (sectionPadding != null) {
        HomeCollectionRowSectionContent(
            collection = collection,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            animateGifs = animateGifs,
            onFolderClick = onFolderClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeCollectionRowSectionContent(
                collection = collection,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                animateGifs = animateGifs,
                onFolderClick = onFolderClick,
            )
        }
    }
}

@Composable
private fun HomeCollectionRowSectionContent(
    collection: Collection,
    modifier: Modifier,
    sectionPadding: Dp,
    animateGifs: Boolean,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    NuvioShelfSection(
        title = collection.title,
        entries = collection.folders,
        modifier = modifier,
        headerHorizontalPadding = sectionPadding,
        rowContentPadding = PaddingValues(horizontal = sectionPadding),
        itemSpacing = if (isDesktop) tokens.spacing.railGap else 10.dp,
        key = { folder -> "collection_${collection.id}_folder_${folder.id}" },
    ) { folder ->
        CollectionFolderCard(
            folder = folder,
            animateGifs = animateGifs,
            onClick = onFolderClick?.let { { it(collection.id, folder.id) } },
        )
    }
}

@Composable
private fun CollectionFolderCard(
    folder: CollectionFolder,
    modifier: Modifier = Modifier,
    animateGifs: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val posterCardStyle = rememberPosterCardStyleUiState()
    val basePosterWidthDp = desktopCatalogShelfPosterBaseWidthDp(posterCardStyle.widthDp)
    val isLandscapeMode = posterCardStyle.catalogLandscapeModeEnabled
    val shape = if (isLandscapeMode) PosterShape.Landscape else folder.posterShape
    val cardWidth: Dp
    val aspectRatio: Float

    when (shape) {
        PosterShape.Poster -> {
            cardWidth = basePosterWidthDp.dp
            aspectRatio = 0.675f
        }
        PosterShape.Landscape -> {
            cardWidth = landscapePosterWidth(basePosterWidthDp)
            aspectRatio = PosterLandscapeAspectRatio
        }
        PosterShape.Square -> {
            cardWidth = basePosterWidthDp.dp
            aspectRatio = 1f
        }
    }

    val shapeCorner = RoundedCornerShape(resolvedPosterCornerRadius(posterCardStyle.cornerRadiusDp))
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) {
            tokens.colors.accent.copy(alpha = 0.70f)
        } else {
            tokens.colors.borderSubtle
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "collection_folder_card_border",
    )

    Column(
        modifier = Modifier
            .desktopPosterHoverScale()
            .nuvioFocusBorder(shapeCorner)
            .then(if (isDesktop) Modifier.hoverable(interactionSource) else Modifier)
            .posterCardClickable(onClick = onClick, onLongClick = null)
            .then(modifier)
            .width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s6),
    ) {
        val imageUrl = collectionFolderCardImageUrl(folder)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .nuvioCardDepth(
                    shape = shapeCorner,
                    surface = NuvioCardDepthSurface.Posters,
                ),
            shape = shapeCorner,
            colors = CardDefaults.cardColors(
                containerColor = tokens.colors.surfaceCard,
            ),
            border = BorderStroke(NuvioTokens.Border.thin, borderColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = tokens.elevation.flat,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !imageUrl.isNullOrBlank() -> {
                        CollectionCardRemoteImage(
                            imageUrl = imageUrl,
                            staticImageUrl = firstNonBlank(folder.coverImageUrl),
                            contentDescription = folder.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            animateIfPossible = animateGifs &&
                                isAnimatedCollectionFolderImage(folder, imageUrl) &&
                                LocalScreenActive.current,
                        )
                    }
                    !folder.coverEmoji.isNullOrBlank() -> {
                        Text(
                            text = folder.coverEmoji,
                            fontSize = 36.sp,
                        )
                    }
                    else -> {
                        Text(
                            text = folder.title.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 0.sp),
                            color = tokens.colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (!folder.hideTitle) {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    lineHeight = 19.sp,
                ),
                color = tokens.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun collectionFolderCardImageUrl(folder: CollectionFolder): String? {
    return if (folder.mobileFocusGifEnabled) {
        firstNonBlank(folder.focusGifUrl, folder.coverImageUrl)
    } else {
        firstNonBlank(folder.coverImageUrl)
    }
}

private fun firstNonBlank(vararg candidates: String?): String? {
    return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()
}

private fun isAnimatedCollectionFolderImage(
    folder: CollectionFolder,
    imageUrl: String,
): Boolean {
    val gifUrl = firstNonBlank(folder.focusGifUrl) ?: return false
    return folder.mobileFocusGifEnabled && imageUrl == gifUrl
}

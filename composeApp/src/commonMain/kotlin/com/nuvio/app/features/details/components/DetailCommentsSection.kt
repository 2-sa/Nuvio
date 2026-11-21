package com.nuvio.app.features.details.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.SkeletonBlock
import com.nuvio.app.core.ui.nuvioDesktopDragScroll
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.features.trakt.TraktCommentReview
import com.nuvio.app.isDesktop
import kotlinx.coroutines.flow.distinctUntilChanged
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailCommentsSection(
    comments: List<TraktCommentReview>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    val tokens = MaterialTheme.nuvio
    val listState = rememberLazyListState()

    LaunchedEffect(listState, comments.size, canLoadMore, isLoadingMore, isLoading, error) {
        if (isLoading || !error.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) onLoadMore()
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            CommentsHeader()
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                val loadingListState = rememberLazyListState()
                LazyRow(
                    state = loadingListState,
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(loadingListState),
                    contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(3) {
                        LoadingCommentCard()
                    }
                }
            }

            !error.isNullOrBlank() -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.colors.surfaceElevated,
                            contentColor = tokens.colors.textPrimary,
                        ),
                    ) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            comments.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.detail_comments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
            }

            else -> {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth()
                        .nuvioDesktopDragScroll(listState),
                    contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = comments.withDuplicateSafeLazyKeys { it.id },
                        key = { it.lazyKey },
                    ) { keyedReview ->
                        val review = keyedReview.value
                        CommentCard(
                            review = review,
                            onClick = { onCommentClick(review) },
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading_more_comments") {
                            LoadingCommentCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsHeader() {
    val tokens = MaterialTheme.nuvio
    BoxWithConstraints {
        val isTablet = maxWidth >= 720.dp
        val titleSize = if (isTablet) 22.sp else 20.sp

        Text(
            text = stringResource(Res.string.detail_comments_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = titleSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = tokens.colors.textPrimary,
        )
    }
}

@Composable
private fun CommentCard(
    review: TraktCommentReview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isDesktop && isHovered) tokens.colors.borderStrong else tokens.colors.borderSubtle,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "comment_card_border",
    )

    BoxWithConstraints {
        val isTablet = maxWidth >= 720.dp
        val cardWidth = if (isTablet) 340.dp else 280.dp
        val cardHeight = if (isTablet) 216.dp else 196.dp

        Surface(
            modifier = modifier
                .width(cardWidth)
                .height(cardHeight)
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
            shape = cardShape,
            color = tokens.colors.surfaceCard,
            border = BorderStroke(NuvioTokens.Border.thin, borderColor),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NuvioTokens.Space.s16),
                verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = review.authorDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (review.review) {
                        CommentChip(text = stringResource(Res.string.detail_comments_badge_review))
                    }
                }

                if (review.hasSpoilerContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(NuvioTokens.Radius.md))
                            .background(tokens.colors.surfaceElevated)
                            .border(NuvioTokens.Border.thin, tokens.colors.borderDefault, RoundedCornerShape(NuvioTokens.Radius.md))
                            .padding(NuvioTokens.Space.s12),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = tokens.colors.warning,
                                modifier = Modifier.size(NuvioTokens.Icon.sm),
                            )
                            Text(
                                text = stringResource(Res.string.detail_comments_spoiler_card),
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = tokens.colors.warning,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Text(
                        text = review.comment,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = tokens.colors.textSecondary,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(
                    thickness = NuvioTokens.Border.hairline,
                    color = tokens.colors.borderSubtle,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    review.rating?.let { rating ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = tokens.colors.warning,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = stringResource(Res.string.detail_comments_badge_rating, rating),
                                style = MaterialTheme.typography.labelMedium,
                                color = tokens.colors.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = stringResource(Res.string.detail_comments_likes, review.likes),
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentChip(text: String) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.full)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(tokens.colors.focusBackground)
            .border(NuvioTokens.Border.thin, tokens.colors.accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s4),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tokens.colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadingCommentCard() {
    val tokens = MaterialTheme.nuvio
    val cardShape = RoundedCornerShape(14.dp)
    BoxWithConstraints {
        val isTablet = maxWidth >= 720.dp
        val cardWidth = if (isTablet) 340.dp else 280.dp
        val cardHeight = if (isTablet) 216.dp else 196.dp

        Surface(
            modifier = Modifier.width(cardWidth).height(cardHeight),
            shape = cardShape,
            color = tokens.colors.surfaceCard,
            border = BorderStroke(NuvioTokens.Border.thin, tokens.colors.borderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBlock(width = 96.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.94f), height = 12.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.68f), height = 12.dp)
                Spacer(modifier = Modifier.weight(1f))
                SkeletonBlock(width = 64.dp, height = 10.dp)
            }
        }
    }
}

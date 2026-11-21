package com.nuvio.app.features.details.components

import com.nuvio.app.core.ui.NuvioDesktopAnimation
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.DesktopBackdropVerticalBias
import com.nuvio.app.core.ui.StandardDesktopViewportAspectRatio
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.desktopPageHorizontalPaddingForWidth
import com.nuvio.app.core.ui.fullscreenActionHorizontalInsetForWidth
import com.nuvio.app.core.ui.expandingWideArtworkWidthDp
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.ui.WideDesktopViewportAspectRatio
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioFocusBorder
import com.nuvio.app.core.ui.secondaryClick
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.formatRuntimeForDisplay
import com.nuvio.app.features.tmdb.originalTmdbImageUrl
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.detail_logo_content_description
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.playback_unavailable
import nuvio.composeapp.generated.resources.random_episode_title
import nuvio.composeapp.generated.resources.shuffle_stop
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopDetailBackdrop(
    meta: MetaDetails,
    viewportHeight: Dp,
    heroTrailerSourceUrl: String?,
    heroTrailerSourceAudioUrl: String?,
    heroTrailerReady: Boolean,
    heroTrailerPlayWhenReady: Boolean,
    heroTrailerMuted: Boolean,
    heroGradientColor: Color? = null,
    blurBackdrop: Boolean = false,
    onBackdropLoaded: (Painter) -> Unit = {},
    onHeroTrailerReady: () -> Unit,
    onHeroTrailerEnded: () -> Unit,
    onHeroTrailerError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val sideGradientColor = heroGradientColor ?: tokens.colors.background
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val opacity = NuvioTokens.Opacity
    val trailerAlpha by animateFloatAsState(
        targetValue = if (heroTrailerReady) 1f else 0f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_trailer_alpha",
    )
    val gradientIntensity by animateFloatAsState(
        targetValue = if (heroTrailerReady) 0.3f else 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_gradient_intensity",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(sideGradientColor),
    ) {
        val hasValidViewportHeight = viewportHeight.value > 0f
        val aspectRatio = if (hasValidViewportHeight) {
            maxWidth.value / viewportHeight.value
        } else {
            StandardDesktopViewportAspectRatio
        }
        val useWideArtworkFrame = aspectRatio > StandardDesktopViewportAspectRatio
        val cropSideBackdrop = aspectRatio > WideDesktopViewportAspectRatio
        val backdropWidth = if (useWideArtworkFrame) {
            expandingWideArtworkWidthDp(maxWidth.value, viewportHeight.value).dp
        } else {
            maxWidth
        }
        val backdropModifier = Modifier
            .align(Alignment.CenterEnd)
            .width(backdropWidth)
            .fillMaxHeight()
        val artworkModifier = if (blurBackdrop) backdropModifier.blur(30.dp) else backdropModifier
        val bottomSpreadStrength = if (hasValidViewportHeight) {
            ((21f / 9f - aspectRatio) / (5f / 9f)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val sideFadeStops = remember(sideGradientColor, gradientIntensity, opacity, isRtl) {
            val ltrStops = arrayOf(
                0.00f to sideGradientColor,
                0.14f to sideGradientColor.copy(alpha = 0.96f * gradientIntensity),
                0.36f to sideGradientColor.copy(alpha = opacity.overlayHeavy * gradientIntensity),
                0.62f to sideGradientColor.copy(alpha = opacity.overlayLight * gradientIntensity),
                0.86f to sideGradientColor.copy(alpha = opacity.subtle * gradientIntensity),
                1.00f to Color.Transparent,
            )
            if (!isRtl) {
                ltrStops
            } else {
                ltrStops.reversed().map { (stop, color) -> (1f - stop) to color }.toTypedArray()
            }
        }
        val baseSideFade = Brush.horizontalGradient(colorStops = sideFadeStops)
        val bottomBlendFade = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.52f to Color.Transparent,
                0.76f to sideGradientColor.copy(alpha = 0.50f * gradientIntensity),
                0.92f to sideGradientColor.copy(alpha = 0.88f * gradientIntensity),
                1.00f to sideGradientColor,
            ),
        )
        val imageUrl = meta.background ?: meta.poster
        if (imageUrl != null) {
            AsyncImage(
                model = originalTmdbImageUrl(imageUrl),
                contentDescription = meta.name,
                desktopAnimation = NuvioDesktopAnimation.Always,
                modifier = artworkModifier,
                alignment = BiasAlignment(0f, DesktopBackdropVerticalBias),
                contentScale = if (useWideArtworkFrame && !cropSideBackdrop) ContentScale.Fit else ContentScale.Crop,
                desktopImageScaling = NuvioDesktopImageScaling.Disabled,
                onSuccess = { state -> onBackdropLoaded(state.painter) },
            )
        } else {
            DesktopStripePlaceholder(modifier = backdropModifier)
        }

        if (heroTrailerSourceUrl != null) {
            HeroTrailerPlayerSurface(
                sourceUrl = heroTrailerSourceUrl,
                sourceAudioUrl = heroTrailerSourceAudioUrl,
                playWhenReady = heroTrailerPlayWhenReady,
                muted = heroTrailerMuted,
                fillFrame = true,
                modifier = artworkModifier.graphicsLayer { alpha = trailerAlpha },
                onReady = onHeroTrailerReady,
                onEnded = onHeroTrailerEnded,
                onError = onHeroTrailerError,
            )
        }

        Box(
            modifier = backdropModifier
                .drawWithCache {
                    if (size.width <= 0f || size.height <= 0f) {
                        return@drawWithCache onDrawBehind {}
                    }
                    val bottomSpread = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to sideGradientColor.copy(alpha = 0.14f * gradientIntensity * bottomSpreadStrength),
                            0.55f to sideGradientColor.copy(alpha = 0.12f * gradientIntensity * bottomSpreadStrength),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset(if (isRtl) size.width else 0f, size.height * 1.15f),
                        radius = (size.width * 0.72f).coerceAtLeast(1f),
                    )
                    onDrawBehind {
                        drawRect(baseSideFade)
                        drawRect(bottomBlendFade)
                        if (bottomSpreadStrength > 0f) drawRect(bottomSpread)
                    }
                },
        )
    }
}

@Composable
fun DesktopDetailHero(
    meta: MetaDetails,
    showOverallRatings: Boolean,
    isMdbListActive: Boolean,
    playButtonLabel: String,
    isPrimaryPlayEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    onHeightChanged: (Int) -> Unit,
    heroTrailerSourceUrl: String?,
    heroTrailerReady: Boolean,
    heroTrailerMuted: Boolean,
    onHeroTrailerMuteToggle: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    shuffleEnabled: Boolean,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val space = NuvioTokens.Space
    val trailerAlpha by animateFloatAsState(
        targetValue = if (heroTrailerReady) 1f else 0f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.sheetEnterMillis),
        label = "desktop_detail_hero_controls_alpha",
    )
    var logoLoadError by remember(meta.id, meta.logo) { mutableStateOf(false) }
    val logoUrl = meta.logo?.takeIf { it.isNotBlank() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 660.dp)
            .onSizeChanged { onHeightChanged(it.height) },
    ) {
        val actionHorizontalInset = fullscreenActionHorizontalInsetForWidth(maxWidth.value)
        val pageHorizontalPadding = desktopPageHorizontalPaddingForWidth(maxWidth.value)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(max = 760.dp)
                .padding(
                    start = pageHorizontalPadding,
                    end = space.s32,
                    bottom = space.s40,
                ),
        ) {
            if (logoUrl != null && !logoLoadError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = stringResource(Res.string.detail_logo_content_description, meta.name),
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .height(120.dp),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    onError = { logoLoadError = true },
                )
            } else {
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = NuvioTokens.Type.displayMd,
                        lineHeight = NuvioTokens.LineHeight.displayMd,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                        shadow = Shadow(
                            color = tokens.colors.overlayScrim,
                            offset = Offset(0f, 2f),
                            blurRadius = 10f,
                        ),
                    ),
                    color = tokens.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(space.s20))
            DesktopHeroMetaRow(meta = meta, showOverallRatings = showOverallRatings && !isMdbListActive)
            if (isMdbListActive && meta.externalRatings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(space.s12))
                DetailRatingsRow(
                    ratings = meta.externalRatings,
                    modifier = Modifier.widthIn(max = 560.dp),
                )
            }
            if (meta.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(space.s12))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(space.s8),
                    verticalArrangement = Arrangement.spacedBy(space.s6),
                ) {
                    meta.genres.take(4).forEach { genre ->
                        DesktopHeroGlassPill(text = genre)
                    }
                }
            }
            meta.description?.takeIf { it.isNotBlank() }?.let { synopsis ->
                Spacer(modifier = Modifier.height(space.s16))
                ExpandableDescription(
                    text = synopsis,
                    collapsedMaxLines = 4,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = NuvioTokens.Type.bodyLg,
                        lineHeight = 22.sp,
                        letterSpacing = NuvioTokens.LetterSpacing.none,
                    ),
                    color = tokens.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.height(space.s24))
            DesktopHeroActionRow(
                playLabel = if (isPrimaryPlayEnabled) playButtonLabel else stringResource(Res.string.playback_unavailable),
                playEnabled = isPrimaryPlayEnabled,
                isSaved = isSaved,
                isWatched = isWatched,
                shuffleEnabled = shuffleEnabled,
                onPlayClick = onPlayClick,
                onPlayLongClick = onPlayLongClick,
                onShuffleClick = onShuffleClick,
                onWatchedClick = onWatchedClick,
                onSaveClick = onSaveClick,
                onSaveLongClick = onSaveLongClick,
            )
        }

        if (heroTrailerSourceUrl != null) {
            val muteInteraction = remember { MutableInteractionSource() }
            val isMuteHovered by muteInteraction.collectIsHoveredAsState()
            val muteBgAlpha by animateFloatAsState(
                targetValue = if (isMuteHovered) 0.92f else 0.78f,
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "desktop_hero_mute_alpha",
            )
            val muteBorderColor by animateColorAsState(
                targetValue = if (isMuteHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "desktop_hero_mute_border",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(
                        top = space.s32,
                        end = actionHorizontalInset + if (isFullscreenActionSupported) 52.dp else 0.dp,
                    )
                    .size(40.dp)
                    .graphicsLayer { alpha = trailerAlpha }
                    .nuvioFocusBorder(CircleShape)
                    .clip(CircleShape)
                    .background(tokens.colors.surfaceElevated.copy(alpha = muteBgAlpha))
                    .border(NuvioTokens.Border.thin, muteBorderColor, CircleShape)
                    .hoverable(muteInteraction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = muteInteraction,
                        indication = null,
                        enabled = heroTrailerReady,
                        role = Role.Button,
                        onClick = onHeroTrailerMuteToggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (heroTrailerMuted) {
                        Icons.AutoMirrored.Rounded.VolumeOff
                    } else {
                        Icons.AutoMirrored.Rounded.VolumeUp
                    },
                    contentDescription = null,
                    tint = tokens.colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (isFullscreenActionSupported) {
            val fsInteraction = remember { MutableInteractionSource() }
            val isFsHovered by fsInteraction.collectIsHoveredAsState()
            val fsBgAlpha by animateFloatAsState(
                targetValue = if (isFsHovered) 0.92f else 0.78f,
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "desktop_hero_fs_alpha",
            )
            val fsBorderColor by animateColorAsState(
                targetValue = if (isFsHovered) tokens.colors.borderStrong else tokens.colors.borderDefault,
                animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
                label = "desktop_hero_fs_border",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = space.s32, end = actionHorizontalInset)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tokens.colors.surfaceElevated.copy(alpha = fsBgAlpha))
                    .border(NuvioTokens.Border.thin, fsBorderColor, CircleShape)
                    .hoverable(fsInteraction)
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center,
            ) {
                FullscreenActionButton(
                    buttonSize = 40.dp,
                    iconSize = 20.dp,
                    containerColor = Color.Transparent,
                    contentColor = tokens.colors.textPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopHeroActionRow(
    playLabel: String,
    playEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    shuffleEnabled: Boolean,
    onPlayClick: () -> Unit,
    onPlayLongClick: (() -> Unit)?,
    onShuffleClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val space = NuvioTokens.Space
    val ctaShape = RoundedCornerShape(NuvioTokens.Radius.lg)
    val playInteraction = remember { MutableInteractionSource() }
    val isPlayHovered by playInteraction.collectIsHoveredAsState()
    val playScale by animateFloatAsState(
        targetValue = if (playEnabled && isPlayHovered) 1.025f else 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktop_detail_play_scale",
    )
    val playElevation by animateDpAsState(
        targetValue = if (playEnabled && isPlayHovered) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktop_detail_play_elevation",
    )
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space.s12),
        verticalArrangement = Arrangement.spacedBy(space.s10),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .height(46.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .then(
                    if (playEnabled) {
                        Modifier.shadow(elevation = playElevation, shape = ctaShape, clip = false)
                    } else {
                        Modifier
                    },
                )
                .nuvioFocusBorder(ctaShape)
                .clip(ctaShape)
                .then(
                    if (playEnabled) {
                        Modifier
                            .background(palette.accentBrush())
                            .then(
                                if (isPlayHovered) {
                                    Modifier.background(Color.White.copy(alpha = NuvioTokens.Opacity.hover))
                                } else {
                                    Modifier
                                },
                            )
                    } else {
                        Modifier
                            .background(tokens.colors.surfaceElevated.copy(alpha = 0.76f))
                            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, ctaShape)
                    },
                )
                .hoverable(playInteraction)
                .then(if (playEnabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .combinedClickable(
                    interactionSource = playInteraction,
                    indication = null,
                    enabled = playEnabled,
                    role = Role.Button,
                    onClick = onPlayClick,
                    onLongClick = onPlayLongClick,
                )
                .secondaryClick(onPlayLongClick.takeIf { playEnabled })
                .padding(horizontal = space.s20),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = playPainter,
                    contentDescription = null,
                    tint = if (playEnabled) tokens.colors.onAccent else tokens.colors.textDisabled,
                    modifier = Modifier.size(NuvioTokens.Icon.md),
                )
                Text(
                    text = playLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (playEnabled) tokens.colors.onAccent else tokens.colors.textDisabled,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DesktopHeroGlassActionPill(
            label = stringResource(if (isSaved) Res.string.hero_remove_from_library else Res.string.hero_add_to_library),
            icon = if (isSaved) Icons.Default.Check else Icons.Default.Add,
            isActive = isSaved,
            onClick = onSaveClick,
            onLongClick = onSaveLongClick,
        )

        DesktopHeroGlassActionPill(
            label = stringResource(if (isWatched) Res.string.hero_mark_unwatched else Res.string.hero_mark_watched),
            icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
            isActive = isWatched,
            onClick = onWatchedClick,
        )

        if (onShuffleClick != null) {
            DesktopHeroGlassActionPill(
                label = stringResource(if (shuffleEnabled) Res.string.shuffle_stop else Res.string.random_episode_title),
                icon = Icons.Default.Shuffle,
                isActive = shuffleEnabled,
                onClick = onShuffleClick,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopHeroGlassActionPill(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    val pillShape = RoundedCornerShape(NuvioTokens.Radius.lg)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> tokens.colors.focusBackground.copy(alpha = 0.88f)
            isHovered -> tokens.colors.surfaceCard.copy(alpha = 0.88f)
            else -> tokens.colors.surfaceElevated.copy(alpha = 0.76f)
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktop_detail_glass_pill_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> tokens.colors.accent
            isHovered -> tokens.colors.borderStrong
            else -> tokens.colors.borderDefault
        },
        animationSpec = tween(durationMillis = NuvioTokens.Motion.fastMillis),
        label = "desktop_detail_glass_pill_border",
    )
    val contentColor = if (isActive || isHovered) tokens.colors.textPrimary else tokens.colors.textSecondary

    Row(
        modifier = Modifier
            .height(44.dp)
            .nuvioFocusBorder(pillShape)
            .clip(pillShape)
            .background(backgroundColor)
            .border(NuvioTokens.Border.thin, borderColor, pillShape)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .secondaryClick(onLongClick)
            .padding(horizontal = NuvioTokens.Space.s16),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) tokens.colors.accent else contentColor,
            modifier = Modifier.size(NuvioTokens.Icon.sm),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesktopHeroGlassPill(
    text: String,
    contentColor: Color = MaterialTheme.nuvio.colors.textSecondary,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.md)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(tokens.colors.surfaceElevated.copy(alpha = 0.74f))
            .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, shape)
            .padding(horizontal = NuvioTokens.Space.s10, vertical = NuvioTokens.Space.s4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = NuvioTokens.LetterSpacing.none,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesktopHeroMetaRow(meta: MetaDetails, showOverallRatings: Boolean) {
    val tokens = MaterialTheme.nuvio
    val space = NuvioTokens.Space
    val metaItems = buildList {
        desktopYearLabel(meta)?.let(::add)
        desktopSeasonCountLabel(meta)?.let(::add)
        formatRuntimeForDisplay(meta.runtime)?.let(::add)
    }
    val validImdbRating = meta.imdbRating
        ?.takeIf { raw -> raw.toDoubleOrNull()?.let { it > 0.0 } == true }
    val ratingMedia = remember(meta) { meta.ratingReference() }
    val ratingState by rememberRatingState(ratingMedia)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space.s12),
        verticalArrangement = Arrangement.spacedBy(space.s8),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        metaItems.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = NuvioTokens.Type.bodyLg,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = tokens.colors.textPrimary,
                maxLines = 1,
            )
        }
        meta.ageRating?.takeIf { it.isNotBlank() }?.let { rating ->
            DesktopHeroGlassPill(text = rating)
        }
        if (validImdbRating != null && showOverallRatings) {
            val imdbShape = RoundedCornerShape(NuvioTokens.Radius.md)
            val imdbTextStyle = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Row(
                modifier = Modifier
                    .clip(imdbShape)
                    .background(tokens.colors.surfaceElevated.copy(alpha = 0.74f))
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, imdbShape)
                    .padding(horizontal = space.s8, vertical = space.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImdbRatingSourceLabel(
                    storeTextStyle = imdbTextStyle,
                    storeTextColor = ImdbYellow,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = validImdbRating,
                    style = imdbTextStyle,
                    color = ImdbYellow,
                )
            }
        }
        if (ratingState.isAvailable) {
            DetailRating(
                media = ratingMedia,
                title = meta.name,
                state = ratingState,
            )
        }
    }
}

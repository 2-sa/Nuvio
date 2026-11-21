package com.nuvio.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.LocalVerticalBarPadding
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.centeredVerticalBarPadding
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioTypeScale
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.player.subtitleLoadingStatusMessage
import com.nuvio.app.features.streams.StreamLaunch
import com.nuvio.app.features.streams.StreamsUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_close
import nuvio.composeapp.generated.resources.debrid_resolving_stream
import nuvio.composeapp.generated.resources.player_loading_preparing
import nuvio.composeapp.generated.resources.streams_episode_badge
import nuvio.composeapp.generated.resources.streams_finding_source
import nuvio.composeapp.generated.resources.streams_loading_subtitles
import nuvio.composeapp.generated.resources.youtube_resolving_stream
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StreamLoadingScreen(
    launch: StreamLaunch,
    state: StreamsUiState,
    showStatus: Boolean,
    resolvingDebridStream: Boolean,
    resolvingYouTubeStream: Boolean = false,
    onBack: () -> Unit,
    preparingPlayback: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val message = when {
        !showStatus -> null
        resolvingDebridStream -> stringResource(Res.string.debrid_resolving_stream)
        resolvingYouTubeStream -> stringResource(Res.string.youtube_resolving_stream)
        preparingPlayback -> stringResource(Res.string.player_loading_preparing)
        state.overlayMessage == stringResource(Res.string.streams_loading_subtitles) -> subtitleLoadingStatusMessage()
        state.overlayMessage != null -> state.overlayMessage
        state.autoPlayStream != null -> stringResource(Res.string.player_loading_preparing)
        else -> stringResource(Res.string.streams_finding_source)
    }

    val artwork = launch.background?.takeIf { it.isNotBlank() }
        ?: launch.poster?.takeIf { it.isNotBlank() }
        ?: launch.episodeThumbnail?.takeIf { it.isNotBlank() }
    val logoUrl = launch.logo?.takeIf { it.isNotBlank() }
    var logoLoadError by remember(logoUrl) { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = NuvioTokens.Motion.slowMillis, easing = LinearEasing),
        label = "streamLoadingContentAlpha",
    )
    val pulse = rememberInfiniteTransition(label = "streamLoadingPulse")
    val barTravel by pulse.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "streamLoadingBarTravel",
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.background),
    ) {
        val logoWidth = minOf(320.dp, maxWidth - 80.dp)
        val logoHeight = minOf(150.dp, maxHeight * 0.3f)
        val titleFontSize = if (maxWidth < 600.dp) 28.sp else 38.sp

        if (artwork != null) {
            NuvioAsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to tokens.colors.background.copy(alpha = 0.45f),
                                0.4f to tokens.colors.background.copy(alpha = 0.72f),
                                0.78f to tokens.colors.background.copy(alpha = 0.92f),
                                1.0f to tokens.colors.background,
                            ),
                        ),
                    ),
            )
        }

        // Subtle Cinema Obsidian accent glow centered behind the focal stage.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to tokens.colors.accent.copy(alpha = 0.16f),
                            0.45f to tokens.colors.accentStrong.copy(alpha = 0.06f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )

        NuvioBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(LocalVerticalBarPadding.current)
                .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                .padding(top = 20.dp, end = 20.dp),
            containerColor = tokens.colors.surfaceElevated.copy(alpha = 0.88f),
            showContainerOnDesktop = true,
            contentColor = tokens.colors.textPrimary,
            buttonSize = 44.dp,
            iconSize = 22.dp,
            contentDescription = stringResource(Res.string.compose_player_close),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(centeredVerticalBarPadding(24.dp))
                .graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.Center,
        ) {
            val stageShape = RoundedCornerShape(24.dp)
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(stageShape)
                    .background(tokens.colors.surfaceElevated.copy(alpha = 0.78f))
                    .border(NuvioTokens.Border.thin, tokens.colors.borderSubtle, stageShape)
                    .padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (logoUrl != null && !logoLoadError) {
                    NuvioAsyncImage(
                        model = logoUrl,
                        contentDescription = launch.title,
                        modifier = Modifier
                            .width(logoWidth)
                            .height(logoHeight),
                        contentScale = ContentScale.Fit,
                        onError = { logoLoadError = true },
                    )
                } else if (launch.title.isNotBlank()) {
                    Text(
                        text = launch.title,
                        color = tokens.colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.nuvioTypeScale.displayMd.copy(
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }

                if (launch.seasonNumber != null && launch.episodeNumber != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tokens.colors.focusBackground)
                                .border(NuvioTokens.Border.thin, tokens.colors.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.streams_episode_badge, launch.seasonNumber, launch.episodeNumber),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = tokens.colors.accent,
                            )
                        }
                        launch.episodeTitle?.takeIf { it.isNotBlank() }?.let { episodeTitle ->
                            Text(
                                text = episodeTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                NuvioLoadingIndicator(
                    modifier = Modifier.size(44.dp),
                )

                val trackShape = RoundedCornerShape(999.dp)
                BoxWithConstraints(
                    modifier = Modifier
                        .width(240.dp)
                        .height(5.dp)
                        .clip(trackShape)
                        .background(tokens.colors.surfaceCard)
                        .border(NuvioTokens.Border.hairline, tokens.colors.borderDefault, trackShape),
                ) {
                    val segmentFraction = 0.38f
                    val travelPx = maxWidth.value
                    val offsetFraction = if (isRtl) -barTravel else barTravel
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(segmentFraction)
                            .graphicsLayer {
                                translationX = offsetFraction * travelPx * density
                            }
                            .clip(trackShape)
                            .background(palette.accentBrush()),
                    )
                }

                Crossfade(
                    targetState = message?.takeIf { it.isNotBlank() },
                    animationSpec = tween(260),
                    label = "streamLoadingMessage",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                ) { loadingMessage ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (loadingMessage != null) {
                            Text(
                                text = loadingMessage,
                                color = tokens.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

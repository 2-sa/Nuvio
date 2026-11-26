package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.jelly.JellyNavigationBar
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.isDesktop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

internal data class DesktopHistoryActions(
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val onNavigateBack: (() -> Unit)? = null,
    val onNavigateForward: (() -> Unit)? = null,
)

internal val LocalDesktopHistoryActions = compositionLocalOf { DesktopHistoryActions() }

@Composable
internal fun DesktopNavigationBar(
    items: List<FloatingNavigationItem>,
    modifier: Modifier = Modifier,
    scrollState: NuvioNavBarScrollState? = null,
    hazeState: HazeState? = null,
    contentPadding: PaddingValues = PaddingValues(top = 10.dp, bottom = 8.dp),
    navBarStyle: NavBarStyle = NavBarStyle.ADAPTIVE,
    isHeroEnabled: Boolean = false,
    profileSwitcherOpen: Boolean = false,
    windowWidth: Dp? = null,
    @Suppress("UNUSED_PARAMETER") glowEnabled: Boolean = true,
    canGoBack: Boolean? = null,
    canGoForward: Boolean? = null,
    onBack: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val historyActions = LocalDesktopHistoryActions.current
    val resolvedOnBack = onBack ?: historyActions.onNavigateBack
    val resolvedOnForward = onForward ?: historyActions.onNavigateForward
    val resolvedCanGoBack = canGoBack ?: historyActions.canGoBack
    val resolvedCanGoForward = canGoForward ?: historyActions.canGoForward
    val tokens = MaterialTheme.nuvio
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHomeSelected = items.first().selected
    val isSettingsSelected = items.last().selected
    val scrollOffset = scrollState?.totalScrollOffset ?: 0f
    val isScrolledAwayFromTop = scrollOffset > if (isSettingsSelected) 70f else 35f
    val isFrosted = isHovered || profileSwitcherOpen ||
        ((isHomeSelected || isSettingsSelected) && isScrolledAwayFromTop)
    val labelFraction by animateFloatAsState(
        targetValue = desktopNavigationLabelFraction(navBarStyle, isHomeSelected, isHeroEnabled, isHovered, profileSwitcherOpen, isScrolledAwayFromTop),
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "desktop_nav_labels",
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (isFrosted) {
            tokens.colors.surfaceElevated.copy(alpha = 0.84f)
        } else {
            tokens.colors.surfaceElevated.copy(alpha = 0.56f)
        },
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "desktop_nav_surface",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFrosted) tokens.colors.borderDefault else tokens.colors.borderSubtle,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "desktop_nav_border",
    )
    val sheenAlpha by animateFloatAsState(
        targetValue = if (isFrosted) 1f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "desktop_nav_sheen",
    )
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().nuvioTrackTopBarFocus().padding(contentPadding).padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (resolvedOnBack != null || resolvedOnForward != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesktopHistoryIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    enabled = resolvedCanGoBack && resolvedOnBack != null,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    onClick = { resolvedOnBack?.invoke() },
                )
                DesktopHistoryIconButton(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Forward",
                    enabled = resolvedCanGoForward && resolvedOnForward != null,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    onClick = { resolvedOnForward?.invoke() },
                )
            }
        }
        val viewportWidth = windowWidth ?: maxWidth
        val iconSize = when {
            viewportWidth < 800.dp -> 26.dp
            viewportWidth > 1600.dp -> 30.dp
            else -> 28.dp
        }
        val trackWidth = ((iconSize + 20.dp + 64.dp * labelFraction) * items.size + 8.dp).coerceAtMost(maxWidth)
        JellyNavigationBar(
            items = items,
            labelFraction = labelFraction,
            glowStrength = 0f,
            horizontalLabels = true,
            iconSize = iconSize,
            modifier = Modifier
                .width(trackWidth)
                .height(iconSize + 20.dp)
                .hoverable(interactionSource)
                .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier),
        ) {
            val pillShape = RoundedCornerShape(50)
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(pillShape)
                    .then(
                        if (isFrosted && hazeState != null) {
                            Modifier.hazeEffect(state = hazeState) { blurRadius = 14.dp }
                        } else {
                            Modifier
                        },
                    )
                    .background(surfaceColor)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.14f * sheenAlpha),
                                Color.White.copy(alpha = 0.03f * sheenAlpha),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = pillShape,
                    ),
            )
        }
    }
}

@Composable
private fun DesktopHistoryIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .then(
                if (enabled) {
                    Modifier
                        .then(if (isDesktop) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tokens.colors.textPrimary else tokens.colors.textMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun desktopNavigationLabelFraction(
    style: NavBarStyle,
    isHomeSelected: Boolean,
    isHeroEnabled: Boolean,
    isHovered: Boolean,
    profileSwitcherOpen: Boolean,
    isScrolledAwayFromTop: Boolean,
): Float = when (style) {
    NavBarStyle.EXPANDED -> 1f
    NavBarStyle.COMPACT -> 0f
    else -> 1f
}

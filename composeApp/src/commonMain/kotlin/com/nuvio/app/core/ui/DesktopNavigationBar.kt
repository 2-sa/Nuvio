package com.nuvio.app.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.jelly.JellyNavigationBar
import com.nuvio.app.features.settings.NavBarStyle
import com.nuvio.app.isDesktop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

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
) {
    if (items.isEmpty()) return
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

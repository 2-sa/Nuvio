package com.nuvio.app.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import com.nuvio.app.controller.controllerFocusable
import com.nuvio.app.isDesktop

internal val LocalPosterClickAnchor = staticCompositionLocalOf<((PosterZoomAnchor) -> Unit)?> { null }

private var nextPosterCardFocusId = 1L

internal class PosterLiftSource(private val graphicsContext: GraphicsContext) : RememberObserver {
    val layer = graphicsContext.createGraphicsLayer()
    var bounds: Rect? = null
    var isLifted by mutableStateOf(false)
        private set
    private var rememberCount = 0

    val canLift: Boolean
        get() = rememberCount > 0 && !layer.isReleased && layer.size.width > 0 && layer.size.height > 0

    fun lift(): Boolean {
        if (!canLift) return false
        isLifted = true
        return true
    }

    fun land() {
        isLifted = false
        if (rememberCount == 0) release()
    }

    override fun onRemembered() {
        rememberCount++
    }

    override fun onForgotten() {
        rememberCount--
        if (rememberCount == 0 && !isLifted) release()
    }

    override fun onAbandoned() {
        if (rememberCount == 0 && !isLifted) release()
    }

    private fun release() {
        if (!layer.isReleased) graphicsContext.releaseGraphicsLayer(layer)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Modifier.captureSecondaryClickRootOffset(
    enabled: Boolean = true,
): Modifier {
    if (!isDesktop || !enabled) return this
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentEnabled by rememberUpdatedState(enabled)
    return this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (!currentEnabled) continue
                    if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null) {
                            PosterZoomAnchorHolder.lastPointerPositionInRoot =
                                coordinates?.takeIf { it.isAttached }?.localToRoot(pos)
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.posterCardClickable(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    zoomImageUrl: String? = null,
    zoomCornerRadius: Dp = NuvioTokens.Radius.poster,
    hoverScaleEnabled: Boolean = true,
    focusId: String? = null,
): Modifier {
    val graphicsContext = LocalGraphicsContext.current
    val source = remember(graphicsContext, zoomImageUrl) { PosterLiftSource(graphicsContext) }
    val onPosterClickAnchor = LocalPosterClickAnchor.current
    val controllerFocusId = remember(focusId, zoomImageUrl) {
        focusId ?: zoomImageUrl?.takeIf { it.isNotBlank() }?.let { "poster_${it.hashCode()}" }
            ?: "poster_card_${nextPosterCardFocusId++}"
    }
    val posterModifier = Modifier
        .drawWithContent {
            source.layer.record { this@drawWithContent.drawContent() }
            if (!source.isLifted) drawLayer(source.layer)
        }
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            source.bounds = Rect(
                left = position.x,
                top = position.y,
                right = position.x + coordinates.size.width,
                bottom = position.y + coordinates.size.height,
            )
        }
        .then(this)
    if (onClick == null && onLongClick == null) return posterModifier
    val interactionSource = remember { MutableInteractionSource() }
    val handleClick: () -> Unit = {
        PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
        if (onClick != null) {
            source.bounds?.let { bounds ->
                onPosterClickAnchor?.invoke(
                    PosterZoomAnchor(bounds, zoomImageUrl, zoomCornerRadius).also { it.source = source },
                )
            }
            onClick()
        }
    }
    val handleLongClick = onLongClick?.let { longClick ->
        {
            val pointerOffset = PosterZoomAnchorHolder.consumeLastPointerPositionInRoot()
            source.bounds?.let { cardBounds ->
                PosterZoomAnchorHolder.stash(
                    PosterZoomAnchor(
                        boundsInRoot = cardBounds,
                        imageUrl = zoomImageUrl,
                        cornerRadius = zoomCornerRadius,
                        pointerPositionInRoot = pointerOffset,
                    ).also {
                        it.source = source
                    },
                )
            }
            longClick()
        }
    }
    return Modifier
        .desktopPosterHoverScale(
            enabled = hoverScaleEnabled,
            interactionSource = interactionSource,
        )
        .then(posterModifier)
        .then(
            if (isDesktop) {
                Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .controllerFocusable(
                        id = controllerFocusId,
                        enabled = onClick != null,
                        cornerRadius = zoomCornerRadius,
                        focusScale = 1.07f,
                        selectLabel = "Open",
                        secondaryActionLabel = if (handleLongClick != null) "Options" else null,
                        onSecondaryAction = handleLongClick,
                        onSelect = handleClick,
                    )
            } else {
                Modifier
            },
        )
        .captureSecondaryClickRootOffset(enabled = handleLongClick != null)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = if (!isDesktop && onPosterClickAnchor == null) LocalIndication.current else null,
            onClick = handleClick,
            onLongClick = handleLongClick,
        )
        .secondaryClick(handleLongClick)
}

internal fun DrawScope.drawLiftedPoster(source: PosterLiftSource) {
    val layer = source.layer
    if (layer.isReleased || layer.size.width <= 0 || layer.size.height <= 0) return
    scale(
        scaleX = size.width / layer.size.width,
        scaleY = size.height / layer.size.height,
        pivot = Offset.Zero,
    ) {
        drawLayer(layer)
    }
}

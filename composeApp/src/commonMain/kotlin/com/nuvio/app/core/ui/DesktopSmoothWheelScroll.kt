package com.nuvio.app.core.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nuvio.app.isDesktop
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign

private val DesktopWheelStepDp = 96.dp
private const val SmoothScrollDecayPerSecond = 15.5f
private const val SmoothScrollSnapThresholdPx = 0.5f
private const val SmoothScrollMaxDeltaSeconds = 0.05f
private const val SmoothScrollMaxPendingPx = 2_400f

internal data class SmoothWheelScrollStep(
    val consumedDeltaPx: Float,
    val remainingPx: Float,
)

/**
 * Pure frame-rate-independent exponential smoothing step for desktop mouse-wheel scrolling.
 */
internal fun computeSmoothWheelScrollStep(
    remainingPx: Float,
    deltaSeconds: Float,
    decayPerSecond: Float = SmoothScrollDecayPerSecond,
    snapThresholdPx: Float = SmoothScrollSnapThresholdPx,
): SmoothWheelScrollStep {
    if (!remainingPx.isFinite() || abs(remainingPx) <= snapThresholdPx) {
        return SmoothWheelScrollStep(
            consumedDeltaPx = if (remainingPx.isFinite()) remainingPx else 0f,
            remainingPx = 0f,
        )
    }
    val safeDt = if (deltaSeconds.isFinite()) {
        deltaSeconds.coerceIn(0.001f, SmoothScrollMaxDeltaSeconds)
    } else {
        1f / 60f
    }
    val factor = 1f - exp(-decayPerSecond * safeDt)
    val step = remainingPx * factor
    val nextRemaining = remainingPx - step
    return if (abs(nextRemaining) <= snapThresholdPx) {
        SmoothWheelScrollStep(consumedDeltaPx = remainingPx, remainingPx = 0f)
    } else {
        SmoothWheelScrollStep(consumedDeltaPx = step, remainingPx = nextRemaining)
    }
}

/**
 * Combines a newly arrived wheel scroll delta with any in-flight remaining target distance.
 * Reversing scroll direction immediately cancels opposing momentum so the UI feels crisp.
 */
internal fun accumulateSmoothWheelTarget(
    currentRemainingPx: Float,
    addedDeltaPx: Float,
    maxPendingPx: Float = SmoothScrollMaxPendingPx,
): Float {
    if (!addedDeltaPx.isFinite() || addedDeltaPx == 0f) {
        return if (currentRemainingPx.isFinite()) currentRemainingPx else 0f
    }
    val base = if (!currentRemainingPx.isFinite() ||
        (currentRemainingPx != 0f && sign(currentRemainingPx) != sign(addedDeltaPx))
    ) {
        0f
    } else {
        currentRemainingPx
    }
    return (base + addedDeltaPx).coerceIn(-maxPendingPx, maxPendingPx)
}

/**
 * Smooths vertical mouse-wheel scrolling on Desktop so discrete wheel ticks glide smoothly
 * across display frames (60–144Hz) instead of teleporting rows in a single frame.
 */
internal fun Modifier.desktopSmoothWheelScroll(
    scrollState: ScrollableState,
    enabled: Boolean = isDesktop,
): Modifier {
    if (!enabled) return this
    return composed {
        val density = LocalDensity.current
        val stepPx = with(density) { DesktopWheelStepDp.toPx() }
        val coroutineScope = rememberCoroutineScope()
        val controller = remember(scrollState) { SmoothWheelScrollController(scrollState) }

        DisposableEffect(controller) {
            onDispose { controller.cancel() }
        }

        pointerInput(scrollState, stepPx) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type != PointerEventType.Scroll) continue
                    val change = event.changes.firstOrNull() ?: continue
                    if (change.isConsumed) continue

                    val deltaX = change.scrollDelta.x
                    val deltaY = change.scrollDelta.y
                    if (!deltaY.isFinite() || deltaY == 0f) continue
                    if (deltaX.isFinite() && abs(deltaX) > abs(deltaY)) continue

                    // Only intercept when this container can actually scroll in the requested direction
                    // or already has active momentum in that direction; otherwise let parents handle it.
                    val canScrollInDirection =
                        (deltaY > 0f && scrollState.canScrollForward) ||
                            (deltaY < 0f && scrollState.canScrollBackward)
                    if (!canScrollInDirection) {
                        controller.cancel()
                        continue
                    }

                    change.consume()
                    controller.enqueue(
                        addedDeltaPx = deltaY * stepPx,
                        coroutineScope = coroutineScope,
                    )
                }
            }
        }
    }
}

private class SmoothWheelScrollController(
    private val scrollState: ScrollableState,
) {
    private var remainingPx: Float = 0f
    private var animationJob: Job? = null

    fun cancel() {
        remainingPx = 0f
        animationJob?.cancel()
        animationJob = null
    }

    fun enqueue(
        addedDeltaPx: Float,
        coroutineScope: kotlinx.coroutines.CoroutineScope,
    ) {
        remainingPx = accumulateSmoothWheelTarget(remainingPx, addedDeltaPx)
        if (animationJob?.isActive == true) return

        animationJob = coroutineScope.launch {
            var lastFrameNanos = withFrameNanos { it }
            while (abs(remainingPx) > 0f) {
                val nowNanos = withFrameNanos { it }
                val dtSeconds = ((nowNanos - lastFrameNanos).coerceAtLeast(1L)) / 1_000_000_000f
                lastFrameNanos = nowNanos

                val step = computeSmoothWheelScrollStep(
                    remainingPx = remainingPx,
                    deltaSeconds = dtSeconds,
                )
                remainingPx = step.remainingPx
                if (step.consumedDeltaPx == 0f) break

                val actuallyConsumed = scrollState.scrollBy(step.consumedDeltaPx)
                if (abs(actuallyConsumed) < 0.1f && abs(step.consumedDeltaPx) >= 1f) {
                    // Hit top or bottom edge; stop immediately.
                    remainingPx = 0f
                    break
                }
            }
            remainingPx = 0f
        }
    }
}

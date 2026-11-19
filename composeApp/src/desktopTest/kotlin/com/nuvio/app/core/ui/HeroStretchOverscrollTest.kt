package com.nuvio.app.core.ui

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
class HeroStretchOverscrollTest {

    private class SteppingFrameClock : MonotonicFrameClock {
        private var frameTimeNanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            yield()
            frameTimeNanos += 16_000_000L
            return onFrame(frameTimeNanos)
        }
    }

    @Test
    fun disabledZeroMaxStretchStaysZeroAndFiniteAcrossNestedScrollCallbacks() = runTest {
        val state = HeroStretchState(this + SteppingFrameClock(), maxStretchPx = 0f) { true }
        val connection = state.nestedScrollConnection
        val deltas = listOf(120f, -120f, 80f, -80f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val sources = listOf(NestedScrollSource.UserInput, NestedScrollSource.SideEffect)

        fun assertSafeZero() {
            assertFalse(state.stretchPx.isNaN(), "stretchPx must never be NaN")
            assertTrue(state.stretchPx.isFinite(), "stretchPx must remain finite")
            assertEquals(0f, state.stretchPx)
            assertEquals(0, state.stretchPx.roundToInt())
        }

        assertSafeZero()

        for (source in sources) {
            for (deltaY in deltas) {
                val postConsumed = connection.onPostScroll(Offset.Zero, Offset(0f, deltaY), source)
                advanceUntilIdle()
                assertEquals(Offset.Zero, postConsumed)
                assertSafeZero()

                val preConsumed = connection.onPreScroll(Offset(0f, deltaY), source)
                advanceUntilIdle()
                assertEquals(Offset.Zero, preConsumed)
                assertSafeZero()
            }
        }

        val velocities = listOf(Velocity(0f, 1500f), Velocity(0f, -1500f), Velocity(0f, Float.NaN))
        for (velocity in velocities) {
            val preFling = connection.onPreFling(velocity)
            advanceUntilIdle()
            assertEquals(Velocity.Zero, preFling)
            assertSafeZero()

            val postFling = connection.onPostFling(Velocity.Zero, velocity)
            advanceUntilIdle()
            assertEquals(Velocity.Zero, postFling)
            assertSafeZero()
        }
    }

    @Test
    fun enabledMaxStretchStretchesWithinBoundsWithoutNaN() = runTest {
        val state = HeroStretchState(this + SteppingFrameClock(), maxStretchPx = 200f) { true }
        val connection = state.nestedScrollConnection

        fun assertInBounds() {
            assertFalse(state.stretchPx.isNaN(), "stretchPx must never be NaN")
            assertTrue(state.stretchPx.isFinite(), "stretchPx must remain finite")
            assertTrue(
                state.stretchPx in 0f..200f,
                "Expected stretchPx in 0f..200f but was ${state.stretchPx}",
            )
        }

        val initialConsumed = connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
        advanceUntilIdle()
        assertEquals(Offset(0f, 100f), initialConsumed)
        assertTrue(state.stretchPx > 0f, "Expected positive stretch after pull down")
        assertInBounds()

        repeat(15) {
            connection.onPostScroll(Offset.Zero, Offset(0f, 500f), NestedScrollSource.UserInput)
            advanceUntilIdle()
            assertInBounds()
        }

        val beforePreScroll = state.stretchPx
        val preConsumed = connection.onPreScroll(Offset(0f, -50f), NestedScrollSource.UserInput)
        advanceUntilIdle()
        assertEquals(Offset(0f, -50f), preConsumed)
        assertTrue(state.stretchPx < beforePreScroll, "Expected upward preScroll to reduce stretch")
        assertInBounds()

        connection.onPostScroll(Offset.Zero, Offset(0f, Float.POSITIVE_INFINITY), NestedScrollSource.UserInput)
        advanceUntilIdle()
        assertInBounds()

        connection.onPostScroll(Offset.Zero, Offset(0f, Float.NaN), NestedScrollSource.UserInput)
        advanceUntilIdle()
        assertInBounds()

        connection.onPreFling(Velocity(0f, 800f))
        connection.onPostFling(Velocity.Zero, Velocity(0f, 400f))
        advanceUntilIdle()
        assertInBounds()
        assertEquals(0f, state.stretchPx, 0.01f)
    }

    @Test
    fun nonFiniteOrNegativeMaxStretchSafelyDisablesStretch() = runTest {
        val invalidMaxValues = listOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -10f)
        val sources = listOf(NestedScrollSource.UserInput, NestedScrollSource.SideEffect)

        for (invalidMax in invalidMaxValues) {
            val state = HeroStretchState(this + SteppingFrameClock(), maxStretchPx = invalidMax) { true }
            val connection = state.nestedScrollConnection

            fun assertSafelyDisabled() {
                assertFalse(state.stretchPx.isNaN(), "stretchPx must not be NaN for maxStretchPx=$invalidMax")
                assertTrue(state.stretchPx.isFinite(), "stretchPx must be finite for maxStretchPx=$invalidMax")
                assertEquals(0f, state.stretchPx, "stretchPx must stay 0f for maxStretchPx=$invalidMax")
                assertEquals(0, state.stretchPx.roundToInt())
            }

            assertSafelyDisabled()

            for (source in sources) {
                for (deltaY in listOf(120f, -120f)) {
                    assertEquals(
                        Offset.Zero,
                        connection.onPostScroll(Offset.Zero, Offset(0f, deltaY), source),
                    )
                    advanceUntilIdle()
                    assertSafelyDisabled()

                    assertEquals(
                        Offset.Zero,
                        connection.onPreScroll(Offset(0f, deltaY), source),
                    )
                    advanceUntilIdle()
                    assertSafelyDisabled()
                }
            }

            for (velY in listOf(1000f, -1000f, Float.NaN)) {
                assertEquals(Velocity.Zero, connection.onPreFling(Velocity(0f, velY)))
                advanceUntilIdle()
                assertSafelyDisabled()

                assertEquals(Velocity.Zero, connection.onPostFling(Velocity.Zero, Velocity(0f, velY)))
                advanceUntilIdle()
                assertSafelyDisabled()
            }
        }
    }
}

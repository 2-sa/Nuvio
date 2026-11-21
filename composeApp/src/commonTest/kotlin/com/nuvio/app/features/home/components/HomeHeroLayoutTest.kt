package com.nuvio.app.features.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeHeroLayoutTest {

    @Test
    fun `desktop hero height stays clamped between 440dp and 640dp when viewport height is null`() {
        val widths = listOf(1024f, 1280f, 1440f, 1920f, 2560f)

        widths.forEach { width ->
            val layout = homeHeroLayout(
                maxWidthDp = width,
                viewportHeightDp = null,
                preferDesktopLayout = true,
            )

            assertTrue(
                layout.heroHeight.value in 440f..640f,
                "Expected heroHeight in 440..640 for width=$width, got ${layout.heroHeight.value}",
            )
            assertEquals(560f, layout.contentMaxWidth.value, 0.001f)
            assertLayoutFiniteAndPositive(layout, allowZeroMotion = false)
        }
    }

    @Test
    fun `desktop hero preserves above-the-fold peek across standard viewport sizes`() {
        val standardViewports = listOf(
            1024f to 720f,
            1024f to 800f,
            1280f to 720f,
            1280f to 800f,
            1440f to 900f,
            1920f to 1080f,
            2560f to 1440f,
        )

        standardViewports.forEach { (width, height) ->
            val layout = homeHeroLayout(
                maxWidthDp = width,
                viewportHeightDp = height,
                preferDesktopLayout = true,
            )

            assertTrue(
                layout.heroHeight.value in 440f..640f,
                "Expected heroHeight in 440..640 for ${width}x${height}, got ${layout.heroHeight.value}",
            )
            if (height >= 760f) {
                assertTrue(
                    layout.heroHeight.value <= height * 0.72f,
                    "Expected heroHeight (${layout.heroHeight.value}) <= 72% of $height for ${width}x${height}",
                )
            }
            assertEquals(560f, layout.contentMaxWidth.value, 0.001f)
            assertLayoutFiniteAndPositive(layout, allowZeroMotion = false)
        }
    }

    @Test
    fun `desktop hero targets 60 percent of viewport height within 440dp to 640dp bounds`() {
        assertEquals(
            440f,
            homeHeroLayout(maxWidthDp = 1280f, viewportHeightDp = 720f, preferDesktopLayout = true).heroHeight.value,
            0.001f,
        )
        assertEquals(
            480f,
            homeHeroLayout(maxWidthDp = 1280f, viewportHeightDp = 800f, preferDesktopLayout = true).heroHeight.value,
            0.001f,
        )
        assertEquals(
            540f,
            homeHeroLayout(maxWidthDp = 1440f, viewportHeightDp = 900f, preferDesktopLayout = true).heroHeight.value,
            0.001f,
        )
        assertEquals(
            640f,
            homeHeroLayout(maxWidthDp = 1920f, viewportHeightDp = 1080f, preferDesktopLayout = true).heroHeight.value,
            0.001f,
        )
    }

    @Test
    fun `desktop hero handles non-finite or non-positive dimensions safely`() {
        val invalidInputs = listOf(
            0f to null,
            -320f to -100f,
            Float.NaN to Float.NaN,
            Float.POSITIVE_INFINITY to Float.NEGATIVE_INFINITY,
            1440f to 0f,
            1440f to Float.NaN,
        )

        invalidInputs.forEach { (width, height) ->
            val layout = homeHeroLayout(
                maxWidthDp = width,
                viewportHeightDp = height,
                preferDesktopLayout = true,
            )

            assertTrue(layout.heroHeight.value in 440f..640f)
            assertLayoutFiniteAndPositive(layout, allowZeroMotion = false)
        }
    }

    @Test
    fun `mobile and tablet hero calculations remain unchanged`() {
        val phone = homeHeroLayout(maxWidthDp = 390f, viewportHeightDp = 844f, preferDesktopLayout = false)
        assertFalse(phone.isTablet)
        assertEquals(452.4f, phone.heroHeight.value, 0.001f)
        assertEquals(480f, phone.contentMaxWidth.value, 0.001f)

        val smallTablet = homeHeroLayout(maxWidthDp = 600f, viewportHeightDp = 960f, preferDesktopLayout = false)
        assertTrue(smallTablet.isTablet)
        assertEquals(348f, smallTablet.heroHeight.value, 0.001f)
        assertEquals(520f, smallTablet.contentMaxWidth.value, 0.001f)

        val largeTablet = homeHeroLayout(maxWidthDp = 840f, viewportHeightDp = 1200f, preferDesktopLayout = false)
        assertTrue(largeTablet.isTablet)
        assertEquals(386.4f, largeTablet.heroHeight.value, 0.001f)
        assertEquals(560f, largeTablet.contentMaxWidth.value, 0.001f)

        val wideTablet = homeHeroLayout(maxWidthDp = 1200f, viewportHeightDp = 900f, preferDesktopLayout = false)
        assertTrue(wideTablet.isTablet)
        assertEquals(440f, wideTablet.heroHeight.value, 0.001f)
        assertEquals(640f, wideTablet.contentMaxWidth.value, 0.001f)
    }

    private fun assertLayoutFiniteAndPositive(
        layout: HomeHeroLayout,
        allowZeroMotion: Boolean,
    ) {
        val positiveValues = listOf(
            layout.heroHeight.value,
            layout.contentMaxWidth.value,
            layout.contentContainerMaxWidth.value,
            layout.contentWidthFraction,
            layout.contentHorizontalPadding.value,
            layout.contentVerticalPadding.value,
            layout.topFadeHeight.value,
            layout.bottomFadeHeight.value,
            layout.logoWidthFraction,
        )
        positiveValues.forEach { value ->
            assertTrue(value.isFinite(), "Expected finite value, got $value")
            assertTrue(value > 0f, "Expected positive value, got $value")
        }
        assertTrue(layout.backgroundMotionStrength.isFinite())
        assertTrue(
            if (allowZeroMotion) layout.backgroundMotionStrength >= 0f else layout.backgroundMotionStrength > 0f,
            "Unexpected backgroundMotionStrength=${layout.backgroundMotionStrength}",
        )
    }
}

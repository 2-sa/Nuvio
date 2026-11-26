package com.nuvio.app.features.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterRatingFormatTest {

    @Test
    fun `rounds raw floating point ratings to one decimal place`() {
        assertEquals("8.4", normalizedPosterRating("8.365063171386719"))
        assertEquals("7.6", normalizedPosterRating("7.56804666519165"))
        assertEquals("9.0", normalizedPosterRating("9"))
        assertEquals("9.2", normalizedPosterRating("9.23380573815518"))
    }

    @Test
    fun `rejects blank zero and NA ratings`() {
        assertNull(normalizedPosterRating(null))
        assertNull(normalizedPosterRating("   "))
        assertNull(normalizedPosterRating("0"))
        assertNull(normalizedPosterRating("0.0"))
        assertNull(normalizedPosterRating("0.0000"))
        assertNull(normalizedPosterRating("N/A"))
    }

    @Test
    fun `preserves non-numeric rating strings safely`() {
        assertEquals("TV-MA", normalizedPosterRating("TV-MA"))
    }
}

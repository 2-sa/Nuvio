package com.nuvio.app.features.addons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddonCardPresentationTest {

    @Test
    fun `stream-only addon with zero catalogs omits catalogs badge and includes stream`() {
        val badges = extractAddonCapabilityBadges(
            resources = listOf("stream"),
            catalogCount = 0,
        )

        assertEquals(
            listOf(AddonCapabilityBadge(AddonCapabilityKind.STREAM)),
            badges,
        )
    }

    @Test
    fun `metadata addon with catalogs includes catalogs count meta and subtitles`() {
        val badges = extractAddonCapabilityBadges(
            resources = listOf("catalog", "meta", "subtitles"),
            catalogCount = 58,
        )

        assertEquals(
            listOf(
                AddonCapabilityBadge(AddonCapabilityKind.CATALOGS, count = 58),
                AddonCapabilityBadge(AddonCapabilityKind.META),
                AddonCapabilityBadge(AddonCapabilityKind.SUBTITLES),
            ),
            badges,
        )
    }

    @Test
    fun `subtitles-only addon with zero catalogs emits only subtitles`() {
        val badges = extractAddonCapabilityBadges(
            resources = listOf("  SUBTITLES  "),
            catalogCount = 0,
        )

        assertEquals(
            listOf(AddonCapabilityBadge(AddonCapabilityKind.SUBTITLES)),
            badges,
        )
    }

    @Test
    fun `capability badges normalize resource names and never emit non-positive catalog counts`() {
        val badges = extractAddonCapabilityBadges(
            resources = listOf(" Subtitles ", "META", "Stream", "catalog"),
            catalogCount = -3,
        )

        assertEquals(
            listOf(
                AddonCapabilityBadge(AddonCapabilityKind.STREAM),
                AddonCapabilityBadge(AddonCapabilityKind.META),
                AddonCapabilityBadge(AddonCapabilityKind.SUBTITLES),
            ),
            badges,
        )
    }

    @Test
    fun `normalizeAddonContentTypes collapses anime variants and strips technical noise`() {
        val normalized = normalizeAddonContentTypes(
            listOf(
                "movie",
                "series",
                "anime.movie",
                "anime.series",
                "anime",
                "trakt",
                "collection",
                "person",
            ),
        )

        assertEquals(
            listOf(
                AddonContentTypeKind.MOVIE,
                AddonContentTypeKind.SERIES,
                AddonContentTypeKind.ANIME,
            ),
            normalized,
        )
    }

    @Test
    fun `normalizeAddonContentTypes maps channel aliases and suppresses other when primary types exist`() {
        val normalized = normalizeAddonContentTypes(
            listOf("tv", "channel", "channels", "movie", "other", "subtitles"),
        )

        assertEquals(
            listOf(
                AddonContentTypeKind.CHANNEL,
                AddonContentTypeKind.MOVIE,
            ),
            normalized,
        )
    }

    @Test
    fun `normalizeAddonContentTypes emits other only when no primary types exist`() {
        assertEquals(
            listOf(AddonContentTypeKind.OTHER),
            normalizeAddonContentTypes(listOf("other")),
        )
        assertEquals(
            listOf(AddonContentTypeKind.OTHER),
            normalizeAddonContentTypes(listOf("trakt", "other")),
        )
        assertEquals(
            emptyList(),
            normalizeAddonContentTypes(listOf("trakt", "collection", "person", "subtitles")),
        )
        assertEquals(
            emptyList(),
            normalizeAddonContentTypes(emptyList()),
        )
    }

    @Test
    fun `isPrimarilyLatinText distinguishes English and Arabic descriptions`() {
        assertTrue(isPrimarilyLatinText("Provides torrent streams from scraped torrent providers."))
        assertTrue(isPrimarilyLatinText("4K HDR Torrentio streams"))
        assertTrue(isPrimarilyLatinText("[Official] Cinemeta metadata"))

        assertFalse(isPrimarilyLatinText("إضافة مجانية لبث الأفلام والمسلسلات (4K)"))
        assertFalse(isPrimarilyLatinText("1080p - ترجمة عربية فورية"))
        assertFalse(isPrimarilyLatinText("ترجمة فورية للأفلام"))
        assertFalse(isPrimarilyLatinText(""))
        assertFalse(isPrimarilyLatinText("12345 - !@#"))
    }
}

package com.nuvio.app.features.trakt

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.toLibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TraktIdUtilsTest {

    @Test
    fun `parse content ids supports imdb tmdb trakt and numeric aliases`() {
        assertEquals(TraktExternalIds(imdb = "tt1234567"), parseTraktContentIds("tt1234567:movie"))
        assertEquals(TraktExternalIds(imdb = "tt9176158"), parseTraktContentIds("imdb:TT9176158"))
        assertEquals(TraktExternalIds(tmdb = 42), parseTraktContentIds("tmdb:42"))
        assertEquals(TraktExternalIds(tmdb = 84677), parseTraktContentIds("tmdb:tv:84677"))
        assertEquals(TraktExternalIds(tmdb = 84677), parseTraktContentIds("tmdb:84677:1:1"))
        assertEquals(TraktExternalIds(trakt = 99), parseTraktContentIds("trakt:99"))
        assertEquals(TraktExternalIds(trakt = 99), parseTraktContentIds("trakt:99:1:2"))
        assertEquals(TraktExternalIds(trakt = 77), parseTraktContentIds("77:show"))
        assertEquals(TraktExternalIds(), parseTraktContentIds("kitsu:41838"))
    }

    @Test
    fun `normalize content id prefers imdb then tmdb then trakt then fallback`() {
        assertEquals(
            "tt1234567",
            normalizeTraktContentId(TraktExternalIds(trakt = 99, imdb = "tt1234567", tmdb = 42), fallback = "fallback"),
        )
        assertEquals("tmdb:42", normalizeTraktContentId(TraktExternalIds(trakt = 99, tmdb = 42)))
        assertEquals("trakt:99", normalizeTraktContentId(TraktExternalIds(trakt = 99)))
        assertEquals("fallback", normalizeTraktContentId(TraktExternalIds(), fallback = "fallback"))
    }

    @Test
    fun `external ids report slug-only identifiers as present`() {
        assertTrue(TraktExternalIds(slug = "favorites").hasAnyId())
        assertTrue(TraktExternalIds(tmdb = 42).hasAnyId())
        assertFalse(TraktExternalIds().hasAnyId())
    }

    @Test
    fun `meta details toLibraryItem preserves explicit imdbId and video fallback ids for anime`() {
        val fromExplicitImdb = MetaDetails(
            id = "kitsu:41838",
            type = "series",
            name = "Domestic Girlfriend",
            imdbId = "tt9176158",
        ).toLibraryItem(savedAtEpochMs = 1L)

        assertEquals("tt9176158", fromExplicitImdb.imdbId)
        assertNull(fromExplicitImdb.tmdbId)
        assertNull(fromExplicitImdb.traktId)

        val fromVideoFallback = MetaDetails(
            id = "kitsu:41838",
            type = "series",
            name = "Domestic Girlfriend",
            videos = listOf(
                MetaVideo(id = "tt9176158:1:1", title = "Episode 1", season = 1, episode = 1),
            ),
        ).toLibraryItem(savedAtEpochMs = 1L)

        assertEquals("tt9176158", fromVideoFallback.imdbId)

        val fromTmdbPreview = MetaPreview(
            id = "tmdb:tv:84677",
            type = "series",
            name = "Domestic Girlfriend",
        ).toLibraryItem(savedAtEpochMs = 1L)

        assertEquals(84677, fromTmdbPreview.tmdbId)
    }

    @Test
    fun `selectBestTraktSearchMatch prefers exact title and year and rejects ambiguous mismatch`() {
        val candidates = listOf(
            TraktSearchMatchCandidate(
                title = "Domestic Girlfriend",
                year = 2019,
                ids = TraktExternalIds(trakt = 141935, imdb = "tt9176158", tmdb = 84677),
            ),
            TraktSearchMatchCandidate(
                title = "Domestic Girlfriend Special",
                year = 2020,
                ids = TraktExternalIds(trakt = 999999, imdb = "tt0000001", tmdb = 1),
            ),
        )

        assertEquals(
            TraktExternalIds(trakt = 141935, imdb = "tt9176158", tmdb = 84677),
            selectBestTraktSearchMatch(candidates, queryTitle = "Domestic Girlfriend", queryYear = 2019),
        )
        assertEquals(
            TraktExternalIds(trakt = 141935, imdb = "tt9176158", tmdb = 84677),
            selectBestTraktSearchMatch(candidates, queryTitle = "صديقة منزلية", queryYear = 2019),
        )
        assertNull(
            selectBestTraktSearchMatch(candidates, queryTitle = "Unrelated Show", queryYear = null),
        )
    }
}

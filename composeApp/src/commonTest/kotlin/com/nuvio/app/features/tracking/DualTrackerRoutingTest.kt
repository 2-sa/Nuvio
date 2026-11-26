package com.nuvio.app.features.tracking

import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DualTrackerRoutingTest {
    @Test
    fun `isAnimeTrackingTarget detects anime ids, category, and AniList presence`() {
        assertTrue(isAnimeTrackingTarget("anilist:235"))
        assertTrue(isAnimeTrackingTarget("mal:235"))
        assertTrue(isAnimeTrackingTarget("kitsu:210"))
        assertTrue(isAnimeTrackingTarget("tt0131159", mediaCategory = "anime"))
        assertTrue(isAnimeTrackingTarget("tt0131159", isKnownInAniList = true))

        assertFalse(isAnimeTrackingTarget("tt0141842"))
        assertFalse(isAnimeTrackingTarget("tt0141842", mediaCategory = "series"))
    }

    @Test
    fun `resolveEffectiveWriteProvider routes anime to AniList and non-anime to Trakt`() {
        assertEquals(
            TrackingProviderId.ANILIST,
            resolveEffectiveWriteProvider(
                primaryProvider = TrackingProviderId.TRAKT,
                isAnime = true,
                useAniListForAnime = true,
                isAniListAuthenticated = true,
            ),
        )
        assertEquals(
            TrackingProviderId.TRAKT,
            resolveEffectiveWriteProvider(
                primaryProvider = TrackingProviderId.TRAKT,
                isAnime = false,
                useAniListForAnime = true,
                isAniListAuthenticated = true,
            ),
        )
        assertEquals(
            TrackingProviderId.TRAKT,
            resolveEffectiveWriteProvider(
                primaryProvider = TrackingProviderId.TRAKT,
                isAnime = true,
                useAniListForAnime = false,
                isAniListAuthenticated = true,
            ),
        )
    }

    @Test
    fun `mergeDualWatchProgress combines Trakt movies-tv with AniList anime and deduplicates including Unicode`() {
        val traktSopranos = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0141842",
            parentMetaType = "series",
            videoId = "tt0141842:1:2",
            title = "The Sopranos",
            seasonNumber = 1,
            episodeNumber = 2,
            lastPositionMs = 0L,
            durationMs = 3_000_000L,
            lastUpdatedEpochMs = 200L,
            source = "trakt",
        )
        val traktConanDuplicate = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0131159",
            parentMetaType = "series",
            videoId = "tt0131159:1:5",
            title = "المحقق كونان",
            seasonNumber = 1,
            episodeNumber = 5,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 100L,
            source = "trakt",
        )
        val anilistConan = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "anilist:235",
            parentMetaType = "series",
            videoId = "anilist:235:25:62",
            title = "المحقق كونان",
            seasonNumber = 25,
            episodeNumber = 62,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 300L,
            source = "anilist",
        )

        val merged = mergeDualWatchProgress(
            primarySource = WatchProgressSource.TRAKT,
            primaryEntries = listOf(traktSopranos, traktConanDuplicate),
            anilistEntries = listOf(anilistConan),
            useAniListForAnime = true,
        )

        assertEquals(listOf("anilist:235", "tt0141842"), merged.map { it.parentMetaId })
    }

    @Test
    fun `mergeDualWatchProgress filters anime prefixes from primary even when AniList entries are empty`() {
        val traktSopranos = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0141842",
            parentMetaType = "series",
            videoId = "tt0141842:1:2",
            title = "The Sopranos",
            seasonNumber = 1,
            episodeNumber = 2,
            lastPositionMs = 0L,
            durationMs = 3_000_000L,
            lastUpdatedEpochMs = 200L,
            source = "trakt",
        )
        val traktMalEntry = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "mal:235",
            parentMetaType = "series",
            videoId = "mal:235:1:2",
            title = "Detective Conan",
            seasonNumber = 1,
            episodeNumber = 2,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 100L,
            source = "trakt",
        )

        val merged = mergeDualWatchProgress(
            primarySource = WatchProgressSource.TRAKT,
            primaryEntries = listOf(traktSopranos, traktMalEntry),
            anilistEntries = emptyList(),
            useAniListForAnime = true,
        )

        assertEquals(listOf("tt0141842"), merged.map { it.parentMetaId })
    }

    @Test
    fun `mergeDualWatchedItems preserves Trakt non-anime and prefers AniList for anime`() {
        val traktShow = WatchedItem(
            id = "tt0141842",
            type = "series",
            name = "The Sopranos",
            season = 1,
            episode = 1,
            markedAtEpochMs = 100L,
        )
        val traktAnimeDup = WatchedItem(
            id = "tt0131159",
            type = "series",
            name = "Detective Conan",
            season = 1,
            episode = 1,
            markedAtEpochMs = 100L,
        )
        val anilistAnime = WatchedItem(
            id = "anilist:235",
            type = "series",
            name = "Detective Conan",
            season = 25,
            episode = 62,
            markedAtEpochMs = 200L,
        )

        val merged = mergeDualWatchedItems(
            primarySource = WatchProgressSource.TRAKT,
            primaryItems = listOf(traktShow, traktAnimeDup),
            anilistItems = listOf(anilistAnime),
            useAniListForAnime = true,
        )

        assertEquals(listOf("anilist:235", "tt0141842"), merged.map { it.id })
    }
}

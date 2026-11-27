package com.nuvio.app.features.anilist

import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.tracking.isAnimeTrackingTarget
import com.nuvio.app.features.tracking.mergeDualWatchProgress
import com.nuvio.app.features.tracking.mergeDualWatchedItems
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AniListWatchedProjectionTest {
    private val sampleBridgeIndex: AnimeIdBridgeIndex = parseFribbAnimeListJson(
        """
        [
          {"anilist_id": 16498, "mal_id": 16498, "kitsu_id": 7442, "anidb_id": 9541, "imdb_id": "tt2560140", "themoviedb_id": 1429, "thetvdb_id": 267440, "type": "TV"},
          {"anilist_id": 20958, "mal_id": 25777, "kitsu_id": 8671, "anidb_id": 10944, "imdb_id": "tt2560140", "themoviedb_id": 1429, "thetvdb_id": 267440, "type": "TV"},
          {"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
          {"anilist_id": 395, "mal_id": 395, "kitsu_id": 352, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
          {"anilist_id": 235, "mal_id": 235, "kitsu_id": 210, "imdb_id": "tt0131179", "themoviedb_id": 30983, "thetvdb_id": 72454, "type": "TV"},
          {"anilist_id": 199, "mal_id": 199, "kitsu_id": 176, "anidb_id": 112, "imdb_id": "tt0245429", "themoviedb_id": 129, "thetvdb_id": 55555, "type": "MOVIE"}
        ]
        """.trimIndent(),
        loadedAtEpochMs = 1_000L,
    )

    private val testUser = AniListUser(id = 1, name = "tester")

    @AfterTest
    fun tearDown() {
        AnimeIdBridge.resetForTests()
        AniListEpisodeMapper.clearCache()
    }

    @Test
    fun `animeAlternateWatchedKeys emits per-season and franchise keys with franchise completion safety`() {
        val aotS1 = AniListMediaEntry(
            mediaId = 16498,
            malId = 16498,
            titleRomaji = "Shingeki no Kyojin",
            titleEnglish = "Attack on Titan",
            format = "TV",
            episodes = 25,
            userStatus = AniListListStatus.COMPLETED,
            progress = 25,
            updatedAtSeconds = 100L,
        )
        val aotS2 = AniListMediaEntry(
            mediaId = 20958,
            malId = 25777,
            titleRomaji = "Shingeki no Kyojin 2",
            titleEnglish = "Attack on Titan Season 2",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 3,
            prequelIds = listOf(16498),
            updatedAtSeconds = 200L,
        )
        val snapshot = AniListSyncSnapshot(
            user = testUser,
            entries = listOf(aotS1, aotS2),
            checkedAtEpochMs = 1_000L,
        )

        val extraKeys = snapshot.animeAlternateWatchedKeys(sampleBridgeIndex)

        // S1 COMPLETED per-season posters are marked watched
        assertTrue("series:mal:16498:-1:-1" in extraKeys)
        assertTrue("series:kitsu:7442:-1:-1" in extraKeys)
        assertTrue("series:anilist:16498:-1:-1" in extraKeys)

        // Franchise poster MUST NOT be marked fully watched while S2 is unfinished
        assertFalse("series:tt2560140:-1:-1" in extraKeys)
        assertFalse("series:tmdb:1429:-1:-1" in extraKeys)
        assertFalse("series:tvdb:267440:-1:-1" in extraKeys)

        // S2 CURRENT ep 1..3 emitted under franchise and per-season IDs
        for (ep in 1..3) {
            assertTrue("series:tt2560140:2:$ep" in extraKeys)
            assertTrue("series:mal:25777:1:$ep" in extraKeys)
            assertTrue("series:kitsu:8671:1:$ep" in extraKeys)
        }
        assertFalse("series:tt2560140:2:4" in extraKeys)
        assertFalse("series:mal:25777:1:4" in extraKeys)
    }

    @Test
    fun `single canonical WatchedItem emitted per episode with zero duplicates`() {
        val aotS2 = AniListMediaEntry(
            mediaId = 20958,
            malId = 25777,
            titleRomaji = "Shingeki no Kyojin 2",
            titleEnglish = "Attack on Titan Season 2",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 3,
            updatedAtSeconds = 200L,
        )
        val items = buildAniListWatchedItems(
            entries = listOf(aotS2),
            bridgeIndex = sampleBridgeIndex,
        )

        assertEquals(3, items.size)
        assertTrue(items.all { it.id == "mal:25777" })
        assertTrue(items.none { it.id.startsWith("anilist:") })
        assertEquals(listOf(1, 2, 3), items.map { it.episode })
    }

    @Test
    fun `completed movie emits one canonical WatchedItem and alternate movie keys across mal kitsu imdb tmdb tvdb`() {
        val spiritedAway = AniListMediaEntry(
            mediaId = 199,
            malId = 199,
            titleRomaji = "Sen to Chihiro no Kamikakushi",
            titleEnglish = "Spirited Away",
            format = "MOVIE",
            episodes = 1,
            userStatus = AniListListStatus.COMPLETED,
            progress = 1,
            updatedAtSeconds = 300L,
        )
        val watchedItems = buildAniListWatchedItems(
            entries = listOf(spiritedAway),
            bridgeIndex = sampleBridgeIndex,
        )
        assertEquals(1, watchedItems.size)
        assertEquals("mal:199", watchedItems.first().id)
        assertEquals("movie", watchedItems.first().type)

        val snapshot = AniListSyncSnapshot(
            user = testUser,
            entries = listOf(spiritedAway),
            checkedAtEpochMs = 1_000L,
        )
        val extraKeys = snapshot.animeAlternateWatchedKeys(sampleBridgeIndex)
        assertTrue("movie:mal:199:-1:-1" in extraKeys)
        assertTrue("movie:kitsu:176:-1:-1" in extraKeys)
        assertTrue("movie:anilist:199:-1:-1" in extraKeys)
        assertTrue("movie:anidb:112:-1:-1" in extraKeys)
        assertTrue("movie:tt0245429:-1:-1" in extraKeys)
        assertTrue("movie:tmdb:129:-1:-1" in extraKeys)
        assertTrue("movie:tvdb:55555:-1:-1" in extraKeys)
    }

    @Test
    fun `isAniListEntryEpisodeWatched matches across kitsu imdb tmdb tvdb with seasonNumber and respects active-mode guard and optimistic unmark`() {
        val aotS1 = AniListMediaEntry(
            mediaId = 16498,
            malId = 16498,
            titleRomaji = "Shingeki no Kyojin",
            titleEnglish = "Attack on Titan",
            format = "TV",
            episodes = 25,
            userStatus = AniListListStatus.COMPLETED,
            progress = 25,
        )
        val aotS2 = AniListMediaEntry(
            mediaId = 20958,
            malId = 25777,
            titleRomaji = "Shingeki no Kyojin 2",
            titleEnglish = "Attack on Titan Season 2",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 3,
            prequelIds = listOf(16498),
        )
        val gantzS1 = AniListMediaEntry(
            mediaId = 384,
            malId = 384,
            titleRomaji = "Gantz",
            format = "TV",
            episodes = 13,
            userStatus = AniListListStatus.COMPLETED,
            progress = 13,
        )
        val gantzS2 = AniListMediaEntry(
            mediaId = 395,
            malId = 395,
            titleRomaji = "Gantz: Second Stage",
            format = "TV",
            episodes = 13,
            userStatus = AniListListStatus.CURRENT,
            progress = 4,
            prequelIds = listOf(384),
        )
        val entries = listOf(aotS1, aotS2, gantzS1, gantzS2)

        // Kitsu per-season lookup
        assertTrue(isAniListEntryEpisodeWatched(entries, "kitsu:8671", "kitsu:8671:3", 3, 1, sampleBridgeIndex))
        assertFalse(isAniListEntryEpisodeWatched(entries, "kitsu:8671", "kitsu:8671:4", 4, 1, sampleBridgeIndex))

        // Multi-season IMDB / TMDB / TVDB with seasonNumber
        assertTrue(isAniListEntryEpisodeWatched(entries, "tt2560140", "tt2560140:1:25", 25, 1, sampleBridgeIndex))
        assertTrue(isAniListEntryEpisodeWatched(entries, "tt2560140", "tt2560140:2:3", 3, 2, sampleBridgeIndex))
        assertFalse(isAniListEntryEpisodeWatched(entries, "tt2560140", "tt2560140:2:4", 4, 2, sampleBridgeIndex))
        assertTrue(isAniListEntryEpisodeWatched(entries, "tmdb:1429", "tmdb:1429:2:2", 2, 2, sampleBridgeIndex))
        assertTrue(isAniListEntryEpisodeWatched(entries, "tvdb:267440", "tvdb:267440:2:3", 3, 2, sampleBridgeIndex))

        // Titled sequel without explicit "Season 2" in title via prequel chain depth
        assertTrue(isAniListEntryEpisodeWatched(entries, "tt0434665", "tt0434665:2:4", 4, 2, sampleBridgeIndex))
        assertFalse(isAniListEntryEpisodeWatched(entries, "tt0434665", "tt0434665:2:5", 5, 2, sampleBridgeIndex))

        // Optimistic unmark suppresses watched checkmark immediately
        assertFalse(
            isAniListEntryEpisodeWatched(
                entries = entries,
                metaId = "tt2560140",
                videoId = "tt2560140:2:3",
                episodeNumber = 3,
                seasonNumber = 2,
                bridgeIndex = sampleBridgeIndex,
                optimisticallyRemovedVideoIds = setOf("tt2560140:2:3"),
            ),
        )

        // Active-mode guard prevents leaking checkmarks when dual mode is off or AniList disconnected
        assertTrue(isAniListWatchedLookupActive(true, WatchProgressSource.TRAKT, useAniListForAnime = true))
        assertTrue(isAniListWatchedLookupActive(true, WatchProgressSource.ANILIST, useAniListForAnime = false))
        assertFalse(isAniListWatchedLookupActive(true, WatchProgressSource.TRAKT, useAniListForAnime = false))
        assertFalse(isAniListWatchedLookupActive(false, WatchProgressSource.TRAKT, useAniListForAnime = true))
        assertFalse(isAniListWatchedLookupActive(false, WatchProgressSource.ANILIST, useAniListForAnime = true))
    }

    @Test
    fun `isAnimeTrackingTarget classifies Fribb imdb and tmdb anime ids while rejecting non-anime ids`() {
        AnimeIdBridge.setIndexForTests(sampleBridgeIndex)

        assertTrue(isAnimeTrackingTarget("tt0434665"))
        assertTrue(isAnimeTrackingTarget("tmdb:45782"))
        assertTrue(isAnimeTrackingTarget("tvdb:78957"))
        assertTrue(isAnimeTrackingTarget("tt2560140"))
        assertFalse(isAnimeTrackingTarget("tt0903747")) // Breaking Bad
        assertFalse(isAnimeTrackingTarget("tmdb:1396")) // Breaking Bad
    }

    @Test
    fun `mergeDualWatchProgress and mergeDualWatchedItems strip bridged franchise duplicates from primary`() {
        AnimeIdBridge.setIndexForTests(sampleBridgeIndex)

        val traktGantz = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0434665",
            parentMetaType = "series",
            videoId = "tt0434665:1:5",
            title = "GANTZ (Trakt)",
            seasonNumber = 1,
            episodeNumber = 5,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 100L,
            source = "trakt",
        )
        val traktBreakingBad = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0903747",
            parentMetaType = "series",
            videoId = "tt0903747:1:1",
            title = "Breaking Bad",
            seasonNumber = 1,
            episodeNumber = 1,
            lastPositionMs = 0L,
            durationMs = 3_000_000L,
            lastUpdatedEpochMs = 200L,
            source = "trakt",
        )
        val anilistGantzS1 = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "mal:384",
            parentMetaType = "series",
            videoId = "mal:384:1:10",
            title = "Gantz",
            seasonNumber = 1,
            episodeNumber = 10,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 300L,
            source = "anilist",
        )
        val anilistGantzS2 = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "mal:395",
            parentMetaType = "series",
            videoId = "mal:395:1:3",
            title = "Gantz: Second Stage",
            seasonNumber = 1,
            episodeNumber = 3,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 400L,
            source = "anilist",
        )

        val mergedProgress = mergeDualWatchProgress(
            primarySource = WatchProgressSource.TRAKT,
            primaryEntries = listOf(traktGantz, traktBreakingBad),
            anilistEntries = listOf(anilistGantzS1, anilistGantzS2),
            useAniListForAnime = true,
        )
        assertEquals(listOf("mal:395", "mal:384", "tt0903747"), mergedProgress.map { it.parentMetaId })

        val mergedWatched = mergeDualWatchedItems(
            primarySource = WatchProgressSource.TRAKT,
            primaryItems = listOf(
                WatchedItem(id = "tt0434665", type = "series", name = "GANTZ", season = 1, episode = 5, markedAtEpochMs = 100L),
                WatchedItem(id = "tt0903747", type = "series", name = "Breaking Bad", season = 1, episode = 1, markedAtEpochMs = 200L),
            ),
            anilistItems = listOf(
                WatchedItem(id = "mal:395", type = "series", name = "Gantz: Second Stage", season = 1, episode = 3, markedAtEpochMs = 300L),
            ),
            useAniListForAnime = true,
        )
        assertEquals(listOf("mal:395", "tt0903747"), mergedWatched.map { it.id })
    }

    @Test
    fun `buildAniListShowIdSiblings respects franchise completion safety`() {
        val aotS1 = AniListMediaEntry(
            mediaId = 16498,
            malId = 16498,
            titleRomaji = "Attack on Titan",
            format = "TV",
            episodes = 25,
            userStatus = AniListListStatus.COMPLETED,
            progress = 25,
        )
        val aotS2 = AniListMediaEntry(
            mediaId = 20958,
            malId = 25777,
            titleRomaji = "Attack on Titan Season 2",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 3,
        )
        val conan = AniListMediaEntry(
            mediaId = 235,
            malId = 235,
            titleRomaji = "Detective Conan",
            format = "TV",
            episodes = 1100,
            userStatus = AniListListStatus.COMPLETED,
            progress = 1100,
        )

        val siblings = buildAniListShowIdSiblings(listOf(aotS1, aotS2, conan), sampleBridgeIndex)

        // Unfinished multi-season franchise: per-season IDs are siblings, franchise ID is NOT included
        assertTrue("kitsu:7442" in siblings["mal:16498"].orEmpty())
        assertTrue("anilist:16498" in siblings["mal:16498"].orEmpty())
        assertFalse("tt2560140" in siblings["mal:16498"].orEmpty())

        // Single-entry franchise: franchise ID IS safely included
        assertTrue("tt0131179" in siblings["mal:235"].orEmpty())
        assertTrue("tmdb:30983" in siblings["mal:235"].orEmpty())
    }
}

package com.nuvio.app.features.anilist

import com.nuvio.app.features.player.shouldSendStopScrobble
import com.nuvio.app.features.tracking.TrackingExternalIds
import com.nuvio.app.features.tracking.TrackingMediaKind
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingScrobbleAction
import com.nuvio.app.features.tracking.TrackingScrobbleEvent
import com.nuvio.app.features.tracking.TrackingScrobbler
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import com.nuvio.app.features.tracking.dispatchTrackingProgressThresholdScrobble
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AniListScrobbleTest {
    private val sampleBridgeIndex: AnimeIdBridgeIndex = parseFribbAnimeListJson(
        """
        [
          {"anilist_id": 16498, "mal_id": 16498, "kitsu_id": 7442, "anidb_id": 9541, "imdb_id": "tt2560140", "themoviedb_id": 1429, "thetvdb_id": 267440, "type": "TV"},
          {"anilist_id": 20958, "mal_id": 25777, "kitsu_id": 8671, "anidb_id": 10944, "imdb_id": "tt2560140", "themoviedb_id": 1429, "thetvdb_id": 267440, "type": "TV"},
          {"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
          {"anilist_id": 395, "mal_id": 395, "kitsu_id": 352, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
          {"anilist_id": 199, "mal_id": 199, "kitsu_id": 176, "imdb_id": "tt0245429", "themoviedb_id": 129, "type": "MOVIE"}
        ]
        """.trimIndent(),
        loadedAtEpochMs = 1_000L,
    )

    @AfterTest
    fun tearDown() {
        AnimeIdBridge.resetForTests()
        AniListEpisodeMapper.clearCache()
    }

    @Test
    fun `decideAniListScrobbleMutation promotes PLANNING PAUSED and DROPPED to CURRENT and completes final episode or movie`() {
        for (status in listOf(AniListListStatus.PLANNING, AniListListStatus.PAUSED, AniListListStatus.DROPPED)) {
            val entry = AniListMediaEntry(
                mediaId = 20958,
                titleRomaji = "Shingeki no Kyojin 2",
                episodes = 12,
                userStatus = status,
                progress = 2,
            )
            val decision = decideAniListScrobbleMutation(existing = entry, targetProgress = 3, isMovie = false)
            assertTrue(decision.shouldWrite, "Expected write for status=$status")
            assertEquals(AniListListStatus.CURRENT, decision.newStatus)
            assertEquals(3, decision.newProgress)
        }

        // Brand-new untracked show starts as CURRENT
        val newEntryDecision = decideAniListScrobbleMutation(existing = null, targetProgress = 1, isMovie = false)
        assertTrue(newEntryDecision.shouldWrite)
        assertEquals(AniListListStatus.CURRENT, newEntryDecision.newStatus)
        assertEquals(1, newEntryDecision.newProgress)

        // Final episode auto-completes
        val finalEpEntry = AniListMediaEntry(
            mediaId = 20958,
            titleRomaji = "Shingeki no Kyojin 2",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 11,
        )
        val finalEpDecision = decideAniListScrobbleMutation(existing = finalEpEntry, targetProgress = 12, isMovie = false)
        assertTrue(finalEpDecision.shouldWrite)
        assertEquals(AniListListStatus.COMPLETED, finalEpDecision.newStatus)
        assertEquals(12, finalEpDecision.newProgress)

        // Anime movie auto-completes
        val movieEntry = AniListMediaEntry(
            mediaId = 199,
            titleRomaji = "Sen to Chihiro no Kamikakushi",
            format = "MOVIE",
            episodes = 1,
            userStatus = AniListListStatus.PLANNING,
            progress = 0,
        )
        val movieDecision = decideAniListScrobbleMutation(existing = movieEntry, targetProgress = 1, isMovie = true)
        assertTrue(movieDecision.shouldWrite)
        assertEquals(AniListListStatus.COMPLETED, movieDecision.newStatus)
        assertEquals(1, movieDecision.newProgress)
    }

    @Test
    fun `decideAniListScrobbleMutation is no-op for COMPLETED replay and skips when CURRENT or REPEATING already at or past targetProgress`() {
        val completedSeries = AniListMediaEntry(
            mediaId = 16498,
            titleRomaji = "Shingeki no Kyojin",
            episodes = 25,
            userStatus = AniListListStatus.COMPLETED,
            progress = 25,
        )
        assertFalse(decideAniListScrobbleMutation(completedSeries, targetProgress = 5, isMovie = false).shouldWrite)

        val completedMovie = AniListMediaEntry(
            mediaId = 199,
            titleRomaji = "Sen to Chihiro no Kamikakushi",
            format = "MOVIE",
            episodes = 1,
            userStatus = AniListListStatus.COMPLETED,
            progress = 1,
        )
        assertFalse(decideAniListScrobbleMutation(completedMovie, targetProgress = 1, isMovie = true).shouldWrite)

        val currentAhead = AniListMediaEntry(
            mediaId = 20958,
            titleRomaji = "Shingeki no Kyojin 2",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 5,
        )
        assertFalse(decideAniListScrobbleMutation(currentAhead, targetProgress = 5, isMovie = false).shouldWrite)
        assertFalse(decideAniListScrobbleMutation(currentAhead, targetProgress = 3, isMovie = false).shouldWrite)

        val repeatingAhead = currentAhead.copy(userStatus = AniListListStatus.REPEATING)
        assertFalse(decideAniListScrobbleMutation(repeatingAhead, targetProgress = 5, isMovie = false).shouldWrite)
        assertFalse(decideAniListScrobbleMutation(repeatingAhead, targetProgress = 2, isMovie = false).shouldWrite)
    }

    @Test
    fun `decideAniListScrobbleMutation increments REPEATING progress and transitions to COMPLETED when reaching total episodes`() {
        val repeating = AniListMediaEntry(
            mediaId = 20958,
            titleRomaji = "Shingeki no Kyojin 2",
            episodes = 12,
            userStatus = AniListListStatus.REPEATING,
            progress = 5,
        )
        val midDecision = decideAniListScrobbleMutation(repeating, targetProgress = 6, isMovie = false)
        assertTrue(midDecision.shouldWrite)
        assertEquals(AniListListStatus.REPEATING, midDecision.newStatus)
        assertEquals(6, midDecision.newProgress)

        val endDecision = decideAniListScrobbleMutation(
            repeating.copy(progress = 11),
            targetProgress = 12,
            isMovie = false,
        )
        assertTrue(endDecision.shouldWrite)
        assertEquals(AniListListStatus.COMPLETED, endDecision.newStatus)
        assertEquals(12, endDecision.newProgress)
    }

    @Test
    fun `AniListScrobbleDeduplicator allows single write across PROGRESS PAUSE STOP blocks concurrent in-flight duplicates and releases key on failure for STOP retry`() {
        val dedup = AniListScrobbleDeduplicator()

        // Transient network failure at 50% PROGRESS releases key so STOP can retry
        assertTrue(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
        // Concurrent in-flight duplicate is blocked
        assertFalse(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
        // Network failure releases key
        dedup.markFailed(profileId = 1, mediaId = 20958, targetProgress = 5)

        // Retry on PAUSE/STOP succeeds once
        assertTrue(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
        dedup.markSucceeded(profileId = 1, mediaId = 20958, targetProgress = 5)

        // Subsequent PAUSE/STOP calls for the same episode are deduplicated
        assertFalse(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
        // Next episode is allowed independently
        assertTrue(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 6))
        dedup.markSucceeded(profileId = 1, mediaId = 20958, targetProgress = 6)

        // Manual episode unmark invalidates the show's keys so rewatching can scrobble again
        dedup.invalidate(profileId = 1, mediaId = 20958)
        assertTrue(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
        dedup.markSucceeded(profileId = 1, mediaId = 20958, targetProgress = 5)

        // Clear resets all entries (profile change / disconnect)
        dedup.clear()
        assertTrue(dedup.tryBegin(profileId = 1, mediaId = 20958, targetProgress = 5))
    }

    @Test
    fun `scrobbleProgressThreshold dispatches PROGRESS only to scrobblers with scrobbleOnThresholdReached and progressPercent at or above threshold`() = runBlocking {
        val trakt = RecordingScrobbler(
            providerId = TrackingProviderId.TRAKT,
            scrobbleThresholdPercent = 80.0,
            scrobbleOnThresholdReached = false,
        )
        val simkl = RecordingScrobbler(
            providerId = TrackingProviderId.SIMKL,
            scrobbleThresholdPercent = 80.0,
            scrobbleOnThresholdReached = false,
        )
        val anilist = RecordingScrobbler(
            providerId = TrackingProviderId.ANILIST,
            scrobbleThresholdPercent = 50.0,
            scrobbleOnThresholdReached = true,
        )
        val media = TrackingMediaReference(
            kind = TrackingMediaKind.ANIME,
            title = "Attack on Titan Season 2",
            ids = TrackingExternalIds(mal = 25777),
        )

        // Below 50% dispatches to nobody
        val belowFailures = dispatchTrackingProgressThresholdScrobble(
            scrobblers = listOf(trakt, simkl, anilist),
            profileId = 1,
            event = TrackingScrobbleEvent(media = media, progressPercent = 49.9),
        )
        assertTrue(belowFailures.isEmpty())
        assertEquals(0, trakt.actions.size)
        assertEquals(0, simkl.actions.size)
        assertEquals(0, anilist.actions.size)

        // At 50% (and 70% paused-scrub flush) dispatches PROGRESS ONLY to AniList
        val atThresholdFailures = dispatchTrackingProgressThresholdScrobble(
            scrobblers = listOf(trakt, simkl, anilist),
            profileId = 1,
            event = TrackingScrobbleEvent(media = media, progressPercent = 70.0),
        )
        assertTrue(atThresholdFailures.isEmpty())
        assertEquals(0, trakt.actions.size)
        assertEquals(0, simkl.actions.size)
        assertEquals(listOf(TrackingScrobbleAction.PROGRESS), anilist.actions)

        // Transient failure is reported so PlayerScreenRuntime can reset hasSentMidPlaybackScrobbleForCurrentItem
        val failingAniList = RecordingScrobbler(
            providerId = TrackingProviderId.ANILIST,
            scrobbleThresholdPercent = 50.0,
            scrobbleOnThresholdReached = true,
            failure = IllegalStateException("network timeout"),
        )
        val failures = dispatchTrackingProgressThresholdScrobble(
            scrobblers = listOf(trakt, failingAniList),
            profileId = 1,
            event = TrackingScrobbleEvent(media = media, progressPercent = 52.0),
        )
        assertEquals(listOf(TrackingProviderId.ANILIST), failures.map { it.providerId })
        assertEquals(0, trakt.actions.size)
    }

    @Test
    fun `AniListTrackingWrites declares 50 percent mid-playback threshold while Trakt Simkl 80 percent stop guard remains untouched`() {
        assertEquals(50.0, AniListTracker.writes.scrobbleThresholdPercent)
        assertTrue(AniListTracker.writes.scrobbleOnThresholdReached)
        // Paused at 10% (hasActiveScrobble = false), scrubbed to 70%, and exited: Trakt/Simkl STOP stays suppressed < 80%
        assertFalse(shouldSendStopScrobble(hasActiveScrobble = false, progressPercent = 70f))
        assertTrue(shouldSendStopScrobble(hasActiveScrobble = false, progressPercent = 80f))
    }

    @Test
    fun `resolveTargetMediaAndProgress resolves per-season videoId per-season parentMetaId and multi-season franchise tt id S2E5 to exact AniList mediaId and relative episode`() = runBlocking {
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
            titleEnglish = "Gantz",
            format = "TV",
            episodes = 13,
            userStatus = AniListListStatus.COMPLETED,
            progress = 13,
        )
        val gantzS2 = AniListMediaEntry(
            mediaId = 395,
            malId = 395,
            titleRomaji = "Gantz: 2nd Stage",
            titleEnglish = "Gantz: Second Stage",
            format = "TV",
            episodes = 13,
            userStatus = AniListListStatus.CURRENT,
            progress = 2,
            prequelIds = listOf(384),
        )
        val allEntries = listOf(aotS1, aotS2, gantzS1, gantzS2)

        // Path A: Anime-prefixed videoId (mal / kitsu) resolves 1-to-1 mediaId and flat episode
        val videoIdMedia = buildTrackingMediaReference(
            contentType = "series",
            parentMetaId = "tt2560140",
            videoId = "kitsu:8671:7",
            title = "Attack on Titan",
            seasonNumber = 2,
            episodeNumber = 7,
        )
        val videoIdTarget = resolveTargetMediaAndProgress(
            media = videoIdMedia,
            allEntries = allEntries,
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { emptyList() },
        )
        assertNotNull(videoIdTarget)
        assertEquals(20958, videoIdTarget.mediaId)
        assertEquals(7, videoIdTarget.targetProgress)
        assertFalse(videoIdTarget.isMovie)

        // Path B: 1-to-1 per-season parentMetaId (mal:395) resolves directly without prequel offset
        val perSeasonMetaMedia = buildTrackingMediaReference(
            contentType = "series",
            parentMetaId = "mal:395",
            videoId = null,
            title = "Gantz: Second Stage",
            seasonNumber = 1,
            episodeNumber = 4,
        )
        val perSeasonTarget = resolveTargetMediaAndProgress(
            media = perSeasonMetaMedia,
            allEntries = allEntries,
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { emptyList() },
        )
        assertNotNull(perSeasonTarget)
        assertEquals(395, perSeasonTarget.mediaId)
        assertEquals(4, perSeasonTarget.targetProgress)
        assertFalse(perSeasonTarget.isMovie)

        // Path C (explicit season title): Franchise tt2560140 S2E5 -> AoT S2 (20958), progress = 5
        val franchiseAotMedia = buildTrackingMediaReference(
            contentType = "series",
            parentMetaId = "tt2560140",
            videoId = "tt2560140:2:5",
            title = "Attack on Titan",
            seasonNumber = 2,
            episodeNumber = 5,
        )
        val franchiseAotTarget = resolveTargetMediaAndProgress(
            media = franchiseAotMedia,
            allEntries = allEntries,
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { emptyList() },
        )
        assertNotNull(franchiseAotTarget)
        assertEquals(20958, franchiseAotTarget.mediaId)
        assertEquals(5, franchiseAotTarget.targetProgress)
        assertFalse(franchiseAotTarget.isMovie)

        // Path C (cumulative prequel offset): Franchise tt0434665 (Gantz) S2E5 with multi-season addon episodes -> Gantz S2 (395), relative ep 5
        val gantzAddonEpisodes = (1..13).map { SeasonEpisodeRef(season = 1, episode = it) } +
            (1..13).map { SeasonEpisodeRef(season = 2, episode = it) }
        val franchiseGantzMedia = buildTrackingMediaReference(
            contentType = "series",
            parentMetaId = "tt0434665",
            videoId = "tt0434665:2:5",
            title = "Gantz",
            seasonNumber = 2,
            episodeNumber = 5,
        )
        val franchiseGantzTarget = resolveTargetMediaAndProgress(
            media = franchiseGantzMedia,
            allEntries = allEntries,
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { gantzAddonEpisodes },
        )
        assertNotNull(franchiseGantzTarget)
        assertEquals(395, franchiseGantzTarget.mediaId)
        assertEquals(5, franchiseGantzTarget.targetProgress)
        assertFalse(franchiseGantzTarget.isMovie)

        // Path C (untracked Season 2 fresh start): falls back to title + Season N search
        var lastSearchedTitle: String? = null
        val freshSeason2Target = resolveTargetMediaAndProgress(
            media = franchiseAotMedia,
            allEntries = listOf(aotS1),
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { emptyList() },
            searchMediaIdByTitleOrMal = { title, _ ->
                lastSearchedTitle = title
                20958
            },
        )
        assertNotNull(freshSeason2Target)
        assertEquals("Attack on Titan Season 2", lastSearchedTitle)
        assertEquals(20958, freshSeason2Target.mediaId)
        assertEquals(5, freshSeason2Target.targetProgress)

        // Anime movie via mal:199 resolves isMovie = true even though TrackingMediaKind is ANIME
        val animeMovieMedia = buildTrackingMediaReference(
            contentType = "movie",
            parentMetaId = "mal:199",
            title = "Spirited Away",
        )
        val animeMovieTarget = resolveTargetMediaAndProgress(
            media = animeMovieMedia,
            allEntries = allEntries,
            bridgeIndex = sampleBridgeIndex,
            resolveAddonEpisodes = { emptyList() },
        )
        assertNotNull(animeMovieTarget)
        assertEquals(199, animeMovieTarget.mediaId)
        assertEquals(1, animeMovieTarget.targetProgress)
        assertTrue(animeMovieTarget.isMovie)
    }

    private class RecordingScrobbler(
        override val providerId: TrackingProviderId,
        override val scrobbleThresholdPercent: Double,
        override val scrobbleOnThresholdReached: Boolean,
        private val failure: Throwable? = null,
    ) : TrackingScrobbler {
        val actions = mutableListOf<TrackingScrobbleAction>()

        override suspend fun scrobble(
            profileId: Int,
            action: TrackingScrobbleAction,
            event: TrackingScrobbleEvent,
        ) {
            actions += action
            failure?.let { throw it }
        }
    }
}

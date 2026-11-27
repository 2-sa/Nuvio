package com.nuvio.app.features.anilist

import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.home.CompletedSeriesCandidate
import com.nuvio.app.features.home.buildHomeContinueWatchingItems
import com.nuvio.app.features.home.buildSyntheticAniListNextUpItem
import com.nuvio.app.features.home.filterCachedResolvedNextUpItems
import com.nuvio.app.features.home.filterHomeNextUpCandidatesForContinueWatchingWindow
import com.nuvio.app.features.home.opensDetails
import com.nuvio.app.features.home.shouldBypassContinueWatchingCutoffForCandidate
import com.nuvio.app.features.plugins.PluginsUiState
import com.nuvio.app.features.streams.hasCompatiblePlaybackSource
import com.nuvio.app.features.streams.resolveStreamAddonTargets
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.watching.domain.WatchingContentRef
import com.nuvio.app.features.watchprogress.ContinueWatchingClickAction
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressSourceTraktShowProgress
import com.nuvio.app.features.watchprogress.prepareNextUpProgressEntriesWithDelegate
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniListContinueWatchingTest {

    @AfterTest
    fun tearDown() {
        AnimeIdBridge.resetForTests()
    }

    @Test
    fun `buildSyntheticAniListNextUpItem returns clickable NextUp card for airing show with episodes null when addon lacks next episode`() {
        val nowMs = 1_770_000_000_000L

        val blackCloverS2 = AniListMediaEntry(
            mediaId = 200001,
            malId = 55001,
            titleRomaji = "Black Clover 2nd Season",
            titleEnglish = "Black Clover Season 2",
            format = "TV",
            status = "RELEASING",
            episodes = null,
            coverImageUrl = "https://img.anilist.co/bc2.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 2,
            updatedAtSeconds = 1_769_999_000L,
        )
        val bcCard = buildSyntheticAniListNextUpItem(
            entry = blackCloverS2,
            contentId = "mal:55001",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 2,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNotNull(bcCard)
        assertTrue(bcCard.isNextUp)
        assertTrue(bcCard.isSyntheticFallback)
        assertEquals("mal:55001", bcCard.parentMetaId)
        assertEquals("series", bcCard.parentMetaType)
        assertEquals(1, bcCard.seasonNumber)
        assertEquals(3, bcCard.episodeNumber)
        assertEquals("mal:55001:1:3", bcCard.videoId)
        assertEquals("Black Clover Season 2", bcCard.title)
        assertEquals("https://img.anilist.co/bc2.jpg", bcCard.poster)
        assertEquals("https://img.anilist.co/bc2.jpg", bcCard.imageUrl)
        assertEquals(1, bcCard.nextUpSeedSeasonNumber)
        assertEquals(2, bcCard.nextUpSeedEpisodeNumber)
        assertNull(bcCard.released)

        val dbsBeerus = AniListMediaEntry(
            mediaId = 200002,
            malId = 55002,
            titleRomaji = "Dragon Ball Super: Beerus",
            titleEnglish = "Dragon Ball Super: Beerus",
            format = "TV",
            status = "RELEASING",
            episodes = null,
            coverImageUrl = "https://img.anilist.co/dbs.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 1,
            nextAiringEpisode = 2,
            nextAiringAtSeconds = (nowMs / 1000L) - 3_600L, // already aired 1h ago
        )
        val dbsCard = buildSyntheticAniListNextUpItem(
            entry = dbsBeerus,
            contentId = "mal:55002",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 1,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNotNull(dbsCard)
        assertEquals(2, dbsCard.episodeNumber)
        assertTrue(dbsCard.isSyntheticFallback)
        assertNotNull(dbsCard.released)

        val overgeared = AniListMediaEntry(
            mediaId = 200003,
            malId = 55003,
            titleRomaji = "Overgeared",
            format = "ONA",
            status = "RELEASING",
            episodes = null,
            coverImageUrl = "https://img.anilist.co/overgeared.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 4,
        )
        val overgearedCard = buildSyntheticAniListNextUpItem(
            entry = overgeared,
            contentId = "mal:55003",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 4,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNotNull(overgearedCard)
        assertEquals(5, overgearedCard.episodeNumber)
        assertEquals("Overgeared", overgearedCard.title)
        assertTrue(overgearedCard.isSyntheticFallback)
    }

    @Test
    fun `buildSyntheticAniListNextUpItem returns null when nextAiringEpisode is in future and showUnairedNextUp is false and includes ISO released date when true`() {
        val nowMs = 1_770_000_000_000L
        val futureAiringSeconds = 1_770_086_400L // 2026-02-03T16:00:00Z (+24h)
        val entry = AniListMediaEntry(
            mediaId = 200002,
            malId = 55002,
            titleRomaji = "Dragon Ball Super: Beerus",
            format = "TV",
            status = "RELEASING",
            episodes = null,
            coverImageUrl = "https://img.anilist.co/dbs.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 1,
            nextAiringEpisode = 2,
            nextAiringAtSeconds = futureAiringSeconds,
        )

        val hidden = buildSyntheticAniListNextUpItem(
            entry = entry,
            contentId = "mal:55002",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 1,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNull(hidden)

        val shown = buildSyntheticAniListNextUpItem(
            entry = entry,
            contentId = "mal:55002",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 1,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = true,
        )
        assertNotNull(shown)
        assertTrue(shown.isSyntheticFallback)
        assertEquals(2, shown.episodeNumber)
        assertEquals("2026-02-03T16:00:00Z", shown.released)
    }

    @Test
    fun `buildSyntheticAniListNextUpItem supports progress 0 as Episode 1 and suppresses unaired Ep 1 when showUnairedNextUp is false`() {
        val nowMs = 1_770_000_000_000L
        val airingEp1 = AniListMediaEntry(
            mediaId = 200010,
            malId = 55010,
            titleRomaji = "New Airing Show",
            format = "TV",
            status = "RELEASING",
            episodes = 12,
            coverImageUrl = "https://img.anilist.co/new.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 0,
        )
        val ep1Card = buildSyntheticAniListNextUpItem(
            entry = airingEp1,
            contentId = "mal:55010",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 0,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNotNull(ep1Card)
        assertEquals(1, ep1Card.seasonNumber)
        assertEquals(1, ep1Card.episodeNumber)
        assertEquals(0, ep1Card.nextUpSeedEpisodeNumber)

        val unairedWithSchedule = airingEp1.copy(
            status = "NOT_YET_RELEASED",
            nextAiringEpisode = 1,
            nextAiringAtSeconds = (nowMs / 1000L) + 86_400L,
        )
        assertNull(
            buildSyntheticAniListNextUpItem(
                entry = unairedWithSchedule,
                contentId = "mal:55010",
                seedSeasonNumber = 1,
                seedEpisodeNumber = 0,
                meta = null,
                nowEpochMs = nowMs,
                showUnairedNextUp = false,
            ),
        )
        val unairedShown = buildSyntheticAniListNextUpItem(
            entry = unairedWithSchedule,
            contentId = "mal:55010",
            seedSeasonNumber = 1,
            seedEpisodeNumber = 0,
            meta = null,
            nowEpochMs = nowMs,
            showUnairedNextUp = true,
        )
        assertNotNull(unairedShown)
        assertEquals(1, unairedShown.episodeNumber)
        assertNotNull(unairedShown.released)

        val unairedWithoutSchedule = airingEp1.copy(
            status = "NOT_YET_RELEASED",
            nextAiringEpisode = null,
            nextAiringAtSeconds = null,
        )
        assertNull(
            buildSyntheticAniListNextUpItem(
                entry = unairedWithoutSchedule,
                contentId = "mal:55010",
                seedSeasonNumber = 1,
                seedEpisodeNumber = 0,
                meta = null,
                nowEpochMs = nowMs,
                showUnairedNextUp = false,
            ),
        )
    }

    @Test
    fun `buildSyntheticAniListNextUpItem uses explicit season when fallback meta has multiple seasons`() {
        val nowMs = 1_770_000_000_000L
        val blackCloverS2 = AniListMediaEntry(
            mediaId = 200001,
            malId = 55001,
            titleRomaji = "Black Clover 2nd Season",
            titleEnglish = "Black Clover Season 2",
            format = "TV",
            status = "RELEASING",
            episodes = null,
            coverImageUrl = "https://img.anilist.co/bc2.jpg",
            userStatus = AniListListStatus.CURRENT,
            progress = 2,
        )
        val multiSeasonMeta = MetaDetails(
            id = "tt7441658",
            type = "series",
            name = "Black Clover",
            poster = "https://cinemeta/bc.jpg",
            background = "https://cinemeta/bc-bg.jpg",
            videos = listOf(
                MetaVideo(id = "tt7441658:1:1", title = "Ep 1", season = 1, episode = 1),
                MetaVideo(id = "tt7441658:2:1", title = "S2 Ep 1", season = 2, episode = 1),
                MetaVideo(id = "tt7441658:2:2", title = "S2 Ep 2", season = 2, episode = 2),
            ),
        )

        val card = buildSyntheticAniListNextUpItem(
            entry = blackCloverS2,
            contentId = "mal:55001",
            seedSeasonNumber = 2,
            seedEpisodeNumber = 2,
            meta = multiSeasonMeta,
            nowEpochMs = nowMs,
            showUnairedNextUp = false,
        )
        assertNotNull(card)
        assertEquals(2, card.seasonNumber)
        assertEquals(3, card.episodeNumber)
        assertEquals("https://cinemeta/bc-bg.jpg", card.imageUrl)
        assertEquals("Black Clover", card.title)
    }

    @Test
    fun `filterCachedResolvedNextUpItems excludes synthetic fallback items so they always re-resolve when addon metadata catches up`() {
        val candidateA = CompletedSeriesCandidate(
            content = WatchingContentRef(type = "series", id = "mal:55001"),
            seasonNumber = 1,
            episodeNumber = 2,
            markedAtEpochMs = 5_000L,
        )
        val candidateB = CompletedSeriesCandidate(
            content = WatchingContentRef(type = "series", id = "mal:384"),
            seasonNumber = 1,
            episodeNumber = 4,
            markedAtEpochMs = 4_000L,
        )
        val syntheticCached = ContinueWatchingItem(
            parentMetaId = "mal:55001",
            parentMetaType = "series",
            videoId = "mal:55001:1:3",
            title = "Black Clover Season 2",
            subtitle = "S1E3",
            imageUrl = "https://img.anilist.co/bc2.jpg",
            poster = "https://img.anilist.co/bc2.jpg",
            seasonNumber = 1,
            episodeNumber = 3,
            isNextUp = true,
            nextUpSeedSeasonNumber = 1,
            nextUpSeedEpisodeNumber = 2,
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
            isSyntheticFallback = true,
        )
        val realCached = ContinueWatchingItem(
            parentMetaId = "mal:384",
            parentMetaType = "series",
            videoId = "mal:384:1:5",
            title = "Gantz",
            subtitle = "S1E5 • Real Episode",
            imageUrl = "https://addon/gantz-ep5.jpg",
            poster = "https://addon/gantz.jpg",
            seasonNumber = 1,
            episodeNumber = 5,
            isNextUp = true,
            nextUpSeedSeasonNumber = 1,
            nextUpSeedEpisodeNumber = 4,
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
            isSyntheticFallback = false,
        )

        val resolved = filterCachedResolvedNextUpItems(
            candidates = listOf(candidateA, candidateB),
            cachedNextUpItems = mapOf(
                "mal:55001" to (5_000L to syntheticCached),
                "mal:384" to (4_000L to realCached),
            ),
        )

        assertFalse("mal:55001" in resolved)
        assertTrue("mal:384" in resolved)
    }

    @Test
    fun `shouldBypassContinueWatchingCutoffForCandidate preserves active AniList entries older than Trakt daysCap`() {
        val cutoff = 10_000L
        val oldAniList = CompletedSeriesCandidate(
            content = WatchingContentRef(type = "series", id = "mal:384"),
            seasonNumber = 1,
            episodeNumber = 5,
            markedAtEpochMs = 1_000L,
        )
        val oldTrakt = CompletedSeriesCandidate(
            content = WatchingContentRef(type = "series", id = "tt0903747"),
            seasonNumber = 2,
            episodeNumber = 3,
            markedAtEpochMs = 1_000L,
        )
        val recentTrakt = CompletedSeriesCandidate(
            content = WatchingContentRef(type = "series", id = "tt0944947"),
            seasonNumber = 1,
            episodeNumber = 2,
            markedAtEpochMs = 20_000L,
        )
        val isAniList: (String) -> Boolean = { it == "mal:384" }

        assertTrue(shouldBypassContinueWatchingCutoffForCandidate(oldAniList, isAniList))
        assertFalse(shouldBypassContinueWatchingCutoffForCandidate(oldTrakt, isAniList))

        val filtered = filterHomeNextUpCandidatesForContinueWatchingWindow(
            candidates = listOf(oldAniList, oldTrakt, recentTrakt),
            cutoffEpochMs = cutoff,
            isAniListCandidate = isAniList,
        )
        assertEquals(listOf("mal:384", "tt0944947"), filtered.map { it.content.id })
    }

    @Test
    fun `distinct mal canonical ids keep Gantz and Gantz Second Stage separate in buildHomeContinueWatchingItems`() {
        val gantzS1 = ContinueWatchingItem(
            parentMetaId = "mal:384",
            parentMetaType = "series",
            videoId = "mal:384:1:6",
            title = "Gantz",
            subtitle = "S1E6",
            imageUrl = "https://img.anilist.co/gantz1.jpg",
            poster = "https://img.anilist.co/gantz1.jpg",
            seasonNumber = 1,
            episodeNumber = 6,
            isNextUp = true,
            nextUpSeedSeasonNumber = 1,
            nextUpSeedEpisodeNumber = 5,
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
        )
        val gantzS2 = ContinueWatchingItem(
            parentMetaId = "mal:395",
            parentMetaType = "series",
            videoId = "mal:395:1:4",
            title = "Gantz: Second Stage",
            subtitle = "S1E4",
            imageUrl = "https://img.anilist.co/gantz2.jpg",
            poster = "https://img.anilist.co/gantz2.jpg",
            seasonNumber = 1,
            episodeNumber = 4,
            isNextUp = true,
            nextUpSeedSeasonNumber = 1,
            nextUpSeedEpisodeNumber = 3,
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
        )

        val items = buildHomeContinueWatchingItems(
            visibleEntries = emptyList(),
            nextUpItemsBySeries = mapOf(
                "mal:384" to (2_000L to gantzS1),
                "mal:395" to (3_000L to gantzS2),
            ),
        )

        assertEquals(listOf("mal:395", "mal:384"), items.map { it.parentMetaId })
    }

    @Test
    fun `prepareNextUpProgressEntries bypasses primary provider remapping for anilist source entries`() = runTest {
        val aniListEntry = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "mal:395",
            parentMetaType = "series",
            videoId = "mal:395:2:3",
            title = "Gantz: Second Stage",
            seasonNumber = 2,
            episodeNumber = 3,
            lastPositionMs = 0L,
            durationMs = 1_440_000L,
            lastUpdatedEpochMs = 5_000L,
            isCompleted = true,
            progressPercent = 100f,
            source = TrackingProviderId.ANILIST.storageId,
        )
        val traktEntry = WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt0903747",
            parentMetaType = "series",
            videoId = "tt0903747:1:2",
            title = "Breaking Bad",
            seasonNumber = 1,
            episodeNumber = 2,
            lastPositionMs = 0L,
            durationMs = 2_700_000L,
            lastUpdatedEpochMs = 5_000L,
            isCompleted = true,
            progressPercent = 100f,
            source = WatchProgressSourceTraktShowProgress,
        )

        var delegateInvoked = false
        val aniListResult = prepareNextUpProgressEntriesWithDelegate(
            entries = listOf(aniListEntry),
            contentId = "mal:395",
            delegate = { entries, _ ->
                delegateInvoked = true
                entries.map { it.copy(seasonNumber = 99) }
            },
        )
        assertFalse(delegateInvoked)
        assertEquals(listOf(aniListEntry), aniListResult)

        val traktResult = prepareNextUpProgressEntriesWithDelegate(
            entries = listOf(traktEntry),
            contentId = "tt0903747",
            delegate = { entries, _ ->
                delegateInvoked = true
                entries.map { it.copy(seasonNumber = 2) }
            },
        )
        assertTrue(delegateInvoked)
        assertEquals(2, traktResult.single().seasonNumber)
    }

    @Test
    fun `streams and playback availability fall back via AnimeIdBridge and synthetic ClickAction STREAM routes cleanly when no addon matches`() {
        val index = parseFribbAnimeListJson(
            """[{"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "imdb_id": "tt0434665", "themoviedb_id": 45782, "type": "TV"}]""",
            loadedAtEpochMs = 1_000L,
        )
        val imdbOnlyStreamAddon = ManagedAddon(
            manifestUrl = "https://torrentio.example/manifest.json",
            manifest = AddonManifest(
                id = "torrentio",
                name = "Torrentio",
                description = "Streams",
                version = "1.0.0",
                resources = listOf(
                    AddonResource(
                        name = "stream",
                        types = listOf("series", "movie"),
                        idPrefixes = listOf("tt"),
                    ),
                ),
                types = listOf("series", "movie"),
                transportUrl = "https://torrentio.example/manifest.json",
            ),
            enabled = true,
        )

        val (resolvedVideoId, targets) = resolveStreamAddonTargets(
            installedAddons = listOf(imdbOnlyStreamAddon),
            type = "series",
            videoId = "mal:384:1:5",
            season = 1,
            episode = 5,
            bridgeIndex = index,
        )
        assertEquals("tt0434665:1:5", resolvedVideoId)
        assertEquals(1, targets.size)
        assertEquals("Torrentio", targets.single().addonName)

        assertTrue(
            hasCompatiblePlaybackSource(
                addons = listOf(imdbOnlyStreamAddon),
                plugins = PluginsUiState(pluginsEnabled = false),
                type = "series",
                videoId = "mal:384:1:5",
                bridgeIndex = index,
            ),
        )
        assertFalse(
            hasCompatiblePlaybackSource(
                addons = listOf(imdbOnlyStreamAddon),
                plugins = PluginsUiState(pluginsEnabled = false),
                type = "series",
                videoId = "anilist:999999:1:1",
                bridgeIndex = index,
            ),
        )

        val syntheticCard = ContinueWatchingItem(
            parentMetaId = "anilist:999999",
            parentMetaType = "series",
            videoId = "anilist:999999:1:1",
            title = "Unbridged Anime",
            subtitle = "S1E1",
            imageUrl = "https://img.anilist.co/unbridged.jpg",
            poster = "https://img.anilist.co/unbridged.jpg",
            seasonNumber = 1,
            episodeNumber = 1,
            isNextUp = true,
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
            isSyntheticFallback = true,
        )
        assertTrue(
            ContinueWatchingClickAction.STREAM.opensDetails(
                item = syntheticCard,
                canSelectStreams = false,
            ),
        )
        assertFalse(
            ContinueWatchingClickAction.STREAM.opensDetails(
                item = syntheticCard,
                canSelectStreams = true,
            ),
        )
    }
}

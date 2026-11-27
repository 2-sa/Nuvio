package com.nuvio.app.features.anilist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AniListEpisodeMapperTest {
    private val multiSeasonShow = buildList {
        // Specials in season 0 must be ignored when indexing absolute episodes
        add(SeasonEpisodeRef(season = 0, episode = 1))
        add(SeasonEpisodeRef(season = 0, episode = 2))
        // Season 1: 42 episodes (abs 1..42)
        for (ep in 1..42) add(SeasonEpisodeRef(season = 1, episode = ep))
        // Season 2: 43 episodes (abs 43..85)
        for (ep in 1..43) add(SeasonEpisodeRef(season = 2, episode = ep))
        // Season 3: 40 episodes (abs 86..125)
        for (ep in 1..40) add(SeasonEpisodeRef(season = 3, episode = ep))
    }

    @Test
    fun `maps absolute episode across multi-season show ignoring season 0 specials`() {
        assertEquals(
            SeasonEpisodeRef(season = 2, episode = 43),
            mapAbsoluteEpisodeToSeasonEpisode(85, multiSeasonShow),
        )
        assertEquals(
            SeasonEpisodeRef(season = 3, episode = 1),
            mapAbsoluteEpisodeToSeasonEpisode(86, multiSeasonShow),
        )
    }

    @Test
    fun `maps absolute progress to watched episodes across all prior seasons without specials`() {
        val watched = mapAbsoluteProgressToWatchedEpisodes(86, multiSeasonShow)
        assertEquals(86, watched.size)
        assertTrue(watched.none { it.season == 0 })
        assertEquals(SeasonEpisodeRef(season = 1, episode = 1), watched.first())
        assertEquals(SeasonEpisodeRef(season = 3, episode = 1), watched.last())
    }

    @Test
    fun `reverse maps season and episode back to absolute episode number`() {
        assertEquals(85, mapSeasonEpisodeToAbsolute(2, 43, multiSeasonShow))
        assertEquals(86, mapSeasonEpisodeToAbsolute(3, 1, multiSeasonShow))
    }

    @Test
    fun `clamps safely when progress exceeds known addon episodes`() {
        val mapped = mapAbsoluteEpisodeToSeasonEpisode(999, multiSeasonShow)
        assertEquals(SeasonEpisodeRef(season = 3, episode = 40), mapped)

        val watched = mapAbsoluteProgressToWatchedEpisodes(999, multiSeasonShow)
        assertEquals(42 + 43 + 40, watched.size)
    }

    @Test
    fun `falls back to season 1 when addon episodes list is empty`() {
        assertEquals(
            SeasonEpisodeRef(season = 1, episode = 850),
            mapAbsoluteEpisodeToSeasonEpisode(850, emptyList()),
        )
        assertEquals(850, mapSeasonEpisodeToAbsolute(1, 850, emptyList()))
    }

    @Test
    fun `maps separate Season 2 AniList entry to Season 2 and marks Season 1 fully watched`() {
        val blackCloverEpisodes = buildList {
            for (ep in 1..170) add(SeasonEpisodeRef(season = 1, episode = ep))
            for (ep in 1..12) add(SeasonEpisodeRef(season = 2, episode = ep))
        }
        val season1Entry = AniListMediaEntry(
            mediaId = 97940,
            titleRomaji = "Black Clover",
            titleEnglish = "Black Clover",
            format = "TV",
            episodes = 170,
            userStatus = AniListListStatus.COMPLETED,
            progress = 170,
        )
        val season2Entry = AniListMediaEntry(
            mediaId = 180001,
            titleRomaji = "Black Clover Season 2",
            titleEnglish = "Black Clover Season 2",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 5,
            prequelIds = listOf(97940),
        )
        val allEntries = listOf(season1Entry, season2Entry)

        val currentRef = mapEntryProgressToSeasonEpisode(season2Entry, allEntries, blackCloverEpisodes)
        assertEquals(SeasonEpisodeRef(season = 2, episode = 5), currentRef)

        val watched = mapEntryProgressToWatchedEpisodes(season2Entry, allEntries, blackCloverEpisodes)
        assertEquals(175, watched.size)
        assertTrue(watched.contains(SeasonEpisodeRef(season = 1, episode = 170)))
        assertTrue(watched.contains(SeasonEpisodeRef(season = 2, episode = 5)))
        assertTrue(!watched.contains(SeasonEpisodeRef(season = 2, episode = 6)))

        assertEquals(6, mapSeasonEpisodeToEntryProgress(2, 6, season2Entry, allEntries, blackCloverEpisodes))
    }

    @Test
    fun `maps titled sequel without explicit season number using prequelIds offset`() {
        val showEpisodes = buildList {
            for (ep in 1..24) add(SeasonEpisodeRef(season = 1, episode = ep))
            for (ep in 1..12) add(SeasonEpisodeRef(season = 2, episode = ep))
        }
        val s1 = AniListMediaEntry(
            mediaId = 1001,
            titleRomaji = "Example Show",
            format = "TV",
            episodes = 24,
            userStatus = AniListListStatus.COMPLETED,
            progress = 24,
        )
        val s2 = AniListMediaEntry(
            mediaId = 1002,
            titleRomaji = "Example Show: Arc II",
            format = "TV",
            episodes = 12,
            userStatus = AniListListStatus.CURRENT,
            progress = 5,
            prequelIds = listOf(1001),
        )
        val allEntries = listOf(s1, s2)

        assertEquals(
            SeasonEpisodeRef(season = 2, episode = 5),
            mapEntryProgressToSeasonEpisode(s2, allEntries, showEpisodes),
        )
        assertEquals(29, mapEntryProgressToWatchedEpisodes(s2, allEntries, showEpisodes).size)
        assertEquals(6, mapSeasonEpisodeToEntryProgress(2, 6, s2, allEntries, showEpisodes))
    }
}

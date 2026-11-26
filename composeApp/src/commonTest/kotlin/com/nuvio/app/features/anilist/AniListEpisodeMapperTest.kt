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
}

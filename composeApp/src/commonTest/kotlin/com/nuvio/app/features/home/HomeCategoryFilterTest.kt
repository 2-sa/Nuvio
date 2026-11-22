package com.nuvio.app.features.home

import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCategoryFilterTest {

    @Test
    fun `isAnimeSignal detects anime type and identifier tokens case-insensitively`() {
        assertTrue(isAnimeSignal(type = "anime"))
        assertTrue(isAnimeSignal(type = "ANIME"))
        assertTrue(isAnimeSignal(type = "series", catalogId = "kitsu-trending"))
        assertTrue(isAnimeSignal(type = "series", catalogId = "MAL_top"))
        assertTrue(isAnimeSignal(type = "movie", catalogName = "AniList Top"))
        assertTrue(isAnimeSignal(type = "series", catalogName = "Top Anime"))
        assertTrue(isAnimeSignal(type = "series", itemId = "mal:12345"))
        assertTrue(isAnimeSignal(type = "series", itemId = "anidb-99"))
        assertFalse(isAnimeSignal(type = "movie", catalogId = "top-movies", catalogName = "Popular Movies", itemId = "tt123456"))
    }

    @Test
    fun `isAnimeSignal detects english and arabic anime and animation genres`() {
        assertTrue(isAnimeSignal(type = "series", genres = listOf("Action", "Anime")))
        assertTrue(isAnimeSignal(type = "movie", genres = listOf("Animation", "Adventure")))
        assertTrue(isAnimeSignal(type = "series", genres = listOf("أكشن", "أنمي")))
        assertTrue(isAnimeSignal(type = "movie", genres = listOf("رسوم متحركة")))
        assertFalse(isAnimeSignal(type = "movie", genres = listOf("Drama", "Thriller", "دراما")))
    }

    @Test
    fun `filterHomeCatalogSections handles empty list for all filters`() {
        HomeCategoryFilter.entries.forEach { filter ->
            assertEquals(emptyList(), filterHomeCatalogSections(emptyList(), filter))
        }
    }

    @Test
    fun `filterHomeCatalogSections ALL returns all sections unchanged`() {
        val sections = listOf(
            catalogSection(key = "m1", title = "Movies", contentType = "movie", catalogId = "top-movies"),
            catalogSection(key = "s1", title = "Series", contentType = "series", catalogId = "top-series"),
            catalogSection(key = "a1", title = "Kitsu", contentType = "anime", catalogId = "kitsu-airing"),
        )

        val result = filterHomeCatalogSections(sections, HomeCategoryFilter.ALL)

        assertEquals(sections, result)
    }

    @Test
    fun `filterHomeCatalogSections MOVIES keeps movie catalogs and excludes anime-tagged movie catalogs`() {
        val movieSection = catalogSection(key = "m1", title = "Popular Movies", contentType = "movie", catalogId = "tmdb.top")
        val animeMovieByTitle = catalogSection(key = "m2", title = "Anime Movies", contentType = "movie", catalogId = "top-anime-films")
        val animeMovieById = catalogSection(key = "m3", title = "Featured Films", contentType = "movie", catalogId = "anilist_movies")
        val seriesSection = catalogSection(key = "s1", title = "Popular Series", contentType = "series", catalogId = "tmdb.series")

        val result = filterHomeCatalogSections(
            sections = listOf(movieSection, animeMovieByTitle, animeMovieById, seriesSection),
            filter = HomeCategoryFilter.MOVIES,
        )

        assertEquals(listOf(movieSection), result)
    }

    @Test
    fun `filterHomeCatalogSections SERIES keeps series and tv catalogs while excluding anime-tagged catalogs`() {
        val seriesSection = catalogSection(key = "s1", title = "Trending Shows", contentType = "series", catalogId = "tmdb.series")
        val tvSection = catalogSection(key = "s2", title = "Live TV", contentType = "tv", catalogId = "tv.popular")
        val kitsuSeries = catalogSection(key = "a1", title = "Top Shows", contentType = "series", catalogId = "kitsu-top")
        val malSeries = catalogSection(key = "a2", title = "Seasonal", contentType = "series", catalogId = "mal_season")
        val movieSection = catalogSection(key = "m1", title = "Popular Movies", contentType = "movie", catalogId = "tmdb.top")

        val result = filterHomeCatalogSections(
            sections = listOf(seriesSection, tvSection, kitsuSeries, malSeries, movieSection),
            filter = HomeCategoryFilter.SERIES,
        )

        assertEquals(listOf(seriesSection, tvSection), result)
    }

    @Test
    fun `filterHomeCatalogSections ANIME keeps catalog-level anime and filters mixed catalogs omitting empty results`() {
        val kitsuCatalog = catalogSection(
            key = "a1",
            title = "Kitsu Top",
            contentType = "series",
            catalogId = "kitsu-trending",
            items = listOf(metaItem(id = "kitsu:1", type = "series", name = "Frieren")),
        )
        val animeItem = metaItem(id = "tt999", type = "series", name = "Attack on Titan", genres = listOf("Action", "Anime"))
        val arabicAnimeMovie = metaItem(id = "tt888", type = "movie", name = "Spirited Away", genres = listOf("رسوم متحركة"))
        val liveActionItem = metaItem(id = "tt111", type = "movie", name = "Inception", genres = listOf("Sci-Fi"))
        val mixedSection = catalogSection(
            key = "mix",
            title = "Trending All",
            contentType = "movie",
            catalogId = "trending",
            items = listOf(liveActionItem, animeItem, arabicAnimeMovie),
        )
        val nonAnimeOnlySection = catalogSection(
            key = "drama",
            title = "Dramas",
            contentType = "movie",
            catalogId = "drama-movies",
            items = listOf(liveActionItem),
        )

        val result = filterHomeCatalogSections(
            sections = listOf(kitsuCatalog, mixedSection, nonAnimeOnlySection),
            filter = HomeCategoryFilter.ANIME,
        )

        assertEquals(2, result.size)
        assertEquals("a1", result[0].key)
        assertEquals(kitsuCatalog.items, result[0].items)
        assertEquals("mix", result[1].key)
        assertEquals(listOf(animeItem, arabicAnimeMovie), result[1].items)
    }

    @Test
    fun `filterContinueWatchingItems filters across ALL, MOVIES, SERIES, and ANIME`() {
        val movieItem = cwItem(id = "tt100", type = "movie", title = "Dune")
        val seriesItem = cwItem(id = "tt200", type = "series", title = "Severance")
        val tvItem = cwItem(id = "tt201", type = "tv", title = "News Night")
        val animeTypeItem = cwItem(id = "tt300", type = "anime", title = "One Piece")
        val kitsuItem = cwItem(id = "kitsu:456", type = "series", title = "Jujutsu Kaisen")
        val malItem = cwItem(id = "mal:789", type = "movie", title = "Your Name")
        val anilistItem = cwItem(id = "anilist:12", type = "series", title = "Solo Leveling")

        val allItems = listOf(movieItem, seriesItem, tvItem, animeTypeItem, kitsuItem, malItem, anilistItem)

        assertEquals(emptyList(), filterContinueWatchingItems(emptyList(), HomeCategoryFilter.MOVIES))
        assertEquals(allItems, filterContinueWatchingItems(allItems, HomeCategoryFilter.ALL))
        assertEquals(listOf(movieItem), filterContinueWatchingItems(allItems, HomeCategoryFilter.MOVIES))
        assertEquals(listOf(seriesItem, tvItem), filterContinueWatchingItems(allItems, HomeCategoryFilter.SERIES))
        assertEquals(
            listOf(animeTypeItem, kitsuItem, malItem, anilistItem),
            filterContinueWatchingItems(allItems, HomeCategoryFilter.ANIME),
        )
    }

    @Test
    fun `isMovieType and isSeriesType normalize case whitespace plural and Arabic variants`() {
        assertTrue(isMovieType("movie"))
        assertTrue(isMovieType(" Movies "))
        assertTrue(isMovieType("Films"))
        assertTrue(isMovieType("أفلام"))
        assertTrue(isMovieType("فيلم"))
        assertFalse(isMovieType("series"))
        assertFalse(isMovieType("Shows"))

        assertTrue(isSeriesType("series"))
        assertTrue(isSeriesType(" Shows "))
        assertTrue(isSeriesType("TV"))
        assertTrue(isSeriesType("tv-series"))
        assertTrue(isSeriesType("TV Series"))
        assertTrue(isSeriesType("مسلسلات"))
        assertFalse(isSeriesType("Movies"))
        assertFalse(isSeriesType("film"))
    }

    @Test
    fun `filterHomeCatalogSections MOVIES matches plural and film contentType catalogs and excludes series`() {
        val lumiereMovies = catalogSection(
            key = "lumiere_movies",
            title = "LumiereDB Popular - Movies",
            contentType = "Movies",
            catalogId = "lumiere.popular",
        )
        val lowerMovies = catalogSection(
            key = "lower_movies",
            title = "Trending",
            contentType = "movies",
            catalogId = "aio.movies",
        )
        val filmsSection = catalogSection(
            key = "films",
            title = "Cinema",
            contentType = "Films",
            catalogId = "cinema.films",
        )
        val showsSection = catalogSection(
            key = "shows",
            title = "Popular Shows",
            contentType = "Shows",
            catalogId = "aio.shows",
        )

        val sections = listOf(lumiereMovies, lowerMovies, filmsSection, showsSection)

        assertEquals(
            listOf(lumiereMovies, lowerMovies, filmsSection),
            filterHomeCatalogSections(sections, HomeCategoryFilter.MOVIES),
        )
        assertEquals(
            listOf(showsSection),
            filterHomeCatalogSections(sections, HomeCategoryFilter.SERIES),
        )
    }

    @Test
    fun `filterHomeCatalogSections SERIES matches Shows and TV contentType catalogs and excludes MOVIES`() {
        val showsSection = catalogSection(
            key = "s_shows",
            title = "Top Shows",
            contentType = "Shows",
            catalogId = "aio.shows",
        )
        val tvSection = catalogSection(
            key = "s_tv",
            title = "Prime TV",
            contentType = "TV",
            catalogId = "aio.tv",
        )
        val moviesSection = catalogSection(
            key = "m_movies",
            title = "Top Movies",
            contentType = "Movies",
            catalogId = "aio.movies",
        )

        val sections = listOf(showsSection, tvSection, moviesSection)

        assertEquals(
            listOf(showsSection, tvSection),
            filterHomeCatalogSections(sections, HomeCategoryFilter.SERIES),
        )
        assertEquals(
            listOf(moviesSection),
            filterHomeCatalogSections(sections, HomeCategoryFilter.MOVIES),
        )
    }

    @Test
    fun `filterHomeCatalogSections filters mixed and generic catalogs at item level and respects clear titles`() {
        val movieA = metaItem(id = "tmdb:550", type = "Movies", name = "Fight Club")
        val movieB = metaItem(id = "tt0111161", type = "movie", name = "Shawshank")
        val seriesA = metaItem(id = "tt0903747", type = "series", name = "Breaking Bad")
        val showB = metaItem(id = "tmdb:1399", type = "Shows", name = "Game of Thrones")
        val animeMovie = metaItem(id = "tt888", type = "Movies", name = "Spirited Away", genres = listOf("Animation"))

        val mixedOther = catalogSection(
            key = "mix_other",
            title = "Trending Now",
            contentType = "other",
            catalogId = "trending.other",
            items = listOf(movieA, seriesA, animeMovie, showB),
        )
        val mixedAllTitle = catalogSection(
            key = "mix_all",
            title = "Movies & Series",
            contentType = "all",
            catalogId = "trending.all",
            items = listOf(movieB, seriesA),
        )
        val clearTitleMovies = catalogSection(
            key = "clear_movies",
            title = "أفلام الأسبوع",
            contentType = "catalog",
            catalogId = "custom.week",
            items = listOf(metaItem(id = "tt10", type = "unknown", name = "Custom Item")),
        )

        val moviesResult = filterHomeCatalogSections(
            sections = listOf(mixedOther, mixedAllTitle, clearTitleMovies),
            filter = HomeCategoryFilter.MOVIES,
        )
        assertEquals(3, moviesResult.size)
        assertEquals("mix_other", moviesResult[0].key)
        assertEquals(listOf(movieA), moviesResult[0].items)
        assertEquals("mix_all", moviesResult[1].key)
        assertEquals(listOf(movieB), moviesResult[1].items)
        assertEquals("clear_movies", moviesResult[2].key)
        assertEquals(clearTitleMovies.items, moviesResult[2].items)

        val seriesResult = filterHomeCatalogSections(
            sections = listOf(mixedOther, mixedAllTitle, clearTitleMovies),
            filter = HomeCategoryFilter.SERIES,
        )
        assertEquals(2, seriesResult.size)
        assertEquals("mix_other", seriesResult[0].key)
        assertEquals(listOf(seriesA, showB), seriesResult[0].items)
        assertEquals("mix_all", seriesResult[1].key)
        assertEquals(listOf(seriesA), seriesResult[1].items)
    }

    @Test
    fun `filterHomeCatalogSections ANIME still finds anime items inside plural Movies and Series catalogs`() {
        val animeMovie = metaItem(id = "tt888", type = "Movies", name = "Your Name", genres = listOf("Anime"))
        val normalMovie = metaItem(id = "tt550", type = "Movies", name = "Fight Club", genres = listOf("Drama"))
        val moviesCatalog = catalogSection(
            key = "plural_movies",
            title = "Popular",
            contentType = "Movies",
            catalogId = "popular.movies",
            items = listOf(normalMovie, animeMovie),
        )

        val result = filterHomeCatalogSections(listOf(moviesCatalog), HomeCategoryFilter.ANIME)
        assertEquals(1, result.size)
        assertEquals(listOf(animeMovie), result[0].items)
    }

    @Test
    fun `filterContinueWatchingItems supports plural Movies and shows parentMetaType`() {
        val pluralMovie = cwItem(id = "tmdb:550", type = "Movies", title = "Fight Club")
        val pluralShows = cwItem(id = "tmdb:1396", type = "shows", title = "Breaking Bad")
        val animePlural = cwItem(id = "kitsu:456", type = "Movies", title = "Akira")

        val items = listOf(pluralMovie, pluralShows, animePlural)

        assertEquals(listOf(pluralMovie), filterContinueWatchingItems(items, HomeCategoryFilter.MOVIES))
        assertEquals(listOf(pluralShows), filterContinueWatchingItems(items, HomeCategoryFilter.SERIES))
        assertEquals(listOf(animePlural), filterContinueWatchingItems(items, HomeCategoryFilter.ANIME))
    }

    private fun catalogSection(
        key: String,
        title: String,
        contentType: String,
        catalogId: String,
        items: List<MetaPreview> = listOf(metaItem(id = "tt1", type = contentType, name = "Sample")),
    ) = HomeCatalogSection(
        key = key,
        title = title,
        subtitle = "Test Addon",
        addonName = "Test Addon",
        target = CatalogTarget.Addon(
            manifestUrl = "https://example.com/manifest.json",
            contentType = contentType,
            catalogId = catalogId,
        ),
        items = items,
    )

    private fun metaItem(
        id: String,
        type: String,
        name: String,
        genres: List<String> = emptyList(),
    ) = MetaPreview(
        id = id,
        type = type,
        name = name,
        genres = genres,
    )

    private fun cwItem(
        id: String,
        type: String,
        title: String,
    ) = ContinueWatchingItem(
        parentMetaId = id,
        parentMetaType = type,
        videoId = "$id:1:1",
        title = title,
        subtitle = "S1 E1",
        imageUrl = null,
        resumePositionMs = 100_000L,
        durationMs = 1_000_000L,
        progressFraction = 0.1f,
    )
}

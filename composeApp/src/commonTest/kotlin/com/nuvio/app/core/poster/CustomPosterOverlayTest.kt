package com.nuvio.app.core.poster

import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomPosterOverlayTest {

    private val rpdbPattern = "https://api.ratingposterdb.com/key/imdb/poster-default/{imdb_id}.jpg"
    private val universalPattern = "https://example.com/{id_type}/poster-default/{typed_id}.jpg"
    private val shapePattern = "https://example.com/{id_type}/{shape}/{typed_id}.jpg"

    // -- MetaPreview overlay --

    @Test
    fun withCustomPosterUrl_replaces_poster_and_preserves_rawPosterUrl() {
        val item = MetaPreview(
            id = "tt0137523",
            type = "movie",
            name = "Fight Club",
            poster = "https://original.com/poster.jpg",
        )
        val result = item.withCustomPosterUrl(rpdbPattern)
        assertEquals("https://api.ratingposterdb.com/key/imdb/poster-default/tt0137523.jpg", result.poster)
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrl_blank_pattern_returns_unchanged() {
        val item = MetaPreview(id = "tt0137523", type = "movie", name = "Test", poster = "https://original.com/poster.jpg")
        val result = item.withCustomPosterUrl("")
        assertEquals("https://original.com/poster.jpg", result.poster)
        assertNull(result.rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrl_unresolvable_id_returns_unchanged() {
        val item = MetaPreview(id = "kitsu:7442", type = "series", name = "Anime", poster = "https://original.com/poster.jpg")
        val result = item.withCustomPosterUrl(rpdbPattern)
        assertEquals("https://original.com/poster.jpg", result.poster)
        assertNull(result.rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrl_shape_pattern_sets_landscapePoster() {
        val item = MetaPreview(id = "tt0137523", type = "movie", name = "Test", poster = "https://original.com/poster.jpg")
        val result = item.withCustomPosterUrl(shapePattern)
        assertEquals("https://example.com/imdb/poster/tt0137523.jpg", result.poster)
        assertEquals("https://example.com/imdb/landscape/tt0137523.jpg", result.landscapePoster)
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrl_non_poster_shape_without_shape_placeholder_returns_unchanged() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://original.com/poster.jpg",
            posterShape = PosterShape.Landscape,
        )
        val result = item.withCustomPosterUrl(rpdbPattern)
        assertEquals("https://original.com/poster.jpg", result.poster)
    }

    @Test
    fun withCustomPosterUrl_non_poster_shape_with_shape_placeholder_resolves() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://original.com/poster.jpg",
            posterShape = PosterShape.Landscape,
        )
        val result = item.withCustomPosterUrl(shapePattern)
        assertEquals("https://example.com/imdb/landscape/tt0137523.jpg", result.poster)
    }

    // -- reapplyCustomPosterUrl --

    @Test
    fun reapplyCustomPosterUrl_restores_original_then_applies_new_pattern() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://old-service.com/custom.jpg",
            rawPosterUrl = "https://original.com/poster.jpg",
        )
        val result = item.reapplyCustomPosterUrl(universalPattern)
        assertEquals("https://example.com/imdb/poster-default/tt0137523.jpg", result.poster)
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun reapplyCustomPosterUrl_blank_pattern_restores_original() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://custom-service.com/custom.jpg",
            rawPosterUrl = "https://original.com/poster.jpg",
        )
        val result = item.reapplyCustomPosterUrl("")
        assertEquals("https://original.com/poster.jpg", result.poster)
    }

    @Test
    fun reapplyCustomPosterUrl_no_rawPosterUrl_applies_normally() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://original.com/poster.jpg",
        )
        val result = item.reapplyCustomPosterUrl(universalPattern)
        assertEquals("https://example.com/imdb/poster-default/tt0137523.jpg", result.poster)
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun reapplyCustomPosterUrl_keeps_addon_landscapePoster_without_shape_placeholder() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://original.com/poster.jpg",
            landscapePoster = "https://addon.com/landscape.jpg",
        )
        val result = item.withCustomPosterUrl(rpdbPattern).reapplyCustomPosterUrl(rpdbPattern)
        assertEquals("https://addon.com/landscape.jpg", result.landscapePoster)
    }

    @Test
    fun reapplyCustomPosterUrl_blank_pattern_restores_addon_landscapePoster() {
        val item = MetaPreview(
            id = "tt0137523", type = "movie", name = "Test",
            poster = "https://original.com/poster.jpg",
            landscapePoster = "https://addon.com/landscape.jpg",
        )
        val overlaid = item.withCustomPosterUrl(shapePattern)
        assertEquals("https://example.com/imdb/landscape/tt0137523.jpg", overlaid.landscapePoster)

        val result = overlaid.reapplyCustomPosterUrl("")
        assertEquals("https://original.com/poster.jpg", result.poster)
        assertEquals("https://addon.com/landscape.jpg", result.landscapePoster)
    }

    // -- List overlay --

    @Test
    fun withCustomPosterUrls_applies_to_all_items() {
        val items = listOf(
            MetaPreview(id = "tt0137523", type = "movie", name = "Fight Club", poster = "https://a.com/1.jpg"),
            MetaPreview(id = "tt0903747", type = "series", name = "Breaking Bad", poster = "https://a.com/2.jpg"),
        )
        val result = items.withCustomPosterUrls(rpdbPattern)
        assertEquals("https://api.ratingposterdb.com/key/imdb/poster-default/tt0137523.jpg", result[0].poster)
        assertEquals("https://api.ratingposterdb.com/key/imdb/poster-default/tt0903747.jpg", result[1].poster)
        assertEquals("https://a.com/1.jpg", result[0].rawPosterUrl)
        assertEquals("https://a.com/2.jpg", result[1].rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrls_blank_pattern_returns_same_list() {
        val items = listOf(
            MetaPreview(id = "tt0137523", type = "movie", name = "Test", poster = "https://a.com/1.jpg"),
        )
        val result = items.withCustomPosterUrls("")
        assertEquals(items, result)
    }

    // -- Content type normalization --

    @Test
    fun normalizeCustomPosterContentType_maps_plural_and_variant_types_to_canonical_values() {
        assertEquals("movie", normalizeCustomPosterContentType("Movies"))
        assertEquals("movie", normalizeCustomPosterContentType(" films "))
        assertEquals("series", normalizeCustomPosterContentType("series"))
        assertEquals("series", normalizeCustomPosterContentType("tv"))
        assertEquals("series", normalizeCustomPosterContentType("Shows"))
        assertEquals("series", normalizeCustomPosterContentType("TV-Series"))
        assertEquals("series", normalizeCustomPosterContentType("anime"))
        assertEquals("custom", normalizeCustomPosterContentType("custom", fallback = "custom"))
    }

    @Test
    fun metaPreview_withCustomPosterUrl_plural_Movies_tmdb_resolves_movie_typed_id() {
        val item = MetaPreview(
            id = "tmdb:550",
            type = "Movies",
            name = "Fight Club",
            poster = "https://original.com/poster.jpg",
        )
        val result = item.withCustomPosterUrl(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/{typed_id}.jpg",
        )
        assertEquals(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/movie-550.jpg",
            result.poster,
        )
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun metaPreview_withCustomPosterUrl_plural_Movies_imdb_resolves_movie_type_placeholder() {
        val item = MetaPreview(
            id = "tt0137523",
            type = "Movies",
            name = "Fight Club",
            poster = "https://original.com/poster.jpg",
        )
        val result = item.withCustomPosterUrl("https://example.com/{type}/{imdb_id}.jpg")
        assertEquals("https://example.com/movie/tt0137523.jpg", result.poster)
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun continueWatchingItem_withCustomPosterUrl_plural_Movies_tmdb_resolves_movie_typed_id() {
        val item = ContinueWatchingItem(
            parentMetaId = "tmdb:550",
            parentMetaType = "Movies",
            videoId = "tmdb:550",
            title = "Fight Club",
            subtitle = "Movie",
            imageUrl = "https://original.com/poster.jpg",
            poster = "https://original.com/poster.jpg",
            resumePositionMs = 10_000L,
            durationMs = 100_000L,
            progressFraction = 0.1f,
        )
        val result = item.withCustomPosterUrl(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/{typed_id}.jpg",
        )
        assertEquals(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/movie-550.jpg",
            result.poster,
        )
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun libraryItem_withCustomPosterUrl_plural_Movies_tmdb_resolves_movie_typed_id() {
        val item = LibraryItem(
            id = "tmdb:550",
            type = "Movies",
            name = "Fight Club",
            poster = "https://original.com/poster.jpg",
            savedAtEpochMs = 1_000L,
        )
        val result = item.withCustomPosterUrl(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/{typed_id}.jpg",
        )
        assertEquals(
            "https://api.ratingposterdb.com/key/tmdb/poster-default/movie-550.jpg",
            result.poster,
        )
        assertEquals("https://original.com/poster.jpg", result.rawPosterUrl)
    }

    @Test
    fun withCustomPosterUrl_series_tv_and_Shows_variants_resolve_as_series() {
        val pattern = "https://example.com/{type}/{typed_id}.jpg"
        listOf("series", "tv", "Shows").forEach { variant ->
            val preview = MetaPreview(
                id = "tmdb:1396",
                type = variant,
                name = "Breaking Bad",
                poster = "https://original.com/poster.jpg",
            )
            val result = preview.withCustomPosterUrl(pattern)
            assertEquals("https://example.com/series/series-1396.jpg", result.poster)
        }
    }
}

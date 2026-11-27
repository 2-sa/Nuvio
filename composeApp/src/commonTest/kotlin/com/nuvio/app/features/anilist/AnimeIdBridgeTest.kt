package com.nuvio.app.features.anilist

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimeIdBridgeTest {
    @AfterTest
    fun tearDown() {
        AnimeIdBridge.resetForTests()
    }

    @Test
    fun `parseFribbAnimeListJson parses valid entries normalizes imdb and skips malformed or stringified anomalies safely`() {
        val raw = """
            [
              {"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "anidb_id": 2400, "imdb_id": " TT0434665 ", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
              {"anilist_id": "395", "mal_id": "395", "kitsu_id": 352, "imdb_id": "tt0434665", "themoviedb_id": "45782", "thetvdb_id": "78957", "type": "TV"},
              {"anilist_id": -1, "mal_id": 0, "imdb_id": "invalid", "type": "TV"},
              {"anilist_id": 235, "mal_id": 235, "kitsu_id": 210, "imdb_id": "tt0131179", "themoviedb_id": 30983, "thetvdb_id": 72454, "type": "TV"}
            ]
        """.trimIndent()
        val index = parseFribbAnimeListJson(raw, loadedAtEpochMs = 1000L)
        assertTrue(index.isLoaded)
        assertEquals("tt0434665", index.byAniListId[384]?.imdbId)
        assertEquals(395, index.byMalId[395]?.anilistId)
        assertEquals(45782L, index.byAniListId[395]?.tmdbId)
        assertEquals("78957", index.byAniListId[395]?.tvdbId)
        assertEquals(2, index.byImdbId["tt0434665"]?.size)
        assertNull(index.byAniListId[-1])
        assertNull(index.byImdbId["invalid"])
    }

    @Test
    fun `parseFribbAnimeListJson handles truncated corrupt json and non array payloads without throwing`() {
        assertFalse(parseFribbAnimeListJson("").isLoaded)
        assertFalse(parseFribbAnimeListJson("{not valid json").isLoaded)
        assertFalse(parseFribbAnimeListJson("""[{"anilist_id": 384, "mal_id": 384""").isLoaded)
        assertFalse(parseFribbAnimeListJson("""{"anilist_id": 384}""").isLoaded)
        assertFalse(parseFribbAnimeListJson("""[123, "bad", null, {"anilist_id": 0, "mal_id": -5}]""").isLoaded)
    }

    @Test
    fun `canonicalContentId follows mal then kitsu then imdb then anilist fallback order`() {
        assertEquals(
            "mal:384",
            AnimeIdMapping(anilistId = 384, malId = 384, kitsuId = 341, imdbId = "tt0434665").canonicalContentId(),
        )
        assertEquals(
            "kitsu:341",
            AnimeIdMapping(anilistId = 384, malId = null, kitsuId = 341, imdbId = "tt0434665").canonicalContentId(),
        )
        assertEquals(
            "tt0434665",
            AnimeIdMapping(anilistId = 384, malId = null, kitsuId = null, imdbId = "tt0434665").canonicalContentId(),
        )
        assertEquals("anilist:384", AnimeIdMapping(anilistId = 384).canonicalContentId())
        assertNull(AnimeIdMapping().canonicalContentId())
    }

    @Test
    fun `resolveForEntry synthesizes instant idMal fallback for brand new shows missing from Fribb`() {
        val brandNew = AniListMediaEntry(
            mediaId = 999999,
            malId = 88888,
            titleRomaji = "Brand New Anime",
            format = "TV",
        )
        val mapping = AnimeIdBridge.resolveForEntry(brandNew, AnimeIdBridgeIndex())
        assertEquals(999999, mapping.anilistId)
        assertEquals(88888, mapping.malId)
        assertEquals("mal:88888", mapping.canonicalContentId())
    }

    @Test
    fun `canMarkFranchiseIdFullyWatched allows single entry shows and requires all serial entries completed for multi season franchises`() {
        val conanMappings = listOf(AnimeIdMapping(anilistId = 235, malId = 235, imdbId = "tt0131179", type = "TV"))
        assertTrue(
            canMarkFranchiseIdFullyWatched(
                conanMappings,
                mapOf(
                    235 to AniListMediaEntry(
                        mediaId = 235,
                        titleRomaji = "Detective Conan",
                        userStatus = AniListListStatus.COMPLETED,
                    ),
                ),
            ),
        )

        val aotMappings = listOf(
            AnimeIdMapping(anilistId = 16498, malId = 16498, imdbId = "tt2560140", type = "TV"),
            AnimeIdMapping(anilistId = 20958, malId = 25777, imdbId = "tt2560140", type = "TV"),
            AnimeIdMapping(anilistId = 99999, malId = 99999, imdbId = "tt2560140", type = "OVA"),
        )
        val partialEntries = mapOf(
            16498 to AniListMediaEntry(
                mediaId = 16498,
                titleRomaji = "AoT S1",
                userStatus = AniListListStatus.COMPLETED,
            ),
            20958 to AniListMediaEntry(
                mediaId = 20958,
                titleRomaji = "AoT S2",
                userStatus = AniListListStatus.CURRENT,
                progress = 3,
            ),
        )
        assertFalse(canMarkFranchiseIdFullyWatched(aotMappings, partialEntries))

        val fullEntries = partialEntries + (
            20958 to AniListMediaEntry(
                mediaId = 20958,
                titleRomaji = "AoT S2",
                userStatus = AniListListStatus.COMPLETED,
            )
            )
        assertTrue(canMarkFranchiseIdFullyWatched(aotMappings, fullEntries))
    }

    @Test
    fun `addonFallbackIds and addonFallbackVideoIds produce deduplicated cross addon chains`() {
        val index = parseFribbAnimeListJson(
            """[{"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"}]""",
        )
        assertEquals(
            listOf("mal:384", "kitsu:341", "tt0434665", "tmdb:45782", "tvdb:78957"),
            AnimeIdBridge.addonFallbackIds("mal:384", index),
        )
        val videoFallbacks = AnimeIdBridge.addonFallbackVideoIds(
            type = "series",
            videoId = "mal:384:1:5",
            season = 1,
            episode = 5,
            index = index,
        )
        assertEquals("mal:384:1:5", videoFallbacks.first())
        assertTrue("kitsu:341:5" in videoFallbacks)
        assertTrue("tt0434665:1:5" in videoFallbacks)
        assertTrue("tmdb:45782:1:5" in videoFallbacks)
        assertTrue("tvdb:78957:1:5" in videoFallbacks)
        assertEquals(videoFallbacks.distinct(), videoFallbacks)

        val movieFallbacks = AnimeIdBridge.addonFallbackVideoIds(
            type = "movie",
            videoId = "mal:384",
            index = index,
        )
        assertEquals(listOf("mal:384", "kitsu:341", "tt0434665", "tmdb:45782", "tvdb:78957"), movieFallbacks)
    }

    @Test
    fun `isKnownAnimeContentId matches anime prefixes and Fribb imdb tmdb tvdb ids while rejecting live action ids`() {
        val index = parseFribbAnimeListJson(
            """[{"anilist_id": 384, "mal_id": 384, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"}]""",
        )
        assertTrue(AnimeIdBridge.isKnownAnimeContentId("tt0434665", index))
        assertTrue(AnimeIdBridge.isKnownAnimeContentId("tmdb:45782", index))
        assertTrue(AnimeIdBridge.isKnownAnimeContentId("tvdb:78957", index))
        assertTrue(AnimeIdBridge.isKnownAnimeContentId("kitsu:999", index))
        assertFalse(AnimeIdBridge.isKnownAnimeContentId("tt0903747", index)) // Breaking Bad
    }

    @Test
    fun `resolveByContentId and mappingsForFranchiseId resolve across all supported prefixes`() = runTest {
        val index = parseFribbAnimeListJson(
            """
            [
              {"anilist_id": 384, "mal_id": 384, "kitsu_id": 341, "anidb_id": 2400, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"},
              {"anilist_id": 395, "mal_id": 395, "kitsu_id": 352, "imdb_id": "tt0434665", "themoviedb_id": 45782, "thetvdb_id": 78957, "type": "TV"}
            ]
            """.trimIndent(),
            loadedAtEpochMs = 1000L,
        )
        AnimeIdBridge.setIndexForTests(index)

        assertEquals(index, AnimeIdBridge.awaitReady(timeoutMs = 500L))
        assertEquals(384, AnimeIdBridge.resolveByContentId("anilist:384")?.anilistId)
        assertEquals(384, AnimeIdBridge.resolveByContentId("mal:384")?.anilistId)
        assertEquals(341, AnimeIdBridge.resolveByContentId("kitsu:341")?.kitsuId)
        assertEquals(2400, AnimeIdBridge.resolveByContentId("anidb:2400")?.anidbId)
        assertNotNull(AnimeIdBridge.resolveByContentId("TT0434665"))
        assertEquals(2, AnimeIdBridge.mappingsForFranchiseId("tt0434665").size)
        assertEquals(2, AnimeIdBridge.mappingsForFranchiseId("tmdb:45782").size)
        assertEquals(2, AnimeIdBridge.mappingsForFranchiseId("tvdb:78957").size)
        assertTrue(AnimeIdBridge.mappingsForFranchiseId("tt9999999").isEmpty())
    }

    @Test
    fun `fetchFribbAnimeList falls back to secondary cdn and honors 304 Not Modified`() = runTest {
        val primaryUrl = "https://primary.test/anime-list-full.json"
        val fallbackUrl = "https://fallback.test/anime-list-full.json"
        val validBody = """[{"anilist_id": 384, "mal_id": 384, "imdb_id": "tt0434665", "type": "TV"}]"""

        var capturedIfNoneMatch: String? = null
        val fallbackEngine = MockEngine { request ->
            when (request.url.toString()) {
                primaryUrl -> respond("server error", status = HttpStatusCode.InternalServerError)
                fallbackUrl -> respond(
                    content = validBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ETag, "\"v2\""),
                )
                else -> respond("", status = HttpStatusCode.NotFound)
            }
        }
        val fallbackClient = HttpClient(fallbackEngine)
        try {
            val updated = fetchFribbAnimeList(
                httpClient = fallbackClient,
                urls = listOf(primaryUrl, fallbackUrl),
                nowEpochMs = 5_000L,
            )
            val result = assertIs<FribbFetchResult.Updated>(updated)
            assertEquals("\"v2\"", result.etag)
            assertEquals("tt0434665", result.index.byAniListId[384]?.imdbId)
        } finally {
            fallbackClient.close()
        }

        val notModifiedEngine = MockEngine { request ->
            capturedIfNoneMatch = request.headers[HttpHeaders.IfNoneMatch]
            respond(
                content = "",
                status = HttpStatusCode.NotModified,
                headers = headersOf(HttpHeaders.ETag, "\"v2\""),
            )
        }
        val notModifiedClient = HttpClient(notModifiedEngine)
        try {
            val notModified = fetchFribbAnimeList(
                httpClient = notModifiedClient,
                urls = listOf(primaryUrl),
                ifNoneMatch = "\"v2\"",
                nowEpochMs = 9_000L,
            )
            val result = assertIs<FribbFetchResult.NotModified>(notModified)
            assertEquals("\"v2\"", capturedIfNoneMatch)
            assertEquals("\"v2\"", result.etag)
        } finally {
            notModifiedClient.close()
        }
    }
}

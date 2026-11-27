package com.nuvio.app.features.anilist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniListApiAndAuthTest {
    @Test
    fun `parseAccessTokenFromCallbackUrl extracts token from fragment query and rejects non-matching urls`() {
        assertEquals(
            "tok123",
            AniListAuthCallbackParser.parseAccessTokenFromCallbackUrl(
                "nuvio://anilist/callback#access_token=tok123&token_type=Bearer&expires_in=31536000",
            ),
        )
        assertEquals(
            "tok456",
            AniListAuthCallbackParser.parseAccessTokenFromCallbackUrl(
                "NUVIO://ANILIST/CALLBACK?access_token=tok456&state=xyz",
            ),
        )
        assertNull(
            AniListAuthCallbackParser.parseAccessTokenFromCallbackUrl(
                "nuvio://trakt/callback#access_token=tok123",
            ),
        )
        assertNull(
            AniListAuthCallbackParser.parseAccessTokenFromCallbackUrl(
                "nuvio://anilist/callback#token_type=Bearer",
            ),
        )
    }

    @Test
    fun `buildOAuthAuthorizeUrl and normalizeDirectToken format expected values`() {
        assertEquals(
            "https://anilist.co/api/v2/oauth/authorize?client_id=12345&response_type=token",
            AniListAuthCallbackParser.buildOAuthAuthorizeUrl("12345"),
        )
        assertEquals(
            "tok123",
            AniListAuthCallbackParser.normalizeDirectToken("Bearer tok123"),
        )
        assertEquals(
            "tok123",
            AniListAuthCallbackParser.normalizeDirectToken("  bearer   tok123  "),
        )
        assertNull(AniListAuthCallbackParser.normalizeDirectToken("Bearer   "))
    }

    @Test
    fun `parseMediaListCollectionResponse parses multiple statuses and media fields`() {
        val json = """
            {
              "data": {
                "MediaListCollection": {
                  "lists": [
                    {
                      "status": "CURRENT",
                      "entries": [
                        {
                          "id": 901,
                          "status": "CURRENT",
                          "score": 80,
                          "progress": 5,
                          "updatedAt": 1700000100,
                          "media": {
                            "id": 154587,
                            "idMal": 52991,
                            "title": {
                              "romaji": "Sousou no Frieren",
                              "english": "Frieren: Beyond Journey's End"
                            },
                            "format": "TV",
                            "status": "FINISHED",
                            "episodes": 28,
                            "duration": 24,
                            "seasonYear": 2023,
                            "coverImage": {
                              "extraLarge": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg",
                              "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx154587.jpg"
                            },
                            "averageScore": 84
                          }
                        }
                      ]
                    },
                    {
                      "status": "COMPLETED",
                      "entries": [
                        {
                          "id": 902,
                          "status": "COMPLETED",
                          "score": 92,
                          "progress": 12,
                          "updatedAt": 1700000200,
                          "media": {
                            "id": 21,
                            "idMal": 21,
                            "title": {
                              "romaji": "ONE PIECE",
                              "english": "One Piece"
                            },
                            "format": "TV",
                            "status": "RELEASING",
                            "episodes": null,
                            "duration": 24,
                            "seasonYear": 1999,
                            "coverImage": {
                              "extraLarge": null,
                              "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx21.jpg"
                            },
                            "averageScore": 88
                          }
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val entries = parseMediaListCollectionResponse(json)
        assertEquals(2, entries.size)

        val current = entries[0]
        assertEquals(901, current.entryId)
        assertEquals(154587, current.mediaId)
        assertEquals(52991, current.malId)
        assertEquals("Sousou no Frieren", current.titleRomaji)
        assertEquals("Frieren: Beyond Journey's End", current.titleEnglish)
        assertEquals(AniListListStatus.CURRENT, current.userStatus)
        assertEquals(5, current.progress)
        assertEquals(80, current.userScore)
        assertEquals(84, current.averageScore)
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg",
            current.coverImageUrl,
        )

        val completed = entries[1]
        assertEquals(902, completed.entryId)
        assertEquals(AniListListStatus.COMPLETED, completed.userStatus)
        assertEquals(92, completed.userScore)
        assertEquals(12, completed.progress)
        assertEquals(
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx21.jpg",
            completed.coverImageUrl,
        )
    }

    @Test
    fun `parseMediaListCollectionResponse throws AniListApiException when root field is null with errors`() {
        val json = """
            {
              "data": { "MediaListCollection": null },
              "errors": [{ "message": "Invalid token", "status": 400 }]
            }
        """.trimIndent()

        val error = assertFailsWith<AniListApiException> {
            parseMediaListCollectionResponse(json)
        }
        assertEquals(400, error.status)
        assertEquals("Invalid token", error.message)
    }

    @Test
    fun `selectAniListContinueWatchingEntries keeps only active unfinished series sorted by updatedAt`() {
        val entries = listOf(
            AniListMediaEntry(
                mediaId = 1,
                titleRomaji = "Completed Show",
                format = "TV",
                episodes = 12,
                userStatus = AniListListStatus.COMPLETED,
                progress = 12,
                updatedAtSeconds = 500L,
            ),
            AniListMediaEntry(
                mediaId = 2,
                titleRomaji = "Paused Show",
                format = "TV",
                episodes = 24,
                userStatus = AniListListStatus.PAUSED,
                progress = 5,
                updatedAtSeconds = 400L,
            ),
            AniListMediaEntry(
                mediaId = 3,
                titleRomaji = "Dropped Show",
                format = "TV",
                episodes = 24,
                userStatus = AniListListStatus.DROPPED,
                progress = 3,
                updatedAtSeconds = 300L,
            ),
            AniListMediaEntry(
                mediaId = 4,
                titleRomaji = "Finished Count Current",
                format = "TV",
                episodes = 12,
                userStatus = AniListListStatus.CURRENT,
                progress = 12,
                updatedAtSeconds = 600L,
            ),
            AniListMediaEntry(
                mediaId = 5,
                titleRomaji = "Movie Current",
                format = "MOVIE",
                episodes = 1,
                userStatus = AniListListStatus.CURRENT,
                progress = 1,
                updatedAtSeconds = 700L,
            ),
            AniListMediaEntry(
                mediaId = 6,
                titleRomaji = "Active Watching Older",
                format = "TV",
                episodes = 24,
                userStatus = AniListListStatus.CURRENT,
                progress = 5,
                updatedAtSeconds = 100L,
            ),
            AniListMediaEntry(
                mediaId = 7,
                titleRomaji = "Active Repeating Newer",
                format = "TV",
                episodes = 12,
                userStatus = AniListListStatus.REPEATING,
                progress = 2,
                updatedAtSeconds = 250L,
            ),
        )

        val selected = selectAniListContinueWatchingEntries(entries)
        assertEquals(listOf(7, 6), selected.map { it.mediaId })
    }

    @Test
    fun `parseMediaListCollectionResponse extracts serial PREQUEL ids and ignores SEQUEL or MOVIE`() {
        val json = """
            {
              "data": {
                "MediaListCollection": {
                  "lists": [
                    {
                      "status": "CURRENT",
                      "entries": [
                        {
                          "id": 910,
                          "status": "CURRENT",
                          "progress": 5,
                          "updatedAt": 1700000300,
                          "media": {
                            "id": 180001,
                            "title": {
                              "romaji": "Black Clover Season 2",
                              "english": "Black Clover Season 2"
                            },
                            "format": "TV",
                            "episodes": 12,
                            "relations": {
                              "edges": [
                                {
                                  "relationType": "PREQUEL",
                                  "node": { "id": 97940, "format": "TV" }
                                },
                                {
                                  "relationType": "PREQUEL",
                                  "node": { "id": 99999, "format": "MOVIE" }
                                },
                                {
                                  "relationType": "SEQUEL",
                                  "node": { "id": 180002, "format": "TV" }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val entries = parseMediaListCollectionResponse(json)
        assertEquals(1, entries.size)
        assertEquals(listOf(97940), entries.first().prequelIds)
    }

    @Test
    fun `isAniListEntryEpisodeWatched matches MAL and AniList IDs for COMPLETED and CURRENT entries`() {
        val entries = listOf(
            AniListMediaEntry(
                mediaId = 97940,
                malId = 34572,
                titleRomaji = "Black Clover",
                format = "TV",
                episodes = 170,
                userStatus = AniListListStatus.COMPLETED,
                progress = 170,
            ),
            AniListMediaEntry(
                mediaId = 180001,
                malId = 55001,
                titleRomaji = "Black Clover Season 2",
                format = "TV",
                episodes = 12,
                userStatus = AniListListStatus.CURRENT,
                progress = 5,
            ),
        )

        assertTrue(isAniListEntryEpisodeWatched(entries, "mal:34572", null, 170))
        assertTrue(isAniListEntryEpisodeWatched(entries, "anilist:97940", null, 1))
        assertTrue(isAniListEntryEpisodeWatched(entries, "mal:55001", null, 5))
        assertFalse(isAniListEntryEpisodeWatched(entries, "mal:55001", null, 6))
        assertFalse(isAniListEntryEpisodeWatched(entries, "mal:999999", null, 1))
    }

    @Test
    fun `parseMediaListCollectionResponse parses nextAiringEpisode onto AniListMediaEntry`() {
        val json = """
            {
              "data": {
                "MediaListCollection": {
                  "lists": [
                    {
                      "status": "CURRENT",
                      "entries": [
                        {
                          "id": 950,
                          "status": "CURRENT",
                          "progress": 4,
                          "updatedAt": 1700000500,
                          "media": {
                            "id": 180001,
                            "idMal": 55001,
                            "title": {
                              "romaji": "Black Clover Season 2",
                              "english": "Black Clover Season 2"
                            },
                            "format": "TV",
                            "status": "RELEASING",
                            "episodes": null,
                            "nextAiringEpisode": {
                              "airingAt": 1764000000,
                              "episode": 5
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val entries = parseMediaListCollectionResponse(json)
        assertEquals(1, entries.size)
        assertEquals(5, entries.first().nextAiringEpisode)
        assertEquals(1764000000L, entries.first().nextAiringAtSeconds)
    }

    @Test
    fun `selectAniListContinueWatchingEntries includes progress 0 airing or finished and episodes null while excluding unreleased without airing time`() {
        val entries = listOf(
            AniListMediaEntry(
                mediaId = 10,
                titleRomaji = "Black Clover Season 2",
                format = "TV",
                status = "RELEASING",
                episodes = null,
                userStatus = AniListListStatus.CURRENT,
                progress = 0,
                updatedAtSeconds = 500L,
            ),
            AniListMediaEntry(
                mediaId = 11,
                titleRomaji = "Dragon Ball Super: Beerus",
                format = "TV",
                status = "NOT_YET_RELEASED",
                episodes = null,
                userStatus = AniListListStatus.CURRENT,
                progress = 0,
                nextAiringEpisode = 1,
                nextAiringAtSeconds = 1764500000L,
                updatedAtSeconds = 400L,
            ),
            AniListMediaEntry(
                mediaId = 12,
                titleRomaji = "Overgeared",
                format = "ONA",
                status = "RELEASING",
                episodes = null,
                userStatus = AniListListStatus.REPEATING,
                progress = 3,
                updatedAtSeconds = 600L,
            ),
            AniListMediaEntry(
                mediaId = 13,
                titleRomaji = "Far Future Unannounced",
                format = "TV",
                status = "NOT_YET_RELEASED",
                episodes = 12,
                userStatus = AniListListStatus.CURRENT,
                progress = 0,
                nextAiringAtSeconds = null,
                updatedAtSeconds = 700L,
            ),
            AniListMediaEntry(
                mediaId = 14,
                titleRomaji = "Finished Show Just Added",
                format = "TV",
                status = "FINISHED",
                episodes = 12,
                userStatus = AniListListStatus.CURRENT,
                progress = 0,
                updatedAtSeconds = 300L,
            ),
        )

        val selected = selectAniListContinueWatchingEntries(entries)
        assertEquals(listOf(12, 10, 11, 14), selected.map { it.mediaId })
    }

    @Test
    fun `ANILIST_TABS order matches Watching Completed Planning Paused Dropped Repeating`() {
        assertEquals(
            listOf("Watching", "Completed", "Planning", "Paused", "Dropped", "Repeating"),
            ANILIST_TABS.map { it.title },
        )
    }
}

package com.nuvio.app.features.anilist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
}

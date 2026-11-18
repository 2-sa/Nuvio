package com.nuvio.app.features.trakt

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TraktAuthRepositoryTest {

    @Test
    fun `hasRequiredTraktCredentials requires a non-blank client id`() {
        assertFalse(hasRequiredTraktCredentials(""))
        assertFalse(hasRequiredTraktCredentials("   "))
        assertTrue(hasRequiredTraktCredentials("trakt-client-id"))
    }

    @Test
    fun `buildTraktDeviceTokenPayload omits client_secret when blank and includes it when set`() {
        val secretless = Json.parseToJsonElement(
            buildTraktDeviceTokenPayload(
                code = "device-code",
                clientId = "client-id",
                clientSecret = "",
            ),
        ).jsonObject

        assertEquals("device-code", secretless.getValue("code").jsonPrimitive.content)
        assertEquals("client-id", secretless.getValue("client_id").jsonPrimitive.content)
        assertFalse(secretless.containsKey("client_secret"))

        val whitespaceSecret = Json.parseToJsonElement(
            buildTraktDeviceTokenPayload(
                code = "device-code",
                clientId = "client-id",
                clientSecret = "   ",
            ),
        ).jsonObject
        assertFalse(whitespaceSecret.containsKey("client_secret"))

        val legacy = Json.parseToJsonElement(
            buildTraktDeviceTokenPayload(
                code = "device-code",
                clientId = "client-id",
                clientSecret = "client-secret",
            ),
        ).jsonObject

        assertEquals("device-code", legacy.getValue("code").jsonPrimitive.content)
        assertEquals("client-id", legacy.getValue("client_id").jsonPrimitive.content)
        assertEquals("client-secret", legacy.getValue("client_secret").jsonPrimitive.content)
    }

    @Test
    fun `buildTraktAuthorizationCodePayload supports secretless PKCE, legacy secret, and neither`() {
        val secretlessPkce = Json.parseToJsonElement(
            buildTraktAuthorizationCodePayload(
                code = "auth-code",
                clientId = "client-id",
                clientSecret = "",
                redirectUri = "nuvio://auth/trakt",
                codeVerifier = "pkce-verifier-value",
            ),
        ).jsonObject

        assertEquals("auth-code", secretlessPkce.getValue("code").jsonPrimitive.content)
        assertEquals("client-id", secretlessPkce.getValue("client_id").jsonPrimitive.content)
        assertEquals("nuvio://auth/trakt", secretlessPkce.getValue("redirect_uri").jsonPrimitive.content)
        assertEquals("authorization_code", secretlessPkce.getValue("grant_type").jsonPrimitive.content)
        assertFalse(secretlessPkce.containsKey("client_secret"))
        assertEquals("pkce-verifier-value", secretlessPkce.getValue("code_verifier").jsonPrimitive.content)

        val legacy = Json.parseToJsonElement(
            buildTraktAuthorizationCodePayload(
                code = "auth-code",
                clientId = "client-id",
                clientSecret = "client-secret",
                redirectUri = "nuvio://auth/trakt",
                codeVerifier = null,
            ),
        ).jsonObject

        assertEquals("auth-code", legacy.getValue("code").jsonPrimitive.content)
        assertEquals("client-id", legacy.getValue("client_id").jsonPrimitive.content)
        assertEquals("client-secret", legacy.getValue("client_secret").jsonPrimitive.content)
        assertEquals("nuvio://auth/trakt", legacy.getValue("redirect_uri").jsonPrimitive.content)
        assertEquals("authorization_code", legacy.getValue("grant_type").jsonPrimitive.content)
        assertFalse(legacy.containsKey("code_verifier"))

        val neither = Json.parseToJsonElement(
            buildTraktAuthorizationCodePayload(
                code = "auth-code",
                clientId = "client-id",
                clientSecret = "   ",
                redirectUri = "nuvio://auth/trakt",
                codeVerifier = "   ",
            ),
        ).jsonObject

        assertEquals("auth-code", neither.getValue("code").jsonPrimitive.content)
        assertEquals("client-id", neither.getValue("client_id").jsonPrimitive.content)
        assertEquals("nuvio://auth/trakt", neither.getValue("redirect_uri").jsonPrimitive.content)
        assertEquals("authorization_code", neither.getValue("grant_type").jsonPrimitive.content)
        assertFalse(neither.containsKey("client_secret"))
        assertFalse(neither.containsKey("code_verifier"))
    }

    @Test
    fun `buildTraktRefreshTokenPayload omits client_secret when blank and includes it when set`() {
        val secretless = Json.parseToJsonElement(
            buildTraktRefreshTokenPayload(
                refreshToken = "refresh-token",
                clientId = "client-id",
                clientSecret = "",
                redirectUri = "nuvio://auth/trakt",
            ),
        ).jsonObject

        assertEquals("refresh-token", secretless.getValue("refresh_token").jsonPrimitive.content)
        assertEquals("client-id", secretless.getValue("client_id").jsonPrimitive.content)
        assertEquals("nuvio://auth/trakt", secretless.getValue("redirect_uri").jsonPrimitive.content)
        assertEquals("refresh_token", secretless.getValue("grant_type").jsonPrimitive.content)
        assertFalse(secretless.containsKey("client_secret"))

        val legacy = Json.parseToJsonElement(
            buildTraktRefreshTokenPayload(
                refreshToken = "refresh-token",
                clientId = "client-id",
                clientSecret = "client-secret",
                redirectUri = "nuvio://auth/trakt",
            ),
        ).jsonObject

        assertEquals("refresh-token", legacy.getValue("refresh_token").jsonPrimitive.content)
        assertEquals("client-id", legacy.getValue("client_id").jsonPrimitive.content)
        assertEquals("client-secret", legacy.getValue("client_secret").jsonPrimitive.content)
        assertEquals("nuvio://auth/trakt", legacy.getValue("redirect_uri").jsonPrimitive.content)
        assertEquals("refresh_token", legacy.getValue("grant_type").jsonPrimitive.content)
    }

    @Test
    fun `buildTraktRevokePayload omits client_secret when blank and includes it when set`() {
        val secretless = Json.parseToJsonElement(
            buildTraktRevokePayload(
                token = "access-token",
                clientId = "client-id",
                clientSecret = "",
            ),
        ).jsonObject

        assertEquals("access-token", secretless.getValue("token").jsonPrimitive.content)
        assertEquals("client-id", secretless.getValue("client_id").jsonPrimitive.content)
        assertFalse(secretless.containsKey("client_secret"))

        val legacy = Json.parseToJsonElement(
            buildTraktRevokePayload(
                token = "access-token",
                clientId = "client-id",
                clientSecret = "client-secret",
            ),
        ).jsonObject

        assertEquals("access-token", legacy.getValue("token").jsonPrimitive.content)
        assertEquals("client-id", legacy.getValue("client_id").jsonPrimitive.content)
        assertEquals("client-secret", legacy.getValue("client_secret").jsonPrimitive.content)
    }

    @Test
    fun `buildTraktAuthorizationUrl includes S256 PKCE params when challenge is provided and omits when absent`() {
        val withPkce = buildTraktAuthorizationUrl(
            clientId = "client id",
            redirectUri = "nuvio://auth/trakt",
            state = "state-123",
            codeChallenge = "challenge-abc",
        )

        assertTrue(withPkce.startsWith("https://trakt.tv/oauth/authorize?"))
        assertTrue("response_type=code" in withPkce)
        assertTrue("client_id=client+id" in withPkce || "client_id=client%20id" in withPkce)
        assertTrue("redirect_uri=nuvio%3A%2F%2Fauth%2Ftrakt" in withPkce)
        assertTrue("state=state-123" in withPkce)
        assertTrue("code_challenge=challenge-abc" in withPkce)
        assertTrue("code_challenge_method=S256" in withPkce)

        val withoutPkce = buildTraktAuthorizationUrl(
            clientId = "client-id",
            redirectUri = "nuvio://auth/trakt",
            state = "state-123",
            codeChallenge = null,
        )

        assertTrue(withoutPkce.startsWith("https://trakt.tv/oauth/authorize?"))
        assertFalse("code_challenge=" in withoutPkce)
        assertFalse("code_challenge_method=" in withoutPkce)

        val blankPkce = buildTraktAuthorizationUrl(
            clientId = "client-id",
            redirectUri = "nuvio://auth/trakt",
            state = "state-123",
            codeChallenge = "   ",
        )

        assertFalse("code_challenge=" in blankPkce)
        assertFalse("code_challenge_method=" in blankPkce)
    }

    @Test
    fun `TraktAuthState deserializes persisted JSON without pendingAuthorizationCodeVerifier`() {
        val repositoryJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val legacyPayload = """
            {
              "accessToken": "access-123",
              "refreshToken": "refresh-456",
              "tokenType": "bearer",
              "createdAt": 1700000000,
              "expiresIn": 7776000,
              "username": "alice",
              "userSlug": "alice-slug",
              "pendingAuthorizationState": "pending-state",
              "pendingAuthorizationStartedAtMillis": 1700000100000
            }
        """.trimIndent()

        val decoded = repositoryJson.decodeFromString<TraktAuthState>(legacyPayload)

        assertEquals("access-123", decoded.accessToken)
        assertEquals("refresh-456", decoded.refreshToken)
        assertEquals("pending-state", decoded.pendingAuthorizationState)
        assertNull(decoded.pendingAuthorizationCodeVerifier)
        assertTrue(decoded.isAuthenticated)

        val decodedWithDefaultJson = Json.decodeFromString<TraktAuthState>(legacyPayload)
        assertNull(decodedWithDefaultJson.pendingAuthorizationCodeVerifier)
    }

    @Test
    fun `HTTP 400 permanently invalidates a rejected refresh token`() {
        assertEquals(
            TraktTokenRefreshResponseAction.INVALIDATE,
            traktTokenRefreshResponseAction(400),
        )
    }

    @Test
    fun `successful token responses are accepted`() {
        assertEquals(
            TraktTokenRefreshResponseAction.ACCEPT,
            traktTokenRefreshResponseAction(200),
        )
        assertEquals(
            TraktTokenRefreshResponseAction.ACCEPT,
            traktTokenRefreshResponseAction(201),
        )
    }

    @Test
    fun `non-400 failures preserve credentials for a later attempt`() {
        listOf(401, 429, 500, 503).forEach { status ->
            assertEquals(
                TraktTokenRefreshResponseAction.TRANSIENT_FAILURE,
                traktTokenRefreshResponseAction(status),
            )
        }
    }
}

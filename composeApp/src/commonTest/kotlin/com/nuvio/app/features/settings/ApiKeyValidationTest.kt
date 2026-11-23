package com.nuvio.app.features.settings

import com.nuvio.app.features.addons.RawHttpResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiKeyValidationTest {

    @Test
    fun detectsTmdbV4ReadAccessTokenAndGuidesUserToV3Key() = runBlocking {
        val jwt = "eyJhbGciOiJIUzI1NiJ9." + "a".repeat(60) + "." + "b".repeat(40)
        assertTrue(looksLikeTmdbV4ReadAccessToken(jwt))
        assertFalse(looksLikeTmdbV4ReadAccessToken("a8f3d91c7b2e4f80c12a6b9e0d341f77"))

        val result = validateTmdbApiKey(apiKey = jwt) { _, _ ->
            error("HTTP client should not be called for v4 JWT")
        }
        val failure = assertIs<ApiKeyTestResult.Failure>(result)
        assertEquals(ApiKeyTestError.TmdbV4TokenDetected, failure.error)
    }

    @Test
    fun validateTmdbApiKeySucceedsOn200WithImagesPayloadAndFallsBackToDefaultKey() = runBlocking {
        var requestedUrl = ""
        val result = validateTmdbApiKey(
            apiKey = "",
            fallbackDefaultKey = "default_v3_key",
        ) { _, url ->
            requestedUrl = url
            RawHttpResponse(
                status = 200,
                statusText = "OK",
                url = url,
                body = """{"images":{"base_url":"http://image.tmdb.org/t/p/"}}""",
                headers = emptyMap(),
            )
        }

        assertTrue(requestedUrl.contains("api_key=default_v3_key"))
        assertIs<ApiKeyTestResult.Success>(result)
    }

    @Test
    fun validateTmdbApiKeyMaps401And429Cleanly() = runBlocking {
        val unauthorized = validateTmdbApiKey(apiKey = "badkey") { _, url ->
            RawHttpResponse(401, "Unauthorized", url, """{"status_code":7}""", emptyMap())
        }
        assertEquals(ApiKeyTestError.InvalidKey, assertIs<ApiKeyTestResult.Failure>(unauthorized).error)

        val limited = validateTmdbApiKey(apiKey = "busykey") { _, url ->
            RawHttpResponse(429, "Too Many Requests", url, "", emptyMap())
        }
        assertEquals(ApiKeyTestError.RateLimited, assertIs<ApiKeyTestResult.Failure>(limited).error)
    }

    @Test
    fun validateMdbListApiKeyExtractsQuotaAndDetectsErrorPayload() = runBlocking {
        val valid = validateMdbListApiKey(apiKey = "mdb_live_key") { _, url ->
            RawHttpResponse(
                status = 200,
                statusText = "OK",
                url = url,
                body = """{"user_id":42,"patron_status":"Supporter","api_requests":1000,"api_requests_count":125}""",
                headers = emptyMap(),
            )
        }
        val success = assertIs<ApiKeyTestResult.Success>(valid)
        assertEquals("Supporter · 125/1000", success.detail)

        val jsonError = validateMdbListApiKey(apiKey = "mdb_bad_key") { _, url ->
            RawHttpResponse(
                status = 200,
                statusText = "OK",
                url = url,
                body = """{"error":"Invalid API key!"}""",
                headers = emptyMap(),
            )
        }
        assertEquals(ApiKeyTestError.InvalidKey, assertIs<ApiKeyTestResult.Failure>(jsonError).error)
    }

    @Test
    fun validateCustomPosterUrlPatternChecksSchemeResolvesIdAndFallsBackFrom405HeadToGet() = runBlocking {
        val badScheme = validateCustomPosterUrlPattern("ftp://example.com/{id}.jpg")
        assertEquals(ApiKeyTestError.InvalidUrlPattern, assertIs<ApiKeyTestResult.Failure>(badScheme).error)

        val methods = mutableListOf<String>()
        var finalUrl = ""
        val valid = validateCustomPosterUrlPattern(
            pattern = "https://posters.example.com/{id}.jpg",
        ) { method, url ->
            methods += method
            finalUrl = url
            if (method == "HEAD") {
                RawHttpResponse(405, "Method Not Allowed", url, "", emptyMap())
            } else {
                RawHttpResponse(200, "OK", url, "", mapOf("Content-Type" to "image/jpeg"))
            }
        }

        assertEquals(listOf("HEAD", "GET"), methods)
        assertEquals("https://posters.example.com/tt0133093.jpg", finalUrl)
        assertIs<ApiKeyTestResult.Success>(valid)
    }
}

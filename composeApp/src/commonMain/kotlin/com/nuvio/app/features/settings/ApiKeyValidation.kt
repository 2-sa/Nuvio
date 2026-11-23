package com.nuvio.app.features.settings

import com.nuvio.app.core.poster.CustomPosterUrlResolver
import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.tmdb.buildTmdbUrl
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.TimeSource

internal enum class ApiKeyTestError {
    EmptyKey,
    TmdbV4TokenDetected,
    InvalidKey,
    RateLimited,
    InvalidUrlPattern,
    NetworkError,
}

internal sealed interface ApiKeyTestResult {
    data object Idle : ApiKeyTestResult
    data object Testing : ApiKeyTestResult
    data class Success(
        val latencyMs: Long,
        val detail: String? = null,
    ) : ApiKeyTestResult

    data class Failure(
        val error: ApiKeyTestError,
        val latencyMs: Long? = null,
        val httpStatus: Int? = null,
    ) : ApiKeyTestResult
}

internal typealias ApiKeyHttpClient = suspend (method: String, url: String) -> RawHttpResponse

private val DefaultApiKeyHttpClient: ApiKeyHttpClient = { method, url ->
    httpRequestRaw(
        method = method,
        url = url,
        headers = mapOf("Accept" to "application/json"),
        body = "",
        followRedirects = true,
        maxResponseBodyBytes = 64 * 1024,
    )
}

private val ValidationJson = Json { ignoreUnknownKeys = true }

internal fun looksLikeTmdbV4ReadAccessToken(candidate: String): Boolean {
    val trimmed = candidate.trim()
    return trimmed.startsWith("eyJ") && trimmed.count { it == '.' } >= 2 && trimmed.length >= 80
}

internal suspend fun validateTmdbApiKey(
    apiKey: String,
    fallbackDefaultKey: String = "",
    httpClient: ApiKeyHttpClient = DefaultApiKeyHttpClient,
): ApiKeyTestResult {
    val trimmed = apiKey.trim()
    val effectiveKey = trimmed.ifBlank { fallbackDefaultKey.trim() }
    if (effectiveKey.isBlank()) {
        return ApiKeyTestResult.Failure(ApiKeyTestError.EmptyKey)
    }
    if (looksLikeTmdbV4ReadAccessToken(effectiveKey)) {
        return ApiKeyTestResult.Failure(ApiKeyTestError.TmdbV4TokenDetected)
    }

    val url = buildTmdbUrl(endpoint = "configuration", apiKey = effectiveKey)
    val startMark = TimeSource.Monotonic.markNow()
    val response = runCatching { httpClient("GET", url) }.getOrNull()
        ?: return ApiKeyTestResult.Failure(
            error = ApiKeyTestError.NetworkError,
            latencyMs = startMark.elapsedNow().inWholeMilliseconds,
        )
    val latencyMs = startMark.elapsedNow().inWholeMilliseconds

    return when {
        response.status in 200..299 -> {
            val isValidPayload = runCatching {
                val root = ValidationJson.parseToJsonElement(response.body).jsonObject
                root.containsKey("images") || root["success"]?.jsonPrimitive?.content == "true"
            }.getOrDefault(false)
            if (isValidPayload) {
                ApiKeyTestResult.Success(latencyMs = latencyMs)
            } else {
                ApiKeyTestResult.Failure(
                    error = ApiKeyTestError.InvalidKey,
                    latencyMs = latencyMs,
                    httpStatus = response.status,
                )
            }
        }
        response.status == 401 || response.status == 403 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.InvalidKey,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        response.status == 429 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.RateLimited,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        else -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.NetworkError,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
    }
}

internal suspend fun validateMdbListApiKey(
    apiKey: String,
    httpClient: ApiKeyHttpClient = DefaultApiKeyHttpClient,
): ApiKeyTestResult {
    val trimmed = apiKey.trim()
    if (trimmed.isBlank()) {
        return ApiKeyTestResult.Failure(ApiKeyTestError.EmptyKey)
    }

    val url = "https://api.mdblist.com/user?apikey=${trimmed.encodeURLParameter()}"
    val startMark = TimeSource.Monotonic.markNow()
    val response = runCatching { httpClient("GET", url) }.getOrNull()
        ?: return ApiKeyTestResult.Failure(
            error = ApiKeyTestError.NetworkError,
            latencyMs = startMark.elapsedNow().inWholeMilliseconds,
        )
    val latencyMs = startMark.elapsedNow().inWholeMilliseconds

    return when {
        response.status in 200..299 -> {
            val parsed = runCatching {
                ValidationJson.parseToJsonElement(response.body).jsonObject
            }.getOrNull()
            val errorField = parsed?.get("error")?.jsonPrimitive?.content
            if (!errorField.isNullOrBlank()) {
                ApiKeyTestResult.Failure(
                    error = ApiKeyTestError.InvalidKey,
                    latencyMs = latencyMs,
                    httpStatus = response.status,
                )
            } else {
                val limit = parsed?.get("api_requests")?.jsonPrimitive?.intOrNull
                val used = parsed?.get("api_requests_count")?.jsonPrimitive?.intOrNull
                val patron = parsed?.get("patron_status")?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() && it != "null" }
                val detailParts = buildList {
                    if (patron != null) add(patron)
                    if (limit != null && used != null) {
                        add("$used/$limit")
                    } else if (limit != null) {
                        add("$limit/day")
                    }
                }
                ApiKeyTestResult.Success(
                    latencyMs = latencyMs,
                    detail = detailParts.takeIf { it.isNotEmpty() }?.joinToString(" · "),
                )
            }
        }
        response.status == 401 || response.status == 403 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.InvalidKey,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        response.status == 429 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.RateLimited,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        else -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.NetworkError,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
    }
}

private val SampleCustomPosterTestIds = CustomPosterUrlResolver.ContentIds(
    id = "tt0133093",
    imdbId = "tt0133093",
    tmdbId = "603",
)

internal suspend fun validateCustomPosterUrlPattern(
    pattern: String,
    httpClient: ApiKeyHttpClient = { method, url ->
        httpRequestRaw(
            method = method,
            url = url,
            headers = mapOf("Accept" to "image/*,*/*;q=0.8"),
            body = "",
            followRedirects = true,
            maxResponseBodyBytes = 16 * 1024,
        )
    },
): ApiKeyTestResult {
    val trimmed = pattern.trim()
    if (trimmed.isBlank()) {
        return ApiKeyTestResult.Failure(ApiKeyTestError.EmptyKey)
    }
    if (!trimmed.startsWith("http://", ignoreCase = true) &&
        !trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return ApiKeyTestResult.Failure(ApiKeyTestError.InvalidUrlPattern)
    }

    val resolvedUrl = CustomPosterUrlResolver.resolve(
        pattern = trimmed,
        ids = SampleCustomPosterTestIds,
        type = "movie",
        shape = "poster",
    ) ?: return ApiKeyTestResult.Failure(ApiKeyTestError.InvalidUrlPattern)

    val startMark = TimeSource.Monotonic.markNow()
    val headResponse = runCatching { httpClient("HEAD", resolvedUrl) }.getOrNull()
    val response = if (headResponse != null && headResponse.status != 405 && headResponse.status != 501) {
        headResponse
    } else {
        runCatching { httpClient("GET", resolvedUrl) }.getOrNull()
    } ?: return ApiKeyTestResult.Failure(
        error = ApiKeyTestError.NetworkError,
        latencyMs = startMark.elapsedNow().inWholeMilliseconds,
    )
    val latencyMs = startMark.elapsedNow().inWholeMilliseconds

    return when {
        response.status in 200..299 -> {
            val contentType = response.headers.entries
                .firstOrNull { it.key.equals("content-type", ignoreCase = true) }
                ?.value
                ?.lowercase()
            if (contentType == null || contentType.startsWith("image/") || contentType.contains("octet-stream")) {
                ApiKeyTestResult.Success(latencyMs = latencyMs)
            } else {
                ApiKeyTestResult.Failure(
                    error = ApiKeyTestError.InvalidUrlPattern,
                    latencyMs = latencyMs,
                    httpStatus = response.status,
                )
            }
        }
        response.status == 401 || response.status == 403 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.InvalidKey,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        response.status == 429 -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.RateLimited,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
        else -> ApiKeyTestResult.Failure(
            error = ApiKeyTestError.NetworkError,
            latencyMs = latencyMs,
            httpStatus = response.status,
        )
    }
}

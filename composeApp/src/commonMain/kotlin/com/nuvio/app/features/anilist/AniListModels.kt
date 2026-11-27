package com.nuvio.app.features.anilist

import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.Serializable

@Serializable
enum class AniListListStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    PAUSED,
    DROPPED,
    REPEATING,
}

@Serializable
data class AniListUser(
    val id: Int,
    val name: String,
    val avatarUrl: String? = null,
)

@Serializable
data class AniListAuthState(
    val profileId: Int,
    val clientId: String = "",
    val accessToken: String? = null,
    val user: AniListUser? = null,
    val isBusy: Boolean = false,
    val error: String? = null,
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && user != null
}

@Serializable
data class AniListMediaEntry(
    val entryId: Int = 0,
    val mediaId: Int,
    val malId: Int? = null,
    val titleRomaji: String,
    val titleEnglish: String? = null,
    val format: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val seasonYear: Int? = null,
    val coverImageUrl: String? = null,
    val averageScore: Int? = null,
    val userStatus: AniListListStatus? = null,
    val progress: Int = 0,
    val userScore: Int = 0,
    val updatedAtSeconds: Long = 0L,
    val prequelIds: List<Int> = emptyList(),
    val nextAiringEpisode: Int? = null,
    val nextAiringAtSeconds: Long? = null,
)

@Serializable
data class AniListSyncSnapshot(
    val user: AniListUser,
    val entries: List<AniListMediaEntry>,
    val checkedAtEpochMs: Long,
)

object AniListAuthCallbackParser {
    private const val CALLBACK_PREFIX = "nuvio://anilist/callback"

    fun buildOAuthAuthorizeUrl(clientId: String): String =
        "https://anilist.co/api/v2/oauth/authorize?client_id=${clientId.trim()}&response_type=token"

    fun parseAccessTokenFromCallbackUrl(url: String): String? {
        val trimmed = url.trim()
        if (!trimmed.startsWith(CALLBACK_PREFIX, ignoreCase = true)) return null
        val suffix = trimmed.drop(CALLBACK_PREFIX.length)
        if (suffix.isNotEmpty() && suffix[0] !in charArrayOf('?', '#', '/')) return null

        val fragment = trimmed.substringAfter('#', missingDelimiterValue = "")
        extractParam(fragment, "access_token")?.let { return it }

        val query = trimmed.substringBefore('#').substringAfter('?', missingDelimiterValue = "")
        return extractParam(query, "access_token")
    }

    fun normalizeDirectToken(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.equals("Bearer", ignoreCase = true)) return null
        val token = if (trimmed.startsWith("Bearer", ignoreCase = true) && trimmed.length > 6 && trimmed[6].isWhitespace()) {
            trimmed.drop(6).trim()
        } else {
            trimmed
        }
        return token.takeIf { it.isNotBlank() }
    }

    private fun extractParam(rawParams: String, key: String): String? {
        if (rawParams.isBlank()) return null
        for (part in rawParams.split('&')) {
            val idx = part.indexOf('=')
            if (idx <= 0) continue
            val paramKey = part.substring(0, idx)
            if (!paramKey.equals(key, ignoreCase = true)) continue
            val rawValue = part.substring(idx + 1)
            val decoded = runCatching { rawValue.decodeURLQueryComponent() }.getOrElse { rawValue }.trim()
            if (decoded.isNotBlank()) return decoded
        }
        return null
    }
}

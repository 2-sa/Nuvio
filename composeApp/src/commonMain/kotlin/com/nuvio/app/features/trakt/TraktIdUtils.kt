package com.nuvio.app.features.trakt

import kotlinx.serialization.Serializable

@Serializable
internal data class TraktExternalIds(
    val trakt: Int? = null,
    val imdb: String? = null,
    val tmdb: Int? = null,
    val slug: String? = null,
)

internal fun parseTraktContentIds(contentId: String?): TraktExternalIds {
    if (contentId.isNullOrBlank()) return TraktExternalIds()
    val raw = contentId.trim()

    if (raw.startsWith("tt", ignoreCase = true)) {
        val imdb = raw.substringBefore(':').lowercase()
        if (imdb.length > 2) return TraktExternalIds(imdb = imdb)
    }

    if (raw.startsWith("imdb:", ignoreCase = true)) {
        val candidate = raw.substringAfter(':').substringBefore(':').trim()
        if (candidate.startsWith("tt", ignoreCase = true) && candidate.length > 2) {
            return TraktExternalIds(imdb = candidate.lowercase())
        }
    }

    if (raw.startsWith("tmdb:", ignoreCase = true)) {
        val tmdbPart = raw.substringAfter(':').trim()
            .let { value ->
                when {
                    value.startsWith("movie:", ignoreCase = true) -> value.substringAfter(':')
                    value.startsWith("tv:", ignoreCase = true) -> value.substringAfter(':')
                    value.startsWith("series:", ignoreCase = true) -> value.substringAfter(':')
                    value.startsWith("show:", ignoreCase = true) -> value.substringAfter(':')
                    else -> value
                }
            }
            .substringBefore(':')
            .trim()
        return TraktExternalIds(tmdb = tmdbPart.toIntOrNull())
    }

    if (raw.startsWith("trakt:", ignoreCase = true)) {
        val traktPart = raw.substringAfter(':').substringBefore(':').trim()
        return TraktExternalIds(trakt = traktPart.toIntOrNull())
    }

    val numeric = raw.substringBefore(':').toIntOrNull()
    return if (numeric != null) {
        TraktExternalIds(trakt = numeric)
    } else {
        TraktExternalIds()
    }
}

internal fun TraktExternalIds.mergeMissing(other: TraktExternalIds): TraktExternalIds =
    TraktExternalIds(
        trakt = trakt ?: other.trakt,
        imdb = imdb?.takeIf { it.isNotBlank() } ?: other.imdb?.takeIf { it.isNotBlank() },
        tmdb = tmdb ?: other.tmdb,
        slug = slug?.takeIf { it.isNotBlank() } ?: other.slug?.takeIf { it.isNotBlank() },
    )

internal data class TraktSearchMatchCandidate(
    val title: String?,
    val year: Int?,
    val ids: TraktExternalIds,
)

internal fun selectBestTraktSearchMatch(
    candidates: List<TraktSearchMatchCandidate>,
    queryTitle: String,
    queryYear: Int?,
): TraktExternalIds? {
    val valid = candidates.filter { it.ids.hasAnyId() }
    if (valid.isEmpty()) return null

    val normalizedQuery = normalizeTraktSearchTitle(queryTitle)
    if (normalizedQuery.isNotBlank()) {
        if (queryYear != null) {
            valid.firstOrNull { candidate ->
                candidate.year == queryYear &&
                    normalizeTraktSearchTitle(candidate.title.orEmpty()) == normalizedQuery
            }?.let { return it.ids }
        }
        valid.firstOrNull { candidate ->
            normalizeTraktSearchTitle(candidate.title.orEmpty()) == normalizedQuery &&
                (queryYear == null || candidate.year == null || kotlin.math.abs(candidate.year - queryYear) <= 1)
        }?.let { return it.ids }
    }

    if (queryYear != null) {
        val exactYearMatches = valid.filter { it.year == queryYear }
        if (exactYearMatches.size == 1) {
            return exactYearMatches.first().ids
        }
        if (exactYearMatches.isNotEmpty() && valid.first().year == queryYear) {
            return valid.first().ids
        }
    }

    return null
}

private fun normalizeTraktSearchTitle(value: String): String =
    value.trim().lowercase().filter { it.isLetterOrDigit() }

internal fun normalizeTraktContentId(ids: TraktExternalIds?, fallback: String? = null): String {
    val imdb = ids?.imdb?.takeIf { it.isNotBlank() }
    if (!imdb.isNullOrBlank()) return imdb

    val tmdb = ids?.tmdb
    if (tmdb != null) return "tmdb:$tmdb"

    val trakt = ids?.trakt
    if (trakt != null) return "trakt:$trakt"

    return fallback?.takeIf { it.isNotBlank() } ?: ""
}

internal fun extractTraktYear(value: String?): Int? {
    if (value.isNullOrBlank()) return null
    return Regex("(\\d{4})").find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

internal fun TraktExternalIds.hasAnyId(): Boolean =
    trakt != null || !imdb.isNullOrBlank() || tmdb != null || !slug.isNullOrBlank()

/**
 * Returns true if the given contentId uses a Trakt-compatible prefix
 * (IMDB, TMDB, or Trakt numeric). IDs from other sources (kitsu:, mal:,
 * anilist:, anidb:, tvdb:, etc.) are NOT Trakt-resolvable and should be
 * preserved locally rather than being discarded when absent from Trakt
 * remote responses.
 */
internal fun isTraktCompatibleId(contentId: String?): Boolean =
    parseTraktContentIds(contentId).hasAnyId()

/**
 * If [contentId] is not Trakt-resolvable but [videoId] contains a valid
 * IMDB or TMDB prefix, extract and return the resolved ID from videoId.
 * This handles addons that wrap real IDs in non-standard content IDs
 * (e.g. contentId = "tun_tt7821582", videoId = "tt7821582:3:7").
 *
 * Returns the original [contentId] if it's already valid or if no
 * better ID can be extracted from [videoId].
 */
internal fun resolveEffectiveContentId(contentId: String, videoId: String?): String {
    val parsedContent = parseTraktContentIds(contentId)
    if (parsedContent.hasAnyId()) return contentId

    if (videoId.isNullOrBlank() || videoId == contentId) return contentId

    val parsedVideo = parseTraktContentIds(videoId)
    if (!parsedVideo.hasAnyId()) return contentId

    // Rebuild a canonical content ID from the resolved video IDs
    return when {
        !parsedVideo.imdb.isNullOrBlank() -> parsedVideo.imdb
        parsedVideo.tmdb != null -> "tmdb:${parsedVideo.tmdb}"
        parsedVideo.trakt != null -> parsedVideo.trakt.toString()
        else -> contentId
    }
}

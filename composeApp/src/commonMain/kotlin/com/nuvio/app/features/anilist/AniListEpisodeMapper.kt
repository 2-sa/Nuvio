package com.nuvio.app.features.anilist

import com.nuvio.app.features.trakt.TraktEpisodeMappingService
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

internal data class SeasonEpisodeRef(
    val season: Int,
    val episode: Int,
    val videoId: String? = null,
)

internal fun extractExplicitSeasonNumber(
    titleEnglish: String?,
    titleRomaji: String?,
): Int? {
    for (raw in listOfNotNull(titleEnglish, titleRomaji)) {
        val title = raw.trim()
        if (title.isEmpty()) continue
        for (regex in EXPLICIT_SEASON_REGEXES) {
            val match = regex.find(title) ?: continue
            val season = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (season >= 2) return season
        }
    }
    return null
}

internal fun computePrequelEpisodeOffset(
    entry: AniListMediaEntry,
    allEntries: List<AniListMediaEntry>,
): Int {
    if (entry.prequelIds.isEmpty() || allEntries.isEmpty()) return 0
    val byMediaId = allEntries.associateBy { it.mediaId }
    val visited = mutableSetOf(entry.mediaId)
    val queue = ArrayDeque<Int>()
    entry.prequelIds.forEach { queue.addLast(it) }
    var offset = 0
    while (queue.isNotEmpty()) {
        val prequelId = queue.removeFirst()
        if (!visited.add(prequelId)) continue
        val prequel = byMediaId[prequelId] ?: continue
        val fmt = prequel.format?.uppercase()
        if (fmt != null && fmt !in SERIAL_ANILIST_FORMATS) continue
        val count = prequel.episodes ?: prequel.progress
        if (count > 0) offset += count
        prequel.prequelIds.forEach { nextId ->
            if (nextId !in visited) queue.addLast(nextId)
        }
    }
    return offset
}

internal fun mapEntryProgressToSeasonEpisode(
    entry: AniListMediaEntry,
    allEntries: List<AniListMediaEntry>,
    addonEpisodes: List<SeasonEpisodeRef>,
    isPerSeasonContentId: Boolean = false,
): SeasonEpisodeRef {
    val safeProgress = entry.progress.coerceAtLeast(1)
    val ordered = orderedMainEpisodes(addonEpisodes)
    val distinctMainSeasons = ordered.mapTo(mutableSetOf()) { it.season }
    if (distinctMainSeasons.size == 1) {
        val season = ordered.first().season
        return ordered.getOrNull(safeProgress - 1) ?: SeasonEpisodeRef(season = season, episode = safeProgress)
    }
    if (ordered.isEmpty() && isPerSeasonContentId) {
        return SeasonEpisodeRef(season = 1, episode = safeProgress)
    }
    val explicitSeason = extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji)
    if (explicitSeason != null) {
        val seasonEps = ordered.filter { it.season == explicitSeason }
        if (seasonEps.isNotEmpty()) {
            val index = (safeProgress - 1).coerceIn(0, seasonEps.lastIndex)
            return seasonEps[index]
        }
        if (ordered.isEmpty()) {
            return SeasonEpisodeRef(season = explicitSeason, episode = safeProgress)
        }
        val offset = computePrequelEpisodeOffset(entry, allEntries)
        if (offset > 0) {
            return mapAbsoluteEpisodeToSeasonEpisode(offset + safeProgress, ordered)
        }
        return SeasonEpisodeRef(season = explicitSeason, episode = safeProgress)
    }
    val offset = computePrequelEpisodeOffset(entry, allEntries)
    return mapAbsoluteEpisodeToSeasonEpisode(offset + safeProgress, ordered)
}

internal fun mapEntryProgressToWatchedEpisodes(
    entry: AniListMediaEntry,
    allEntries: List<AniListMediaEntry>,
    addonEpisodes: List<SeasonEpisodeRef>,
    isPerSeasonContentId: Boolean = false,
): List<SeasonEpisodeRef> {
    if (entry.progress <= 0) return emptyList()
    val ordered = orderedMainEpisodes(addonEpisodes)
    val distinctMainSeasons = ordered.mapTo(mutableSetOf()) { it.season }
    if (distinctMainSeasons.size == 1) {
        val season = ordered.first().season
        val count = minOf(entry.progress, MAX_FALLBACK_EPISODES)
        return if (count <= ordered.size) {
            ordered.take(count)
        } else {
            ordered + ((ordered.size + 1)..count).map { ep -> SeasonEpisodeRef(season = season, episode = ep) }
        }
    }
    if (ordered.isEmpty() && isPerSeasonContentId) {
        val count = minOf(entry.progress, MAX_FALLBACK_EPISODES)
        return (1..count).map { ep -> SeasonEpisodeRef(season = 1, episode = ep) }
    }
    val explicitSeason = extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji)
    if (explicitSeason != null) {
        val priorSeasonEps = ordered.filter { it.season in 1 until explicitSeason }
        val targetSeasonEps = ordered.filter { it.season == explicitSeason }
        if (targetSeasonEps.isNotEmpty()) {
            return priorSeasonEps + targetSeasonEps.take(minOf(entry.progress, targetSeasonEps.size))
        }
        if (ordered.isEmpty()) {
            val priorFallback = if (explicitSeason == 2) {
                val offset = computePrequelEpisodeOffset(entry, allEntries)
                if (offset > 0) {
                    val count = minOf(offset, MAX_FALLBACK_EPISODES)
                    (1..count).map { ep -> SeasonEpisodeRef(season = 1, episode = ep) }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            val currentCount = minOf(entry.progress, MAX_FALLBACK_EPISODES)
            val currentFallback = (1..currentCount).map { ep -> SeasonEpisodeRef(season = explicitSeason, episode = ep) }
            return priorFallback + currentFallback
        }
        val offset = computePrequelEpisodeOffset(entry, allEntries)
        if (offset > 0) {
            return mapAbsoluteProgressToWatchedEpisodes(offset + entry.progress, ordered)
        }
        val currentCount = minOf(entry.progress, MAX_FALLBACK_EPISODES)
        return priorSeasonEps + (1..currentCount).map { ep -> SeasonEpisodeRef(season = explicitSeason, episode = ep) }
    }
    val offset = computePrequelEpisodeOffset(entry, allEntries)
    return mapAbsoluteProgressToWatchedEpisodes(offset + entry.progress, ordered)
}

internal fun mapSeasonEpisodeToEntryProgress(
    season: Int,
    episode: Int,
    entry: AniListMediaEntry?,
    allEntries: List<AniListMediaEntry>,
    addonEpisodes: List<SeasonEpisodeRef>,
    isPerSeasonContentId: Boolean = false,
): Int {
    if (entry == null) return mapSeasonEpisodeToAbsolute(season, episode, addonEpisodes)
    val ordered = orderedMainEpisodes(addonEpisodes)
    val distinctMainSeasons = ordered.mapTo(mutableSetOf()) { it.season }
    if (distinctMainSeasons.size == 1 || (ordered.isEmpty() && isPerSeasonContentId)) {
        return episode.coerceAtLeast(1)
    }
    val explicitSeason = extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji)
    if (explicitSeason != null && season == explicitSeason) {
        return episode.coerceAtLeast(1)
    }
    val offset = computePrequelEpisodeOffset(entry, allEntries)
    val abs = mapSeasonEpisodeToAbsolute(season, episode, addonEpisodes)
    return if (offset > 0 && abs > offset) abs - offset else abs
}

internal fun mapAbsoluteEpisodeToSeasonEpisode(
    absoluteEpisode: Int,
    addonEpisodes: List<SeasonEpisodeRef>,
): SeasonEpisodeRef {
    val safeEp = absoluteEpisode.coerceAtLeast(1)
    val ordered = orderedMainEpisodes(addonEpisodes)
    if (ordered.isEmpty()) return SeasonEpisodeRef(season = 1, episode = safeEp)
    val index = (safeEp - 1).coerceIn(0, ordered.lastIndex)
    return ordered[index]
}

internal fun mapAbsoluteProgressToWatchedEpisodes(
    absoluteProgress: Int,
    addonEpisodes: List<SeasonEpisodeRef>,
): List<SeasonEpisodeRef> {
    if (absoluteProgress <= 0) return emptyList()
    val ordered = orderedMainEpisodes(addonEpisodes)
    if (ordered.isEmpty()) {
        val count = minOf(absoluteProgress, MAX_FALLBACK_EPISODES)
        return (1..count).map { ep -> SeasonEpisodeRef(season = 1, episode = ep) }
    }
    return ordered.take(minOf(absoluteProgress, ordered.size))
}

internal fun mapSeasonEpisodeToAbsolute(
    season: Int,
    episode: Int,
    addonEpisodes: List<SeasonEpisodeRef>,
): Int {
    val ordered = orderedMainEpisodes(addonEpisodes)
    if (ordered.isEmpty()) return episode.coerceAtLeast(1)
    val idx = ordered.indexOfFirst { it.season == season && it.episode == episode }
    return if (idx >= 0) idx + 1 else episode.coerceAtLeast(1)
}

private fun orderedMainEpisodes(episodes: List<SeasonEpisodeRef>): List<SeasonEpisodeRef> =
    episodes
        .asSequence()
        .filter { it.season > 0 && it.episode > 0 }
        .distinctBy { it.season to it.episode }
        .sortedWith(compareBy<SeasonEpisodeRef> { it.season }.thenBy { it.episode })
        .toList()

internal object AniListEpisodeMapper {
    private val lock = SynchronizedObject()
    private val cache = mutableMapOf<String, List<SeasonEpisodeRef>>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<List<SeasonEpisodeRef>>>()

    fun getCached(contentId: String): List<SeasonEpisodeRef> = synchronized(lock) {
        cache[contentId].orEmpty()
    }

    suspend fun resolveAddonEpisodes(
        contentId: String,
        contentType: String = "series",
    ): List<SeasonEpisodeRef> = resolveAddonEpisodes(listOf(contentId), contentType)

    suspend fun resolveAddonEpisodes(
        contentIds: List<String>,
        contentType: String = "series",
    ): List<SeasonEpisodeRef> {
        val candidates = contentIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (candidates.isEmpty()) return emptyList()

        for (candidate in candidates) {
            synchronized(lock) { cache[candidate] }?.let { return it }
        }

        val cacheKey = candidates.first()
        val existing = synchronized(lock) { inFlight[cacheKey] }
        if (existing != null) {
            return try {
                existing.await()
            } catch (_: Exception) {
                emptyList()
            }
        }

        val deferred = CompletableDeferred<List<SeasonEpisodeRef>>()
        synchronized(lock) { inFlight[cacheKey] = deferred }

        return try {
            var resolved: List<SeasonEpisodeRef> = emptyList()
            for (candidate in candidates) {
                val raw = withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                    TraktEpisodeMappingService.getAddonEpisodes(candidate, contentType)
                }.orEmpty()
                val mapped = orderedMainEpisodes(
                    raw.mapNotNull { entry ->
                        val s = entry.season ?: return@mapNotNull null
                        val e = entry.episode ?: return@mapNotNull null
                        SeasonEpisodeRef(season = s, episode = e)
                    },
                )
                if (mapped.isNotEmpty()) {
                    resolved = mapped
                    break
                }
            }
            synchronized(lock) {
                if (resolved.isNotEmpty()) {
                    for (candidate in candidates) {
                        cache[candidate] = resolved
                    }
                }
            }
            deferred.complete(resolved)
            resolved
        } catch (error: CancellationException) {
            deferred.cancel(error)
            throw error
        } catch (_: Exception) {
            deferred.complete(emptyList())
            emptyList()
        } finally {
            synchronized(lock) {
                if (inFlight[cacheKey] === deferred) {
                    inFlight.remove(cacheKey)
                }
            }
        }
    }

    fun clearCache() {
        synchronized(lock) { cache.clear() }
    }
}

private const val MAX_FALLBACK_EPISODES = 2000
private const val RESOLVE_TIMEOUT_MS = 6_000L

private val SERIAL_ANILIST_FORMATS = setOf("TV", "TV_SHORT", "ONA")
private val EXPLICIT_SEASON_REGEXES = listOf(
    Regex("""\bseason\s+(\d{1,2})\b""", RegexOption.IGNORE_CASE),
    Regex("""\b(\d{1,2})(?:st|nd|rd|th)\s+season\b""", RegexOption.IGNORE_CASE),
    Regex("""\bS(\d{1,2})\b""", RegexOption.IGNORE_CASE),
)

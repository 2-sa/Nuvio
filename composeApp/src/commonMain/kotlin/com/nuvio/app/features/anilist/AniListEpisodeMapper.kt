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

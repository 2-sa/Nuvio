package com.nuvio.app.features.anilist

import com.nuvio.app.core.network.API_RESPONSE_MAX_BYTES
import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import com.nuvio.app.features.tracking.parseTrackingExternalIds
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import kotlin.time.Clock
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

private const val FRIBB_PRIMARY_URL =
    "https://cdn.jsdelivr.net/gh/Fribb/anime-lists@master/anime-list-full.json"
private const val FRIBB_FALLBACK_URL =
    "https://raw.githubusercontent.com/Fribb/anime-lists/master/anime-list-full.json"
private const val ANIME_ID_BRIDGE_TTL_MS = 24L * 60 * 60 * 1000
private const val ANIME_ID_BRIDGE_RETRY_COOLDOWN_MS = 15L * 60 * 1000

private val SERIAL_ANILIST_FORMATS = setOf("TV", "TV_SHORT", "ONA")
private val VIDEO_ID_PREFIXES = setOf("mal", "kitsu", "anilist", "anidb", "tmdb", "tvdb", "imdb")
private val fribbJson = Json { ignoreUnknownKeys = true }

internal data class AnimeIdMapping(
    val anilistId: Int? = null,
    val malId: Int? = null,
    val kitsuId: Int? = null,
    val anidbId: Int? = null,
    val imdbId: String? = null,
    val tmdbId: Long? = null,
    val tvdbId: String? = null,
    val type: String? = null,
) {
    /** Canonical content ID used for Library, WatchProgress, and primary WatchedItem emission. */
    fun canonicalContentId(): String? = when {
        malId != null && malId > 0 -> "mal:$malId"
        kitsuId != null && kitsuId > 0 -> "kitsu:$kitsuId"
        !imdbId.isNullOrBlank() -> imdbId
        anilistId != null && anilistId > 0 -> "anilist:$anilistId"
        else -> null
    }

    /** 1-to-1 per-season IDs safe for both poster and episode watched keys without franchise collisions. */
    fun perSeasonContentIds(): Set<String> = buildSet {
        malId?.takeIf { it > 0 }?.let { add("mal:$it") }
        kitsuId?.takeIf { it > 0 }?.let { add("kitsu:$it") }
        anilistId?.takeIf { it > 0 }?.let { add("anilist:$it") }
        anidbId?.takeIf { it > 0 }?.let { add("anidb:$it") }
    }

    /** Franchise-scoped IDs (may map 1-to-many across multiple AniList seasons). */
    fun franchiseContentIds(): Set<String> = buildSet {
        imdbId?.takeIf { it.isNotBlank() }?.let(::add)
        tmdbId?.takeIf { it > 0L }?.let { add("tmdb:$it") }
        tvdbId?.takeIf { it.isNotBlank() }?.let { add("tvdb:$it") }
    }
}

internal class AnimeIdBridgeIndex(
    val byAniListId: Map<Int, AnimeIdMapping> = emptyMap(),
    val byMalId: Map<Int, AnimeIdMapping> = emptyMap(),
    val byKitsuId: Map<Int, AnimeIdMapping> = emptyMap(),
    val byAniDbId: Map<Int, AnimeIdMapping> = emptyMap(),
    val byImdbId: Map<String, List<AnimeIdMapping>> = emptyMap(),
    val byTmdbId: Map<Long, List<AnimeIdMapping>> = emptyMap(),
    val byTvdbId: Map<String, List<AnimeIdMapping>> = emptyMap(),
    val loadedAtEpochMs: Long = 0L,
) {
    val isLoaded: Boolean
        get() = byAniListId.isNotEmpty() || byMalId.isNotEmpty()
}

internal fun parseFribbAnimeListJson(rawJson: String, loadedAtEpochMs: Long = 0L): AnimeIdBridgeIndex {
    if (rawJson.isBlank()) return AnimeIdBridgeIndex()
    val array = runCatching { fribbJson.parseToJsonElement(rawJson) }.getOrNull() as? JsonArray
        ?: return AnimeIdBridgeIndex()

    val byAniListId = HashMap<Int, AnimeIdMapping>()
    val byMalId = HashMap<Int, AnimeIdMapping>()
    val byKitsuId = HashMap<Int, AnimeIdMapping>()
    val byAniDbId = HashMap<Int, AnimeIdMapping>()
    val byImdbId = HashMap<String, MutableList<AnimeIdMapping>>()
    val byTmdbId = HashMap<Long, MutableList<AnimeIdMapping>>()
    val byTvdbId = HashMap<String, MutableList<AnimeIdMapping>>()

    for (element in array) {
        val obj = element as? JsonObject ?: continue
        val anilistId = obj.positiveInt("anilist_id")
        val malId = obj.positiveInt("mal_id")
        val kitsuId = obj.positiveInt("kitsu_id")
        val anidbId = obj.positiveInt("anidb_id")
        val imdbId = obj.normalizedImdbId("imdb_id")
        val tmdbId = obj.positiveLong("themoviedb_id")
        val tvdbId = obj.positiveLong("thetvdb_id")?.toString()
        val type = obj.nonBlankString("type")?.uppercase()

        if (anilistId == null &&
            malId == null &&
            kitsuId == null &&
            anidbId == null &&
            imdbId == null &&
            tmdbId == null &&
            tvdbId == null
        ) {
            continue
        }

        val mapping = AnimeIdMapping(
            anilistId = anilistId,
            malId = malId,
            kitsuId = kitsuId,
            anidbId = anidbId,
            imdbId = imdbId,
            tmdbId = tmdbId,
            tvdbId = tvdbId,
            type = type,
        )

        if (anilistId != null && !byAniListId.containsKey(anilistId)) {
            byAniListId[anilistId] = mapping
        }
        if (malId != null && !byMalId.containsKey(malId)) {
            byMalId[malId] = mapping
        }
        if (kitsuId != null && !byKitsuId.containsKey(kitsuId)) {
            byKitsuId[kitsuId] = mapping
        }
        if (anidbId != null && !byAniDbId.containsKey(anidbId)) {
            byAniDbId[anidbId] = mapping
        }
        if (imdbId != null) {
            byImdbId.getOrPut(imdbId) { mutableListOf() }.add(mapping)
        }
        if (tmdbId != null) {
            byTmdbId.getOrPut(tmdbId) { mutableListOf() }.add(mapping)
        }
        if (tvdbId != null) {
            byTvdbId.getOrPut(tvdbId) { mutableListOf() }.add(mapping)
        }
    }

    if (byAniListId.isEmpty() && byMalId.isEmpty()) {
        return AnimeIdBridgeIndex()
    }

    return AnimeIdBridgeIndex(
        byAniListId = byAniListId,
        byMalId = byMalId,
        byKitsuId = byKitsuId,
        byAniDbId = byAniDbId,
        byImdbId = byImdbId,
        byTmdbId = byTmdbId,
        byTvdbId = byTvdbId,
        loadedAtEpochMs = loadedAtEpochMs,
    )
}

internal fun canMarkFranchiseIdFullyWatched(
    franchiseMappings: List<AnimeIdMapping>,
    entriesByAniListId: Map<Int, AniListMediaEntry>,
): Boolean {
    val serialMappings = franchiseMappings.filter { mapping ->
        mapping.anilistId != null &&
            (mapping.type == null || mapping.type.uppercase() in SERIAL_ANILIST_FORMATS)
    }
    if (serialMappings.size <= 1) return true
    return serialMappings.all { mapping ->
        entriesByAniListId[mapping.anilistId]?.userStatus == AniListListStatus.COMPLETED
    }
}

internal sealed interface FribbFetchResult {
    data class Updated(
        val rawJson: String,
        val index: AnimeIdBridgeIndex,
        val etag: String?,
    ) : FribbFetchResult

    data class NotModified(
        val etag: String?,
    ) : FribbFetchResult

    data object Failed : FribbFetchResult
}

internal suspend fun fetchFribbAnimeList(
    httpClient: HttpClient,
    urls: List<String> = listOf(FRIBB_PRIMARY_URL, FRIBB_FALLBACK_URL),
    ifNoneMatch: String? = null,
    nowEpochMs: Long = Clock.System.now().toEpochMilliseconds(),
): FribbFetchResult {
    val normalizedEtag = ifNoneMatch?.trim()?.takeIf { it.isNotEmpty() }
    for (url in urls) {
        val result = try {
            httpClient.prepareGet(url) {
                header(HttpHeaders.Accept, "application/json")
                if (normalizedEtag != null) {
                    header(HttpHeaders.IfNoneMatch, normalizedEtag)
                }
            }.execute { response ->
                val responseEtag = response.headers[HttpHeaders.ETag]?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    response.status == HttpStatusCode.NotModified -> {
                        FribbFetchResult.NotModified(etag = responseEtag ?: normalizedEtag)
                    }
                    response.status.value in 200..299 -> {
                        val body = readBoundedResponseBody(
                            channel = response.bodyAsChannel(),
                            contentLength = response.contentLength(),
                            maxBytes = API_RESPONSE_MAX_BYTES,
                        )
                        val parsed = parseFribbAnimeListJson(body, loadedAtEpochMs = nowEpochMs)
                        if (parsed.isLoaded) {
                            FribbFetchResult.Updated(
                                rawJson = body,
                                index = parsed,
                                etag = responseEtag,
                            )
                        } else {
                            null
                        }
                    }
                    else -> null
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
        if (result != null) return result
    }
    return FribbFetchResult.Failed
}

internal object AnimeIdBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()
    private val stateLock = SynchronizedObject()
    private val httpClient: HttpClient by lazy { createApiHttpClient() }

    private val _indexState = MutableStateFlow(AnimeIdBridgeIndex())
    val indexState: StateFlow<AnimeIdBridgeIndex> = _indexState.asStateFlow()

    private val _initialLoadAttemptFinished = MutableStateFlow(false)
    private var diskLoaded: Boolean = false
    private var testOverrideActive: Boolean = false
    private var cachedEtag: String? = null
    private var lastFetchedAtEpochMs: Long = 0L
    private var lastFailedRefreshAtEpochMs: Long = 0L

    fun currentIndex(): AnimeIdBridgeIndex = _indexState.value

    fun ensureLoaded() {
        val freshInMemory = synchronized(stateLock) {
            if (testOverrideActive) return@synchronized true
            val now = Clock.System.now().toEpochMilliseconds()
            diskLoaded &&
                _indexState.value.isLoaded &&
                lastFetchedAtEpochMs > 0L &&
                (now - lastFetchedAtEpochMs) < ANIME_ID_BRIDGE_TTL_MS
        }
        if (freshInMemory) return

        scope.launch {
            loadDiskAndRefreshIfNeeded()
        }
    }

    suspend fun awaitReady(timeoutMs: Long = 5_000L): AnimeIdBridgeIndex {
        if (_indexState.value.isLoaded) {
            ensureLoaded()
            return _indexState.value
        }
        ensureLoaded()
        if (timeoutMs > 0L) {
            withTimeoutOrNull(timeoutMs) {
                combine(_indexState, _initialLoadAttemptFinished) { index, finished ->
                    index.isLoaded || finished
                }.first { it }
            }
        }
        return _indexState.value
    }

    fun resolveForEntry(
        entry: AniListMediaEntry,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): AnimeIdMapping {
        val found = index.byAniListId[entry.mediaId]
            ?: entry.malId?.takeIf { it > 0 }?.let { index.byMalId[it] }
        if (found != null) {
            return found.copy(
                anilistId = found.anilistId ?: entry.mediaId.takeIf { it > 0 },
                malId = found.malId ?: entry.malId?.takeIf { it > 0 },
                type = found.type ?: entry.format?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
            )
        }
        return AnimeIdMapping(
            anilistId = entry.mediaId.takeIf { it > 0 },
            malId = entry.malId?.takeIf { it > 0 },
            type = entry.format?.trim()?.uppercase()?.takeIf { it.isNotEmpty() },
        )
    }

    fun resolveByContentId(
        contentId: String?,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): AnimeIdMapping? {
        if (contentId.isNullOrBlank()) return null
        val ids = parseTrackingExternalIds(contentId)
        ids.anilist?.toInt()?.takeIf { it > 0 }?.let { index.byAniListId[it] }?.let { return it }
        ids.mal?.toInt()?.takeIf { it > 0 }?.let { index.byMalId[it] }?.let { return it }
        ids.kitsu?.toInt()?.takeIf { it > 0 }?.let { index.byKitsuId[it] }?.let { return it }
        ids.anidb?.toInt()?.takeIf { it > 0 }?.let { index.byAniDbId[it] }?.let { return it }
        ids.imdb?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?.let { index.byImdbId[it]?.firstOrNull() }?.let { return it }
        ids.tmdb?.takeIf { it > 0L }
            ?.let { index.byTmdbId[it]?.firstOrNull() }?.let { return it }
        ids.tvdb?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { index.byTvdbId[it]?.firstOrNull() }?.let { return it }
        return null
    }

    fun mappingsForFranchiseId(
        contentId: String?,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): List<AnimeIdMapping> {
        if (contentId.isNullOrBlank()) return emptyList()
        val ids = parseTrackingExternalIds(contentId)
        ids.imdb?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?.let { index.byImdbId[it] }?.takeIf { it.isNotEmpty() }?.let { return it }
        ids.tmdb?.takeIf { it > 0L }
            ?.let { index.byTmdbId[it] }?.takeIf { it.isNotEmpty() }?.let { return it }
        ids.tvdb?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { index.byTvdbId[it] }?.takeIf { it.isNotEmpty() }?.let { return it }
        return emptyList()
    }

    fun isKnownAnimeContentId(
        contentId: String?,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): Boolean {
        if (contentId.isNullOrBlank()) return false
        val ids = parseTrackingExternalIds(contentId)
        if (ids.anilist != null || ids.mal != null || ids.kitsu != null || ids.anidb != null) {
            return true
        }
        ids.imdb?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let {
            if (index.byImdbId.containsKey(it)) return true
        }
        ids.tmdb?.takeIf { it > 0L }?.let {
            if (index.byTmdbId.containsKey(it)) return true
        }
        ids.tvdb?.trim()?.takeIf { it.isNotEmpty() }?.let {
            if (index.byTvdbId.containsKey(it)) return true
        }
        return false
    }

    fun addonFallbackIds(
        contentId: String,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): List<String> {
        val trimmed = contentId.trim()
        if (trimmed.isEmpty()) return emptyList()
        val mapping = resolveByContentId(trimmed, index) ?: return listOf(trimmed)
        val seen = LinkedHashSet<String>()
        seen += trimmed
        mapping.malId?.takeIf { it > 0 }?.let { seen += "mal:$it" }
        mapping.kitsuId?.takeIf { it > 0 }?.let { seen += "kitsu:$it" }
        mapping.imdbId?.takeIf { it.isNotBlank() }?.let { seen += it }
        mapping.tmdbId?.takeIf { it > 0L }?.let { seen += "tmdb:$it" }
        mapping.tvdbId?.takeIf { it.isNotBlank() }?.let { seen += "tvdb:$it" }
        return seen.toList()
    }

    fun addonFallbackVideoIds(
        type: String,
        videoId: String,
        season: Int? = null,
        episode: Int? = null,
        index: AnimeIdBridgeIndex = currentIndex(),
    ): List<String> {
        val trimmed = videoId.trim()
        if (trimmed.isEmpty()) return emptyList()
        val normalizedType = type.trim().lowercase()
        if (normalizedType == "movie" || normalizedType == "film") {
            return addonFallbackIds(trimmed, index)
        }

        val parsed = parseVideoIdParts(trimmed, season, episode)
        val mapping = resolveByContentId(parsed.baseContentId, index) ?: return listOf(trimmed)
        val resolvedEpisode = parsed.episode?.takeIf { it > 0 }
            ?: return addonFallbackIds(trimmed, index)
        val effectiveSeason = parsed.season?.takeIf { it > 0 } ?: 1

        val seen = LinkedHashSet<String>()
        seen += trimmed
        mapping.malId?.takeIf { it > 0 }?.let { seen += "mal:$it:$resolvedEpisode" }
        mapping.kitsuId?.takeIf { it > 0 }?.let { seen += "kitsu:$it:$resolvedEpisode" }
        mapping.imdbId?.takeIf { it.isNotBlank() }?.let { seen += "$it:$effectiveSeason:$resolvedEpisode" }
        mapping.tmdbId?.takeIf { it > 0L }?.let { seen += "tmdb:$it:$effectiveSeason:$resolvedEpisode" }
        mapping.tvdbId?.takeIf { it.isNotBlank() }?.let { seen += "tvdb:$it:$effectiveSeason:$resolvedEpisode" }
        return seen.toList()
    }

    internal fun setIndexForTests(index: AnimeIdBridgeIndex) {
        synchronized(stateLock) {
            diskLoaded = true
            testOverrideActive = true
            cachedEtag = null
            lastFetchedAtEpochMs = Clock.System.now().toEpochMilliseconds()
            lastFailedRefreshAtEpochMs = 0L
            _indexState.value = index
            _initialLoadAttemptFinished.value = true
        }
    }

    internal fun resetForTests() {
        synchronized(stateLock) {
            diskLoaded = false
            testOverrideActive = false
            cachedEtag = null
            lastFetchedAtEpochMs = 0L
            lastFailedRefreshAtEpochMs = 0L
            _indexState.value = AnimeIdBridgeIndex()
            _initialLoadAttemptFinished.value = false
        }
    }

    private suspend fun loadDiskAndRefreshIfNeeded() {
        refreshMutex.withLock {
            try {
                if (synchronized(stateLock) { testOverrideActive }) return
                val needsDiskLoad = synchronized(stateLock) { !diskLoaded }
                if (needsDiskLoad) {
                    val stored = runCatching { AnimeIdBridgeStorage.load() }.getOrNull()
                    if (stored != null) {
                        val parsed = if (stored.schemaVersion == ANIME_ID_BRIDGE_SCHEMA_VERSION) {
                            parseFribbAnimeListJson(stored.rawJson, loadedAtEpochMs = stored.fetchedAtEpochMs)
                        } else {
                            AnimeIdBridgeIndex()
                        }
                        if (parsed.isLoaded) {
                            synchronized(stateLock) {
                                if (!diskLoaded) {
                                    _indexState.value = parsed
                                    cachedEtag = stored.etag
                                    lastFetchedAtEpochMs = stored.fetchedAtEpochMs
                                    diskLoaded = true
                                }
                            }
                        } else {
                            runCatching { AnimeIdBridgeStorage.clear() }
                            synchronized(stateLock) { diskLoaded = true }
                        }
                    } else {
                        synchronized(stateLock) { diskLoaded = true }
                    }
                }

                val now = Clock.System.now().toEpochMilliseconds()
                val conditionalEtag: String?
                val shouldRefresh = synchronized(stateLock) {
                    val hasValidIndex = _indexState.value.isLoaded
                    val isFresh = hasValidIndex &&
                        lastFetchedAtEpochMs > 0L &&
                        (now - lastFetchedAtEpochMs) < ANIME_ID_BRIDGE_TTL_MS
                    val inCooldown = lastFailedRefreshAtEpochMs > 0L &&
                        (now - lastFailedRefreshAtEpochMs) < ANIME_ID_BRIDGE_RETRY_COOLDOWN_MS
                    conditionalEtag = if (hasValidIndex) cachedEtag else null
                    !isFresh && !inCooldown
                }
                if (!shouldRefresh) return

                val fetchNow = Clock.System.now().toEpochMilliseconds()
                when (
                    val fetchResult = fetchFribbAnimeList(
                        httpClient = httpClient,
                        ifNoneMatch = conditionalEtag,
                        nowEpochMs = fetchNow,
                    )
                ) {
                    is FribbFetchResult.Updated -> {
                        synchronized(stateLock) {
                            _indexState.value = fetchResult.index
                            cachedEtag = fetchResult.etag
                            lastFetchedAtEpochMs = fetchNow
                            lastFailedRefreshAtEpochMs = 0L
                        }
                        runCatching {
                            AnimeIdBridgeStorage.save(
                                StoredAnimeIdBridgePayload(
                                    rawJson = fetchResult.rawJson,
                                    fetchedAtEpochMs = fetchNow,
                                    etag = fetchResult.etag,
                                    schemaVersion = ANIME_ID_BRIDGE_SCHEMA_VERSION,
                                ),
                            )
                        }
                    }
                    is FribbFetchResult.NotModified -> {
                        synchronized(stateLock) {
                            cachedEtag = fetchResult.etag
                            lastFetchedAtEpochMs = fetchNow
                            lastFailedRefreshAtEpochMs = 0L
                        }
                        runCatching {
                            AnimeIdBridgeStorage.touchFetchedAt(
                                fetchedAtEpochMs = fetchNow,
                                etag = fetchResult.etag,
                            )
                        }
                    }
                    FribbFetchResult.Failed -> {
                        synchronized(stateLock) {
                            lastFailedRefreshAtEpochMs = fetchNow
                        }
                    }
                }
            } finally {
                _initialLoadAttemptFinished.value = true
            }
        }
    }
}

private data class ParsedVideoIdParts(
    val baseContentId: String,
    val season: Int?,
    val episode: Int?,
)

private fun parseVideoIdParts(
    videoId: String,
    explicitSeason: Int?,
    explicitEpisode: Int?,
): ParsedVideoIdParts {
    val parts = videoId.split(':')
    if (videoId.startsWith("tt", ignoreCase = true)) {
        val base = parts[0]
        val season = explicitSeason ?: if (parts.size >= 3) parts[1].toIntOrNull()?.takeIf { it > 0 } else null
        val episode = explicitEpisode ?: when {
            parts.size >= 3 -> parts[2].toIntOrNull()?.takeIf { it > 0 }
            parts.size == 2 -> parts[1].toIntOrNull()?.takeIf { it > 0 }
            else -> null
        }
        return ParsedVideoIdParts(baseContentId = base, season = season, episode = episode)
    }

    val prefix = parts.firstOrNull()?.lowercase()
    if (parts.size >= 2 && prefix in VIDEO_ID_PREFIXES) {
        val base = "${parts[0]}:${parts[1]}"
        val season = explicitSeason ?: if (parts.size >= 4) parts[2].toIntOrNull()?.takeIf { it > 0 } else null
        val episode = explicitEpisode ?: when {
            parts.size >= 4 -> parts[3].toIntOrNull()?.takeIf { it > 0 }
            parts.size == 3 -> parts[2].toIntOrNull()?.takeIf { it > 0 }
            else -> null
        }
        return ParsedVideoIdParts(baseContentId = base, season = season, episode = episode)
    }

    return ParsedVideoIdParts(
        baseContentId = videoId,
        season = explicitSeason,
        episode = explicitEpisode,
    )
}

private fun JsonObject.positiveInt(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val parsed = primitive.intOrNull ?: primitive.content.trim().toIntOrNull() ?: return null
    return parsed.takeIf { it > 0 }
}

private fun JsonObject.positiveLong(key: String): Long? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    val parsed = primitive.longOrNull ?: primitive.content.trim().toLongOrNull() ?: return null
    return parsed.takeIf { it > 0L }
}

private fun JsonObject.nonBlankString(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    if (primitive is JsonNull) return null
    return primitive.content.trim().takeIf { it.isNotEmpty() }
}

private fun JsonObject.normalizedImdbId(key: String): String? {
    val normalized = nonBlankString(key)?.lowercase() ?: return null
    if (!normalized.startsWith("tt") || normalized.length <= 2) return null
    if (!normalized.drop(2).all(Char::isDigit)) return null
    return normalized
}

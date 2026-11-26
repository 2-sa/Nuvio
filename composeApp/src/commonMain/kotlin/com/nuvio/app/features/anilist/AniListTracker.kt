package com.nuvio.app.features.anilist

import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibrarySection
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.tracking.TrackingAuthProvider
import com.nuvio.app.features.tracking.TrackingCapability
import com.nuvio.app.features.tracking.TrackingHistoryItem
import com.nuvio.app.features.tracking.TrackingHistoryWriter
import com.nuvio.app.features.tracking.TrackingLibraryProvider
import com.nuvio.app.features.tracking.TrackingLibrarySnapshot
import com.nuvio.app.features.tracking.TrackingLibraryTab
import com.nuvio.app.features.tracking.TrackingLibraryTabKind
import com.nuvio.app.features.tracking.TrackingListStatus
import com.nuvio.app.features.tracking.TrackingListWriter
import com.nuvio.app.features.tracking.TrackingMediaKind
import com.nuvio.app.features.tracking.TrackingMediaReference
import com.nuvio.app.features.tracking.TrackingMembershipResolution
import com.nuvio.app.features.tracking.TrackingMutationResult
import com.nuvio.app.features.tracking.TrackingProgressProvider
import com.nuvio.app.features.tracking.TrackingProgressSnapshot
import com.nuvio.app.features.tracking.TrackingProviderDescriptor
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.tracking.TrackingProviderRegistry
import com.nuvio.app.features.tracking.TrackingRatingProvider
import com.nuvio.app.features.tracking.TrackingRefreshIntent
import com.nuvio.app.features.tracking.TrackingScrobbleAction
import com.nuvio.app.features.tracking.TrackingScrobbleEvent
import com.nuvio.app.features.tracking.TrackingScrobbler
import com.nuvio.app.features.tracking.TrackingWatchedProvider
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import com.nuvio.app.features.tracking.parseTrackingExternalIds
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watched.watchedItemKeys
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.buildPlaybackVideoId
import kotlin.time.Clock
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class AniListSyncUiState(
    val snapshot: AniListSyncSnapshot? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

object AniListTracker : TrackingAuthProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = SynchronizedObject()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val apiClient = AniListApiClient()

    private val activeProfile = MutableStateFlow(ProfileRepository.activeProfileId)
    private val _authState = MutableStateFlow(loadAuthState(activeProfile.value))
    val authState: StateFlow<AniListAuthState> = _authState.asStateFlow()

    private val _syncState = MutableStateFlow(loadSyncUiState(activeProfile.value))
    val syncState: StateFlow<AniListSyncUiState> = _syncState.asStateFlow()

    private val authenticated = MutableStateFlow(_authState.value.isAuthenticated)
    override val isAuthenticated: StateFlow<Boolean> = authenticated.asStateFlow()

    private var generationCounter = 0L
    override val accountGeneration: Long
        get() = synchronized(lock) { generationCounter }

    override val descriptor = TrackingProviderDescriptor(
        id = TrackingProviderId.ANILIST,
        displayName = "AniList",
        capabilities = setOf(
            TrackingCapability.AUTHENTICATION,
            TrackingCapability.LIBRARY_READ,
            TrackingCapability.LIBRARY_WRITE,
            TrackingCapability.WATCHED_READ,
            TrackingCapability.WATCHED_WRITE,
            TrackingCapability.PROGRESS_READ,
            TrackingCapability.PROGRESS_WRITE,
            TrackingCapability.SCROBBLE,
            TrackingCapability.RATINGS,
        ),
    )

    val libraryProvider: TrackingLibraryProvider = AniListLibraryProvider()
    val watchedProvider: TrackingWatchedProvider = AniListWatchedProvider()
    val progressProvider: TrackingProgressProvider = AniListProgressProvider()
    val writes = AniListTrackingWrites()
    val ratingProvider: TrackingRatingProvider = AniListRatingProvider()

    fun register() {
        if (TrackingProviderRegistry.authProvider(providerId) === this) return
        TrackingProviderRegistry.register(this)
        TrackingProviderRegistry.registerLibraryProvider(libraryProvider)
        TrackingProviderRegistry.registerWatchedProvider(watchedProvider)
        TrackingProviderRegistry.registerProgressProvider(progressProvider)
        TrackingProviderRegistry.registerHistoryWriter(writes)
        TrackingProviderRegistry.registerScrobbler(writes)
        TrackingProviderRegistry.registerListWriter(writes)
        TrackingProviderRegistry.registerRatingProvider(ratingProvider)
    }

    override fun ensureLoaded() {
        val currentProfile = ProfileRepository.activeProfileId
        if (activeProfile.value != currentProfile) {
            onProfileChanged()
        } else {
            authenticated.value = _authState.value.isAuthenticated
        }
    }

    override fun onProfileChanged() {
        val nextProfile = ProfileRepository.activeProfileId
        synchronized(lock) { generationCounter++ }
        activeProfile.value = nextProfile
        val loadedAuth = loadAuthState(nextProfile)
        _authState.value = loadedAuth
        _syncState.value = loadSyncUiState(nextProfile)
        authenticated.value = loadedAuth.isAuthenticated
    }

    override fun clearLocalState() {
        synchronized(lock) { generationCounter++ }
        AniListStorage.clearAll()
        val profileId = ProfileRepository.activeProfileId
        activeProfile.value = profileId
        _authState.value = AniListAuthState(profileId = profileId)
        _syncState.value = AniListSyncUiState()
        authenticated.value = false
    }

    override fun removeStoredProfile(profileId: Int) {
        AniListStorage.removeProfile(profileId)
        if (profileId == activeProfile.value) {
            synchronized(lock) { generationCounter++ }
            _authState.value = AniListAuthState(profileId = profileId)
            _syncState.value = AniListSyncUiState()
            authenticated.value = false
        }
    }

    override fun handleAuthCallback(url: String): Boolean {
        val token = AniListAuthCallbackParser.parseAccessTokenFromCallbackUrl(url) ?: return false
        coroutineScope.launch {
            connectWithToken(token)
        }
        return true
    }

    fun hasKnownMedia(contentId: String?, title: String? = null): Boolean {
        val entries = _syncState.value.snapshot?.entries.orEmpty()
        if (entries.isEmpty()) return false
        val parsed = contentId?.let(::parseTrackingExternalIds)
        if (parsed?.anilist?.toIntOrNull()?.let { id -> entries.any { it.mediaId == id } } == true) return true
        if (parsed?.mal?.toIntOrNull()?.let { id -> entries.any { it.malId == id } } == true) return true
        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: return false
        return entries.any {
            it.titleEnglish.equals(normalizedTitle, ignoreCase = true) ||
                it.titleRomaji.equals(normalizedTitle, ignoreCase = true)
        }
    }

    fun setClientId(clientId: String) {
        ensureLoaded()
        val profileId = activeProfile.value
        val updated = _authState.value.copy(
            profileId = profileId,
            clientId = clientId.trim(),
            error = null,
        )
        persistAuthState(profileId, updated)
    }

    suspend fun connectWithToken(rawToken: String) {
        ensureLoaded()
        val profileId = activeProfile.value
        val normalized = AniListAuthCallbackParser.normalizeDirectToken(rawToken)
        if (normalized == null) {
            val failed = _authState.value.copy(
                profileId = profileId,
                isBusy = false,
                error = "Enter a valid AniList access token",
            )
            _authState.value = failed
            return
        }

        _authState.value = _authState.value.copy(profileId = profileId, isBusy = true, error = null)
        try {
            val viewer = apiClient.fetchViewer(normalized)
            synchronized(lock) { generationCounter++ }
            val connected = _authState.value.copy(
                profileId = profileId,
                accessToken = normalized,
                user = viewer,
                isBusy = false,
                error = null,
            )
            persistAuthState(profileId, connected)
            syncNow(TrackingRefreshIntent.USER_INITIATED)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _authState.value = _authState.value.copy(
                profileId = profileId,
                isBusy = false,
                error = error.message ?: "AniList authentication failed",
            )
        }
    }

    suspend fun disconnect() {
        ensureLoaded()
        val profileId = activeProfile.value
        synchronized(lock) { generationCounter++ }
        val preservedClientId = _authState.value.clientId
        AniListStorage.removeProfile(profileId)
        val disconnected = AniListAuthState(
            profileId = profileId,
            clientId = preservedClientId,
        )
        persistAuthState(profileId, disconnected)
        _syncState.value = AniListSyncUiState()
    }

    suspend fun syncNow(intent: TrackingRefreshIntent = TrackingRefreshIntent.AUTOMATIC) {
        ensureLoaded()
        val profileId = activeProfile.value
        val auth = _authState.value
        val token = auth.accessToken?.takeIf { it.isNotBlank() } ?: return
        val user = auth.user ?: return

        if (intent == TrackingRefreshIntent.AUTOMATIC && _syncState.value.snapshot != null) {
            val ageMs = Clock.System.now().toEpochMilliseconds() - _syncState.value.snapshot!!.checkedAtEpochMs
            if (ageMs in 0 until 60_000L) return
        }

        _syncState.value = _syncState.value.copy(isLoading = true, error = null)
        try {
            val entries = apiClient.fetchAnimeLibrary(token, user.id)
            if (activeProfile.value != profileId) return
            val activeSeries = selectAniListContinueWatchingEntries(entries)
            if (activeSeries.isNotEmpty()) {
                val semaphore = Semaphore(4)
                coroutineScope {
                    for (entry in activeSeries) {
                        launch {
                            semaphore.withPermit {
                                AniListEpisodeMapper.resolveAddonEpisodes("anilist:${entry.mediaId}")
                            }
                        }
                    }
                }
            }
            if (activeProfile.value != profileId) return
            val snapshot = AniListSyncSnapshot(
                user = user,
                entries = entries,
                checkedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
            saveSyncSnapshot(profileId, snapshot)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (activeProfile.value == profileId) {
                _syncState.value = _syncState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to sync AniList library",
                )
            }
        }
    }

    private fun loadAuthState(profileId: Int): AniListAuthState {
        val raw = AniListStorage.loadAuthPayload(profileId) ?: return AniListAuthState(profileId = profileId)
        return runCatching {
            json.decodeFromString<AniListAuthState>(raw).copy(profileId = profileId, isBusy = false)
        }.getOrElse { AniListAuthState(profileId = profileId) }
    }

    private fun persistAuthState(profileId: Int, state: AniListAuthState) {
        if (profileId != activeProfile.value) return
        _authState.value = state
        authenticated.value = state.isAuthenticated
        AniListStorage.saveAuthPayload(profileId, json.encodeToString(state.copy(isBusy = false, error = null)))
    }

    private fun loadSyncUiState(profileId: Int): AniListSyncUiState {
        val raw = AniListStorage.loadSyncPayload(profileId)
            ?.takeIf { it.isNotBlank() }
            ?: return AniListSyncUiState()
        val snapshot = runCatching { json.decodeFromString<AniListSyncSnapshot>(raw) }.getOrNull()
        return AniListSyncUiState(snapshot = snapshot)
    }

    private fun saveSyncSnapshot(profileId: Int, snapshot: AniListSyncSnapshot) {
        if (profileId != activeProfile.value) return
        _syncState.value = AniListSyncUiState(snapshot = snapshot, isLoading = false, error = null)
        AniListStorage.saveSyncPayload(profileId, json.encodeToString(snapshot))
    }

    private fun upsertLocalEntry(profileId: Int, entry: AniListMediaEntry) {
        if (profileId != activeProfile.value) return
        val current = _syncState.value.snapshot ?: return
        val updatedEntries = current.entries.filterNot { it.mediaId == entry.mediaId } + entry
        saveSyncSnapshot(
            profileId,
            current.copy(
                entries = updatedEntries,
                checkedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun removeLocalEntry(profileId: Int, mediaId: Int) {
        if (profileId != activeProfile.value) return
        val current = _syncState.value.snapshot ?: return
        saveSyncSnapshot(
            profileId,
            current.copy(
                entries = current.entries.filterNot { it.mediaId == mediaId },
                checkedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun requireToken(profileId: Int): String? {
        ensureLoaded()
        if (profileId != activeProfile.value) throw CancellationException("AniList profile changed")
        return _authState.value.accessToken?.takeIf { it.isNotBlank() }
    }

    private fun findEntryForReference(reference: TrackingMediaReference): AniListMediaEntry? {
        val entries = _syncState.value.snapshot?.entries.orEmpty()
        reference.ids.anilist?.toInt()?.let { anilistId ->
            entries.firstOrNull { it.mediaId == anilistId }?.let { return it }
        }
        reference.ids.mal?.toInt()?.let { malId ->
            entries.firstOrNull { it.malId == malId }?.let { return it }
        }
        val title = reference.title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return entries.firstOrNull { entry ->
            entry.titleEnglish.equals(title, ignoreCase = true) ||
                entry.titleRomaji.equals(title, ignoreCase = true)
        }
    }

    private fun findEntryByContentId(contentId: String): AniListMediaEntry? {
        val entries = _syncState.value.snapshot?.entries.orEmpty()
        val parsed = parseTrackingExternalIds(contentId)
        parsed.anilist?.toInt()?.let { anilistId ->
            entries.firstOrNull { it.mediaId == anilistId }?.let { return it }
        }
        parsed.mal?.toInt()?.let { malId ->
            entries.firstOrNull { it.malId == malId }?.let { return it }
        }
        return null
    }

    private suspend fun resolveMediaId(token: String?, reference: TrackingMediaReference): Int? {
        reference.ids.anilist?.toInt()?.takeIf { it > 0 }?.let { return it }
        findEntryForReference(reference)?.let { return it.mediaId }
        return apiClient.searchMediaIdByTitleOrMal(
            accessToken = token,
            title = reference.title,
            malId = reference.ids.mal?.toInt(),
        )
    }

    private suspend fun resolveMediaIdForItem(token: String?, item: LibraryItem): Int? {
        val parsed = parseTrackingExternalIds(item.id)
        parsed.anilist?.toInt()?.takeIf { it > 0 }?.let { return it }
        findEntryByContentId(item.id)?.let { return it.mediaId }
        return apiClient.searchMediaIdByTitleOrMal(
            accessToken = token,
            title = item.name,
            malId = parsed.mal?.toInt(),
        )
    }

    private class AniListLibraryProvider : TrackingLibraryProvider {
        override val providerId = TrackingProviderId.ANILIST
        override val changes: Flow<Unit> = combine(_syncState, _authState) { _, _ -> Unit }
        override val connectionRefreshIntent = TrackingRefreshIntent.AUTOMATIC

        override fun ensureLoaded() = AniListTracker.ensureLoaded()
        override fun onProfileChanged() = AniListTracker.onProfileChanged()
        override fun clearLocalState() = AniListTracker.clearLocalState()

        override suspend fun refresh(intent: TrackingRefreshIntent) {
            syncNow(intent)
        }

        override fun snapshot(): TrackingLibrarySnapshot {
            val uiState = _syncState.value
            val entries = uiState.snapshot?.entries.orEmpty()
            val tabs = ANILIST_TABS
            val items = entries.map { entry -> entry.toLibraryItem() }
            val sections = tabs.mapNotNull { tab ->
                val tabItems = items.filter { tab.key in it.listKeys }
                if (tabItems.isEmpty()) null else LibrarySection(type = tab.key, displayTitle = tab.title, items = tabItems)
            }
            return TrackingLibrarySnapshot(
                items = items,
                sections = sections,
                tabs = tabs,
                hasLoaded = uiState.snapshot != null,
                isLoading = uiState.isLoading,
                errorMessage = uiState.error,
            )
        }

        override fun contains(contentId: String, contentType: String?): Boolean =
            findEntryByContentId(contentId) != null

        override fun find(contentId: String): LibraryItem? =
            findEntryByContentId(contentId)?.toLibraryItem()

        override suspend fun membership(item: LibraryItem): Map<String, Boolean> {
            val entry = findEntryByContentId(item.id)
                ?: findEntryForReference(
                    buildTrackingMediaReference(
                        contentType = item.type,
                        parentMetaId = item.id,
                        title = item.name,
                        releaseInfo = item.releaseInfo,
                    ),
                )
            val activeKey = entry?.userStatus?.let(::statusTabKey)
            return ANILIST_TABS.associate { tab -> tab.key to (tab.key == activeKey) }
        }

        override fun toggledDefaultMembership(currentMembership: Map<String, Boolean>): Map<String, Boolean> {
            val anySelected = currentMembership.values.any { it }
            return ANILIST_TABS.associate { tab ->
                tab.key to (!anySelected && tab.key == statusTabKey(AniListListStatus.PLANNING))
            }
        }

        override suspend fun applyMembership(
            profileId: Int,
            item: LibraryItem,
            desiredMembership: Map<String, Boolean>,
            destructiveRemovalConfirmed: Boolean,
        ): TrackingMembershipResolution? {
            val token = requireToken(profileId) ?: return null
            val desiredStatus = desiredMembership.entries
                .firstOrNull { it.value }
                ?.key
                ?.let(::tabKeyToStatus)
            val existingEntry = findEntryByContentId(item.id)
            val mediaId = existingEntry?.mediaId ?: resolveMediaIdForItem(token, item) ?: return null

            if (desiredStatus != null) {
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = mediaId,
                    status = desiredStatus,
                )
                if (saved != null) upsertLocalEntry(profileId, saved)
            } else if (existingEntry != null && existingEntry.entryId > 0) {
                if (apiClient.deleteMediaListEntry(token, existingEntry.entryId)) {
                    removeLocalEntry(profileId, existingEntry.mediaId)
                }
            }
            return null
        }
    }

    private class AniListWatchedProvider : TrackingWatchedProvider {
        override val providerId = TrackingProviderId.ANILIST

        override suspend fun pull(profileId: Int, pageSize: Int): List<WatchedItem> {
            requireToken(profileId) ?: return emptyList()
            syncNow(TrackingRefreshIntent.AUTOMATIC)
            if (activeProfile.value != profileId) throw CancellationException("AniList profile changed")
            val entries = _syncState.value.snapshot?.entries.orEmpty()
            val watched = mutableListOf<WatchedItem>()
            for (entry in entries) {
                if (entry.userStatus != AniListListStatus.CURRENT &&
                    entry.userStatus != AniListListStatus.REPEATING
                ) {
                    continue
                }
                if (entry.episodes != null && entry.progress >= entry.episodes) {
                    continue
                }
                val itemId = "anilist:${entry.mediaId}"
                val title = entry.displayTitle()
                val markedAt = entry.updatedAtSeconds * 1000L
                val isMovie = entry.format.equals("MOVIE", ignoreCase = true)
                if (!isMovie && entry.progress > 0) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes(itemId)
                    val mappedEpisodes = mapAbsoluteProgressToWatchedEpisodes(entry.progress, addonEpisodes)
                    for (ref in mappedEpisodes) {
                        watched += WatchedItem(
                            id = itemId,
                            type = "series",
                            name = title,
                            poster = entry.coverImageUrl,
                            releaseInfo = entry.seasonYear?.toString(),
                            season = ref.season,
                            episode = ref.episode,
                            videoId = ref.videoId ?: "$itemId:${ref.season}:${ref.episode}",
                            trackingProviderId = TrackingProviderId.ANILIST.storageId,
                            trackingProviderItemId = entry.mediaId.toString(),
                            markedAtEpochMs = markedAt,
                        )
                    }
                }
            }
            return watched
        }

        override suspend fun pullFullyWatchedSeriesKeys(profileId: Int): Set<String>? {
            if (profileId != activeProfile.value) return emptySet()
            val entries = _syncState.value.snapshot?.entries.orEmpty()
            return entries
                .asSequence()
                .filter { it.userStatus == AniListListStatus.COMPLETED && !it.format.equals("MOVIE", ignoreCase = true) }
                .flatMap { entry ->
                    watchedItemKeys(type = "series", id = "anilist:${entry.mediaId}").asSequence()
                }
                .toSet()
        }

        override suspend fun pullExtraWatchedKeys(profileId: Int): Set<String> {
            if (profileId != activeProfile.value) return emptySet()
            val entries = _syncState.value.snapshot?.entries.orEmpty()
            return entries
                .asSequence()
                .filter { it.userStatus == AniListListStatus.COMPLETED }
                .flatMap { entry ->
                    val type = if (entry.format.equals("MOVIE", ignoreCase = true)) "movie" else "series"
                    watchedItemKeys(type = type, id = "anilist:${entry.mediaId}").asSequence()
                }
                .toSet()
        }

        override suspend fun push(profileId: Int, items: Collection<WatchedItem>) {
            val token = requireToken(profileId) ?: return
            for (item in items) {
                val ref = buildTrackingMediaReference(
                    contentType = item.type,
                    parentMetaId = item.id,
                    videoId = item.videoId,
                    title = item.name,
                    releaseInfo = item.releaseInfo,
                    seasonNumber = item.season,
                    episodeNumber = item.episode,
                )
                val mediaId = resolveMediaId(token, ref) ?: continue
                val existing = findEntryForReference(ref)
                val isMovie = item.episode == null && item.type.equals("movie", ignoreCase = true)
                val absoluteEpisode = if (!isMovie && item.episode != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes("anilist:$mediaId")
                    mapSeasonEpisodeToAbsolute(item.season ?: 1, item.episode, addonEpisodes)
                } else {
                    null
                }
                val newProgress = if (isMovie) {
                    existing?.episodes ?: 1
                } else {
                    maxOf(existing?.progress ?: 0, absoluteEpisode ?: 1)
                }
                val newStatus = when {
                    isMovie -> AniListListStatus.COMPLETED
                    existing?.episodes != null && newProgress >= existing.episodes -> AniListListStatus.COMPLETED
                    existing?.userStatus == null -> AniListListStatus.CURRENT
                    else -> existing.userStatus
                }
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = mediaId,
                    status = newStatus,
                    progress = newProgress,
                )
                if (saved != null) upsertLocalEntry(profileId, saved)
            }
        }

        override suspend fun delete(profileId: Int, items: Collection<WatchedItem>) {
            val token = requireToken(profileId) ?: return
            for (item in items) {
                val ref = buildTrackingMediaReference(
                    contentType = item.type,
                    parentMetaId = item.id,
                    videoId = item.videoId,
                    title = item.name,
                    releaseInfo = item.releaseInfo,
                    seasonNumber = item.season,
                    episodeNumber = item.episode,
                )
                val existing = findEntryForReference(ref) ?: continue
                val ep = item.episode
                val absoluteEp = if (ep != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes("anilist:${existing.mediaId}")
                    mapSeasonEpisodeToAbsolute(item.season ?: 1, ep, addonEpisodes)
                } else {
                    null
                }
                if (absoluteEp == null || absoluteEp <= 1) {
                    if (existing.entryId > 0 && apiClient.deleteMediaListEntry(token, existing.entryId)) {
                        removeLocalEntry(profileId, existing.mediaId)
                    }
                } else {
                    val newProgress = (absoluteEp - 1).coerceAtLeast(0)
                    val saved = apiClient.saveMediaListEntry(
                        accessToken = token,
                        mediaId = existing.mediaId,
                        progress = newProgress,
                    )
                    if (saved != null) upsertLocalEntry(profileId, saved)
                }
            }
        }
    }

    private class AniListProgressProvider : TrackingProgressProvider {
        override val providerId = TrackingProviderId.ANILIST
        override val changes: Flow<Unit> = combine(_syncState, _authState) { _, _ -> Unit }

        override fun ensureLoaded() = AniListTracker.ensureLoaded()
        override fun onProfileChanged() = AniListTracker.onProfileChanged()
        override fun clearLocalState() = AniListTracker.clearLocalState()

        override suspend fun refresh(force: Boolean, sourceChanged: Boolean) {
            ensureLoaded()
            syncNow(if (force) TrackingRefreshIntent.USER_INITIATED else TrackingRefreshIntent.AUTOMATIC)
        }

        override fun snapshot(): TrackingProgressSnapshot {
            val uiState = _syncState.value
            val entries = selectAniListContinueWatchingEntries(uiState.snapshot?.entries.orEmpty())
                .map { entry ->
                    val contentId = "anilist:${entry.mediaId}"
                    val cachedEpisodes = AniListEpisodeMapper.getCached(contentId)
                    val mapped = mapAbsoluteEpisodeToSeasonEpisode(entry.progress, cachedEpisodes)
                    WatchProgressEntry(
                        contentType = "series",
                        parentMetaId = contentId,
                        parentMetaType = "series",
                        videoId = mapped.videoId ?: buildPlaybackVideoId(contentId, mapped.season, mapped.episode),
                        title = entry.displayTitle(),
                        poster = entry.coverImageUrl,
                        seasonNumber = mapped.season,
                        episodeNumber = mapped.episode,
                        lastPositionMs = 0L,
                        durationMs = (entry.duration ?: 24) * 60_000L,
                        lastUpdatedEpochMs = entry.updatedAtSeconds * 1000L,
                        isCompleted = true,
                        progressPercent = 100f,
                        source = TrackingProviderId.ANILIST.storageId,
                        trackingProviderId = TrackingProviderId.ANILIST.storageId,
                        trackingProviderItemId = entry.mediaId.toString(),
                    )
                }
            return TrackingProgressSnapshot(
                entries = entries,
                hasLoadedRemoteProgress = uiState.snapshot != null,
                errorMessage = uiState.error,
            )
        }

        override suspend fun removeProgress(entries: Collection<WatchProgressEntry>) {
            val token = requireToken(activeProfile.value) ?: return
            for (entry in entries.distinctBy { it.parentMetaId }) {
                val existing = findEntryByContentId(entry.parentMetaId) ?: continue
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = existing.mediaId,
                    progress = 0,
                )
                if (saved != null) upsertLocalEntry(activeProfile.value, saved)
            }
        }
    }

    class AniListTrackingWrites : TrackingHistoryWriter, TrackingScrobbler, TrackingListWriter {
        override val providerId = TrackingProviderId.ANILIST

        override suspend fun addToHistory(
            profileId: Int,
            items: Collection<TrackingHistoryItem>,
        ): TrackingMutationResult {
            val token = requireToken(profileId)
                ?: return TrackingMutationResult(attemptedCount = items.size, notFoundCount = items.size)
            var notFound = 0
            for (item in items) {
                val mediaId = resolveMediaId(token, item.media)
                if (mediaId == null) {
                    notFound++
                    continue
                }
                val existing = findEntryForReference(item.media)
                val episodeNum = item.media.episode?.number
                val absoluteEp = if (episodeNum != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes("anilist:$mediaId")
                    mapSeasonEpisodeToAbsolute(item.media.episode?.season ?: 1, episodeNum, addonEpisodes)
                } else {
                    null
                }
                val newProgress = if (absoluteEp != null) {
                    maxOf(existing?.progress ?: 0, absoluteEp)
                } else {
                    existing?.episodes ?: 1
                }
                val newStatus = when {
                    item.media.kind == TrackingMediaKind.MOVIE -> AniListListStatus.COMPLETED
                    existing?.episodes != null && newProgress >= existing.episodes -> AniListListStatus.COMPLETED
                    existing?.userStatus == null -> AniListListStatus.CURRENT
                    else -> existing.userStatus
                }
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = mediaId,
                    status = newStatus,
                    progress = newProgress,
                )
                if (saved != null) upsertLocalEntry(profileId, saved)
            }
            return TrackingMutationResult(attemptedCount = items.size, notFoundCount = notFound)
        }

        override suspend fun removeFromHistory(
            profileId: Int,
            items: Collection<TrackingMediaReference>,
        ): TrackingMutationResult {
            val token = requireToken(profileId)
                ?: return TrackingMutationResult(attemptedCount = items.size, notFoundCount = items.size)
            var notFound = 0
            for (media in items) {
                val existing = findEntryForReference(media)
                if (existing == null) {
                    notFound++
                    continue
                }
                val ep = media.episode?.number
                val absoluteEp = if (ep != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes("anilist:${existing.mediaId}")
                    mapSeasonEpisodeToAbsolute(media.episode?.season ?: 1, ep, addonEpisodes)
                } else {
                    null
                }
                if (absoluteEp == null || absoluteEp <= 1) {
                    if (existing.entryId > 0 && apiClient.deleteMediaListEntry(token, existing.entryId)) {
                        removeLocalEntry(profileId, existing.mediaId)
                    }
                } else {
                    val saved = apiClient.saveMediaListEntry(
                        accessToken = token,
                        mediaId = existing.mediaId,
                        progress = (absoluteEp - 1).coerceAtLeast(0),
                    )
                    if (saved != null) upsertLocalEntry(profileId, saved)
                }
            }
            return TrackingMutationResult(attemptedCount = items.size, notFoundCount = notFound)
        }

        override suspend fun scrobble(
            profileId: Int,
            action: TrackingScrobbleAction,
            event: TrackingScrobbleEvent,
        ) {
            if (action != TrackingScrobbleAction.STOP || event.progressPercent < 80.0) return
            val token = requireToken(profileId) ?: return
            val mediaId = resolveMediaId(token, event.media) ?: return
            val existing = findEntryForReference(event.media)
            val episodeNum = event.media.episode?.number
            val absoluteEp = if (episodeNum != null) {
                val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes("anilist:$mediaId")
                mapSeasonEpisodeToAbsolute(event.media.episode?.season ?: 1, episodeNum, addonEpisodes)
            } else {
                null
            }
            val newProgress = if (absoluteEp != null) {
                maxOf(existing?.progress ?: 0, absoluteEp)
            } else {
                existing?.episodes ?: 1
            }
            val newStatus = when {
                event.media.kind == TrackingMediaKind.MOVIE -> AniListListStatus.COMPLETED
                existing?.episodes != null && newProgress >= existing.episodes -> AniListListStatus.COMPLETED
                existing?.userStatus == null -> AniListListStatus.CURRENT
                else -> existing.userStatus
            }
            val saved = apiClient.saveMediaListEntry(
                accessToken = token,
                mediaId = mediaId,
                status = newStatus,
                progress = newProgress,
            )
            if (saved != null) upsertLocalEntry(profileId, saved)
        }

        override suspend fun moveToList(
            profileId: Int,
            items: Collection<TrackingMediaReference>,
            destination: TrackingListStatus,
        ): TrackingMutationResult {
            val token = requireToken(profileId)
                ?: return TrackingMutationResult(attemptedCount = items.size, notFoundCount = items.size)
            val targetStatus = destination.toAniListStatus()
            var notFound = 0
            for (media in items) {
                val mediaId = resolveMediaId(token, media)
                if (mediaId == null) {
                    notFound++
                    continue
                }
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = mediaId,
                    status = targetStatus,
                )
                if (saved != null) upsertLocalEntry(profileId, saved)
            }
            return TrackingMutationResult(attemptedCount = items.size, notFoundCount = notFound)
        }

        override suspend fun removeFromList(
            profileId: Int,
            items: Collection<TrackingMediaReference>,
        ): TrackingMutationResult {
            val token = requireToken(profileId)
                ?: return TrackingMutationResult(attemptedCount = items.size, notFoundCount = items.size)
            var notFound = 0
            for (media in items) {
                val existing = findEntryForReference(media)
                if (existing == null || existing.entryId <= 0) {
                    notFound++
                    continue
                }
                if (apiClient.deleteMediaListEntry(token, existing.entryId)) {
                    removeLocalEntry(profileId, existing.mediaId)
                }
            }
            return TrackingMutationResult(attemptedCount = items.size, notFoundCount = notFound)
        }
    }

    private class AniListRatingProvider : TrackingRatingProvider {
        override val providerId = TrackingProviderId.ANILIST
        override val changes: Flow<Any?> = _syncState

        override fun supports(media: TrackingMediaReference): Boolean =
            media.kind == TrackingMediaKind.ANIME ||
                media.ids.anilist != null ||
                media.ids.mal != null

        override fun rating(media: TrackingMediaReference): Double? {
            val entry = findEntryForReference(media) ?: return null
            return entry.userScore.takeIf { it > 0 }?.let { it / 10.0 }
        }

        override suspend fun rate(
            profileId: Int,
            media: TrackingMediaReference,
            rating: Int?,
        ): TrackingMutationResult {
            val token = requireToken(profileId)
                ?: return TrackingMutationResult(attemptedCount = 1, notFoundCount = 1)
            val mediaId = resolveMediaId(token, media)
                ?: return TrackingMutationResult(attemptedCount = 1, notFoundCount = 1)
            val scoreRaw = (rating ?: 0) * 10
            val saved = apiClient.saveMediaListEntry(
                accessToken = token,
                mediaId = mediaId,
                scoreRaw = scoreRaw,
            )
            if (saved != null) upsertLocalEntry(profileId, saved)
            return TrackingMutationResult(attemptedCount = 1, notFoundCount = 0)
        }
    }
}

private val ANILIST_TABS = listOf(
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.CURRENT),
        title = "Watching",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
        selectionGroup = "anilist_status",
    ),
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.PLANNING),
        title = "Planning",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.WATCHLIST,
        selectionGroup = "anilist_status",
    ),
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.COMPLETED),
        title = "Completed",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
        selectionGroup = "anilist_status",
    ),
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.PAUSED),
        title = "Paused",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
        selectionGroup = "anilist_status",
    ),
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.DROPPED),
        title = "Dropped",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
        selectionGroup = "anilist_status",
    ),
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.REPEATING),
        title = "Repeating",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
        selectionGroup = "anilist_status",
    ),
)

private fun statusTabKey(status: AniListListStatus): String =
    "anilist:${status.name.lowercase()}"

private fun tabKeyToStatus(key: String): AniListListStatus? =
    AniListListStatus.entries.firstOrNull { statusTabKey(it) == key }

private fun TrackingListStatus.toAniListStatus(): AniListListStatus = when (this) {
    TrackingListStatus.WATCHING -> AniListListStatus.CURRENT
    TrackingListStatus.PLAN_TO_WATCH -> AniListListStatus.PLANNING
    TrackingListStatus.ON_HOLD -> AniListListStatus.PAUSED
    TrackingListStatus.COMPLETED -> AniListListStatus.COMPLETED
    TrackingListStatus.DROPPED -> AniListListStatus.DROPPED
}

private fun AniListMediaEntry.displayTitle(): String =
    titleEnglish?.takeIf { it.isNotBlank() } ?: titleRomaji

private fun AniListMediaEntry.toLibraryItem(): LibraryItem {
    val tabKey = statusTabKey(userStatus ?: AniListListStatus.CURRENT)
    return LibraryItem(
        id = "anilist:$mediaId",
        type = if (format.equals("MOVIE", ignoreCase = true)) "movie" else "series",
        name = displayTitle(),
        poster = coverImageUrl,
        releaseInfo = seasonYear?.toString(),
        imdbRating = averageScore?.let { (it / 10.0).toString() },
        listKeys = setOf(tabKey),
        mediaCategory = "anime",
        trackingProviderId = TrackingProviderId.ANILIST.storageId,
        trackingProviderItemId = mediaId.toString(),
        savedAtEpochMs = updatedAtSeconds * 1000L,
    )
}

internal fun selectAniListContinueWatchingEntries(
    entries: List<AniListMediaEntry>,
): List<AniListMediaEntry> =
    entries
        .asSequence()
        .filter { entry ->
            !entry.format.equals("MOVIE", ignoreCase = true) &&
                entry.progress > 0 &&
                (entry.userStatus == AniListListStatus.CURRENT || entry.userStatus == AniListListStatus.REPEATING) &&
                (entry.episodes == null || entry.progress < entry.episodes)
        }
        .sortedByDescending { it.updatedAtSeconds }
        .toList()

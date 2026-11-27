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
import com.nuvio.app.features.tracking.TrackingSettingsRepository
import com.nuvio.app.features.tracking.TrackingWatchedProvider
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.tracking.buildTrackingMediaReference
import com.nuvio.app.features.tracking.effectiveWatchProgressSource
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val optimisticallyRemovedVideoIds = mutableSetOf<String>()
    private val scrobbleDeduplicator = AniListScrobbleDeduplicator()
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
        if (authenticated.value) {
            AnimeIdBridge.ensureLoaded()
        }
    }

    override fun onProfileChanged() {
        val nextProfile = ProfileRepository.activeProfileId
        synchronized(lock) {
            generationCounter++
            optimisticallyRemovedVideoIds.clear()
        }
        scrobbleDeduplicator.clear()
        activeProfile.value = nextProfile
        val loadedAuth = loadAuthState(nextProfile)
        _authState.value = loadedAuth
        _syncState.value = loadSyncUiState(nextProfile)
        authenticated.value = loadedAuth.isAuthenticated
    }

    override fun clearLocalState() {
        synchronized(lock) {
            generationCounter++
            optimisticallyRemovedVideoIds.clear()
        }
        scrobbleDeduplicator.clear()
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
            synchronized(lock) {
                generationCounter++
                optimisticallyRemovedVideoIds.clear()
            }
            scrobbleDeduplicator.clear()
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
        if (!contentId.isNullOrBlank() && findEntryByContentId(contentId) != null) return true
        val normalizedTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: return false
        return entries.any {
            it.titleEnglish.equals(normalizedTitle, ignoreCase = true) ||
                it.titleRomaji.equals(normalizedTitle, ignoreCase = true)
        }
    }

    internal fun findActiveContinueWatchingEntry(contentId: String): AniListMediaEntry? {
        val entries = _syncState.value.snapshot?.entries.orEmpty()
        if (entries.isEmpty()) return null
        val active = selectAniListContinueWatchingEntries(entries)
        if (active.isEmpty()) return null
        val index = AnimeIdBridge.currentIndex()
        val normalizedTarget = contentId.trim()
        if (normalizedTarget.isEmpty()) return null

        active.firstOrNull { entry ->
            val canonical = AnimeIdBridge.resolveForEntry(entry, index).canonicalContentId()
                ?: "anilist:${entry.mediaId}"
            canonical.equals(normalizedTarget, ignoreCase = true)
        }?.let { return it }

        val parsed = parseTrackingExternalIds(normalizedTarget)
        parsed.anilist?.toInt()?.let { id ->
            active.firstOrNull { it.mediaId == id }?.let { return it }
        }
        parsed.mal?.toInt()?.let { id ->
            active.firstOrNull { it.malId == id }?.let { return it }
        }
        AnimeIdBridge.resolveByContentId(normalizedTarget, index)?.let { mapping ->
            active.firstOrNull { entry ->
                (mapping.anilistId != null && entry.mediaId == mapping.anilistId) ||
                    (mapping.malId != null && entry.malId == mapping.malId)
            }?.let { return it }
        }
        val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(normalizedTarget, index)
        if (franchiseMappings.isNotEmpty()) {
            val franchiseAniListIds = franchiseMappings.mapNotNullTo(mutableSetOf()) { it.anilistId }
            val franchiseMalIds = franchiseMappings.mapNotNullTo(mutableSetOf()) { it.malId }
            active.firstOrNull { entry ->
                entry.mediaId in franchiseAniListIds ||
                    (entry.malId != null && entry.malId in franchiseMalIds)
            }?.let { return it }
        }
        return null
    }

    internal fun isActiveContinueWatchingContentId(contentId: String): Boolean =
        findActiveContinueWatchingEntry(contentId) != null

    internal fun markEpisodeOptimisticallyUnwatched(videoId: String?) {
        val normalized = videoId?.trim()?.takeIf { it.isNotEmpty() } ?: return
        synchronized(lock) {
            optimisticallyRemovedVideoIds += normalized
        }
    }

    internal fun isAnimeEpisodeWatched(
        metaId: String,
        videoId: String?,
        seasonNumber: Int?,
        episodeNumber: Int,
    ): Boolean {
        val effectiveSource = effectiveWatchProgressSource(
            requestedSource = TrackingSettingsRepository.uiState.value.watchProgressSource,
            isProviderAuthenticated = TrackingProviderRegistry::isAuthenticated,
        )
        if (
            !isAniListWatchedLookupActive(
                isAniListAuthenticated = isAuthenticated.value,
                effectiveSource = effectiveSource,
                useAniListForAnime = TrackingSettingsRepository.uiState.value.useAniListForAnime,
            )
        ) {
            return false
        }
        val removedIds = synchronized(lock) { optimisticallyRemovedVideoIds.toSet() }
        return isAniListEntryEpisodeWatched(
            entries = _syncState.value.snapshot?.entries.orEmpty(),
            metaId = metaId,
            videoId = videoId,
            episodeNumber = episodeNumber,
            seasonNumber = seasonNumber,
            bridgeIndex = AnimeIdBridge.currentIndex(),
            optimisticallyRemovedVideoIds = removedIds,
        )
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
        synchronized(lock) {
            generationCounter++
            optimisticallyRemovedVideoIds.clear()
        }
        scrobbleDeduplicator.clear()
        val preservedClientId = _authState.value.clientId
        AniListStorage.removeProfile(profileId)
        AniListEpisodeMapper.clearCache()
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
            AnimeIdBridge.ensureLoaded()
            val bridgeIndex = AnimeIdBridge.awaitReady(5_000L)
            val entries = apiClient.fetchAnimeLibrary(token, user.id)
            if (activeProfile.value != profileId) return
            val activeSeries = selectAniListContinueWatchingEntries(entries)
            if (activeSeries.isNotEmpty()) {
                val semaphore = Semaphore(4)
                coroutineScope {
                    for (entry in activeSeries) {
                        launch {
                            semaphore.withPermit {
                                val mapping = AnimeIdBridge.resolveForEntry(entry, bridgeIndex)
                                val canonicalId = mapping.canonicalContentId() ?: "anilist:${entry.mediaId}"
                                AniListEpisodeMapper.resolveAddonEpisodes(
                                    listOfNotNull(
                                        canonicalId,
                                        mapping.kitsuId?.let { "kitsu:$it" },
                                        mapping.imdbId,
                                        "anilist:${entry.mediaId}",
                                    ),
                                )
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
        synchronized(lock) {
            optimisticallyRemovedVideoIds.clear()
        }
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
        if (entries.isEmpty()) return null
        reference.ids.anilist?.toInt()?.let { anilistId ->
            entries.firstOrNull { it.mediaId == anilistId }?.let { return it }
        }
        reference.ids.mal?.toInt()?.let { malId ->
            entries.firstOrNull { it.malId == malId }?.let { return it }
        }
        val candidateIds = listOfNotNull(
            reference.catalog?.videoId,
            reference.catalog?.contentId,
            reference.ids.kitsu?.let { "kitsu:$it" },
            reference.ids.anidb?.let { "anidb:$it" },
            reference.ids.imdb,
            reference.ids.tmdb?.let { "tmdb:$it" },
            reference.ids.tvdb?.let { "tvdb:$it" },
        )
        for (candidateId in candidateIds) {
            findEntryByContentId(candidateId)?.let { return it }
        }
        val title = reference.title?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return entries.firstOrNull { entry ->
            entry.titleEnglish.equals(title, ignoreCase = true) ||
                entry.titleRomaji.equals(title, ignoreCase = true)
        }
    }

    private fun findEntryByContentId(
        contentId: String,
        index: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
    ): AniListMediaEntry? {
        val entries = _syncState.value.snapshot?.entries.orEmpty()
        if (entries.isEmpty()) return null
        val parsed = parseTrackingExternalIds(contentId)
        parsed.anilist?.toInt()?.let { anilistId ->
            entries.firstOrNull { it.mediaId == anilistId }?.let { return it }
        }
        parsed.mal?.toInt()?.let { malId ->
            entries.firstOrNull { it.malId == malId }?.let { return it }
        }
        val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(contentId, index)
        if (franchiseMappings.isNotEmpty()) {
            val matchedEntries = franchiseMappings.mapNotNull { mapping ->
                entries.firstOrNull { entry ->
                    (mapping.anilistId != null && entry.mediaId == mapping.anilistId) ||
                        (mapping.malId != null && entry.malId == mapping.malId)
                }
            }.distinctBy { it.mediaId }
            if (matchedEntries.isNotEmpty()) {
                return matchedEntries.firstOrNull {
                    it.userStatus == AniListListStatus.CURRENT || it.userStatus == AniListListStatus.REPEATING
                } ?: matchedEntries.first()
            }
        }
        AnimeIdBridge.resolveByContentId(contentId, index)?.let { mapping ->
            entries.firstOrNull { entry ->
                (mapping.anilistId != null && entry.mediaId == mapping.anilistId) ||
                    (mapping.malId != null && entry.malId == mapping.malId)
            }?.let { return it }
        }
        return null
    }

    private suspend fun resolveMediaId(token: String?, reference: TrackingMediaReference): Int? {
        reference.ids.anilist?.toInt()?.takeIf { it > 0 }?.let { return it }
        findEntryForReference(reference)?.let { return it.mediaId }
        val index = AnimeIdBridge.currentIndex()
        val perSeasonCandidates = listOfNotNull(
            reference.catalog?.videoId?.takeIf(::isPerSeasonContentId),
            reference.catalog?.contentId?.takeIf(::isPerSeasonContentId),
            reference.ids.mal?.let { "mal:$it" },
            reference.ids.kitsu?.let { "kitsu:$it" },
            reference.ids.anidb?.let { "anidb:$it" },
        )
        for (candidate in perSeasonCandidates) {
            AnimeIdBridge.resolveByContentId(candidate, index)?.anilistId?.takeIf { it > 0 }?.let { return it }
        }
        val franchiseCandidate = reference.catalog?.contentId
            ?: reference.ids.imdb
            ?: reference.ids.tmdb?.let { "tmdb:$it" }
            ?: reference.ids.tvdb?.let { "tvdb:$it" }
        if (franchiseCandidate != null) {
            val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(franchiseCandidate, index)
            if (franchiseMappings.size == 1) {
                franchiseMappings.first().anilistId?.takeIf { it > 0 }?.let { return it }
            }
        }
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
        AnimeIdBridge.resolveByContentId(item.id)?.anilistId?.takeIf { it > 0 }?.let { return it }
        return apiClient.searchMediaIdByTitleOrMal(
            accessToken = token,
            title = item.name,
            malId = parsed.mal?.toInt(),
        )
    }

    private class AniListLibraryProvider : TrackingLibraryProvider {
        override val providerId = TrackingProviderId.ANILIST
        override val changes: Flow<Unit> = combine(_syncState, _authState, AnimeIdBridge.indexState) { _, _, _ -> Unit }
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
            val bridgeIndex = AnimeIdBridge.currentIndex()
            val tabs = ANILIST_TABS
            val items = entries.map { entry -> entry.toLibraryItem(bridgeIndex) }
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
            findEntryByContentId(contentId)?.toLibraryItem(AnimeIdBridge.currentIndex())

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
                    scrobbleDeduplicator.invalidate(profileId, existingEntry.mediaId)
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
            val bridgeIndex = AnimeIdBridge.awaitReady(5_000L)
            val entries = _syncState.value.snapshot?.entries.orEmpty()
            val addonEpisodesByMediaId = mutableMapOf<Int, List<SeasonEpisodeRef>>()
            for (entry in entries) {
                val isActiveSeries = !entry.format.equals("MOVIE", ignoreCase = true) &&
                    entry.progress > 0 &&
                    (entry.userStatus == AniListListStatus.CURRENT || entry.userStatus == AniListListStatus.REPEATING) &&
                    (entry.episodes == null || entry.progress < entry.episodes)
                if (!isActiveSeries) continue
                val mapping = AnimeIdBridge.resolveForEntry(entry, bridgeIndex)
                val canonicalId = mapping.canonicalContentId() ?: "anilist:${entry.mediaId}"
                addonEpisodesByMediaId[entry.mediaId] = AniListEpisodeMapper.resolveAddonEpisodes(
                    listOfNotNull(
                        canonicalId,
                        mapping.kitsuId?.let { "kitsu:$it" },
                        mapping.imdbId,
                        "anilist:${entry.mediaId}",
                    ),
                )
            }
            return buildAniListWatchedItems(
                entries = entries,
                bridgeIndex = bridgeIndex,
                addonEpisodesForEntry = { entry, _, _ -> addonEpisodesByMediaId[entry.mediaId].orEmpty() },
            )
        }

        override suspend fun pullFullyWatchedSeriesKeys(profileId: Int): Set<String>? {
            if (profileId != activeProfile.value) return emptySet()
            val snapshot = _syncState.value.snapshot ?: return emptySet()
            return snapshot.fullyWatchedSeriesKeys(AnimeIdBridge.currentIndex())
        }

        override suspend fun pullExtraWatchedKeys(profileId: Int): Set<String> {
            if (profileId != activeProfile.value) return emptySet()
            val snapshot = _syncState.value.snapshot ?: return emptySet()
            return snapshot.animeAlternateWatchedKeys(AnimeIdBridge.currentIndex())
        }

        override fun observeExtraWatchedKeys(profileId: Int): Flow<Set<String>> =
            combine(_syncState, activeProfile, AnimeIdBridge.indexState) { uiState, currentProfile, bridgeIndex ->
                if (currentProfile != profileId) {
                    emptySet()
                } else {
                    uiState.snapshot?.animeAlternateWatchedKeys(bridgeIndex).orEmpty()
                }
            }.distinctUntilChanged()

        override fun observeWatchedChanges(profileId: Int): Flow<Unit> =
            combine(_syncState, activeProfile, AnimeIdBridge.indexState) { uiState, currentProfile, bridgeIndex ->
                if (currentProfile != profileId) {
                    null
                } else {
                    Pair(uiState.snapshot, bridgeIndex)
                }
            }
                .distinctUntilChanged()
                .map { Unit }

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
                val allEntries = _syncState.value.snapshot?.entries.orEmpty()
                val isMovie = item.episode == null && item.type.equals("movie", ignoreCase = true)
                val absoluteEpisode = if (!isMovie && item.episode != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes(
                        listOfNotNull(item.id, "anilist:$mediaId", existing?.malId?.let { "mal:$it" }),
                    )
                    mapSeasonEpisodeToEntryProgress(
                        season = item.season ?: 1,
                        episode = item.episode,
                        entry = existing,
                        allEntries = allEntries,
                        addonEpisodes = addonEpisodes,
                        isPerSeasonContentId = isPerSeasonContentId(item.id),
                    )
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
            items.forEach { item ->
                markEpisodeOptimisticallyUnwatched(item.videoId)
                if (item.videoId.isNullOrBlank() && item.episode != null) {
                    markEpisodeOptimisticallyUnwatched(
                        buildPlaybackVideoId(item.id, item.season, item.episode),
                    )
                }
            }
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
                val allEntries = _syncState.value.snapshot?.entries.orEmpty()
                val ep = item.episode
                val absoluteEp = if (ep != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes(
                        listOfNotNull(item.id, "anilist:${existing.mediaId}", existing.malId?.let { "mal:$it" }),
                    )
                    mapSeasonEpisodeToEntryProgress(
                        season = item.season ?: 1,
                        episode = ep,
                        entry = existing,
                        allEntries = allEntries,
                        addonEpisodes = addonEpisodes,
                        isPerSeasonContentId = isPerSeasonContentId(item.id),
                    )
                } else {
                    null
                }
                scrobbleDeduplicator.invalidate(profileId, existing.mediaId)
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
        override val changes: Flow<Unit> = combine(_syncState, _authState, AnimeIdBridge.indexState) { _, _, _ -> Unit }

        override fun showIdSiblings(): Map<String, Set<String>> {
            val entries = _syncState.value.snapshot?.entries.orEmpty()
            if (entries.isEmpty()) return emptyMap()
            return buildAniListShowIdSiblings(entries, AnimeIdBridge.currentIndex())
        }

        override fun ensureLoaded() = AniListTracker.ensureLoaded()
        override fun onProfileChanged() = AniListTracker.onProfileChanged()
        override fun clearLocalState() = AniListTracker.clearLocalState()

        override suspend fun refresh(force: Boolean, sourceChanged: Boolean) {
            ensureLoaded()
            syncNow(if (force) TrackingRefreshIntent.USER_INITIATED else TrackingRefreshIntent.AUTOMATIC)
        }

        override fun snapshot(): TrackingProgressSnapshot {
            val uiState = _syncState.value
            val allEntries = uiState.snapshot?.entries.orEmpty()
            val bridgeIndex = AnimeIdBridge.currentIndex()
            val entries = selectAniListContinueWatchingEntries(allEntries)
                .map { entry ->
                    val mapping = AnimeIdBridge.resolveForEntry(entry, bridgeIndex)
                    val contentId = mapping.canonicalContentId() ?: "anilist:${entry.mediaId}"
                    val perSeason = isPerSeasonContentId(contentId)
                    val (seasonNumber, episodeNumber, videoId) = if (entry.progress <= 0) {
                        Triple(1, 0, buildPlaybackVideoId(contentId, 1, 0))
                    } else {
                        val cachedEpisodes = AniListEpisodeMapper.getCached(contentId)
                            .ifEmpty { mapping.kitsuId?.let { AniListEpisodeMapper.getCached("kitsu:$it") }.orEmpty() }
                            .ifEmpty { mapping.imdbId?.let { AniListEpisodeMapper.getCached(it) }.orEmpty() }
                            .ifEmpty { AniListEpisodeMapper.getCached("anilist:${entry.mediaId}") }
                        val mapped = mapEntryProgressToSeasonEpisode(
                            entry = entry,
                            allEntries = allEntries,
                            addonEpisodes = cachedEpisodes,
                            isPerSeasonContentId = perSeason,
                        )
                        Triple(
                            mapped.season,
                            mapped.episode,
                            mapped.videoId ?: buildPlaybackVideoId(contentId, mapped.season, mapped.episode),
                        )
                    }
                    WatchProgressEntry(
                        contentType = "series",
                        parentMetaId = contentId,
                        parentMetaType = "series",
                        videoId = videoId,
                        title = entry.displayTitle(),
                        poster = entry.coverImageUrl,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
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
                val existing = findEntryByContentId(entry.parentMetaId)
                    ?: findEntryForReference(
                        buildTrackingMediaReference(
                            contentType = entry.contentType,
                            parentMetaId = entry.parentMetaId,
                            title = entry.title,
                        ),
                    )
                    ?: continue
                scrobbleDeduplicator.invalidate(activeProfile.value, existing.mediaId)
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
        override val scrobbleThresholdPercent: Double = 50.0
        override val scrobbleOnThresholdReached: Boolean = true

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
                val allEntries = _syncState.value.snapshot?.entries.orEmpty()
                val episodeNum = item.media.episode?.number
                val absoluteEp = if (episodeNum != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes(
                        listOfNotNull(
                            item.media.catalog?.contentId,
                            "anilist:$mediaId",
                            existing?.malId?.let { "mal:$it" },
                        ),
                    )
                    mapSeasonEpisodeToEntryProgress(item.media.episode?.season ?: 1, episodeNum, existing, allEntries, addonEpisodes)
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
                val allEntries = _syncState.value.snapshot?.entries.orEmpty()
                if (existing == null) {
                    notFound++
                    continue
                }
                val ep = media.episode?.number
                val absoluteEp = if (ep != null) {
                    val addonEpisodes = AniListEpisodeMapper.resolveAddonEpisodes(
                        listOfNotNull(media.catalog?.contentId, "anilist:${existing.mediaId}", existing.malId?.let { "mal:$it" }),
                    )
                    mapSeasonEpisodeToEntryProgress(media.episode?.season ?: 1, ep, existing, allEntries, addonEpisodes)
                } else {
                    null
                }
                scrobbleDeduplicator.invalidate(profileId, existing.mediaId)
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
            if (
                action != TrackingScrobbleAction.PROGRESS &&
                action != TrackingScrobbleAction.PAUSE &&
                action != TrackingScrobbleAction.STOP
            ) {
                return
            }
            if (event.progressPercent < scrobbleThresholdPercent) return
            val token = requireToken(profileId) ?: return
            val allEntries = _syncState.value.snapshot?.entries.orEmpty()
            val target = resolveTargetMediaAndProgress(
                media = event.media,
                allEntries = allEntries,
                bridgeIndex = AnimeIdBridge.currentIndex(),
                resolveAddonEpisodes = { ids -> AniListEpisodeMapper.resolveAddonEpisodes(ids) },
                searchMediaIdByTitleOrMal = { title, malId ->
                    apiClient.searchMediaIdByTitleOrMal(
                        accessToken = token,
                        title = title,
                        malId = malId,
                    )
                },
            ) ?: return
            val existing = target.existingEntry
                ?: allEntries.firstOrNull { it.mediaId == target.mediaId }
            val decision = decideAniListScrobbleMutation(
                existing = existing,
                targetProgress = target.targetProgress,
                isMovie = target.isMovie,
            )
            if (!decision.shouldWrite) {
                if (
                    existing != null &&
                    (existing.userStatus == AniListListStatus.CURRENT || existing.userStatus == AniListListStatus.REPEATING) &&
                    existing.progress >= target.targetProgress
                ) {
                    scrobbleDeduplicator.markSucceeded(profileId, target.mediaId, target.targetProgress)
                }
                return
            }
            if (!scrobbleDeduplicator.tryBegin(profileId, target.mediaId, target.targetProgress)) {
                return
            }
            var succeeded = false
            try {
                val saved = apiClient.saveMediaListEntry(
                    accessToken = token,
                    mediaId = target.mediaId,
                    status = decision.newStatus,
                    progress = decision.newProgress,
                )
                if (saved != null) {
                    scrobbleDeduplicator.markSucceeded(profileId, target.mediaId, target.targetProgress)
                    succeeded = true
                    upsertLocalEntry(profileId, saved)
                }
            } finally {
                if (!succeeded) {
                    scrobbleDeduplicator.markFailed(profileId, target.mediaId, target.targetProgress)
                }
            }
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
                    scrobbleDeduplicator.invalidate(profileId, existing.mediaId)
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

internal val ANILIST_TABS = listOf(
    TrackingLibraryTab(
        key = statusTabKey(AniListListStatus.CURRENT),
        title = "Watching",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.STATUS,
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
        key = statusTabKey(AniListListStatus.PLANNING),
        title = "Planning",
        providerId = TrackingProviderId.ANILIST,
        kind = TrackingLibraryTabKind.WATCHLIST,
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

private const val MAX_EXTRA_KEY_EPISODES_PER_COMPLETED_SHOW = 200
private val SERIAL_FRANCHISE_FORMATS = setOf("TV", "TV_SHORT", "ONA")

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

internal fun AniListMediaEntry.displayTitle(): String =
    titleEnglish?.takeIf { it.isNotBlank() } ?: titleRomaji

private fun isPerSeasonContentId(contentId: String?): Boolean {
    if (contentId.isNullOrBlank()) return false
    val ids = parseTrackingExternalIds(contentId)
    return ids.anilist != null || ids.mal != null || ids.kitsu != null || ids.anidb != null
}

internal fun AniListMediaEntry.toLibraryItem(
    index: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
): LibraryItem {
    val mapping = AnimeIdBridge.resolveForEntry(this, index)
    val tabKey = statusTabKey(userStatus ?: AniListListStatus.CURRENT)
    return LibraryItem(
        id = mapping.canonicalContentId() ?: "anilist:$mediaId",
        type = if (format.equals("MOVIE", ignoreCase = true)) "movie" else "series",
        name = displayTitle(),
        poster = coverImageUrl,
        releaseInfo = seasonYear?.toString(),
        imdbRating = averageScore?.let { (it / 10.0).toString() },
        listKeys = setOf(tabKey),
        imdbId = mapping.imdbId,
        tmdbId = mapping.tmdbId?.toInt(),
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
                entry.progress >= 0 &&
                (entry.userStatus == AniListListStatus.CURRENT || entry.userStatus == AniListListStatus.REPEATING) &&
                (entry.episodes == null || entry.progress < entry.episodes) &&
                !(
                    entry.progress == 0 &&
                        entry.status.equals("NOT_YET_RELEASED", ignoreCase = true) &&
                        entry.nextAiringAtSeconds == null
                    )
        }
        .sortedByDescending { it.updatedAtSeconds }
        .toList()

internal fun buildAniListWatchedItems(
    entries: List<AniListMediaEntry>,
    bridgeIndex: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
    addonEpisodesForEntry: (AniListMediaEntry, AnimeIdMapping, String) -> List<SeasonEpisodeRef> = { _, _, _ -> emptyList() },
): List<WatchedItem> {
    val watched = mutableListOf<WatchedItem>()
    for (entry in entries) {
        val mapping = AnimeIdBridge.resolveForEntry(entry, bridgeIndex)
        val canonicalId = mapping.canonicalContentId() ?: "anilist:${entry.mediaId}"
        val title = entry.displayTitle()
        val markedAt = entry.updatedAtSeconds * 1000L
        val isMovie = entry.format.equals("MOVIE", ignoreCase = true)
        if (isMovie) {
            if (entry.userStatus == AniListListStatus.COMPLETED) {
                watched += WatchedItem(
                    id = canonicalId,
                    type = "movie",
                    name = title,
                    poster = entry.coverImageUrl,
                    releaseInfo = entry.seasonYear?.toString(),
                    trackingProviderId = TrackingProviderId.ANILIST.storageId,
                    trackingProviderItemId = entry.mediaId.toString(),
                    markedAtEpochMs = markedAt,
                )
            }
            continue
        }
        if (entry.userStatus != AniListListStatus.CURRENT && entry.userStatus != AniListListStatus.REPEATING) {
            continue
        }
        if (entry.episodes != null && entry.progress >= entry.episodes) {
            continue
        }
        if (entry.progress <= 0) continue

        val addonEpisodes = addonEpisodesForEntry(entry, mapping, canonicalId)
        val mappedEpisodes = mapEntryProgressToWatchedEpisodes(
            entry = entry,
            allEntries = entries,
            addonEpisodes = addonEpisodes,
            isPerSeasonContentId = isPerSeasonContentId(canonicalId),
        )
        for (ref in mappedEpisodes) {
            watched += WatchedItem(
                id = canonicalId,
                type = "series",
                name = title,
                poster = entry.coverImageUrl,
                releaseInfo = entry.seasonYear?.toString(),
                season = ref.season,
                episode = ref.episode,
                videoId = ref.videoId ?: buildPlaybackVideoId(canonicalId, ref.season, ref.episode),
                trackingProviderId = TrackingProviderId.ANILIST.storageId,
                trackingProviderItemId = entry.mediaId.toString(),
                markedAtEpochMs = markedAt,
            )
        }
    }
    return watched
}

internal fun AniListSyncSnapshot.fullyWatchedSeriesKeys(
    index: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
): Set<String> {
    val entriesByAniListId = entries.associateBy { it.mediaId }
    val keys = linkedSetOf<String>()
    for (entry in entries) {
        if (entry.userStatus != AniListListStatus.COMPLETED || entry.format.equals("MOVIE", ignoreCase = true)) {
            continue
        }
        val mapping = AnimeIdBridge.resolveForEntry(entry, index)
        mapping.perSeasonContentIds().forEach { id ->
            keys += watchedItemKeys(type = "series", id = id)
        }
        mapping.franchiseContentIds().forEach { franchiseId ->
            val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(franchiseId, index)
            if (canMarkFranchiseIdFullyWatched(franchiseMappings, entriesByAniListId)) {
                keys += watchedItemKeys(type = "series", id = franchiseId)
            }
        }
    }
    return keys
}

internal fun AniListSyncSnapshot.animeAlternateWatchedKeys(
    index: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
): Set<String> {
    val entriesByAniListId = entries.associateBy { it.mediaId }
    val keys = linkedSetOf<String>()
    for (entry in entries) {
        val mapping = AnimeIdBridge.resolveForEntry(entry, index)
        val perSeasonIds = mapping.perSeasonContentIds()
        val franchiseIds = mapping.franchiseContentIds()
        val isMovie = entry.format.equals("MOVIE", ignoreCase = true)

        if (isMovie) {
            if (entry.userStatus == AniListListStatus.COMPLETED) {
                (perSeasonIds + franchiseIds).forEach { altId ->
                    keys += watchedItemKeys(type = "movie", id = altId)
                }
            }
            continue
        }

        if (entry.userStatus == AniListListStatus.COMPLETED) {
            perSeasonIds.forEach { id ->
                keys += watchedItemKeys(type = "series", id = id)
            }
            franchiseIds.forEach { franchiseId ->
                val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(franchiseId, index)
                if (canMarkFranchiseIdFullyWatched(franchiseMappings, entriesByAniListId)) {
                    keys += watchedItemKeys(type = "series", id = franchiseId)
                }
            }
            val totalEps = (entry.episodes ?: entry.progress).coerceAtLeast(0)
            if (totalEps in 1..MAX_EXTRA_KEY_EPISODES_PER_COMPLETED_SHOW) {
                perSeasonIds.forEach { id ->
                    for (ep in 1..totalEps) {
                        keys += watchedItemKeys(type = "series", id = id, season = 1, episode = ep)
                    }
                }
                val completedEntry = if (entry.progress >= totalEps) entry else entry.copy(progress = totalEps)
                franchiseIds.forEach { franchiseId ->
                    val cachedFranchiseEps = AniListEpisodeMapper.getCached(franchiseId)
                    val mappedRefs = mapEntryProgressToWatchedEpisodes(
                        entry = completedEntry,
                        allEntries = entries,
                        addonEpisodes = cachedFranchiseEps,
                        isPerSeasonContentId = false,
                    )
                    for (ref in mappedRefs) {
                        keys += watchedItemKeys(
                            type = "series",
                            id = franchiseId,
                            season = ref.season,
                            episode = ref.episode,
                        )
                    }
                }
            }
            continue
        }

        if ((entry.userStatus == AniListListStatus.CURRENT || entry.userStatus == AniListListStatus.REPEATING) &&
            entry.progress > 0
        ) {
            val startEp = (entry.progress - MAX_EXTRA_KEY_EPISODES_PER_COMPLETED_SHOW + 1).coerceAtLeast(1)
            perSeasonIds.forEach { id ->
                for (ep in startEp..entry.progress) {
                    keys += watchedItemKeys(type = "series", id = id, season = 1, episode = ep)
                }
            }
            franchiseIds.forEach { franchiseId ->
                val cachedFranchiseEps = AniListEpisodeMapper.getCached(franchiseId)
                val mappedRefs = mapEntryProgressToWatchedEpisodes(
                    entry = entry,
                    allEntries = entries,
                    addonEpisodes = cachedFranchiseEps,
                    isPerSeasonContentId = false,
                )
                val trimmedRefs = if (mappedRefs.size > MAX_EXTRA_KEY_EPISODES_PER_COMPLETED_SHOW) {
                    mappedRefs.takeLast(MAX_EXTRA_KEY_EPISODES_PER_COMPLETED_SHOW)
                } else {
                    mappedRefs
                }
                for (ref in trimmedRefs) {
                    keys += watchedItemKeys(
                        type = "series",
                        id = franchiseId,
                        season = ref.season,
                        episode = ref.episode,
                    )
                }
            }
        }
    }
    return keys
}

internal fun buildAniListShowIdSiblings(
    entries: List<AniListMediaEntry>,
    index: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
): Map<String, Set<String>> {
    val entriesByAniListId = entries.associateBy { it.mediaId }
    val siblingsMap = linkedMapOf<String, MutableSet<String>>()
    for (entry in entries) {
        if (entry.format.equals("MOVIE", ignoreCase = true)) continue
        val mapping = AnimeIdBridge.resolveForEntry(entry, index)
        val group = linkedSetOf<String>()
        group += mapping.perSeasonContentIds()
        mapping.franchiseContentIds().forEach { franchiseId ->
            val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(franchiseId, index)
            if (canMarkFranchiseIdFullyWatched(franchiseMappings, entriesByAniListId)) {
                group += franchiseId
            }
        }
        if (group.size > 1) {
            for (id in group) {
                siblingsMap.getOrPut(id) { linkedSetOf() }.addAll(group)
            }
        }
    }
    return siblingsMap.mapValues { (_, set) -> set.toSet() }
}

internal fun isAniListWatchedLookupActive(
    isAniListAuthenticated: Boolean,
    effectiveSource: WatchProgressSource,
    useAniListForAnime: Boolean,
): Boolean = isAniListAuthenticated &&
    (effectiveSource == WatchProgressSource.ANILIST || useAniListForAnime)

internal fun isAniListEntryEpisodeWatched(
    entries: List<AniListMediaEntry>,
    metaId: String,
    videoId: String?,
    episodeNumber: Int,
    seasonNumber: Int? = null,
    bridgeIndex: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
    optimisticallyRemovedVideoIds: Set<String> = emptySet(),
): Boolean {
    if (entries.isEmpty() || episodeNumber <= 0) return false
    val normalizedVideoId = videoId?.trim()?.takeIf { it.isNotEmpty() }
    if (normalizedVideoId != null && normalizedVideoId in optimisticallyRemovedVideoIds) {
        return false
    }

    val entriesByAniListId = entries.associateBy { it.mediaId }
    val entriesByMalId = buildMap<Int, AniListMediaEntry> {
        for (entry in entries) {
            val malId = entry.malId ?: continue
            if (!containsKey(malId)) put(malId, entry)
        }
    }
    fun findEntryByMapping(mapping: AnimeIdMapping?): AniListMediaEntry? =
        mapping?.anilistId?.let(entriesByAniListId::get)
            ?: mapping?.malId?.let(entriesByMalId::get)

    // 1. Anime-prefixed videoId (e.g. mal:42203:7, kitsu:43137:7, anilist:108632:7, anidb:1234:7)
    if (normalizedVideoId != null && isPerSeasonContentId(normalizedVideoId)) {
        val parsedVideo = parseTrackingExternalIds(normalizedVideoId)
        val matched = parsedVideo.anilist?.toInt()?.let(entriesByAniListId::get)
            ?: parsedVideo.mal?.toInt()?.let(entriesByMalId::get)
            ?: findEntryByMapping(AnimeIdBridge.resolveByContentId(normalizedVideoId, bridgeIndex))
        if (matched != null) {
            val flatEp = extractFlatEpisodeFromAnimeVideoId(normalizedVideoId) ?: episodeNumber
            return matched.userStatus == AniListListStatus.COMPLETED || flatEp <= matched.progress
        }
    }

    // 2. 1-to-1 per-season metaId (anilist:, mal:, kitsu:, anidb:)
    if (isPerSeasonContentId(metaId)) {
        val parsedMeta = parseTrackingExternalIds(metaId)
        val matched = parsedMeta.anilist?.toInt()?.let(entriesByAniListId::get)
            ?: parsedMeta.mal?.toInt()?.let(entriesByMalId::get)
            ?: findEntryByMapping(AnimeIdBridge.resolveByContentId(metaId, bridgeIndex))
        if (matched != null) {
            return matched.userStatus == AniListListStatus.COMPLETED || episodeNumber <= matched.progress
        }
        return false
    }

    // 3. Franchise-scoped metaId (tt..., tmdb:, tvdb:)
    val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(metaId, bridgeIndex)
    if (franchiseMappings.isEmpty()) return false
    val serialMappings = franchiseMappings.filter { mapping ->
        mapping.type == null || mapping.type.uppercase() in SERIAL_FRANCHISE_FORMATS
    }.ifEmpty { franchiseMappings }

    if (serialMappings.size == 1) {
        val matched = findEntryByMapping(serialMappings.first()) ?: return false
        if (matched.userStatus == AniListListStatus.COMPLETED) return true
        val effectiveSeason = seasonNumber?.takeIf { it > 0 }
        val absoluteEp = if (effectiveSeason != null && effectiveSeason > 1) {
            val cachedEpisodes = AniListEpisodeMapper.getCached(metaId)
            if (cachedEpisodes.isNotEmpty()) {
                mapSeasonEpisodeToAbsolute(effectiveSeason, episodeNumber, cachedEpisodes)
            } else {
                episodeNumber
            }
        } else {
            episodeNumber
        }
        return absoluteEp <= matched.progress
    }

    val franchiseEntries = serialMappings.mapNotNull(::findEntryByMapping).distinctBy { it.mediaId }
    if (franchiseEntries.isEmpty()) return false

    val effectiveSeason = seasonNumber?.takeIf { it > 0 } ?: 1
    // Direct explicit season match
    val explicitMatch = if (effectiveSeason >= 2) {
        franchiseEntries.firstOrNull { entry ->
            extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji) == effectiveSeason
        }
    } else {
        franchiseEntries.firstOrNull { entry ->
            extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji) == null &&
                computePrequelEpisodeOffset(entry, entries) == 0
        }
    }
    if (explicitMatch != null) {
        return explicitMatch.userStatus == AniListListStatus.COMPLETED || episodeNumber <= explicitMatch.progress
    }

    // Cumulative episode offset match via cached addon episodes
    val cachedEpisodes = AniListEpisodeMapper.getCached(metaId)
    if (cachedEpisodes.isNotEmpty()) {
        val absoluteEp = mapSeasonEpisodeToAbsolute(effectiveSeason, episodeNumber, cachedEpisodes)
        val sortedByOffset = franchiseEntries.sortedBy { computePrequelEpisodeOffset(it, entries) }
        for (entry in sortedByOffset) {
            val offset = computePrequelEpisodeOffset(entry, entries)
            val count = entry.episodes ?: entry.progress.coerceAtLeast(0)
            if (absoluteEp > offset && (count == 0 || absoluteEp <= offset + count)) {
                val relativeEp = absoluteEp - offset
                return entry.userStatus == AniListListStatus.COMPLETED || relativeEp <= entry.progress
            }
        }
    }

    // Fallback for titled sequels without explicit "Season N" in title (e.g. "Gantz: Second Stage"):
    // match by prequel chain depth (0 = Season 1, 1 = Season 2, ...)
    val depthMatch = franchiseEntries.firstOrNull { entry ->
        computePrequelChainDepth(entry, entries) + 1 == effectiveSeason
    }
    if (depthMatch != null) {
        return depthMatch.userStatus == AniListListStatus.COMPLETED || episodeNumber <= depthMatch.progress
    }
    return false
}

private fun extractFlatEpisodeFromAnimeVideoId(videoId: String): Int? {
    val parts = videoId.trim().split(':')
    return when {
        parts.size >= 4 -> parts[3].toIntOrNull()?.takeIf { it > 0 }
        parts.size == 3 -> parts[2].toIntOrNull()?.takeIf { it > 0 }
        else -> null
    }
}

private fun computePrequelChainDepth(
    entry: AniListMediaEntry,
    allEntries: List<AniListMediaEntry>,
): Int {
    if (entry.prequelIds.isEmpty() || allEntries.isEmpty()) return 0
    val byMediaId = allEntries.associateBy { it.mediaId }
    val visited = mutableSetOf(entry.mediaId)
    var current: AniListMediaEntry? = entry
    var depth = 0
    while (current != null) {
        val nextPrequel = current.prequelIds
            .asSequence()
            .filter { it !in visited }
            .mapNotNull(byMediaId::get)
            .firstOrNull { prequel ->
                val fmt = prequel.format?.uppercase()
                fmt == null || fmt in SERIAL_FRANCHISE_FORMATS
            }
            ?: break
        visited += nextPrequel.mediaId
        depth++
        current = nextPrequel
    }
    return depth
}

internal data class AniListScrobbleTarget(
    val mediaId: Int,
    val targetProgress: Int,
    val isMovie: Boolean,
    val existingEntry: AniListMediaEntry? = null,
)

internal suspend fun resolveTargetMediaAndProgress(
    media: TrackingMediaReference,
    allEntries: List<AniListMediaEntry>,
    bridgeIndex: AnimeIdBridgeIndex = AnimeIdBridge.currentIndex(),
    resolveAddonEpisodes: suspend (List<String>) -> List<SeasonEpisodeRef> = { ids ->
        AniListEpisodeMapper.resolveAddonEpisodes(ids)
    },
    searchMediaIdByTitleOrMal: suspend (title: String?, malId: Int?) -> Int? = { _, _ -> null },
): AniListScrobbleTarget? {
    val entriesByAniListId = allEntries.associateBy { it.mediaId }
    val entriesByMalId = buildMap<Int, AniListMediaEntry> {
        for (entry in allEntries) {
            val malId = entry.malId ?: continue
            if (!containsKey(malId)) put(malId, entry)
        }
    }
    fun findEntry(anilistId: Int?, malId: Int? = null): AniListMediaEntry? =
        anilistId?.let(entriesByAniListId::get) ?: malId?.let(entriesByMalId::get)

    val catalogType = media.catalog?.contentType?.trim()?.lowercase()
    fun isMovieTarget(existing: AniListMediaEntry?, mapping: AnimeIdMapping?): Boolean =
        media.kind == TrackingMediaKind.MOVIE ||
            catalogType == "movie" ||
            catalogType == "film" ||
            existing?.format.equals("MOVIE", ignoreCase = true) ||
            mapping?.type.equals("MOVIE", ignoreCase = true)

    val rawVideoId = media.catalog?.videoId?.trim()?.takeIf { it.isNotEmpty() }
    val rawContentId = media.catalog?.contentId?.trim()?.takeIf { it.isNotEmpty() }

    // Path A: Anime-prefixed videoId (e.g., mal:42203:7, kitsu:43137:7, anilist:108632:7, mal:42203:1:7)
    if (rawVideoId != null && isPerSeasonContentId(rawVideoId)) {
        val parsedVideo = parseTrackingExternalIds(rawVideoId)
        val mapping = AnimeIdBridge.resolveByContentId(rawVideoId, bridgeIndex)
        val existing = findEntry(
            anilistId = parsedVideo.anilist?.toInt() ?: mapping?.anilistId,
            malId = parsedVideo.mal?.toInt() ?: mapping?.malId,
        )
        val mediaId = existing?.mediaId
            ?: parsedVideo.anilist?.toInt()?.takeIf { it > 0 }
            ?: mapping?.anilistId?.takeIf { it > 0 }
            ?: searchMediaIdByTitleOrMal(media.title, parsedVideo.mal?.toInt() ?: mapping?.malId)
        if (mediaId != null && mediaId > 0) {
            val movie = isMovieTarget(existing, mapping)
            val flatEp = extractFlatEpisodeFromAnimeVideoId(rawVideoId)
            val targetProgress = if (movie) {
                existing?.episodes?.takeIf { it > 0 } ?: 1
            } else {
                (flatEp ?: media.episode?.number ?: 1).coerceAtLeast(1)
            }
            return AniListScrobbleTarget(
                mediaId = mediaId,
                targetProgress = targetProgress,
                isMovie = movie,
                existingEntry = existing ?: entriesByAniListId[mediaId],
            )
        }
    }

    // Path B: 1-to-1 anime-prefixed parentMetaId (anilist:, mal:, kitsu:, anidb:) or direct anime IDs
    val perSeasonId = rawContentId?.takeIf(::isPerSeasonContentId)
        ?: if (rawContentId == null) {
            media.ids.anilist?.let { "anilist:$it" }
                ?: media.ids.mal?.let { "mal:$it" }
                ?: media.ids.kitsu?.let { "kitsu:$it" }
                ?: media.ids.anidb?.let { "anidb:$it" }
        } else {
            null
        }
    if (perSeasonId != null) {
        val parsed = parseTrackingExternalIds(perSeasonId)
        val mapping = AnimeIdBridge.resolveByContentId(perSeasonId, bridgeIndex)
        val existing = findEntry(
            anilistId = parsed.anilist?.toInt() ?: mapping?.anilistId,
            malId = parsed.mal?.toInt() ?: mapping?.malId,
        )
        val mediaId = existing?.mediaId
            ?: parsed.anilist?.toInt()?.takeIf { it > 0 }
            ?: mapping?.anilistId?.takeIf { it > 0 }
            ?: searchMediaIdByTitleOrMal(media.title, parsed.mal?.toInt() ?: mapping?.malId)
        if (mediaId != null && mediaId > 0) {
            val resolvedExisting = existing ?: entriesByAniListId[mediaId]
            val movie = isMovieTarget(resolvedExisting, mapping)
            val targetProgress = if (movie) {
                resolvedExisting?.episodes?.takeIf { it > 0 } ?: 1
            } else {
                val episodeRef = media.episode
                if (episodeRef != null) {
                    val addonEpisodes = resolveAddonEpisodes(
                        listOfNotNull(
                            perSeasonId,
                            "anilist:$mediaId",
                            (resolvedExisting?.malId ?: mapping?.malId)?.let { "mal:$it" },
                        ),
                    )
                    mapSeasonEpisodeToEntryProgress(
                        season = episodeRef.season ?: 1,
                        episode = episodeRef.number,
                        entry = resolvedExisting,
                        allEntries = allEntries,
                        addonEpisodes = addonEpisodes,
                        isPerSeasonContentId = true,
                    )
                } else {
                    resolvedExisting?.episodes?.takeIf { it > 0 } ?: 1
                }
            }
            return AniListScrobbleTarget(
                mediaId = mediaId,
                targetProgress = targetProgress,
                isMovie = movie,
                existingEntry = resolvedExisting,
            )
        }
    }

    // Path C: Franchise parentMetaId (tt..., tmdb:, tvdb:)
    val franchiseCandidate = rawContentId
        ?: media.ids.imdb
        ?: media.ids.tmdb?.let { "tmdb:$it" }
        ?: media.ids.tvdb?.let { "tvdb:$it" }
    val franchiseMappings = AnimeIdBridge.mappingsForFranchiseId(franchiseCandidate, bridgeIndex)
    if (franchiseMappings.isNotEmpty()) {
        val movieByContext = media.kind == TrackingMediaKind.MOVIE ||
            catalogType == "movie" ||
            catalogType == "film" ||
            (media.episode == null && franchiseMappings.all { it.type.equals("MOVIE", ignoreCase = true) })
        if (movieByContext) {
            val mapping = franchiseMappings.firstOrNull { it.type.equals("MOVIE", ignoreCase = true) }
                ?: franchiseMappings.first()
            val existing = findEntry(mapping.anilistId, mapping.malId)
            val mediaId = existing?.mediaId
                ?: mapping.anilistId?.takeIf { it > 0 }
                ?: searchMediaIdByTitleOrMal(media.title, mapping.malId)
            if (mediaId != null && mediaId > 0) {
                val resolvedExisting = existing ?: entriesByAniListId[mediaId]
                return AniListScrobbleTarget(
                    mediaId = mediaId,
                    targetProgress = resolvedExisting?.episodes?.takeIf { it > 0 } ?: 1,
                    isMovie = true,
                    existingEntry = resolvedExisting,
                )
            }
        } else {
            val serialMappings = franchiseMappings.filter { mapping ->
                mapping.type == null || mapping.type.uppercase() in SERIAL_FRANCHISE_FORMATS
            }.ifEmpty { franchiseMappings }
            val season = media.episode?.season?.takeIf { it > 0 } ?: 1
            val episode = media.episode?.number?.takeIf { it > 0 } ?: 1

            if (serialMappings.size == 1) {
                val mapping = serialMappings.first()
                val existing = findEntry(mapping.anilistId, mapping.malId)
                val mediaId = existing?.mediaId
                    ?: mapping.anilistId?.takeIf { it > 0 }
                    ?: searchMediaIdByTitleOrMal(media.title, mapping.malId)
                if (mediaId != null && mediaId > 0) {
                    val resolvedExisting = existing ?: entriesByAniListId[mediaId]
                    val addonEpisodes = resolveAddonEpisodes(
                        listOfNotNull(
                            franchiseCandidate,
                            "anilist:$mediaId",
                            (resolvedExisting?.malId ?: mapping.malId)?.let { "mal:$it" },
                        ),
                    )
                    val targetProgress = mapSeasonEpisodeToEntryProgress(
                        season = season,
                        episode = episode,
                        entry = resolvedExisting,
                        allEntries = allEntries,
                        addonEpisodes = addonEpisodes,
                        isPerSeasonContentId = false,
                    )
                    return AniListScrobbleTarget(
                        mediaId = mediaId,
                        targetProgress = targetProgress,
                        isMovie = false,
                        existingEntry = resolvedExisting,
                    )
                }
            } else {
                val franchiseEntries = serialMappings
                    .mapNotNull { mapping -> findEntry(mapping.anilistId, mapping.malId) }
                    .distinctBy { it.mediaId }

                if (franchiseEntries.isNotEmpty()) {
                    // 1. Direct explicit season match
                    val explicitMatch = if (season >= 2) {
                        franchiseEntries.firstOrNull { entry ->
                            extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji) == season
                        }
                    } else {
                        franchiseEntries.firstOrNull { entry ->
                            extractExplicitSeasonNumber(entry.titleEnglish, entry.titleRomaji) == null &&
                                computePrequelEpisodeOffset(entry, allEntries) == 0
                        }
                    }
                    if (explicitMatch != null) {
                        return AniListScrobbleTarget(
                            mediaId = explicitMatch.mediaId,
                            targetProgress = episode,
                            isMovie = false,
                            existingEntry = explicitMatch,
                        )
                    }

                    // 2. Cumulative episode range match using prequel offsets + addon episodes
                    val addonEpisodes = resolveAddonEpisodes(listOfNotNull(franchiseCandidate))
                    if (addonEpisodes.isNotEmpty()) {
                        val absoluteEp = mapSeasonEpisodeToAbsolute(season, episode, addonEpisodes)
                        val sortedByOffset = franchiseEntries.sortedBy { computePrequelEpisodeOffset(it, allEntries) }
                        for (entry in sortedByOffset) {
                            val offset = computePrequelEpisodeOffset(entry, allEntries)
                            val count = entry.episodes ?: entry.progress.coerceAtLeast(0)
                            if (absoluteEp > offset && (count == 0 || absoluteEp <= offset + count)) {
                                return AniListScrobbleTarget(
                                    mediaId = entry.mediaId,
                                    targetProgress = (absoluteEp - offset).coerceAtLeast(1),
                                    isMovie = false,
                                    existingEntry = entry,
                                )
                            }
                        }
                    }

                    // 3. Prequel chain depth match for titled sequels (e.g. "Gantz: Second Stage")
                    val depthMatch = franchiseEntries.firstOrNull { entry ->
                        computePrequelChainDepth(entry, allEntries) + 1 == season
                    }
                    if (depthMatch != null) {
                        return AniListScrobbleTarget(
                            mediaId = depthMatch.mediaId,
                            targetProgress = episode,
                            isMovie = false,
                            existingEntry = depthMatch,
                        )
                    }
                }

                // Target season not yet in user's AniList snapshot
                if (season >= 2) {
                    val searchedId = media.title?.trim()?.takeIf { it.isNotEmpty() }?.let { title ->
                        searchMediaIdByTitleOrMal("$title Season $season", null)
                    }
                    if (searchedId != null && searchedId > 0) {
                        return AniListScrobbleTarget(
                            mediaId = searchedId,
                            targetProgress = episode,
                            isMovie = false,
                            existingEntry = entriesByAniListId[searchedId],
                        )
                    }
                } else {
                    val firstTvId = serialMappings.firstNotNullOfOrNull { it.anilistId?.takeIf { id -> id > 0 } }
                    if (firstTvId != null) {
                        return AniListScrobbleTarget(
                            mediaId = firstTvId,
                            targetProgress = episode,
                            isMovie = false,
                            existingEntry = entriesByAniListId[firstTvId],
                        )
                    }
                }
            }
        }
    }

    // Path D: Fallback by title or MAL ID search
    val normalizedTitle = media.title?.trim()?.takeIf { it.isNotEmpty() }
    val titleEntry = normalizedTitle?.let { title ->
        allEntries.firstOrNull { entry ->
            entry.titleEnglish.equals(title, ignoreCase = true) ||
                entry.titleRomaji.equals(title, ignoreCase = true)
        }
    }
    val fallbackMediaId = titleEntry?.mediaId
        ?: searchMediaIdByTitleOrMal(media.title, media.ids.mal?.toInt())
        ?: return null
    if (fallbackMediaId <= 0) return null
    val resolvedExisting = titleEntry ?: entriesByAniListId[fallbackMediaId]
    val movie = isMovieTarget(resolvedExisting, null)
    val targetProgress = if (movie) {
        resolvedExisting?.episodes?.takeIf { it > 0 } ?: 1
    } else {
        val episodeRef = media.episode
        if (episodeRef != null) {
            val addonEpisodes = resolveAddonEpisodes(
                listOfNotNull(
                    rawContentId,
                    "anilist:$fallbackMediaId",
                    resolvedExisting?.malId?.let { "mal:$it" },
                ),
            )
            mapSeasonEpisodeToEntryProgress(
                season = episodeRef.season ?: 1,
                episode = episodeRef.number,
                entry = resolvedExisting,
                allEntries = allEntries,
                addonEpisodes = addonEpisodes,
                isPerSeasonContentId = isPerSeasonContentId(rawContentId),
            )
        } else {
            resolvedExisting?.episodes?.takeIf { it > 0 } ?: 1
        }
    }
    return AniListScrobbleTarget(
        mediaId = fallbackMediaId,
        targetProgress = targetProgress,
        isMovie = movie,
        existingEntry = resolvedExisting,
    )
}

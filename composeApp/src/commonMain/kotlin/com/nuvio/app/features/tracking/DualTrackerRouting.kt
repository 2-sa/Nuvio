package com.nuvio.app.features.tracking

import com.nuvio.app.features.anilist.AnimeIdBridge
import com.nuvio.app.features.watched.WatchedItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry

private val ANIME_ID_PREFIXES = listOf("anilist:", "mal:", "kitsu:", "anidb:")

internal fun isAnimeTrackingTarget(
    contentId: String?,
    mediaCategory: String? = null,
    isKnownInAniList: Boolean = false,
): Boolean {
    if (isKnownInAniList) return true
    if (mediaCategory.equals("anime", ignoreCase = true)) return true
    val normalizedId = contentId?.trim()?.lowercase().orEmpty()
    if (ANIME_ID_PREFIXES.any { prefix -> normalizedId.startsWith(prefix) }) return true
    return AnimeIdBridge.isKnownAnimeContentId(contentId)
}

internal fun resolveEffectiveWriteProvider(
    primaryProvider: TrackingProviderId?,
    isAnime: Boolean,
    useAniListForAnime: Boolean,
    isAniListAuthenticated: Boolean,
): TrackingProviderId? {
    if (useAniListForAnime && isAniListAuthenticated && isAnime) {
        return TrackingProviderId.ANILIST
    }
    return primaryProvider
}

internal fun mergeDualWatchProgress(
    primarySource: WatchProgressSource,
    primaryEntries: List<WatchProgressEntry>,
    anilistEntries: List<WatchProgressEntry>,
    useAniListForAnime: Boolean,
): List<WatchProgressEntry> {
    if (!useAniListForAnime || primarySource == WatchProgressSource.ANILIST) {
        return primaryEntries
    }
    val anilistTitleKeys = anilistEntries
        .asSequence()
        .map { normalizeTitleKey(it.title) }
        .filter { it.isNotBlank() }
        .toSet()
    val anilistBridgedIds = bridgedContentIdsFor(anilistEntries.asSequence().map { it.parentMetaId })

    val filteredPrimary = primaryEntries.filterNot { entry ->
        isAnimeTrackingTarget(entry.parentMetaId) ||
            (anilistBridgedIds.isNotEmpty() && entry.parentMetaId.trim().lowercase() in anilistBridgedIds) ||
            (anilistTitleKeys.isNotEmpty() && normalizeTitleKey(entry.title) in anilistTitleKeys)
    }
    return (anilistEntries + filteredPrimary)
        .sortedByDescending { it.lastUpdatedEpochMs }
}

internal fun mergeDualWatchedItems(
    primarySource: WatchProgressSource,
    primaryItems: Collection<WatchedItem>,
    anilistItems: Collection<WatchedItem>,
    useAniListForAnime: Boolean,
): List<WatchedItem> {
    if (!useAniListForAnime || primarySource == WatchProgressSource.ANILIST) {
        return primaryItems.toList()
    }
    val anilistTitleKeys = anilistItems
        .asSequence()
        .map { normalizeTitleKey(it.name) }
        .filter { it.isNotBlank() }
        .toSet()
    val anilistBridgedIds = bridgedContentIdsFor(anilistItems.asSequence().map { it.id })

    val filteredPrimary = primaryItems.filterNot { item ->
        isAnimeTrackingTarget(item.id) ||
            (anilistBridgedIds.isNotEmpty() && item.id.trim().lowercase() in anilistBridgedIds) ||
            (anilistTitleKeys.isNotEmpty() && normalizeTitleKey(item.name) in anilistTitleKeys)
    }
    return anilistItems.toList() + filteredPrimary
}

private fun bridgedContentIdsFor(contentIds: Sequence<String>): Set<String> {
    val index = AnimeIdBridge.currentIndex()
    if (!index.isLoaded) return emptySet()
    val ids = linkedSetOf<String>()
    for (rawId in contentIds) {
        val trimmed = rawId.trim()
        if (trimmed.isEmpty()) continue
        ids += trimmed.lowercase()
        val mapping = AnimeIdBridge.resolveByContentId(trimmed, index)
        if (mapping != null) {
            mapping.perSeasonContentIds().forEach { ids += it.lowercase() }
            mapping.franchiseContentIds().forEach { ids += it.lowercase() }
        }
        AnimeIdBridge.mappingsForFranchiseId(trimmed, index).forEach { franchiseMapping ->
            franchiseMapping.perSeasonContentIds().forEach { ids += it.lowercase() }
            franchiseMapping.franchiseContentIds().forEach { ids += it.lowercase() }
        }
    }
    return ids
}

private fun normalizeTitleKey(raw: String?): String =
    raw
        ?.trim()
        ?.lowercase()
        ?.filter { it.isLetterOrDigit() }
        .orEmpty()

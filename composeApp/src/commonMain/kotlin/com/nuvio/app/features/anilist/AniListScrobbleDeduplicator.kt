package com.nuvio.app.features.anilist

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal data class AniListScrobbleDecision(
    val shouldWrite: Boolean,
    val newStatus: AniListListStatus? = null,
    val newProgress: Int = 0,
)

internal fun decideAniListScrobbleMutation(
    existing: AniListMediaEntry?,
    targetProgress: Int,
    isMovie: Boolean,
): AniListScrobbleDecision {
    if (existing?.userStatus == AniListListStatus.COMPLETED) {
        return AniListScrobbleDecision(
            shouldWrite = false,
            newStatus = existing.userStatus,
            newProgress = existing.progress,
        )
    }

    if (isMovie) {
        val movieProgress = existing?.episodes?.takeIf { it > 0 } ?: 1
        return AniListScrobbleDecision(
            shouldWrite = true,
            newStatus = AniListListStatus.COMPLETED,
            newProgress = movieProgress,
        )
    }

    val safeTarget = targetProgress.coerceAtLeast(1)
    if (
        existing != null &&
        existing.progress >= safeTarget &&
        (existing.userStatus == AniListListStatus.CURRENT || existing.userStatus == AniListListStatus.REPEATING)
    ) {
        return AniListScrobbleDecision(
            shouldWrite = false,
            newStatus = existing.userStatus,
            newProgress = existing.progress,
        )
    }

    val newProgress = maxOf(existing?.progress ?: 0, safeTarget)
    val totalEpisodes = existing?.episodes
    val newStatus = when {
        totalEpisodes != null && totalEpisodes > 0 && newProgress >= totalEpisodes ->
            AniListListStatus.COMPLETED
        existing == null ||
            existing.userStatus == null ||
            existing.userStatus == AniListListStatus.PLANNING ||
            existing.userStatus == AniListListStatus.PAUSED ||
            existing.userStatus == AniListListStatus.DROPPED ->
            AniListListStatus.CURRENT
        existing.userStatus == AniListListStatus.REPEATING ->
            AniListListStatus.REPEATING
        else ->
            existing.userStatus
    }
    return AniListScrobbleDecision(
        shouldWrite = true,
        newStatus = newStatus,
        newProgress = newProgress,
    )
}

internal class AniListScrobbleDeduplicator {
    private val lock = SynchronizedObject()
    private val completedKeys = mutableSetOf<String>()
    private val inFlightKeys = mutableSetOf<String>()

    private fun key(profileId: Int, mediaId: Int, targetProgress: Int): String =
        "$profileId:$mediaId:$targetProgress"

    fun tryBegin(profileId: Int, mediaId: Int, targetProgress: Int): Boolean {
        val scrobbleKey = key(profileId, mediaId, targetProgress)
        return synchronized(lock) {
            if (scrobbleKey in completedKeys || scrobbleKey in inFlightKeys) {
                false
            } else {
                inFlightKeys += scrobbleKey
                true
            }
        }
    }

    fun markSucceeded(profileId: Int, mediaId: Int, targetProgress: Int) {
        val scrobbleKey = key(profileId, mediaId, targetProgress)
        synchronized(lock) {
            inFlightKeys.remove(scrobbleKey)
            completedKeys += scrobbleKey
        }
    }

    fun markFailed(profileId: Int, mediaId: Int, targetProgress: Int) {
        val scrobbleKey = key(profileId, mediaId, targetProgress)
        synchronized(lock) {
            inFlightKeys.remove(scrobbleKey)
        }
    }

    fun invalidate(profileId: Int, mediaId: Int, targetProgress: Int? = null) {
        synchronized(lock) {
            if (targetProgress != null) {
                val scrobbleKey = key(profileId, mediaId, targetProgress)
                completedKeys.remove(scrobbleKey)
                inFlightKeys.remove(scrobbleKey)
            } else {
                val prefix = "$profileId:$mediaId:"
                completedKeys.removeAll { it.startsWith(prefix) }
                inFlightKeys.removeAll { it.startsWith(prefix) }
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            completedKeys.clear()
            inFlightKeys.clear()
        }
    }
}

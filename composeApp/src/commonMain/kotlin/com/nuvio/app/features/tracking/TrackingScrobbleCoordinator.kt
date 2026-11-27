package com.nuvio.app.features.tracking

import co.touchlab.kermit.Logger
import com.nuvio.app.features.anilist.AniListTracker
import com.nuvio.app.features.anilist.AnimeIdBridge
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class TrackingScrobbleFailure(
    val providerId: TrackingProviderId,
    val cause: Throwable,
)

object TrackingScrobbleCoordinator {
    private val log = Logger.withTag("TrackingScrobble")

    suspend fun scrobble(
        profileId: Int,
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent,
    ): List<TrackingScrobbleFailure> {
        if (profileId != ProfileRepository.activeProfileId) return emptyList()
        TrackingProviderRegistry.ensureLoaded()
        val scrobblers = selectScrobblersForEvent(event)
        val failures = dispatchTrackingScrobble(
            scrobblers = scrobblers,
            profileId = profileId,
            action = action,
            event = event,
        )
        failures.forEach { failure ->
            log.w(failure.cause) {
                "${failure.providerId.storageId} scrobble ${action.wireValue} failed"
            }
        }
        return failures
    }

    suspend fun scrobbleSeek(
        profileId: Int,
        action: TrackingScrobbleAction,
        event: TrackingScrobbleEvent,
    ): List<TrackingScrobbleFailure> {
        if (profileId != ProfileRepository.activeProfileId) return emptyList()
        TrackingProviderRegistry.ensureLoaded()
        val scrobblers = selectScrobblersForEvent(event)
        val failures = dispatchTrackingSeekScrobble(
            scrobblers = scrobblers,
            profileId = profileId,
            action = action,
            event = event,
        )
        failures.forEach { failure ->
            log.w(failure.cause) {
                "${failure.providerId.storageId} seek scrobble ${action.wireValue} failed"
            }
        }
        return failures
    }

    suspend fun scrobbleProgressThreshold(
        profileId: Int,
        event: TrackingScrobbleEvent,
    ): List<TrackingScrobbleFailure> {
        if (profileId != ProfileRepository.activeProfileId) return emptyList()
        TrackingProviderRegistry.ensureLoaded()
        val scrobblers = selectScrobblersForEvent(event)
        val failures = dispatchTrackingProgressThresholdScrobble(
            scrobblers = scrobblers,
            profileId = profileId,
            event = event,
        )
        failures.forEach { failure ->
            log.w(failure.cause) {
                "${failure.providerId.storageId} scrobble ${TrackingScrobbleAction.PROGRESS.wireValue} failed"
            }
        }
        return failures
    }

    private fun selectScrobblersForEvent(event: TrackingScrobbleEvent): Collection<TrackingScrobbler> {
        val connected = TrackingProviderRegistry.connectedScrobblers()
        TrackingSettingsRepository.ensureLoaded()
        val useDualAniList = TrackingSettingsRepository.uiState.value.useAniListForAnime &&
            TrackingProviderRegistry.isAuthenticated(TrackingProviderId.ANILIST)
        if (!useDualAniList) return connected

        val isAnime = event.media.kind == TrackingMediaKind.ANIME ||
            event.media.ids.anilist != null ||
            event.media.ids.mal != null ||
            event.media.ids.kitsu != null ||
            event.media.ids.anidb != null ||
            AnimeIdBridge.isKnownAnimeContentId(event.media.catalog?.contentId) ||
            AnimeIdBridge.isKnownAnimeContentId(event.media.catalog?.videoId) ||
            AnimeIdBridge.isKnownAnimeContentId(event.media.ids.imdb) ||
            isAnimeTrackingTarget(
                contentId = event.media.catalog?.contentId,
                isKnownInAniList = AniListTracker.hasKnownMedia(event.media.catalog?.contentId, event.media.title),
            )
        return if (isAnime) {
            connected.filter { it.providerId == TrackingProviderId.ANILIST }
        } else {
            connected.filter { it.providerId != TrackingProviderId.ANILIST }
        }
    }
}

internal suspend fun dispatchTrackingProgressThresholdScrobble(
    scrobblers: Collection<TrackingScrobbler>,
    profileId: Int,
    event: TrackingScrobbleEvent,
): List<TrackingScrobbleFailure> = dispatchTrackingScrobble(
    scrobblers = scrobblers.filter { scrobbler ->
        scrobbler.scrobbleOnThresholdReached &&
            event.progressPercent >= scrobbler.scrobbleThresholdPercent
    },
    profileId = profileId,
    action = TrackingScrobbleAction.PROGRESS,
    event = event,
)

internal suspend fun dispatchTrackingSeekScrobble(
    scrobblers: Collection<TrackingScrobbler>,
    profileId: Int,
    action: TrackingScrobbleAction,
    event: TrackingScrobbleEvent,
): List<TrackingScrobbleFailure> = dispatchTrackingScrobble(
    scrobblers = scrobblers.filter { scrobbler ->
        scrobbler.seekScrobblePolicy == TrackingSeekScrobblePolicy.STOP_AND_RESTART
    },
    profileId = profileId,
    action = action,
    event = event,
)

internal suspend fun dispatchTrackingScrobble(
    scrobblers: Collection<TrackingScrobbler>,
    profileId: Int,
    action: TrackingScrobbleAction,
    event: TrackingScrobbleEvent,
): List<TrackingScrobbleFailure> = supervisorScope {
    scrobblers.map { scrobbler ->
        async {
            try {
                scrobbler.scrobble(profileId = profileId, action = action, event = event)
                null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                TrackingScrobbleFailure(providerId = scrobbler.providerId, cause = error)
            }
        }
    }.awaitAll().filterNotNull()
}

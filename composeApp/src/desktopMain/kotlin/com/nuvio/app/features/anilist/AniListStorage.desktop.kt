package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.features.profiles.MAX_PROFILES

internal actual object AniListStorage {
    private val store = DesktopStorage.store("nuvio_anilist")
    private const val authPayloadKey = "anilist_auth_payload"
    private const val syncPayloadKey = "anilist_sync_payload"

    actual fun loadAuthPayload(profileId: Int): String? =
        store.getString(ProfileScopedKey.of(authPayloadKey, profileId))

    actual fun saveAuthPayload(profileId: Int, payload: String) {
        store.putString(ProfileScopedKey.of(authPayloadKey, profileId), payload)
    }

    actual fun loadSyncPayload(profileId: Int): String? =
        store.getString(ProfileScopedKey.of(syncPayloadKey, profileId))

    actual fun saveSyncPayload(profileId: Int, payload: String) {
        store.putString(ProfileScopedKey.of(syncPayloadKey, profileId), payload)
    }

    actual fun removeProfile(profileId: Int) {
        store.removeAll(
            listOf(
                ProfileScopedKey.of(authPayloadKey, profileId),
                ProfileScopedKey.of(syncPayloadKey, profileId),
            ),
        )
    }

    actual fun clearAll() {
        store.removeAll(
            (1..MAX_PROFILES).flatMap { profileId ->
                listOf(
                    ProfileScopedKey.of(authPayloadKey, profileId),
                    ProfileScopedKey.of(syncPayloadKey, profileId),
                )
            },
        )
    }
}

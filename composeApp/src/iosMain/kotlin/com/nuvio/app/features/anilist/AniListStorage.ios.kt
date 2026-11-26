package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object AniListStorage {
    private const val suite = "nuvio_anilist"
    private const val authPayloadKey = "anilist_auth_payload"
    private const val syncPayloadKey = "anilist_sync_payload"

    private val preferences = NSUserDefaults(suiteName = suite)

    actual fun loadAuthPayload(profileId: Int): String? =
        preferences.stringForKey(ProfileScopedKey.of(authPayloadKey, profileId))

    actual fun saveAuthPayload(profileId: Int, payload: String) {
        preferences.setObject(payload, forKey = ProfileScopedKey.of(authPayloadKey, profileId))
    }

    actual fun loadSyncPayload(profileId: Int): String? =
        preferences.stringForKey(ProfileScopedKey.of(syncPayloadKey, profileId))

    actual fun saveSyncPayload(profileId: Int, payload: String) {
        preferences.setObject(payload, forKey = ProfileScopedKey.of(syncPayloadKey, profileId))
    }

    actual fun removeProfile(profileId: Int) {
        preferences.removeObjectForKey(ProfileScopedKey.of(authPayloadKey, profileId))
        preferences.removeObjectForKey(ProfileScopedKey.of(syncPayloadKey, profileId))
    }

    actual fun clearAll() {
        preferences.removePersistentDomainForName(suite)
    }
}

package com.nuvio.app.features.anilist

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object AniListStorage {
    private const val preferencesName = "nuvio_anilist"
    private const val authPayloadKey = "anilist_auth_payload"
    private const val syncPayloadKey = "anilist_sync_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadAuthPayload(profileId: Int): String? =
        preferences?.getString(ProfileScopedKey.of(authPayloadKey, profileId), null)

    actual fun saveAuthPayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(authPayloadKey, profileId), payload)
            ?.apply()
    }

    actual fun loadSyncPayload(profileId: Int): String? =
        preferences?.getString(ProfileScopedKey.of(syncPayloadKey, profileId), null)

    actual fun saveSyncPayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(syncPayloadKey, profileId), payload)
            ?.apply()
    }

    actual fun removeProfile(profileId: Int) {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(authPayloadKey, profileId))
            ?.remove(ProfileScopedKey.of(syncPayloadKey, profileId))
            ?.apply()
    }

    actual fun clearAll() {
        preferences
            ?.edit()
            ?.clear()
            ?.apply()
    }
}

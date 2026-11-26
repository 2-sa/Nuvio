package com.nuvio.app.features.anilist

internal expect object AniListStorage {
    fun loadAuthPayload(profileId: Int): String?
    fun saveAuthPayload(profileId: Int, payload: String)
    fun loadSyncPayload(profileId: Int): String?
    fun saveSyncPayload(profileId: Int, payload: String)
    fun removeProfile(profileId: Int)
    fun clearAll()
}

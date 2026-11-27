package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val ANIME_ID_BRIDGE_SCHEMA_VERSION = 1
internal const val ANIME_ID_BRIDGE_DIR_NAME = "anime_id_bridge"
internal const val ANIME_ID_BRIDGE_DATA_FILE_NAME = "anime-list-full.json"
internal const val ANIME_ID_BRIDGE_META_FILE_NAME = "meta.json"

internal data class StoredAnimeIdBridgePayload(
    val rawJson: String,
    val fetchedAtEpochMs: Long,
    val etag: String? = null,
    val schemaVersion: Int = ANIME_ID_BRIDGE_SCHEMA_VERSION,
)

@Serializable
internal data class AnimeIdBridgeCacheMetadata(
    val fetchedAtEpochMs: Long,
    val etag: String? = null,
    val schemaVersion: Int = ANIME_ID_BRIDGE_SCHEMA_VERSION,
)

internal val animeIdBridgeMetaJson = Json { ignoreUnknownKeys = true }

internal expect object AnimeIdBridgeStorage {
    fun load(): StoredAnimeIdBridgePayload?
    fun save(payload: StoredAnimeIdBridgePayload)
    fun touchFetchedAt(fetchedAtEpochMs: Long, etag: String?)
    fun clear()
}

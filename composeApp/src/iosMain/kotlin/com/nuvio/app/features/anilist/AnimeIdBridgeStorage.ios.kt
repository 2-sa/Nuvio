package com.nuvio.app.features.anilist

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual object AnimeIdBridgeStorage {
    private val lock = SynchronizedObject()

    private fun resolveDirectory(): String? {
        val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return null
        return "$caches/$ANIME_ID_BRIDGE_DIR_NAME"
    }

    actual fun load(): StoredAnimeIdBridgePayload? = synchronized(lock) {
        val directory = resolveDirectory() ?: return@synchronized null
        val fileManager = NSFileManager.defaultManager
        val dataPath = "$directory/$ANIME_ID_BRIDGE_DATA_FILE_NAME"
        val metaPath = "$directory/$ANIME_ID_BRIDGE_META_FILE_NAME"
        if (!fileManager.fileExistsAtPath(dataPath) || !fileManager.fileExistsAtPath(metaPath)) {
            return@synchronized null
        }
        runCatching {
            val rawMeta = NSString.stringWithContentsOfFile(metaPath, NSUTF8StringEncoding, null)
                ?: return@runCatching null
            val meta = animeIdBridgeMetaJson.decodeFromString(AnimeIdBridgeCacheMetadata.serializer(), rawMeta)
            if (meta.schemaVersion != ANIME_ID_BRIDGE_SCHEMA_VERSION) return@runCatching null
            val rawJson = NSString.stringWithContentsOfFile(dataPath, NSUTF8StringEncoding, null)
                ?: return@runCatching null
            if (rawJson.isBlank()) return@runCatching null
            StoredAnimeIdBridgePayload(
                rawJson = rawJson,
                fetchedAtEpochMs = meta.fetchedAtEpochMs,
                etag = meta.etag,
                schemaVersion = meta.schemaVersion,
            )
        }.getOrNull()
    }

    actual fun save(payload: StoredAnimeIdBridgePayload) {
        synchronized(lock) {
            val directory = resolveDirectory() ?: return
            runCatching {
                val fileManager = NSFileManager.defaultManager
                if (!fileManager.createDirectoryAtPath(directory, true, null, null)) return@runCatching
                val dataPath = "$directory/$ANIME_ID_BRIDGE_DATA_FILE_NAME"
                val metaPath = "$directory/$ANIME_ID_BRIDGE_META_FILE_NAME"
                val dataWritten = NSString.create(string = payload.rawJson)
                    .writeToFile(dataPath, true, NSUTF8StringEncoding, null)
                if (!dataWritten) return@runCatching
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = payload.fetchedAtEpochMs,
                        etag = payload.etag,
                        schemaVersion = payload.schemaVersion,
                    ),
                )
                NSString.create(string = metaText)
                    .writeToFile(metaPath, true, NSUTF8StringEncoding, null)
            }
        }
    }

    actual fun touchFetchedAt(fetchedAtEpochMs: Long, etag: String?) {
        synchronized(lock) {
            val directory = resolveDirectory() ?: return
            runCatching {
                val fileManager = NSFileManager.defaultManager
                val dataPath = "$directory/$ANIME_ID_BRIDGE_DATA_FILE_NAME"
                if (!fileManager.fileExistsAtPath(dataPath)) return@runCatching
                val metaPath = "$directory/$ANIME_ID_BRIDGE_META_FILE_NAME"
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = fetchedAtEpochMs,
                        etag = etag,
                        schemaVersion = ANIME_ID_BRIDGE_SCHEMA_VERSION,
                    ),
                )
                NSString.create(string = metaText)
                    .writeToFile(metaPath, true, NSUTF8StringEncoding, null)
            }
        }
    }

    actual fun clear() {
        synchronized(lock) {
            val directory = resolveDirectory() ?: return
            val fileManager = NSFileManager.defaultManager
            runCatching { fileManager.removeItemAtPath("$directory/$ANIME_ID_BRIDGE_META_FILE_NAME", null) }
            runCatching { fileManager.removeItemAtPath("$directory/$ANIME_ID_BRIDGE_DATA_FILE_NAME", null) }
        }
    }
}

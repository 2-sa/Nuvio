package com.nuvio.app.features.anilist

import android.content.Context
import java.io.File

internal actual object AnimeIdBridgeStorage {
    private val lock = Any()
    private var cacheDirectory: File? = null

    fun initialize(context: Context) {
        synchronized(lock) {
            cacheDirectory = File(context.applicationContext.cacheDir, ANIME_ID_BRIDGE_DIR_NAME)
        }
    }

    actual fun load(): StoredAnimeIdBridgePayload? = synchronized(lock) {
        val dir = cacheDirectory ?: return@synchronized null
        runCatching {
            val dataFile = File(dir, ANIME_ID_BRIDGE_DATA_FILE_NAME)
            val metaFile = File(dir, ANIME_ID_BRIDGE_META_FILE_NAME)
            if (!dataFile.exists() || !metaFile.exists()) return@runCatching null
            val rawMeta = metaFile.readText(Charsets.UTF_8)
            val meta = animeIdBridgeMetaJson.decodeFromString(AnimeIdBridgeCacheMetadata.serializer(), rawMeta)
            if (meta.schemaVersion != ANIME_ID_BRIDGE_SCHEMA_VERSION) return@runCatching null
            val rawJson = dataFile.readText(Charsets.UTF_8)
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
            val dir = cacheDirectory ?: return
            runCatching {
                writeAtomically(dir, File(dir, ANIME_ID_BRIDGE_DATA_FILE_NAME), payload.rawJson)
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = payload.fetchedAtEpochMs,
                        etag = payload.etag,
                        schemaVersion = payload.schemaVersion,
                    ),
                )
                writeAtomically(dir, File(dir, ANIME_ID_BRIDGE_META_FILE_NAME), metaText)
            }
        }
    }

    actual fun touchFetchedAt(fetchedAtEpochMs: Long, etag: String?) {
        synchronized(lock) {
            val dir = cacheDirectory ?: return
            runCatching {
                val dataFile = File(dir, ANIME_ID_BRIDGE_DATA_FILE_NAME)
                if (!dataFile.exists()) return@runCatching
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = fetchedAtEpochMs,
                        etag = etag,
                        schemaVersion = ANIME_ID_BRIDGE_SCHEMA_VERSION,
                    ),
                )
                writeAtomically(dir, File(dir, ANIME_ID_BRIDGE_META_FILE_NAME), metaText)
            }
        }
    }

    actual fun clear() {
        synchronized(lock) {
            val dir = cacheDirectory ?: return
            runCatching { File(dir, ANIME_ID_BRIDGE_META_FILE_NAME).delete() }
            runCatching { File(dir, ANIME_ID_BRIDGE_DATA_FILE_NAME).delete() }
        }
    }

    private fun writeAtomically(dir: File, target: File, content: String) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val pending = File.createTempFile(target.name, ".part", dir)
        try {
            pending.writeText(content, Charsets.UTF_8)
            if (!pending.renameTo(target)) {
                if (target.exists()) {
                    target.delete()
                }
                if (!pending.renameTo(target)) {
                    pending.copyTo(target, overwrite = true)
                }
            }
        } finally {
            if (pending.exists()) {
                pending.delete()
            }
        }
    }
}

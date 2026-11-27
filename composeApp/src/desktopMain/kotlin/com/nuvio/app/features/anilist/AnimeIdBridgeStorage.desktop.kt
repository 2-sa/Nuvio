package com.nuvio.app.features.anilist

import com.nuvio.app.core.storage.DesktopStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal actual object AnimeIdBridgeStorage {
    private val lock = Any()

    private val directory: Path
        get() = DesktopStorage.cacheDir.resolve(ANIME_ID_BRIDGE_DIR_NAME)

    private val dataFile: Path
        get() = directory.resolve(ANIME_ID_BRIDGE_DATA_FILE_NAME)

    private val metaFile: Path
        get() = directory.resolve(ANIME_ID_BRIDGE_META_FILE_NAME)

    actual fun load(): StoredAnimeIdBridgePayload? = synchronized(lock) {
        runCatching {
            val dataPath = dataFile
            val metaPath = metaFile
            if (!Files.exists(dataPath) || !Files.exists(metaPath)) return@runCatching null
            val rawMeta = Files.readAllBytes(metaPath).toString(Charsets.UTF_8)
            val meta = animeIdBridgeMetaJson.decodeFromString(AnimeIdBridgeCacheMetadata.serializer(), rawMeta)
            if (meta.schemaVersion != ANIME_ID_BRIDGE_SCHEMA_VERSION) return@runCatching null
            val rawJson = Files.readAllBytes(dataPath).toString(Charsets.UTF_8)
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
            runCatching {
                writeAtomically(dataFile, payload.rawJson.toByteArray(Charsets.UTF_8))
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = payload.fetchedAtEpochMs,
                        etag = payload.etag,
                        schemaVersion = payload.schemaVersion,
                    ),
                )
                writeAtomically(metaFile, metaText.toByteArray(Charsets.UTF_8))
            }
        }
    }

    actual fun touchFetchedAt(fetchedAtEpochMs: Long, etag: String?) {
        synchronized(lock) {
            runCatching {
                if (!Files.exists(dataFile)) return@runCatching
                val metaText = animeIdBridgeMetaJson.encodeToString(
                    AnimeIdBridgeCacheMetadata.serializer(),
                    AnimeIdBridgeCacheMetadata(
                        fetchedAtEpochMs = fetchedAtEpochMs,
                        etag = etag,
                        schemaVersion = ANIME_ID_BRIDGE_SCHEMA_VERSION,
                    ),
                )
                writeAtomically(metaFile, metaText.toByteArray(Charsets.UTF_8))
            }
        }
    }

    actual fun clear() {
        synchronized(lock) {
            runCatching { Files.deleteIfExists(metaFile) }
            runCatching { Files.deleteIfExists(dataFile) }
        }
    }

    private fun writeAtomically(target: Path, bytes: ByteArray) {
        Files.createDirectories(target.parent)
        val pending = Files.createTempFile(target.parent, target.fileName.toString(), ".part")
        try {
            Files.write(pending, bytes)
            runCatching {
                Files.move(
                    pending,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            runCatching { Files.deleteIfExists(pending) }
        }
    }
}

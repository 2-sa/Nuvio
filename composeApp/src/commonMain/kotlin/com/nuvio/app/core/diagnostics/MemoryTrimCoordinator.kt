package com.nuvio.app.core.diagnostics

import com.nuvio.app.core.ui.clearArtworkBackdropCache
import com.nuvio.app.features.details.ImdbEpisodeRatingsRepository
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.streams.BadgeImageLoader
import com.nuvio.app.features.tmdb.TmdbMetadataService
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal enum class MemoryTrimLevel {
    Background,
    Minimized,
}

internal data class MemoryDiagnosticsSnapshot(
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val nonHeapUsedBytes: Long,
    val imageCacheUsedBytes: Long,
    val imageCacheMaxBytes: Long,
    val imageCacheEntries: Int,
    val badgeCacheUsedBytes: Long,
    val metaDetailsEntries: Int,
    val tmdbEntries: Int,
) {
    fun formatSummary(): String {
        val heapUsedMb = bytesToMb(heapUsedBytes)
        val heapCommittedMb = bytesToMb(heapCommittedBytes)
        val heapMaxMb = bytesToMb(heapMaxBytes)
        val nonHeapMb = bytesToMb(nonHeapUsedBytes)
        val imageMb = bytesToMb(imageCacheUsedBytes)
        val imageMaxMb = bytesToMb(imageCacheMaxBytes)
        val metadataCount = metaDetailsEntries + tmdbEntries
        return "Heap ${heapUsedMb}/${heapCommittedMb} MB (max ${heapMaxMb} MB) • " +
            "Non-Heap ${nonHeapMb} MB • " +
            "Images ${imageMb}/${imageMaxMb} MB (${imageCacheEntries}) • " +
            "Meta ${metadataCount}"
    }
}

private const val BytesPerMegabyte = 1024L * 1024L

internal fun bytesToMb(bytes: Long): Long =
    if (bytes <= 0L) 0L else (bytes + BytesPerMegabyte / 2L) / BytesPerMegabyte

internal object MemoryTrimCoordinator {
    private val lock = SynchronizedObject()
    private val platformTrimmers = mutableListOf<(MemoryTrimLevel) -> Unit>()

    fun registerPlatformTrimmer(trimmer: (MemoryTrimLevel) -> Unit) {
        synchronized(lock) {
            if (trimmer !in platformTrimmers) {
                platformTrimmers += trimmer
            }
        }
    }

    fun unregisterPlatformTrimmer(trimmer: (MemoryTrimLevel) -> Unit) {
        synchronized(lock) {
            platformTrimmers.remove(trimmer)
        }
    }

    fun trim(level: MemoryTrimLevel) {
        val aggressive = level == MemoryTrimLevel.Minimized
        TmdbMetadataService.trimMemoryCaches(aggressive = aggressive)
        MetaDetailsRepository.trimMemoryCache(aggressive = aggressive)
        ImdbEpisodeRatingsRepository.trimMemoryCache(aggressive = aggressive)
        BadgeImageLoader.clearMemoryCache()
        clearArtworkBackdropCache()

        val trimmers = synchronized(lock) { platformTrimmers.toList() }
        trimmers.forEach { trimmer ->
            runCatching { trimmer(level) }
        }
    }
}

internal expect fun capturePlatformMemoryDiagnostics(): MemoryDiagnosticsSnapshot?

internal expect fun requestPlatformMemoryTrim(level: MemoryTrimLevel)

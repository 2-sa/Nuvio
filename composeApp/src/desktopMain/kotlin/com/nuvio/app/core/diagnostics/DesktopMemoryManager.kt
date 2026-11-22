package com.nuvio.app.core.diagnostics

import co.touchlab.kermit.Logger
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.nuvio.app.core.ui.AppPresenceState
import com.nuvio.app.core.ui.PresenceSnapshot
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.home.components.clearIdleGifCodecCache
import com.nuvio.app.features.streams.BadgeImageLoader
import com.nuvio.app.features.tmdb.TmdbMetadataService
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean

internal object DesktopMemoryManager {
    private val log = Logger.withTag("DesktopMemory")
    private val gcInFlight = AtomicBoolean(false)

    fun captureSnapshot(): MemoryDiagnosticsSnapshot {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val heap = memoryBean.heapMemoryUsage
        val nonHeap = memoryBean.nonHeapMemoryUsage
        val imageCache = runCatching {
            SingletonImageLoader.get(PlatformContext.INSTANCE).memoryCache
        }.getOrNull()

        return MemoryDiagnosticsSnapshot(
            heapUsedBytes = heap.used.coerceAtLeast(0L),
            heapCommittedBytes = heap.committed.coerceAtLeast(0L),
            heapMaxBytes = heap.max.coerceAtLeast(0L),
            nonHeapUsedBytes = nonHeap.used.coerceAtLeast(0L),
            imageCacheUsedBytes = imageCache?.size?.coerceAtLeast(0L) ?: 0L,
            imageCacheMaxBytes = imageCache?.maxSize?.coerceAtLeast(0L) ?: 0L,
            imageCacheEntries = imageCache?.keys?.size ?: 0,
            badgeCacheUsedBytes = BadgeImageLoader.memoryCacheSizeBytes().coerceAtLeast(0L),
            metaDetailsEntries = MetaDetailsRepository.cacheEntryCount(),
            tmdbEntries = TmdbMetadataService.cacheEntryCount(),
        )
    }

    /**
     * Releases desktop caches and requests a compacting G1GC cycle off the UI thread.
     * Automatic trims skip active video playback unless [force] is true.
     */
    fun trim(
        level: MemoryTrimLevel,
        force: Boolean = false,
        presenceSnapshot: PresenceSnapshot? = AppPresenceState.current.value,
        runGc: (() -> Unit)? = null,
    ): Boolean {
        if (!shouldRunDesktopMemoryTrim(presenceSnapshot = presenceSnapshot, force = force)) {
            return false
        }

        val before = captureSnapshot()
        MemoryTrimCoordinator.trim(level)
        clearIdleGifCodecCache()
        runCatching {
            SingletonImageLoader.get(PlatformContext.INSTANCE).memoryCache?.clear()
        }

        val triggerGc = runGc ?: ::requestBackgroundCompactingGc
        triggerGc()

        val after = captureSnapshot()
        log.i {
            "trim($level) before=[${before.formatSummary()}] after=[${after.formatSummary()}]"
        }
        return true
    }

    private fun requestBackgroundCompactingGc() {
        if (!gcInFlight.compareAndSet(false, true)) return
        Thread {
            try {
                System.gc()
            } finally {
                gcInFlight.set(false)
            }
        }.apply {
            name = "nuvio-memory-trim-gc"
            isDaemon = true
            start()
        }
    }
}

internal fun shouldRunDesktopMemoryTrim(
    presenceSnapshot: PresenceSnapshot?,
    force: Boolean,
): Boolean = force || presenceSnapshot !is PresenceSnapshot.Player

internal actual fun capturePlatformMemoryDiagnostics(): MemoryDiagnosticsSnapshot? =
    DesktopMemoryManager.captureSnapshot()

internal actual fun requestPlatformMemoryTrim(level: MemoryTrimLevel) {
    DesktopMemoryManager.trim(level = level, force = true)
}

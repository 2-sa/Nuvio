package com.nuvio.app.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryTrimCoordinatorTest {

    @Test
    fun invokesRegisteredPlatformTrimmerAndStopsAfterUnregister() {
        val observed = mutableListOf<MemoryTrimLevel>()
        val trimmer: (MemoryTrimLevel) -> Unit = { level -> observed += level }

        MemoryTrimCoordinator.registerPlatformTrimmer(trimmer)
        try {
            MemoryTrimCoordinator.trim(MemoryTrimLevel.Background)
            MemoryTrimCoordinator.trim(MemoryTrimLevel.Minimized)
        } finally {
            MemoryTrimCoordinator.unregisterPlatformTrimmer(trimmer)
        }
        MemoryTrimCoordinator.trim(MemoryTrimLevel.Background)

        assertEquals(
            listOf(MemoryTrimLevel.Background, MemoryTrimLevel.Minimized),
            observed,
        )
    }

    @Test
    fun bytesToMbRoundsToNearestMegabyteAndClampsNegatives() {
        assertEquals(0L, bytesToMb(-100L))
        assertEquals(0L, bytesToMb(0L))
        assertEquals(1L, bytesToMb(1024L * 1024L))
        assertEquals(48L, bytesToMb(48L * 1024L * 1024L))
    }

    @Test
    fun snapshotFormatSummaryReportsAllBuckets() {
        val snapshot = MemoryDiagnosticsSnapshot(
            heapUsedBytes = 120L * 1024L * 1024L,
            heapCommittedBytes = 180L * 1024L * 1024L,
            heapMaxBytes = 448L * 1024L * 1024L,
            nonHeapUsedBytes = 96L * 1024L * 1024L,
            imageCacheUsedBytes = 24L * 1024L * 1024L,
            imageCacheMaxBytes = 48L * 1024L * 1024L,
            imageCacheEntries = 31,
            badgeCacheUsedBytes = 2L * 1024L * 1024L,
            metaDetailsEntries = 4,
            tmdbEntries = 14,
        )

        assertEquals(
            "Heap 120/180 MB (max 448 MB) • Non-Heap 96 MB • Images 24/48 MB (31) • Meta 18",
            snapshot.formatSummary(),
        )
    }
}

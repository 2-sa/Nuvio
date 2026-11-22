package com.nuvio.app.core.diagnostics

import com.nuvio.app.AppScreenTab
import com.nuvio.app.core.ui.PresenceSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopMemoryManagerTest {

    @Test
    fun skipsAutomaticTrimDuringActivePlayback() {
        val playerPresence = PresenceSnapshot.Player(
            title = "Movie",
            episodeLabel = null,
            posterUrl = null,
            isPlaying = true,
            positionMs = 10_000L,
            durationMs = 120_000L,
        )

        assertFalse(shouldRunDesktopMemoryTrim(presenceSnapshot = playerPresence, force = false))
        assertTrue(shouldRunDesktopMemoryTrim(presenceSnapshot = playerPresence, force = true))
    }

    @Test
    fun allowsAutomaticTrimWhileBrowsingOrIdle() {
        assertTrue(shouldRunDesktopMemoryTrim(presenceSnapshot = null, force = false))
        assertTrue(
            shouldRunDesktopMemoryTrim(
                presenceSnapshot = PresenceSnapshot.Tab(AppScreenTab.Home),
                force = false,
            ),
        )
    }

    @Test
    fun trimExecutesGcHookWhenAllowedAndSkipsWhenBlockedByPlayback() {
        var gcCalls = 0
        val playerPresence = PresenceSnapshot.Player(
            title = "Show",
            episodeLabel = "S1E1",
            posterUrl = null,
            isPlaying = true,
            positionMs = 0L,
            durationMs = 60_000L,
        )

        val blocked = DesktopMemoryManager.trim(
            level = MemoryTrimLevel.Background,
            force = false,
            presenceSnapshot = playerPresence,
            runGc = { gcCalls += 1 },
        )
        assertFalse(blocked)
        assertEquals(0, gcCalls)

        val allowed = DesktopMemoryManager.trim(
            level = MemoryTrimLevel.Minimized,
            force = false,
            presenceSnapshot = PresenceSnapshot.Tab(AppScreenTab.Home),
            runGc = { gcCalls += 1 },
        )
        assertTrue(allowed)
        assertEquals(1, gcCalls)
    }
}

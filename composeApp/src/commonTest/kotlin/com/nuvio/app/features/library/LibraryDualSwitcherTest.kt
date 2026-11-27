package com.nuvio.app.features.library

import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.catalog.CatalogUiState
import com.nuvio.app.features.catalog.libraryCatalogStates
import com.nuvio.app.features.catalog.shouldUseAniListLibraryFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryDualSwitcherTest {
    @Test
    fun `switcher is visible only in Saved view when dual AniList mode is active and primary source is not AniList`() {
        assertTrue(isDualAniListLibrarySwitcherVisible(true, true, true, LibrarySourceMode.TRAKT))
        assertTrue(isDualAniListLibrarySwitcherVisible(true, true, true, LibrarySourceMode.LOCAL))
        assertTrue(isDualAniListLibrarySwitcherVisible(true, true, true, LibrarySourceMode.SIMKL))
        assertTrue(isDualAniListLibrarySwitcherVisible(true, true, true, LibrarySourceMode.MDBLIST))
        assertFalse(isDualAniListLibrarySwitcherVisible(false, true, true, LibrarySourceMode.TRAKT))
        assertFalse(isDualAniListLibrarySwitcherVisible(true, false, true, LibrarySourceMode.TRAKT))
        assertFalse(isDualAniListLibrarySwitcherVisible(true, true, false, LibrarySourceMode.TRAKT))
        assertFalse(isDualAniListLibrarySwitcherVisible(true, true, true, LibrarySourceMode.ANILIST))
    }

    @Test
    fun `resolveEffectiveDualLibraryTab coerces back to PRIMARY when switcher hides without mutating settings`() {
        assertEquals(
            DualLibraryTab.ANILIST,
            resolveEffectiveDualLibraryTab(DualLibraryTab.ANILIST, switcherVisible = true),
        )
        assertEquals(
            DualLibraryTab.PRIMARY,
            resolveEffectiveDualLibraryTab(DualLibraryTab.ANILIST, switcherVisible = false),
        )
        assertEquals(
            DualLibraryTab.PRIMARY,
            resolveEffectiveDualLibraryTab(DualLibraryTab.PRIMARY, switcherVisible = true),
        )
    }

    @Test
    fun `CatalogTarget Library preserves explicit ANILIST sourceMode for View All navigation`() {
        val target = CatalogTarget.Library(
            contentType = "series",
            sectionType = "anilist:current",
            sourceMode = LibrarySourceMode.ANILIST,
        )
        assertEquals(LibrarySourceMode.ANILIST, target.sourceMode)
    }

    @Test
    fun `shouldUseAniListLibraryFlow routes dual View All to aniListUiState only when primary source is not AniList`() {
        assertTrue(shouldUseAniListLibraryFlow(LibrarySourceMode.ANILIST, LibrarySourceMode.TRAKT))
        assertTrue(shouldUseAniListLibraryFlow(LibrarySourceMode.ANILIST, LibrarySourceMode.LOCAL))
        assertTrue(shouldUseAniListLibraryFlow(LibrarySourceMode.ANILIST, LibrarySourceMode.SIMKL))
        assertTrue(shouldUseAniListLibraryFlow(LibrarySourceMode.ANILIST, LibrarySourceMode.MDBLIST))
        assertFalse(shouldUseAniListLibraryFlow(LibrarySourceMode.ANILIST, LibrarySourceMode.ANILIST))
        assertFalse(shouldUseAniListLibraryFlow(LibrarySourceMode.TRAKT, LibrarySourceMode.TRAKT))
        assertFalse(shouldUseAniListLibraryFlow(null, LibrarySourceMode.TRAKT))
    }

    @Test
    fun `View All on AniList shelf resolves items from aniListUiState instead of empty Trakt state`() = runBlocking {
        val target = CatalogTarget.Library(
            contentType = "series",
            sectionType = "anilist:current",
            sourceMode = LibrarySourceMode.ANILIST,
        )
        val gantz = LibraryItem(
            id = "mal:384",
            type = "series",
            name = "Gantz",
            savedAtEpochMs = 100L,
        )
        val aniListFlow = MutableStateFlow(
            LibraryUiState(
                sourceMode = LibrarySourceMode.ANILIST,
                items = listOf(gantz),
                sections = listOf(
                    LibrarySection(
                        type = "anilist:current",
                        displayTitle = "Watching",
                        items = listOf(gantz),
                    ),
                ),
                isLoaded = true,
            ),
        )
        val traktFlow = MutableStateFlow(
            LibraryUiState(
                sourceMode = LibrarySourceMode.TRAKT,
                items = emptyList(),
                sections = emptyList(),
                isLoaded = true,
            ),
        )

        val selectedFlow = if (
            shouldUseAniListLibraryFlow(target.sourceMode, traktFlow.value.sourceMode)
        ) {
            aniListFlow
        } else {
            traktFlow
        }
        val state: CatalogUiState = selectedFlow.libraryCatalogStates(target).first()
        assertEquals(listOf("mal:384"), state.items.map { it.id })
        assertFalse(state.isLoading)
    }
}

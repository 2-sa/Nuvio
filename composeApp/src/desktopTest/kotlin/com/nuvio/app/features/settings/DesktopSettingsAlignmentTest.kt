package com.nuvio.app.features.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopSettingsAlignmentTest {
    @Test
    fun `standard workspaces use 920dp readable rail when search is inactive`() {
        val standardWorkspaces = listOf(
            DesktopSettingsWorkspace.AppearanceAndNav,
            DesktopSettingsWorkspace.HomeAndCards,
            DesktopSettingsWorkspace.PlaybackAndStreams,
            DesktopSettingsWorkspace.ServersAndCollections,
            DesktopSettingsWorkspace.TrackingAndSync,
            DesktopSettingsWorkspace.MetadataAndRatings,
            DesktopSettingsWorkspace.DebridServices,
            DesktopSettingsWorkspace.AccountAndProfiles,
            DesktopSettingsWorkspace.SystemAndPerformance,
            DesktopSettingsWorkspace.AboutAndUpdates,
        )

        assertEquals(
            DesktopSettingsWorkspace.entries.filterNot { it == DesktopSettingsWorkspace.AddonsAndPlugins },
            standardWorkspaces,
        )

        for (workspace in standardWorkspaces) {
            assertEquals(
                expected = 920,
                actual = desktopSettingsMaxReadableWidthDp(workspace = workspace, isSearchActive = false),
                message = "Expected 920dp rail for $workspace when search is inactive",
            )
        }
    }

    @Test
    fun `addons and plugins workspace uses 1040dp readable rail when search is inactive`() {
        assertEquals(
            expected = 1040,
            actual = desktopSettingsMaxReadableWidthDp(
                workspace = DesktopSettingsWorkspace.AddonsAndPlugins,
                isSearchActive = false,
            ),
        )
    }

    @Test
    fun `all workspaces use 920dp readable rail when search is active`() {
        for (workspace in DesktopSettingsWorkspace.entries) {
            assertEquals(
                expected = 920,
                actual = desktopSettingsMaxReadableWidthDp(workspace = workspace, isSearchActive = true),
                message = "Expected 920dp rail for $workspace when search is active",
            )
        }
    }

    @Test
    fun `settings commonMain sources do not contain legacy 880dp row width caps`() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val settingsDir = assertNotNull(
            actual = sequenceOf(
                File(userDir, "src/commonMain/kotlin/com/nuvio/app/features/settings"),
                File(userDir, "composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings"),
            ).firstOrNull { it.isDirectory },
            message = "Could not locate features/settings commonMain directory from user.dir=${userDir.absolutePath}",
        )

        val kotlinFiles = settingsDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.name }
            .toList()

        assertTrue(
            actual = kotlinFiles.isNotEmpty(),
            message = "Expected Kotlin source files in ${settingsDir.absolutePath}",
        )

        val violations = buildList {
            for (file in kotlinFiles) {
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if ("widthIn(max = 880.dp)" in line) {
                            add("${file.name}:${index + 1}: ${line.trim()}")
                        }
                    }
                }
            }
        }

        assertTrue(
            actual = violations.isEmpty(),
            message = "Found legacy 880.dp width caps in features/settings:\n${violations.joinToString("\n")}",
        )
    }
}

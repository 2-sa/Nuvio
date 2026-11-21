package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color
import com.nuvio.app.features.membership.CosmeticEntitlements
import com.nuvio.app.features.membership.availableAppThemes
import com.nuvio.app.features.membership.resolveAppTheme
import com.nuvio.app.isDesktop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CinemaObsidianThemeTest {
    @Test
    fun cinemaObsidianPaletteHasExactObsidianAndAccentColors() {
        val palette = ThemeColors.getColorPalette(AppTheme.CINEMA_OBSIDIAN)

        assertEquals(ThemeColors.CinemaObsidian, palette)
        assertEquals(Color(0xFF090C12), palette.background)
        assertEquals(Color(0xFF111622), palette.backgroundElevated)
        assertEquals(Color(0xFF171E2E), palette.backgroundCard)
        assertEquals(Color(0xFF1B2336), palette.surfacePopover)
        assertEquals(Color(0xFF3B82F6), palette.secondary)
        assertEquals(Color(0xFF6366F1), palette.secondaryVariant)
        assertEquals(
            listOf(Color(0xFF38BDF8), Color(0xFF3B82F6), Color(0xFF6366F1)),
            palette.accentGradient,
        )
        assertEquals(Color.White, palette.onSecondary)
        assertEquals(Color.White, palette.onSecondaryVariant)
        assertEquals(Color(0xFF60A5FA), palette.focusRing)
        assertEquals(Color(0xFF172542), palette.focusBackground)
        assertEquals(Color(0xFF1E293B).copy(alpha = 0.55f), palette.borderSubtle)
        assertEquals(Color(0xFF24324A), palette.borderDefault)
        assertEquals(Color(0xFF3B4F72), palette.borderStrong)
        assertEquals(Color(0xFFF8FAFC), palette.textPrimary)
        assertEquals(Color(0xFFCBD5E1), palette.textSecondary)
        assertEquals(Color(0xFF94A3B8), palette.textMuted)
        assertEquals("#3B82F6", palette.nativeAccentHex)
    }

    @Test
    fun defaultNuvioThemeTokensPropagateCinemaObsidianSurfaceBorderAndTextTokens() {
        val tokens = defaultNuvioThemeTokens(
            palette = ThemeColors.CinemaObsidian,
            amoled = false,
            colorScheme = null,
        )

        assertEquals(Color(0xFF090C12), tokens.colors.background)
        assertEquals(Color(0xFF111622), tokens.colors.surface)
        assertEquals(Color(0xFF111622), tokens.colors.surfaceElevated)
        assertEquals(Color(0xFF171E2E), tokens.colors.surfaceCard)
        assertEquals(Color(0xFF1B2336), tokens.colors.surfacePopover)
        assertEquals(Color(0xFF1E293B).copy(alpha = 0.55f), tokens.colors.borderSubtle)
        assertEquals(Color(0xFF24324A), tokens.colors.borderDefault)
        assertEquals(Color(0xFF3B4F72), tokens.colors.borderStrong)
        assertEquals(Color(0xFFF8FAFC), tokens.colors.textPrimary)
        assertEquals(Color(0xFFCBD5E1), tokens.colors.textSecondary)
        assertEquals(Color(0xFF94A3B8), tokens.colors.textMuted)

        val amoledTokens = defaultNuvioThemeTokens(
            palette = ThemeColors.CinemaObsidian,
            amoled = true,
            colorScheme = null,
        )
        assertEquals(Color.Black, amoledTokens.colors.background)
    }

    @Test
    fun existingThemesRetainLegacySurfaceBorderAndTextDefaults() {
        val tokens = defaultNuvioThemeTokens(
            palette = ThemeColors.White,
            amoled = false,
            colorScheme = null,
        )

        assertEquals(Color(0xFF222222), tokens.colors.surfacePopover)
        assertEquals(Color(0xFF252A2A).copy(alpha = 0.55f), tokens.colors.borderSubtle)
        assertEquals(Color(0xFF252A2A), tokens.colors.borderDefault)
        assertEquals(Color(0xFF3A4040), tokens.colors.borderStrong)
        assertEquals(Color(0xFFF5F7F8), tokens.colors.textPrimary)
        assertEquals(Color(0xFFB8BEC5), tokens.colors.textSecondary)
        assertEquals(Color(0xFF969CA3), tokens.colors.textMuted)
    }

    @Test
    fun cinemaObsidianIsAvailableAsFreeStandardThemeAndResolvesCleanly() {
        val freeThemes = availableAppThemes(CosmeticEntitlements.None)

        assertTrue(AppTheme.CINEMA_OBSIDIAN in freeThemes)
        assertEquals(AppTheme.CINEMA_OBSIDIAN, freeThemes[1])
        assertEquals(
            AppTheme.CINEMA_OBSIDIAN,
            resolveAppTheme(AppTheme.CINEMA_OBSIDIAN, CosmeticEntitlements.None),
        )
    }

    @Test
    fun defaultPlatformAppThemeMatchesDesktopVsMobile() {
        val expected = if (isDesktop) AppTheme.CINEMA_OBSIDIAN else AppTheme.WHITE
        assertEquals(expected, defaultPlatformAppTheme())
    }
}

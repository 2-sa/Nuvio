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
        assertEquals(Color(0xFF07090F), palette.background)
        assertEquals(Color(0xFF0B0F1A), palette.backgroundElevated)
        assertEquals(Color(0xFF111827), palette.backgroundCard)
        assertEquals(Color(0xFF172033), palette.surfacePopover)
        assertEquals(Color(0xFF4F7CFF), palette.secondary)
        assertEquals(Color(0xFF7C5CFF), palette.secondaryVariant)
        assertEquals(
            listOf(Color(0xFF38BDF8), Color(0xFF4F7CFF), Color(0xFF818CF8)),
            palette.accentGradient,
        )
        assertEquals(Color.White, palette.onSecondary)
        assertEquals(Color.White, palette.onSecondaryVariant)
        assertEquals(Color(0xFF7DA4FF), palette.focusRing)
        assertEquals(Color(0xFF142142), palette.focusBackground)
        assertEquals(Color.White.copy(alpha = 0.07f), palette.borderSubtle)
        assertEquals(Color.White.copy(alpha = 0.13f), palette.borderDefault)
        assertEquals(Color.White.copy(alpha = 0.24f), palette.borderStrong)
        assertEquals(Color(0xFFF8FAFC), palette.textPrimary)
        assertEquals(Color(0xFFCBD5E1), palette.textSecondary)
        assertEquals(Color(0xFF8E9BB0), palette.textMuted)
        assertEquals("#4F7CFF", palette.nativeAccentHex)
    }

    @Test
    fun defaultNuvioThemeTokensPropagateCinemaObsidianSurfaceBorderAndTextTokens() {
        val tokens = defaultNuvioThemeTokens(
            palette = ThemeColors.CinemaObsidian,
            amoled = false,
            colorScheme = null,
        )

        assertEquals(Color(0xFF07090F), tokens.colors.background)
        assertEquals(Color(0xFF0B0F1A), tokens.colors.surface)
        assertEquals(Color(0xFF0B0F1A), tokens.colors.surfaceElevated)
        assertEquals(Color(0xFF111827), tokens.colors.surfaceCard)
        assertEquals(Color(0xFF172033), tokens.colors.surfacePopover)
        assertEquals(Color.White.copy(alpha = 0.07f), tokens.colors.borderSubtle)
        assertEquals(Color.White.copy(alpha = 0.13f), tokens.colors.borderDefault)
        assertEquals(Color.White.copy(alpha = 0.24f), tokens.colors.borderStrong)
        assertEquals(Color(0xFFF8FAFC), tokens.colors.textPrimary)
        assertEquals(Color(0xFFCBD5E1), tokens.colors.textSecondary)
        assertEquals(Color(0xFF8E9BB0), tokens.colors.textMuted)

        val amoledTokens = defaultNuvioThemeTokens(
            palette = ThemeColors.CinemaObsidian,
            amoled = true,
            colorScheme = null,
        )
        assertEquals(Color.Black, amoledTokens.colors.background)
    }

    @Test
    fun existingThemesUseUpgradedVividSurfaceBorderAndTextDefaults() {
        val tokens = defaultNuvioThemeTokens(
            palette = ThemeColors.White,
            amoled = false,
            colorScheme = null,
        )

        assertEquals(Color(0xFF1F2631), tokens.colors.surfacePopover)
        assertEquals(Color.White.copy(alpha = 0.07f), tokens.colors.borderSubtle)
        assertEquals(Color.White.copy(alpha = 0.13f), tokens.colors.borderDefault)
        assertEquals(Color.White.copy(alpha = 0.24f), tokens.colors.borderStrong)
        assertEquals(Color(0xFFF8FAFC), tokens.colors.textPrimary)
        assertEquals(Color(0xFFCBD5E1), tokens.colors.textSecondary)
        assertEquals(Color(0xFF8E9BB0), tokens.colors.textMuted)
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

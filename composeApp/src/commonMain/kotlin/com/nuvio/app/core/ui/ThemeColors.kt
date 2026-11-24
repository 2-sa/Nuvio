package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Color

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF07090F),
    val backgroundElevated: Color = Color(0xFF0C101B),
    val backgroundCard: Color = Color(0xFF131926),
    val surfacePopover: Color = Color(0xFF192234),
    val borderSubtle: Color = Color.White.copy(alpha = 0.07f),
    val borderDefault: Color = Color.White.copy(alpha = 0.13f),
    val borderStrong: Color = Color.White.copy(alpha = 0.24f),
    val textPrimary: Color = Color(0xFFF8FAFC),
    val textSecondary: Color = Color(0xFFCBD5E1),
    val textMuted: Color = Color(0xFF8E9BB0),
)

internal fun dominantBackdropBlendColor(dominantColor: Color, backgroundColor: Color): Color {
    val fraction = 0.42f
    return Color(
        red = backgroundColor.red + (dominantColor.red - backgroundColor.red) * fraction,
        green = backgroundColor.green + (dominantColor.green - backgroundColor.green) * fraction,
        blue = backgroundColor.blue + (dominantColor.blue - backgroundColor.blue) * fraction,
        alpha = backgroundColor.alpha + (dominantColor.alpha - backgroundColor.alpha) * fraction,
    )
}

object ThemeColors {

    val CinemaObsidian = ThemeColorPalette(
        secondary = Color(0xFF4F7CFF),
        secondaryVariant = Color(0xFF7C5CFF),
        accentGradient = listOf(Color(0xFF38BDF8), Color(0xFF4F7CFF), Color(0xFF818CF8)),
        nativeAccentHex = "#4F7CFF",
        onSecondary = Color.White,
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF7DA4FF),
        focusBackground = Color(0xFF142142),
        background = Color(0xFF07090F),
        backgroundElevated = Color(0xFF0B0F1A),
        backgroundCard = Color(0xFF111827),
        surfacePopover = Color(0xFF172033),
        borderSubtle = Color.White.copy(alpha = 0.07f),
        borderDefault = Color.White.copy(alpha = 0.13f),
        borderStrong = Color.White.copy(alpha = 0.24f),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFCBD5E1),
        textMuted = Color(0xFF8E9BB0),
    )

    val Gold = ThemeColorPalette(
        secondary = Color(0xFFF5B727),
        secondaryVariant = Color(0xFFD97706),
        accentGradient = listOf(
            Color(0xFFD97706),
            Color(0xFFF5B727),
            Color(0xFFFFE58F),
            Color(0xFFFBBF24),
        ),
        nativeAccentHex = "#F5B727",
        onSecondary = Color(0xFF0E0B04),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFFDE047),
        focusBackground = Color(0xFF33240C),
        background = Color(0xFF0B0905),
        backgroundElevated = Color(0xFF141009),
        backgroundCard = Color(0xFF1D170D),
        surfacePopover = Color(0xFF261E11),
    )

    val Jade = ThemeColorPalette(
        secondary = Color(0xFF22E58B),
        secondaryVariant = Color(0xFF06C7A3),
        accentGradient = listOf(Color(0xFF6EE7B7), Color(0xFF22E58B), Color(0xFF06C7A3)),
        nativeAccentHex = "#22E58B",
        onSecondary = Color(0xFF04140D),
        onSecondaryVariant = Color(0xFF04140D),
        focusRing = Color(0xFF6EE7B7),
        focusBackground = Color(0xFF0F3224),
        background = Color(0xFF060C09),
        backgroundElevated = Color(0xFF0B1510),
        backgroundCard = Color(0xFF102018),
        surfacePopover = Color(0xFF162B21),
    )

    val RoseGold = ThemeColorPalette(
        secondary = Color(0xFFF472B6),
        secondaryVariant = Color(0xFFC084FC),
        accentGradient = listOf(Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFFFDBA74)),
        nativeAccentHex = "#F472B6",
        onSecondary = Color(0xFF150810),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFF9A8D4),
        focusBackground = Color(0xFF3B1830),
        background = Color(0xFF0C070B),
        backgroundElevated = Color(0xFF160E15),
        backgroundCard = Color(0xFF20141F),
        surfacePopover = Color(0xFF2B1B2A),
    )

    val ArcticBlue = ThemeColorPalette(
        secondary = Color(0xFF38BDF8),
        secondaryVariant = Color(0xFF3B82F6),
        accentGradient = listOf(Color(0xFF67E8F9), Color(0xFF38BDF8), Color(0xFF3B82F6)),
        nativeAccentHex = "#38BDF8",
        onSecondary = Color(0xFF04131E),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFF7DD3FC),
        focusBackground = Color(0xFF0E2740),
        background = Color(0xFF060A11),
        backgroundElevated = Color(0xFF0B121E),
        backgroundCard = Color(0xFF111C2D),
        surfacePopover = Color(0xFF18263C),
    )

    val Graphite = ThemeColorPalette(
        secondary = Color(0xFFD0D7E2),
        secondaryVariant = Color(0xFF8B97A8),
        accentGradient = listOf(Color(0xFFF8FAFC), Color(0xFFD0D7E2), Color(0xFF8B97A8)),
        nativeAccentHex = "#D0D7E2",
        onSecondary = Color(0xFF0B0D11),
        onSecondaryVariant = Color.White,
        focusRing = Color(0xFFF1F5F9),
        focusBackground = Color(0xFF232833),
        background = Color(0xFF08090C),
        backgroundElevated = Color(0xFF101319),
        backgroundCard = Color(0xFF171B24),
        surfacePopover = Color(0xFF1F2530),
    )

    val Crimson = ThemeColorPalette(
        secondary = Color(0xFFF43F5E),
        secondaryVariant = Color(0xFFEF4444),
        accentGradient = listOf(Color(0xFFFB7185), Color(0xFFF43F5E), Color(0xFFDC2626)),
        nativeAccentHex = "#F43F5E",
        focusRing = Color(0xFFFDA4AF),
        focusBackground = Color(0xFF3B121D),
        background = Color(0xFF0D0709),
        backgroundElevated = Color(0xFF170D11),
        backgroundCard = Color(0xFF221319),
        surfacePopover = Color(0xFF2D1921),
    )

    val Ocean = ThemeColorPalette(
        secondary = Color(0xFF0EA5E9),
        secondaryVariant = Color(0xFF2563EB),
        accentGradient = listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF2563EB)),
        nativeAccentHex = "#0EA5E9",
        focusRing = Color(0xFF7DD3FC),
        focusBackground = Color(0xFF0E2942),
        background = Color(0xFF070A12),
        backgroundElevated = Color(0xFF0C1220),
        backgroundCard = Color(0xFF121B2E),
        surfacePopover = Color(0xFF18243D),
    )

    val Violet = ThemeColorPalette(
        secondary = Color(0xFFA855F7),
        secondaryVariant = Color(0xFFEC4899),
        accentGradient = listOf(Color(0xFFC084FC), Color(0xFFA855F7), Color(0xFFEC4899)),
        nativeAccentHex = "#A855F7",
        focusRing = Color(0xFFD8B4FE),
        focusBackground = Color(0xFF2B1445),
        background = Color(0xFF0A0712),
        backgroundElevated = Color(0xFF120D20),
        backgroundCard = Color(0xFF1A132E),
        surfacePopover = Color(0xFF241B3E),
    )

    val Emerald = ThemeColorPalette(
        secondary = Color(0xFF10B981),
        secondaryVariant = Color(0xFF059669),
        accentGradient = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669)),
        nativeAccentHex = "#10B981",
        onSecondary = Color(0xFF03140E),
        focusRing = Color(0xFF6EE7B7),
        focusBackground = Color(0xFF0C3124),
        background = Color(0xFF060E0A),
        backgroundElevated = Color(0xFF0B1812),
        backgroundCard = Color(0xFF11231B),
        surfacePopover = Color(0xFF183025),
    )

    val Amber = ThemeColorPalette(
        secondary = Color(0xFFF59E0B),
        secondaryVariant = Color(0xFFEA580C),
        accentGradient = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFEA580C)),
        nativeAccentHex = "#F59E0B",
        onSecondary = Color(0xFF140B02),
        focusRing = Color(0xFFFCD34D),
        focusBackground = Color(0xFF38220A),
        background = Color(0xFF0E0905),
        backgroundElevated = Color(0xFF19110A),
        backgroundCard = Color(0xFF24180E),
        surfacePopover = Color(0xFF302114),
    )

    val Rose = ThemeColorPalette(
        secondary = Color(0xFFEC4899),
        secondaryVariant = Color(0xFFE11D48),
        accentGradient = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFE11D48)),
        nativeAccentHex = "#EC4899",
        focusRing = Color(0xFFF9A8D4),
        focusBackground = Color(0xFF3B122A),
        background = Color(0xFF0E070B),
        backgroundElevated = Color(0xFF190E15),
        backgroundCard = Color(0xFF24141E),
        surfacePopover = Color(0xFF301B29),
    )

    val White = ThemeColorPalette(
        secondary = Color(0xFFF8FAFC),
        secondaryVariant = Color(0xFFCBD5E1),
        accentGradient = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
        nativeAccentHex = "#F8FAFC",
        onSecondary = Color(0xFF090B10),
        onSecondaryVariant = Color(0xFF090B10),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF252A34),
        background = Color(0xFF08090C),
        backgroundElevated = Color(0xFF101319),
        backgroundCard = Color(0xFF171C24),
        surfacePopover = Color(0xFF1F2631),
    )

    fun getColorPalette(
        theme: AppTheme,
        customColors: CustomThemeColors = CustomThemeColors.Default,
    ): ThemeColorPalette = when (theme) {
        AppTheme.CUSTOM -> customColors.toColorPalette()
        AppTheme.CINEMA_OBSIDIAN -> CinemaObsidian
        AppTheme.GOLD -> Gold
        AppTheme.JADE -> Jade
        AppTheme.ROSE_GOLD -> RoseGold
        AppTheme.ARCTIC_BLUE -> ArcticBlue
        AppTheme.GRAPHITE -> Graphite
        AppTheme.CRIMSON -> Crimson
        AppTheme.OCEAN -> Ocean
        AppTheme.VIOLET -> Violet
        AppTheme.EMERALD -> Emerald
        AppTheme.AMBER -> Amber
        AppTheme.ROSE -> Rose
        AppTheme.WHITE -> White
    }
}

package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Monochrome color scheme
 * Similar to Komikku's monochrome theme
 */
internal object MonochromeColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFE2E2E2),
        onPrimary = Color(0xFF303030),
        primaryContainer = Color(0xFF474747),
        onPrimaryContainer = Color(0xFFE2E2E2),
        inversePrimary = Color(0xFF303030),
        secondary = Color(0xFFC6C6C6),
        onSecondary = Color(0xFF303030),
        secondaryContainer = Color(0xFF474747),
        onSecondaryContainer = Color(0xFFE2E2E2),
        tertiary = Color(0xFFE2E2E2),
        onTertiary = Color(0xFF303030),
        tertiaryContainer = Color(0xFF474747),
        onTertiaryContainer = Color(0xFFE2E2E2),
        background = Color(0xFF1A1A1A),
        onBackground = Color(0xFFE2E2E2),
        surface = Color(0xFF1A1A1A),
        onSurface = Color(0xFFE2E2E2),
        surfaceVariant = Color(0xFF474747),
        onSurfaceVariant = Color(0xFFC6C6C6),
        surfaceTint = Color(0xFFE2E2E2),
        inverseSurface = Color(0xFFE2E2E2),
        inverseOnSurface = Color(0xFF1A1A1A),
        outline = Color(0xFF919191),
        surfaceContainerLowest = Color(0xFF0F0F0F),
        surfaceContainerLow = Color(0xFF1C1C1C),
        surfaceContainer = Color(0xFF1F1F1F),
        surfaceContainerHigh = Color(0xFF282828),
        surfaceContainerHighest = Color(0xFF333333),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2E2E2),
        onPrimaryContainer = Color(0xFF000000),
        inversePrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF303030),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E2E2),
        onSecondaryContainer = Color(0xFF303030),
        tertiary = Color(0xFF000000),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE2E2E2),
        onTertiaryContainer = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFE2E2E2),
        onSurfaceVariant = Color(0xFF303030),
        surfaceTint = Color(0xFF000000),
        inverseSurface = Color(0xFF303030),
        inverseOnSurface = Color(0xFFFFFFFF),
        outline = Color(0xFF757575),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F7F7),
        surfaceContainer = Color(0xFFF2F2F2),
        surfaceContainerHigh = Color(0xFFEDEDED),
        surfaceContainerHighest = Color(0xFFE2E2E2),
    )
}

package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Material 3 expressive tonal system shared by native M3 components and the
 * existing Compose Material 2 screens during their gradual migration.
 *
 * Respect the user's legacy theme seed (including custom and AMOLED themes)
 * and opt into wallpaper dynamic colors only when explicitly enabled.
 * This bridge intentionally avoids replacing the existing upstream theme
 * preference keys or network-related code.
 */
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun expressiveColorScheme(
    source: ExtendedColors,
    wallpaperColors: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return remember(source, wallpaperColors, context) {
        if (wallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (source.isNightMode) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            expressiveSeedScheme(source)
        }
    }
}

private fun expressiveSeedScheme(source: ExtendedColors): ColorScheme {
    val dark = source.isNightMode
    val seed = source.accent
    val surface = source.background
    val label = source.text
    val muted = source.textSecondary
    val primaryContainer = lerp(surface, seed, if (dark) 0.34f else 0.16f)
    val secondary = lerp(seed, label, if (dark) 0.22f else 0.28f)
    val tertiary = lerp(seed, Color(0xFFBA7C98), 0.34f)
    val secondaryContainer = lerp(surface, secondary, if (dark) 0.28f else 0.13f)
    val tertiaryContainer = lerp(surface, tertiary, if (dark) 0.24f else 0.13f)
    val low = lerp(surface, seed, if (dark) 0.065f else 0.035f)
    val middle = lerp(surface, seed, if (dark) 0.11f else 0.065f)
    val high = lerp(surface, seed, if (dark) 0.16f else 0.105f)
    val highest = lerp(surface, seed, if (dark) 0.22f else 0.145f)
    val outlineVariant = lerp(label, surface, 0.84f)
    val outline = lerp(label, surface, 0.60f)
    val onPrimary = source.onAccent
    val onTintContainer = label

    val common: ColorScheme = if (dark) darkColorScheme(
        primary = seed,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onTintContainer,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = label,
        tertiary = tertiary,
        onTertiary = onPrimary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = label,
        background = surface,
        onBackground = label,
        surface = surface,
        onSurface = label,
        surfaceVariant = high,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = low,
        surfaceContainer = middle,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        surfaceTint = seed,
    ) else lightColorScheme(
        primary = seed,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onTintContainer,
        secondary = secondary,
        onSecondary = onPrimary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = label,
        tertiary = tertiary,
        onTertiary = onPrimary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = label,
        background = surface,
        onBackground = label,
        surface = surface,
        onSurface = label,
        surfaceVariant = high,
        onSurfaceVariant = muted,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = low,
        surfaceContainer = middle,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        surfaceTint = seed,
    )
    return common
}

/**
 * Bridges M3 semantic color roles to the existing ExtendedColors contract,
 * making toolbars, cards, dialogs and settings coherent before all call-sites
 * can be migrated to androidx.compose.material3.MaterialTheme.
 */
fun ExtendedColors.applyExpressiveScheme(scheme: ColorScheme): ExtendedColors = copy(
    primary = scheme.primary,
    onPrimary = scheme.onPrimary,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    topBar = scheme.surface,
    onTopBar = scheme.onSurface,
    onTopBarSecondary = scheme.onSurfaceVariant,
    onTopBarActive = scheme.primary,
    topBarSurface = scheme.surfaceContainerHigh,
    onTopBarSurface = scheme.onSurfaceVariant,
    bottomBar = scheme.surfaceContainerLow,
    bottomBarSurface = scheme.secondaryContainer,
    onBottomBarSurface = scheme.onSecondaryContainer,
    text = scheme.onSurface,
    textSecondary = scheme.onSurfaceVariant,
    textDisabled = scheme.onSurfaceVariant.copy(alpha = 0.6f),
    background = scheme.background,
    chip = scheme.surfaceContainerLow,
    onChip = scheme.onSurfaceVariant,
    unselected = scheme.onSurfaceVariant,
    card = scheme.surfaceContainerLow,
    floorCard = scheme.surfaceContainer,
    divider = scheme.outlineVariant,
    shadow = scheme.onSurface.copy(alpha = 0.16f),
    indicator = scheme.secondaryContainer,
    windowBackground = scheme.background,
    placeholder = scheme.surfaceContainerHigh,
)

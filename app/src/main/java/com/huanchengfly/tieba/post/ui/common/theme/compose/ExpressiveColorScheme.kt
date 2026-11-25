package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeFidelity

private data class PaletteKey(
    val seed: Color?,
    val dark: Boolean,
    val amoled: Boolean,
    val wallpaperConfiguration: Configuration?,
    val wallpaperAccent: Int?,
)

private val palettes = LruCache<PaletteKey, ColorScheme>(16)

/** Native View attributes and Compose controls resolve their roles from the same palette. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun appColorScheme(
    context: Context,
    name: String,
    dark: Boolean,
    dynamic: Boolean,
    customSeed: Color?,
): ColorScheme {
    val seed = when (name) {
        ThemeUtil.THEME_PINK -> Color(context.getColor(R.color.theme_color_primary_pink))
        ThemeUtil.THEME_PURPLE -> Color(context.getColor(R.color.theme_color_primary_purple))
        ThemeUtil.THEME_RED -> Color(context.getColor(R.color.theme_color_primary_red))
        ThemeUtil.THEME_BLACK -> Color.Black
        ThemeUtil.THEME_BLUE, ThemeUtil.THEME_BLUE_DARK -> Color(context.getColor(R.color.tieba))
        ThemeUtil.THEME_CUSTOM -> customSeed
        else -> if (ThemeUtil.isTranslucentTheme(name)) customSeed else null
    }
    val useWallpaper = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val key = PaletteKey(
        seed = seed,
        dark = dark,
        amoled = name == ThemeUtil.THEME_AMOLED_DARK,
        wallpaperConfiguration = if (useWallpaper) Configuration(context.resources.configuration) else null,
        wallpaperAccent = if (useWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getColor(android.R.color.system_accent1_500)
        } else null,
    )
    palettes[key]?.let { return it }
    val base = when {
        useWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        seed != null -> schemeFromUserColor(seed, dark)
        dark -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    val scheme = if (key.amoled) {
        base.copy(surface = Color.Black, background = Color.Black, surfaceContainerLowest = Color.Black)
    } else base
    palettes.put(key, scheme)
    return scheme
}

/** A user-picked color keeps its hue. Material Color Utilities supplies the paired contrast roles. */
private fun schemeFromUserColor(seed: Color, dark: Boolean): ColorScheme {
    val scheme = SchemeFidelity(Hct.fromInt(seed.toArgb()), dark, 0.0)
    return ColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        inversePrimary = Color(scheme.inversePrimary),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceTint = Color(scheme.surfaceTint),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceDim = Color(scheme.surfaceDim),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest),
        primaryFixed = Color(scheme.primaryFixed),
        primaryFixedDim = Color(scheme.primaryFixedDim),
        onPrimaryFixed = Color(scheme.onPrimaryFixed),
        onPrimaryFixedVariant = Color(scheme.onPrimaryFixedVariant),
        secondaryFixed = Color(scheme.secondaryFixed),
        secondaryFixedDim = Color(scheme.secondaryFixedDim),
        onSecondaryFixed = Color(scheme.onSecondaryFixed),
        onSecondaryFixedVariant = Color(scheme.onSecondaryFixedVariant),
        tertiaryFixed = Color(scheme.tertiaryFixed),
        tertiaryFixedDim = Color(scheme.tertiaryFixedDim),
        onTertiaryFixed = Color(scheme.onTertiaryFixed),
        onTertiaryFixedVariant = Color(scheme.onTertiaryFixedVariant),
    )
}

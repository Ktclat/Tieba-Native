package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import com.huanchengfly.tieba.post.utils.ThemeUtil

val LocalAppThemeName = staticCompositionLocalOf { ThemeUtil.THEME_DEFAULT }
val LocalAppDarkTheme = staticCompositionLocalOf { false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TiebaLiteTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences by rememberDisplayPreferences()
    val rawName by ThemeUtil.themeState
    val dynamic = preferences.dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val name = if (dynamic && ThemeUtil.isTranslucentTheme(rawName)) {
        if (preferences.translucentDark) ThemeUtil.THEME_GREY_DARK else ThemeUtil.THEME_DEFAULT
    } else rawName
    val translucent = ThemeUtil.isTranslucentTheme(name)
    val dark = ThemeUtil.isNightMode(name) ||
        (translucent && preferences.translucentDark)
    val seed = if (translucent) preferences.wallpaperSeed else preferences.customSeed
    val palette = ThemeUtil.paletteForTheme(name, preferences.accentPalette, preferences.previousDayPalette)
    val colors = remember(context, LocalConfiguration.current, name, dark, dynamic, seed, palette) {
        appColorScheme(context, name, dark, dynamic, seed, palette)
    }
    CompositionLocalProvider(
        LocalDisplayPreferences provides preferences,
        LocalAppThemeName provides name,
        LocalAppDarkTheme provides dark,
    ) {
        MaterialExpressiveTheme(colorScheme = colors, content = content)
    }
}

internal fun String?.toThemeColor(): Color? =
    this?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }

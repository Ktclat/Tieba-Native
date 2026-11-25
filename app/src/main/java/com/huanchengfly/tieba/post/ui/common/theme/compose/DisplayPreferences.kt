package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
internal data class DisplayPreferences(
    val customSeed: Color? = null,
    val accentPalette: String? = null,
    val previousDayPalette: String? = null,
    val wallpaperSeed: Color? = null,
    val translucentDark: Boolean = false,
    val dynamicColor: Boolean = false,
    val toolbarPrimaryColor: Boolean = false,
    val customDarkStatusIcons: Boolean = false,
    val darkerStatusBar: Boolean = true,
    val hideBlockedContent: Boolean = false,
    val hideMedia: Boolean = false,
    val darkenImages: Boolean = true,
    val imageLoadType: Int = ImageUtil.SETTINGS_SMART_ORIGIN,
)

internal val LocalDisplayPreferences = compositionLocalOf { DisplayPreferences() }

private val customSeedKey = stringPreferencesKey("custom_primary_color")
private val wallpaperSeedKey = stringPreferencesKey("translucent_primary_color")
private val translucentThemeKey = intPreferencesKey("translucent_background_theme")
private val dynamicColorKey = booleanPreferencesKey("useDynamicColorTheme")
private val customDarkStatusIconsKey = booleanPreferencesKey("custom_status_bar_font_dark")
private val toolbarPrimaryKey = booleanPreferencesKey("custom_toolbar_primary_color")
private val darkerStatusBarKey = booleanPreferencesKey("status_bar_darker")
private val hideBlockedKey = booleanPreferencesKey("hideBlockedContent")
private val hideMediaKey = booleanPreferencesKey("hideMedia")
private val darkenImagesKey = booleanPreferencesKey("imageDarkenWhenNightMode")
private val imageLoadKey = stringPreferencesKey("image_load_type")

@Composable
internal fun rememberDisplayPreferences(): State<DisplayPreferences> {
    val store = LocalContext.current.dataStore
    val values = remember(store) {
        store.data.map {
            DisplayPreferences(
                previousDayPalette = it[stringPreferencesKey(ThemeUtil.KEY_OLD_THEME)],
                accentPalette = it[stringPreferencesKey(ThemeUtil.KEY_ACCENT_PALETTE)],
                customSeed = it[customSeedKey].toThemeColor(),
                wallpaperSeed = it[wallpaperSeedKey].toThemeColor(),
                translucentDark = it[translucentThemeKey] == ThemeUtil.TRANSLUCENT_THEME_DARK,
                dynamicColor = it[dynamicColorKey] ?: false,
                customDarkStatusIcons = it[customDarkStatusIconsKey] ?: false,
                toolbarPrimaryColor = it[toolbarPrimaryKey] ?: false,
                darkerStatusBar = it[darkerStatusBarKey] ?: true,
                hideBlockedContent = it[hideBlockedKey] ?: false,
                hideMedia = it[hideMediaKey] ?: false,
                darkenImages = it[darkenImagesKey] ?: true,
                imageLoadType = it[imageLoadKey]?.toIntOrNull() ?: ImageUtil.SETTINGS_SMART_ORIGIN,
            )
        }.distinctUntilChanged()
    }
    // Wait for the saved settings before displaying blocked content or fetching media.
    return values.collectAsState(initial = DisplayPreferences(
        hideBlockedContent = true,
        hideMedia = true,
        imageLoadType = ImageUtil.SETTINGS_ALL_NO,
    ))
}

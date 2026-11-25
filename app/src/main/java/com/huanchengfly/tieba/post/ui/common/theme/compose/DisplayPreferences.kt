package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
internal data class DisplayPreferences(
    val dynamicColor: Boolean = false,
    val toolbarPrimaryColor: Boolean = false,
    val darkerStatusBar: Boolean = true,
    val hideBlockedContent: Boolean = false,
    val hideMedia: Boolean = false,
    val darkenImages: Boolean = true,
    val imageLoadType: Int = ImageUtil.SETTINGS_SMART_ORIGIN,
)

internal val LocalDisplayPreferences = compositionLocalOf { DisplayPreferences() }

private val dynamicColorKey = booleanPreferencesKey("useDynamicColorTheme")
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
                dynamicColor = it[dynamicColorKey] ?: false,
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

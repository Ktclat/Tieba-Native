package com.huanchengfly.tieba.post.ui.common.theme

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.appColorScheme
import com.huanchengfly.tieba.post.utils.ThemeUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PaletteSelectionTest {
    @get:Rule val app = AppContextRule()

    @Test fun presetsAndCustomColorsSurviveWallpaperAndDayNightChanges() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = context.dataStore
        val original = store.data.first()
        val originalTheme = ThemeUtil.themeState.value
        val paletteKey = stringPreferencesKey(ThemeUtil.KEY_ACCENT_PALETTE)
        val customKey = stringPreferencesKey("custom_primary_color")
        val wallpaperKey = booleanPreferencesKey(ThemeUtil.KEY_USE_DYNAMIC_THEME)
        val darkIconsKey = booleanPreferencesKey(ThemeUtil.KEY_CUSTOM_STATUS_BAR_FONT_DARK)
        val custom = Color(0xFF087F5B)
        try {
            ThemeUtil.switchTheme(ThemeUtil.THEME_DEFAULT)
            ThemeUtil.selectPalette(ThemeUtil.THEME_CUSTOM, custom.toArgb(), toolbar = false, darkIcons = false)
            assertEquals("#FF087F5B", store.data.first()[customKey])
            assertEquals(false, store.data.first()[darkIconsKey])
            val manualLight = appColorScheme(context, ThemeUtil.THEME_CUSTOM, false, false, custom)
            assertEquals(manualLight.primary.toArgb(), App.ThemeDelegate.getColorByAttr(context, R.attr.colorPrimary))

            ThemeUtil.setWallpaperColors(true)
            assertEquals(true, store.data.first()[wallpaperKey])
            ThemeUtil.setWallpaperColors(false)
            assertEquals(ThemeUtil.THEME_CUSTOM, store.data.first()[paletteKey])
            assertEquals(manualLight.primary.toArgb(), App.ThemeDelegate.getColorByAttr(context, R.attr.colorPrimary))

            ThemeUtil.switchTheme(ThemeUtil.THEME_AMOLED_DARK)
            val manualDark = appColorScheme(context, ThemeUtil.THEME_AMOLED_DARK, true, false, custom, ThemeUtil.THEME_CUSTOM)
            assertEquals(manualDark.primary.toArgb(), App.ThemeDelegate.getColorByAttr(context, R.attr.colorPrimary))
            assertNotEquals(manualLight.primary, manualDark.primary)
            assertEquals(Color.Black, manualDark.surface)

            ThemeUtil.setWallpaperColors(true)
            ThemeUtil.selectPalette(ThemeUtil.THEME_GREEN)
            val restored = store.data.first()
            assertEquals(false, restored[wallpaperKey])
            assertEquals(ThemeUtil.THEME_AMOLED_DARK, restored[stringPreferencesKey(ThemeUtil.KEY_THEME)])
            assertEquals(ThemeUtil.THEME_GREEN, restored[paletteKey])
            assertEquals("#FF087F5B", restored[customKey])
            ThemeUtil.switchNightMode()
            assertEquals(ThemeUtil.THEME_GREEN, ThemeUtil.getRawTheme())
            assertEquals(appColorScheme(context, ThemeUtil.THEME_GREEN, false, false, null).primary.toArgb(),
                App.ThemeDelegate.getColorByAttr(context, R.attr.colorPrimary))
        } finally {
            store.updateData { original }
            ThemeUtil.themeState.value = originalTheme
        }
    }
}

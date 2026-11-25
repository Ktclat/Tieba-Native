package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.huanchengfly.tieba.post.ui.common.theme.compose.paletteSeed
import com.huanchengfly.tieba.post.ui.common.theme.compose.toThemeColor
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.interfaces.BackgroundTintable
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.theme.TintSwipeRefreshLayout
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import java.util.Locale

object ThemeUtil {
    val themeState: MutableState<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) dataStore.getString(KEY_THEME, THEME_DEFAULT)
            else THEME_DEFAULT
        )
    }

    const val TAG = "ThemeUtil"

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"
    const val KEY_USE_DYNAMIC_THEME = "useDynamicColorTheme"
    const val KEY_SWITCH_REASON = "switch_reason"
    const val KEY_TRANSLUCENT_PRIMARY_COLOR = "translucent_primary_color"
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color"
    const val KEY_TRANSLUCENT_THEME_BACKGROUND_PATH = "translucent_theme_background_path"
    const val KEY_ACCENT_PALETTE = "accent_palette"

    const val THEME_TRANSLUCENT = "translucent"
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"
    const val THEME_CUSTOM = "custom"
    const val THEME_DEFAULT = "tieba"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"
    const val THEME_GREEN = "forest"
    const val THEME_AMBER = "amber"
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    const val TRANSLUCENT_THEME_LIGHT = 0
    const val TRANSLUCENT_THEME_DARK = 1

    private val context: Context
        get() = INSTANCE

    val dataStore: DataStore<Preferences>
        get() = INSTANCE.dataStore

    fun getTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorText)
    }

    @JvmStatic
    fun getSecondaryTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorTextSecondary)
    }

    fun switchToNightMode(context: Activity) {
        switchToNightMode(context, true)
    }

    private fun refreshUI(activity: Activity?) {
        if (activity is BaseActivity) {
            activity.refreshUIIfNeed()
            return
        }
        ThemeUtils.refreshUI(activity)
    }

    private fun getOldTheme(): String {
        val oldTheme =
            dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        val previous = getRawTheme()
        runBlocking {
            dataStore.edit {
                if (recordOldTheme && !isNightMode(previous)) it[stringPreferencesKey(KEY_OLD_THEME)] = previous
                it[stringPreferencesKey(KEY_THEME)] = newTheme
                if (isTranslucentTheme(newTheme)) it[booleanPreferencesKey(KEY_USE_DYNAMIC_THEME)] = false
            }
        }
        themeState.value = newTheme
    }

    suspend fun setWallpaperColors(enabled: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(KEY_USE_DYNAMIC_THEME)] = enabled }
    }

    suspend fun selectPalette(palette: String, customColor: Int? = null, toolbar: Boolean = false, darkIcons: Boolean = false) {
        val current = getRawTheme()
        val values = dataStore.edit {
            it[booleanPreferencesKey(KEY_USE_DYNAMIC_THEME)] = false
            it[stringPreferencesKey(KEY_ACCENT_PALETTE)] = palette
            if (customColor != null) {
                it[stringPreferencesKey("custom_primary_color")] = String.format(Locale.ROOT, "#%08X", customColor)
                it[booleanPreferencesKey(KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR)] = toolbar
                it[booleanPreferencesKey(KEY_CUSTOM_STATUS_BAR_FONT_DARK)] = darkIcons
            }
            val night = isNightMode(current) || (isTranslucentTheme(current) &&
                it[intPreferencesKey("translucent_background_theme")] == TRANSLUCENT_THEME_DARK)
            it[stringPreferencesKey(KEY_OLD_THEME)] = palette
            it[stringPreferencesKey(KEY_THEME)] = if (night) {
                if (isNightMode(current)) current else it[stringPreferencesKey(KEY_DARK_THEME)] ?: THEME_GREY_DARK
            } else palette
        }
        themeState.value = values[stringPreferencesKey(KEY_THEME)] ?: palette
    }

    fun paletteForTheme(theme: String, selected: String?, previousDay: String?): String {
        if (isTranslucentTheme(theme)) return theme
        if (selected != null) return selected
        if (isNightMode(theme) && previousDay != null && !isNightMode(previousDay) &&
            !isTranslucentTheme(previousDay) && previousDay != THEME_DEFAULT) return previousDay
        return theme
    }

    fun isUsingDynamicTheme(): Boolean {
        return context.appPreferences.useDynamicColorTheme
    }

    fun switchNightMode() {
        if (isNightMode()) {
            switchTheme(getOldTheme(), recordOldTheme = false)
        } else {
            switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmStatic
    fun setChipThemeByLevel(level: String?, parent: View, vararg textViews: TextView) {
        setChipTheme(Util.getIconColorByLevel(level), parent, *textViews)
    }

    fun setChipTheme(@ColorInt color: Int, parent: View, vararg textViews: TextView) {
        parent.backgroundTintList = ColorStateList.valueOf(color)
        for (textView in textViews) {
            textView.setTextColor(ThemeUtils.getColorByAttr(parent.context, R.attr.colorOnAccent))
        }
    }

    @JvmStatic
    fun setThemeForSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        if (swipeRefreshLayout is TintSwipeRefreshLayout) {
            swipeRefreshLayout.tint()
            return
        }
        val context = swipeRefreshLayout.context
        val resources = context.resources
        if (resources != null) {
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.color_swipe_refresh_bg))
            swipeRefreshLayout.setColorSchemeColors(
                ThemeUtils.getColorByAttr(
                    context,
                    R.attr.colorAccent
                )
            )
        }
    }

    @JvmStatic
    fun isNightMode(): Boolean {
        return isNightMode(getRawTheme())
    }

    @JvmStatic
    fun isNightMode(theme: String): Boolean {
        return theme.lowercase(Locale.getDefault()).contains("dark") && !theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isTranslucentTheme(): Boolean {
        return isTranslucentTheme(getCurrentTheme())
    }

    @JvmStatic
    fun isTranslucentTheme(theme: String): Boolean {
        return theme.equals(
            THEME_TRANSLUCENT,
            ignoreCase = true
        ) || theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isDynamicTheme(theme: String): Boolean {
        return theme.endsWith("_dynamic")
    }

    fun isStatusBarFontDark(): Boolean {
        val theme = getCurrentTheme(checkDynamic = true)
        val prefs = context.appPreferences
        val palette = paletteForTheme(theme, dataStore.getString(KEY_ACCENT_PALETTE), prefs.oldTheme)
        if (!isDynamicTheme(theme) && palette == THEME_CUSTOM && prefs.toolbarPrimaryColor) {
            return prefs.customStatusBarFontDark
        }
        val toolbar = App.ThemeDelegate.getColorByAttr(context, R.attr.colorToolbar)
        return androidx.core.graphics.ColorUtils.calculateLuminance(toolbar) > 0.5
    }

    fun isNavigationBarFontDark(): Boolean {
        val bar = App.ThemeDelegate.getColorByAttr(context, R.attr.colorNavBar)
        return androidx.core.graphics.ColorUtils.calculateLuminance(bar) > 0.5
    }

    fun setTheme(context: Activity) {
        val nowTheme = getCurrentTheme()
        context.setTheme(getThemeByName(nowTheme))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isUsingDynamicTheme()) {
            context.theme.applyStyle(if (isNightMode(nowTheme))
                com.google.android.material.R.style.ThemeOverlay_Material3Expressive_DynamicColors_Dark
            else com.google.android.material.R.style.ThemeOverlay_Material3Expressive_DynamicColors_Light, true)
        } else if (!isTranslucentTheme(nowTheme) && DynamicColors.isDynamicColorAvailable()) {
            val prefs = context.appPreferences
            val palette = paletteForTheme(nowTheme, dataStore.getString(KEY_ACCENT_PALETTE), prefs.oldTheme)
            val seed = paletteSeed(context, palette, prefs.customPrimaryColor.toThemeColor())
            if (seed != null) {
                DynamicColors.applyToActivityIfAvailable(
                    context, DynamicColorsOptions.Builder().setContentBasedSource(seed.toArgb()).build(),
                )
            }
        }
    }

    @JvmOverloads
    fun getCurrentTheme(
        theme: String = getRawTheme(),
        checkDynamic: Boolean = false,
    ): String {
        var nowTheme = theme
        if (isTranslucentTheme(nowTheme)) {
            val colorTheme =
                dataStore.getInt("translucent_background_theme", TRANSLUCENT_THEME_LIGHT)
            val dark = colorTheme == TRANSLUCENT_THEME_DARK
            nowTheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isUsingDynamicTheme()) {
                if (dark) THEME_GREY_DARK else THEME_DEFAULT
            } else if (dark) THEME_TRANSLUCENT_DARK else THEME_TRANSLUCENT_LIGHT
        }
        if (checkDynamic && isUsingDynamicTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nowTheme = "${nowTheme}_dynamic"
        }
        return nowTheme
    }

    @JvmStatic
    fun setTranslucentThemeWebViewBackground(webView: WebView?) {
        if (webView == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        webView.setBackgroundColor(Color.WHITE)
    }

    private fun setAppBarFitsSystemWindow(view: View?, appBarFitsSystemWindow: Boolean) {
        if (view == null) return
        if (view is AppBarLayout) {
            view.setFitsSystemWindows(appBarFitsSystemWindow)
            view.clipToPadding = !appBarFitsSystemWindow
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setAppBarFitsSystemWindow(view.getChildAt(i), appBarFitsSystemWindow)
            }
        }
    }

    fun setTranslucentBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(Color.TRANSPARENT)
    }

    @JvmStatic
    fun setTranslucentDialogBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(
            ThemeUtils.getColorById(
                view.context,
                R.color.theme_color_card_grey_dark
            )
        )
    }

    fun setTranslucentThemeBackground(
        activity: BaseActivity,
        view: View?,
        setFitsSystemWindow: Boolean = true,
        useCache: Boolean = false,
    ) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            if (setFitsSystemWindow) {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = false
                (view as ViewGroup).clipToPadding = true
            }
            return
        }
        if (setFitsSystemWindow) {
            if (view is CoordinatorLayout) {
                setAppBarFitsSystemWindow(view, true)
                view.setFitsSystemWindows(false)
                (view as ViewGroup).clipToPadding = true
            } else {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = true
                (view as ViewGroup).clipToPadding = false
            }
        }
        view.backgroundTintList = null
        if (view is BackgroundTintable) {
            (view as BackgroundTintable).setBackgroundTintResId(0)
        }
        val backgroundFilePath = dataStore.getString(KEY_TRANSLUCENT_THEME_BACKGROUND_PATH)
        if (backgroundFilePath == null) {
            view.setBackgroundColor(Color.BLACK)
            return
        }
        if (useCache && translucentBackground != null &&
            (translucentBackground !is BitmapDrawable || !(translucentBackground as BitmapDrawable).bitmap.isRecycled)
        ) {
            view.background = translucentBackground
            return
        }
        activity.launch {
            val result = DisplayRequest(activity, newFileUri(backgroundFilePath)) {
                resizeScale(Scale.CENTER_CROP)
            }.execute()
            if (result is DisplayResult.Success) {
                if (useCache) {
                    translucentBackground = result.drawable
                }
                view.background = result.drawable
            } else {
                view.setBackgroundColor(Color.BLACK)
            }
        }
    }

    @StyleRes
    private fun getThemeByName(themeName: String): Int {
        return when (themeName.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT, THEME_TRANSLUCENT_LIGHT -> R.style.TiebaLite_Translucent_Light
            THEME_TRANSLUCENT_DARK -> R.style.TiebaLite_Translucent_Dark
            THEME_DEFAULT -> R.style.TiebaLite_Tieba
            THEME_BLACK -> R.style.TiebaLite_Black
            THEME_PURPLE -> R.style.TiebaLite_Purple
            THEME_PINK -> R.style.TiebaLite_Pink
            THEME_RED -> R.style.TiebaLite_Red
            THEME_BLUE_DARK -> R.style.TiebaLite_Dark_Blue
            THEME_GREY_DARK -> R.style.TiebaLite_Dark_Grey
            THEME_AMOLED_DARK -> R.style.TiebaLite_Dark_Amoled
            THEME_CUSTOM -> R.style.TiebaLite_Custom
            else -> R.style.TiebaLite_Tieba
        }
    }

    @JvmStatic
    fun getRawTheme(): String {
        val theme = themeState.value
        return when (theme.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT,
            THEME_TRANSLUCENT_LIGHT,
            THEME_TRANSLUCENT_DARK,
            THEME_CUSTOM,
            THEME_DEFAULT,
            THEME_BLACK,
            THEME_BLUE,
            THEME_GREEN,
            THEME_AMBER,
            THEME_PURPLE,
            THEME_PINK,
            THEME_RED,
            THEME_BLUE_DARK,
            THEME_GREY_DARK,
            THEME_AMOLED_DARK,
            -> theme.lowercase(Locale.getDefault())

            else -> THEME_DEFAULT
        }
    }
}
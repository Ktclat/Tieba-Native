package com.huanchengfly.tieba.post

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.toArgb
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.SketchFactory
import com.github.panpf.sketch.decode.GifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.decode.HeifAnimatedDrawableDecoder
import com.github.panpf.sketch.decode.WebpAnimatedDrawableDecoder
import com.github.panpf.sketch.http.OkHttpStack
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDrawableDecodeInterceptor
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.OAIDGetter
import com.huanchengfly.tieba.post.ui.common.theme.compose.appColorScheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.toThemeColor
import com.huanchengfly.tieba.post.ui.common.theme.interfaces.ThemeSwitcher
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.BlockManager
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.SharedPreferencesUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.Util
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.packageInfo
import dagger.hilt.android.HiltAndroidApp
import org.litepal.LitePal
import kotlin.concurrent.thread


@HiltAndroidApp
class App : Application(), SketchFactory {
    private val mActivityList: MutableList<Activity> = mutableListOf()

    override fun onCreate() {
        INSTANCE = this
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = Application.getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }
        ClientUtils.init(this)
        LitePal.initialize(this)
        AccountUtil.init(this)
        Config.init(this)
        AppIconUtil.setIcon()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        ThemeUtils.init(ThemeDelegate)
        registerActivityLifecycleCallbacks(ClipBoardLinkDetector)
        registerActivityLifecycleCallbacks(OAIDGetter)
        thread {
            BlockManager.init()
            EmoticonManager.init(this@App)
        }
    }

    //解决魅族 Flyme 系统夜间模式强制反色
    @Keep
    fun mzNightModeUseOf(): Int = 2

    //禁止app字体大小跟随系统字体大小调节
    override fun getResources(): Resources {
        //INSTANCE = this
        val fontScale = appPreferences.fontScale
        val resources = super.getResources()
        if (resources.configuration.fontScale != fontScale) {
            val configuration = resources.configuration
            configuration.fontScale = fontScale
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return resources
    }

    /**
     * 添加Activity
     */
    fun addActivity(activity: Activity) {
        // 判断当前集合中不存在该Activity
        if (!mActivityList.contains(activity)) {
            mActivityList.add(activity) //把当前Activity添加到集合中
        }
    }

    /**
     * 销毁单个Activity
     */
    @JvmOverloads
    fun removeActivity(activity: Activity, finish: Boolean = false) {
        //判断当前集合中存在该Activity
        if (mActivityList.contains(activity)) {
            mActivityList.remove(activity) //从集合中移除
            if (finish) activity.finish() //销毁当前Activity
        }
    }

    /**
     * 销毁所有的Activity
     */
    fun removeAllActivity() {
        //通过循环，把集合中的所有Activity销毁
        for (activity in mActivityList) {
            activity.finish()
        }
    }

    object Config {
        var inited: Boolean = false

        var isOAIDSupported: Boolean = false
        var statusCode: Int = -200
        var oaid: String = ""
        var encodedOAID: String = ""
        var isTrackLimited: Boolean = false
        // Native screens can render before a web request needs the WebView provider.
        val userAgent: String? by lazy {
            try {
                WebSettings.getDefaultUserAgent(INSTANCE)
            } catch (_: RuntimeException) {
                null
            }
        }
        var appFirstInstallTime: Long = 0L
        var appLastUpdateTime: Long = 0L

        fun init(context: Context) {
            if (!inited) {
                isOAIDSupported = DeviceID.supportedOAID(context)
                if (isOAIDSupported) {
                    DeviceID.getOAID(context, OAIDGetter)
                } else {
                    statusCode = -200
                    isTrackLimited = false
                }
                val installed = context.packageInfo
                appFirstInstallTime = installed.firstInstallTime
                appLastUpdateTime = installed.lastUpdateTime
                inited = true
            }
        }
    }

    object ScreenInfo {
        @JvmField
        var EXACT_SCREEN_HEIGHT = 0

        @JvmField
        var EXACT_SCREEN_WIDTH = 0

        @JvmField
        var SCREEN_HEIGHT = 0

        @JvmField
        var SCREEN_WIDTH = 0

        @JvmField
        var DENSITY = 0f
    }

    companion object {
        const val TAG = "App"

        @JvmStatic
        var translucentBackground: Drawable? = null

        @JvmStatic
        lateinit var INSTANCE: App
            private set

        val isInitialized: Boolean
            get() = this::INSTANCE.isInitialized

        val isSystemNight: Boolean
            get() = nightMode == Configuration.UI_MODE_NIGHT_YES

        val isFirstRun: Boolean
            get() = SharedPreferencesUtil.get(SharedPreferencesUtil.SP_APP_DATA)
                .getBoolean("first", true)

        private val nightMode: Int
            get() = INSTANCE.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    object ThemeDelegate : ThemeSwitcher {
        fun getColorByAttr(context: Context, attrId: Int, theme: String): Int {
            val name = theme.removeSuffix("_dynamic")
            val translucent = ThemeUtil.isTranslucentTheme(name)
            val dark = ThemeUtil.isNightMode(name) || name == ThemeUtil.THEME_TRANSLUCENT_DARK
            val prefs = context.appPreferences
            val palette = ThemeUtil.paletteForTheme(
                name, context.dataStore.getString(ThemeUtil.KEY_ACCENT_PALETTE),
                if (dark) prefs.oldTheme else null,
            )
            val userSeed = when {
                translucent -> prefs.translucentPrimaryColor.toThemeColor()
                palette == ThemeUtil.THEME_CUSTOM -> prefs.customPrimaryColor.toThemeColor()
                else -> null
            }
            val scheme = appColorScheme(context, name, dark, ThemeUtil.isDynamicTheme(theme) && !translucent, userSeed, palette)
            val color = when (attrId) {
                R.attr.colorPrimary, R.attr.colorNewPrimary, R.attr.colorAccent,
                R.attr.colorToolbarItemActive -> scheme.primary
                R.attr.colorTextOnPrimary, R.attr.colorOnAccent -> scheme.onPrimary
                R.attr.colorToolbar -> if (prefs.toolbarPrimaryColor) scheme.primary else scheme.surface
                R.attr.colorToolbarItem -> if (prefs.toolbarPrimaryColor) scheme.onPrimary else scheme.onSurface
                R.attr.colorToolbarItemSecondary -> if (prefs.toolbarPrimaryColor) scheme.onPrimary else scheme.onSurfaceVariant
                R.attr.colorToolbarSurface -> scheme.surfaceContainerHigh
                R.attr.colorText -> scheme.onSurface
                R.attr.color_text_disabled -> scheme.onSurface.copy(alpha = 0.38f)
                R.attr.colorTextSecondary, R.attr.colorUnselected, R.attr.colorOnToolbarSurface -> scheme.onSurfaceVariant
                R.attr.colorBackground, R.attr.colorWindowBackground -> scheme.surface
                R.attr.colorChip, R.attr.colorNavBarSurface -> scheme.secondaryContainer
                R.attr.colorOnChip, R.attr.colorOnNavBarSurface -> scheme.onSecondaryContainer
                R.attr.colorCard, R.attr.colorNavBar -> scheme.surfaceContainerLow
                R.attr.colorFloorCard -> scheme.surfaceContainer
                R.attr.colorDivider -> scheme.outlineVariant
                R.attr.shadow_color -> scheme.scrim
                R.attr.colorIndicator -> scheme.primaryContainer
                R.attr.colorPlaceholder -> scheme.surfaceContainerHigh
                else -> return Util.getColorByAttr(context, attrId, R.color.transparent)
            }
            return color.toArgb()
        }

        override fun getColorByAttr(context: Context, attrId: Int): Int =
            getColorByAttr(
                context, attrId,
                if (isInitialized) ThemeUtil.getCurrentTheme(checkDynamic = true) else ThemeUtil.THEME_DEFAULT,
            )

        override fun getColorById(context: Context, colorId: Int): Int {
            when (colorId) {
                R.color.default_color_primary -> return getColorByAttr(context, R.attr.colorPrimary)
                R.color.default_color_accent -> return getColorByAttr(context, R.attr.colorAccent)
                R.color.default_color_on_accent -> return getColorByAttr(
                    context,
                    R.attr.colorOnAccent
                )

                R.color.default_color_chip -> return getColorByAttr(
                    context,
                    R.attr.colorChip
                )

                R.color.default_color_background -> return getColorByAttr(
                    context,
                    R.attr.colorBackground
                )

                R.color.default_color_window_background -> return getColorByAttr(
                    context,
                    R.attr.colorWindowBackground
                )

                R.color.default_color_toolbar -> return getColorByAttr(context, R.attr.colorToolbar)
                R.color.default_color_toolbar_item -> return getColorByAttr(
                    context,
                    R.attr.colorToolbarItem
                )

                R.color.default_color_toolbar_item_active -> return getColorByAttr(
                    context,
                    R.attr.colorToolbarItemActive
                )
                R.color.default_color_toolbar_item_secondary -> return getColorByAttr(
                    context,
                    R.attr.colorToolbarItemSecondary
                )
                R.color.default_color_toolbar_bar -> return getColorByAttr(
                    context,
                    R.attr.colorToolbarSurface
                )
                R.color.default_color_on_toolbar_bar -> return getColorByAttr(
                    context,
                    R.attr.colorOnToolbarSurface
                )
                R.color.default_color_nav_bar_surface -> return getColorByAttr(
                    context,
                    R.attr.colorNavBarSurface
                )
                R.color.default_color_on_nav_bar_surface -> return getColorByAttr(
                    context,
                    R.attr.colorOnNavBarSurface
                )
                R.color.default_color_card -> return getColorByAttr(context, R.attr.colorCard)
                R.color.default_color_floor_card -> return getColorByAttr(
                    context,
                    R.attr.colorFloorCard
                )
                R.color.default_color_nav -> return getColorByAttr(context, R.attr.colorNavBar)
                R.color.default_color_shadow -> return getColorByAttr(context, R.attr.shadow_color)
                R.color.default_color_unselected -> return getColorByAttr(
                    context,
                    R.attr.colorUnselected
                )
                R.color.default_color_text -> return getColorByAttr(context, R.attr.colorText)
                R.color.default_color_text_on_primary -> return getColorByAttr(
                    context,
                    R.attr.colorTextOnPrimary
                )
                R.color.default_color_text_secondary -> return getColorByAttr(
                    context,
                    R.attr.colorTextSecondary
                )
                R.color.default_color_text_disabled -> return getColorByAttr(
                    context,
                    R.attr.color_text_disabled
                )
                R.color.default_color_divider -> return getColorByAttr(context, R.attr.colorDivider)
                R.color.default_color_swipe_refresh_view_background -> return getColorByAttr(
                    context,
                    R.attr.colorIndicator
                )
            }
            return context.getColorCompat(colorId)
        }
    }

    override fun createSketch(): Sketch = Sketch.Builder(this).apply {
        httpStack(OkHttpStack.Builder().apply {
            userAgent(System.getProperty("http.agent"))
        }.build())
        components {
            addDrawableDecodeInterceptor(PauseLoadWhenScrollingDrawableDecodeInterceptor())
            addDrawableDecoder(
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> GifAnimatedDrawableDecoder.Factory()
                    else -> GifMovieDrawableDecoder.Factory()
                }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addDrawableDecoder(WebpAnimatedDrawableDecoder.Factory())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                addDrawableDecoder(HeifAnimatedDrawableDecoder.Factory())
            }
        }
    }.build()
}

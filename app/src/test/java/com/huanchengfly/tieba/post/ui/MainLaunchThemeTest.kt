package com.huanchengfly.tieba.post.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35], application = Application::class)
class MainLaunchThemeTest {
    @Test fun splashHandsOffToAnOpaqueWindowWithTheSameDayOrNightSurface() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        for (mode in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            val config = Configuration(app.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
            }
            val context = app.createConfigurationContext(config)
            val splash = ContextThemeWrapper(context, R.style.Theme_Splash)
            val attrs = splash.obtainStyledAttributes(intArrayOf(
                androidx.core.splashscreen.R.attr.windowSplashScreenBackground,
                androidx.core.splashscreen.R.attr.postSplashScreenTheme,
            ))
            val launchColor = attrs.getColor(0, Color.TRANSPARENT)
            val hostStyle = attrs.getResourceId(1, 0)
            attrs.recycle()

            assertEquals(255, Color.alpha(launchColor))
            assertNotEquals(Color.BLACK, launchColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val platform = splash.obtainStyledAttributes(intArrayOf(android.R.attr.windowSplashScreenBackground))
                try {
                    assertEquals(launchColor, platform.getColor(0, Color.TRANSPARENT))
                } finally {
                    platform.recycle()
                }
            }
            assertEquals(R.style.Theme_NewAppTheme, hostStyle)
            val host = ContextThemeWrapper(context, hostStyle)
            val window = host.obtainStyledAttributes(intArrayOf(
                android.R.attr.windowBackground, android.R.attr.windowIsTranslucent,
            ))
            try {
                assertFalse(window.getBoolean(1, true))
                assertEquals(launchColor, (window.getDrawable(0) as ColorDrawable).color)
            } finally {
                window.recycle()
            }
        }
    }
}

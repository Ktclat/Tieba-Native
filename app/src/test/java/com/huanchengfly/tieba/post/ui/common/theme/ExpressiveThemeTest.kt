package com.huanchengfly.tieba.post.ui.common.theme

import android.app.Application
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.ThemeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ExpressiveThemeTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun appThemeUsesExpressiveMotionAndReadableContentInBothModes() {
        ThemeUtils.init(App.ThemeDelegate)
        val original = ThemeUtil.themeState.value
        try {
            ThemeUtil.themeState.value = ThemeUtil.THEME_DEFAULT
            var lightSurface = 0
            var lastSurface = 0
            compose.setContent {
                TiebaLiteTheme {
                    val motion = MaterialTheme.motionScheme
                    val colors = MaterialTheme.colorScheme
                    val shapes = MaterialTheme.shapes
                    val typography = MaterialTheme.typography
                    SideEffect {
                        assertEquals(MotionScheme.expressive().fastSpatialSpec<Float>(), motion.fastSpatialSpec<Float>())
                        assertEquals(Shapes().medium, shapes.medium)
                        assertEquals(Typography().bodyLarge, typography.bodyLarge)
                        lastSurface = colors.surface.toArgb()
                        assertTrue(ColorUtils.calculateContrast(colors.onSurface.toArgb(), lastSurface) >= 4.5)
                        assertTrue(ColorUtils.calculateContrast(colors.onPrimary.toArgb(), colors.primary.toArgb()) >= 4.5)
                    }
                }
            }
            compose.runOnIdle {
                lightSurface = lastSurface
                ThemeUtil.themeState.value = ThemeUtil.THEME_GREY_DARK
            }
            compose.runOnIdle {
                assertTrue(ColorUtils.calculateLuminance(lastSurface) < ColorUtils.calculateLuminance(lightSurface))
            }
        } finally {
            ThemeUtil.themeState.value = original
        }
    }

    @Test fun lightAndDarkActivityThemesInflateExpressiveMaterialButtons() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        for (style in listOf(R.style.AppTheme, R.style.AppTheme_Dark)) {
            val native = ContextThemeWrapper(context, style)
            val value = TypedValue()
            assertTrue(native.theme.resolveAttribute(MaterialR.attr.materialButtonStyle, value, true))
            assertEquals(MaterialR.style.Widget_Material3Expressive_Button, value.resourceId)
        }
    }
}

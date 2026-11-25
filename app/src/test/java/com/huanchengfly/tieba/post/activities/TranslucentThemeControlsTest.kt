package com.huanchengfly.tieba.post.activities

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.dialogs.showThemeColorPicker
import com.huanchengfly.tieba.post.ui.captureReviewImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TranslucentThemeControlsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandedThemeEditorProvidesOneSelectionAndLabeledSliders() {
        Robolectric.buildActivity(Activity::class.java).use { controller ->
            val activity = controller.get().apply { setTheme(R.style.AppTheme) }
            controller.setup()
            activity.setContentView(R.layout.activity_translucent_theme)
            val root = activity.findViewById<View>(R.id.background)
            val sheet = activity.findViewById<View>(R.id.bottom_sheet)
            BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            val themes = activity.findViewById<MaterialButtonToggleGroup>(R.id.color_theme)
            themes.check(R.id.light_color)
            activity.findViewById<View>(R.id.dark_color).performClick()
            assertEquals(listOf(R.id.dark_color), themes.checkedButtonIds)
            assertEquals("返回", activity.findViewById<View>(R.id.button_back).contentDescription)
            assertEquals(255f, activity.findViewById<Slider>(R.id.alpha).valueTo, 0f)
            assertEquals(100f, activity.findViewById<Slider>(R.id.blur).valueTo, 0f)
            shadowOf(Looper.getMainLooper()).idle()
            compose.captureReviewImage(root, "native-wallpaper-controls.png")
        }
    }

    @Test
    fun colorDialogRejectsIncompleteInputAndPreservesAlpha() {
        Robolectric.buildActivity(Activity::class.java).use { controller ->
            val activity = controller.get().apply { setTheme(R.style.AppTheme_Dark) }
            controller.setup()
            var selected = 0
            showThemeColorPicker(activity, Color.BLUE) { selected = it }
            shadowOf(Looper.getMainLooper()).idle()
            val dialog = ShadowDialog.getLatestDialog() as AlertDialog
            val hex = checkNotNull(dialog.findViewById<TextInputEditText>(R.id.color_hex))
            val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            hex.setText("#11")
            assertFalse(confirm.isEnabled)
            hex.setText("#80123456")
            assertTrue(confirm.isEnabled)
            hex.clearFocus()
            shadowOf(Looper.getMainLooper()).idle()
            compose.captureReviewImage(dialog.window!!.decorView, "native-wallpaper-color-dark.png")
            confirm.performClick()
            assertEquals(Color.parseColor("#80123456"), selected)
        }
    }
}

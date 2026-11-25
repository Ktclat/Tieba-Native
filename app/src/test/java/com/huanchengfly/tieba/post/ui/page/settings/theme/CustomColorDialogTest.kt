@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.app.Application
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.captureReviewImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomColorDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savesValidHexAndSliderChangesOnlyOnConfirmation() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var saved: Int? = null
        compose.setContent {
            MaterialExpressiveTheme {
                CustomColorDialog(0xFF416A84.toInt(), false, false, dismiss = {}, save = { argb, _, _ -> saved = argb })
            }
        }
        val confirm = compose.onNodeWithText(context.getString(R.string.button_finish))
        confirm.assertIsEnabled()
        val dialogView = compose.runOnIdle { checkNotNull(ShadowDialog.getLatestDialog().window).decorView }
        compose.captureReviewImage(dialogView, "theme-color-editor.png")
        val input = compose.onNode(hasSetTextAction())
        input.performTextReplacement("#12")
        confirm.assertIsNotEnabled()
        input.performTextReplacement("#ff0000")
        confirm.assertIsEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.theme_color_hue)).performSemanticsAction(SemanticsActions.SetProgress) {
            it(120f)
        }
        compose.onNodeWithText("#00FF00").assertExists()
        compose.runOnIdle { assertEquals(null, saved) }
        confirm.performClick()
        compose.runOnIdle { assertEquals(android.graphics.Color.GREEN, saved) }
    }
}

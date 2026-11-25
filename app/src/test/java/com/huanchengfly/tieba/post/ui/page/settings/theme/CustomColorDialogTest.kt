@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi::class,
)

package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BasicAlertDialogOverride
import androidx.compose.material3.BasicAlertDialogOverrideScope
import androidx.compose.material3.LocalBasicAlertDialogOverride
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CustomColorDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun savesValidHexAndSliderChangesOnlyOnConfirmation() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var saved: Int? = null
        lateinit var hostView: View
        compose.setContent {
            val view = LocalView.current
            SideEffect { hostView = view }
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalBasicAlertDialogOverride provides TestDialogWindow) {
                    CustomColorDialog(0xFF416A84.toInt(), false, false, dismiss = {}, save = { argb, _, _ -> saved = argb })
                }
            }
        }
        val confirm = compose.onNodeWithText(context.getString(R.string.button_finish))
        confirm.assertIsEnabled()
        compose.captureReviewImage(hostView, "theme-color-editor.png")
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

// The host JVM has one drawable window. Render the production AlertDialog content in that window.
private object TestDialogWindow : BasicAlertDialogOverride {
    @Composable
    override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier.widthIn(min = 280.dp, max = 320.dp), propagateMinConstraints = true) { content() }
        }
    }
}

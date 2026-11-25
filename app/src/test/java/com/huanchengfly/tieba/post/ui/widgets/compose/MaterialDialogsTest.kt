package com.huanchengfly.tieba.post.ui.widgets.compose

import android.app.Application
import android.view.View
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialDialogsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editsAndCancelsWithoutConfirmingThenSubmitsANewDraft() {
        val saved = mutableListOf<String>()
        var cancelled = 0
        compose.setContent {
            val state = rememberDialogState()
            TiebaLiteTheme {
                Button(onClick = state::show) { Text("编辑") }
                PromptDialog(
                    dialogState = state,
                    initialValue = "旧签名",
                    title = { Text("编辑签名") },
                    confirmText = "保存",
                    cancelText = "取消",
                    onCancel = { cancelled++ },
                    onConfirm = { saved.add(it) },
                )
            }
        }
        compose.onNodeWithText("编辑").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("临时输入")
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("旧签名").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("新签名")
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle {
            assertEquals(1, cancelled)
            assertEquals(listOf("新签名"), saved)
        }
    }

    @Test fun scheduledTimeUses24HourValuesAndShowsActionsAtLargeFont() {
        val saved = mutableListOf<String>()
        lateinit var dialogView: View
        compose.setContent {
            val state = rememberDialogState()
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.3f)) {
                TiebaLiteTheme {
                    Button(onClick = state::show) { Text("定时签到") }
                    TimePickerDialog(
                        title = {
                            val view = LocalView.current
                            SideEffect { dialogView = view }
                            Text("定时签到")
                        },
                        currentTime = "12:07",
                        confirmText = "保存",
                        cancelText = "取消",
                        onConfirm = { saved.add(it) },
                        dialogState = state,
                    )
                }
            }
        }
        compose.onNodeWithText("定时签到").performClick()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("07").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.onNodeWithText("保存").assertIsDisplayed()
        compose.captureReviewImage(dialogView, "scheduled-time-320-large.png")
        compose.onNodeWithText("保存").performClick()
        compose.runOnIdle { assertEquals(listOf("12:07"), saved) }
    }
}

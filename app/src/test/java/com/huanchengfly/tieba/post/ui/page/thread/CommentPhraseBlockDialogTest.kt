@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.huanchengfly.tieba.post.ui.InTestDialogWindow
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommentPhraseBlockDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blankSelectionCannotBeSavedAndCancellingDiscardsThePhrase() {
        var writes = 0
        var dismissals = 0
        compose.setContent {
            MaterialExpressiveTheme {
                InTestDialogWindow {
                    CommentPhraseBlockDialog("甲   乙", { writes++ }, { dismissals++ })
                }
            }
        }
        val confirm = compose.onNodeWithText("加入屏蔽")
        confirm.assertIsNotEnabled()
        select(1, 4)
        confirm.assertIsNotEnabled()
        select(0, 1)
        confirm.assertIsEnabled()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, writes)
        }
    }

    @Test fun failedSaveRetainsThePreciseMultilineSelectionForRetry() {
        val body = "前缀 C++.\n限定词 后缀"
        val phrase = " C++.\n限定词 "
        val writes = mutableListOf<String>()
        var dismissals = 0
        compose.setContent {
            MaterialExpressiveTheme {
                InTestDialogWindow {
                    CommentPhraseBlockDialog(
                        body = body,
                        onSave = {
                            writes += it
                            if (writes.size == 1) throw IOException("write")
                        },
                        onDismiss = { dismissals++ },
                    )
                }
            }
        }
        val start = body.indexOf(phrase)
        select(start, start + phrase.length)
        val confirm = compose.onNodeWithText("加入屏蔽")
        confirm.performClick()
        compose.onNodeWithText("保存失败，请重试。").assertExists()
        compose.runOnIdle { assertEquals(0, dismissals) }
        confirm.performClick()
        compose.runOnIdle {
            assertEquals(listOf(phrase, phrase), writes)
            assertEquals(1, dismissals)
        }
    }

    private fun select(start: Int, end: Int) {
        compose.onNodeWithTag("comment-phrase-selection")
            .performSemanticsAction(SemanticsActions.SetSelection) { setSelection ->
                check(setSelection(start, end, false))
            }
    }
}

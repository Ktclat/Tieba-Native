@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.settings.block

import android.app.Application
import android.view.View
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.utils.BlockRuleTarget
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
class BlockRuleEditorTest {
    @get:Rule val compose = createComposeRule()

    private fun awaitEnabledSave() {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("保存规则") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun invalidLineKeepsExistingRulesAndBackDiscardsTheDraft() {
        val saved = "加群|代练"
        val writes = mutableListOf<String>()
        var backs = 0
        lateinit var host: View
        compose.setContent {
            val view = LocalView.current
            SideEffect { host = view }
            MaterialExpressiveTheme {
                BlockRuleEditor(BlockRuleTarget.COMMENT, saved, { writes.add(it) }, { backs++ })
            }
        }
        val save = compose.onNodeWithText("保存规则")
        val field = compose.onNode(hasSetTextAction())
        save.assertIsNotEnabled()
        field.performTextReplacement("加群|代练\n免费领.*奖励")
        awaitEnabledSave()
        compose.captureReviewImage(host, "local-comment-regex-editor.png")

        field.performTextReplacement("加群|代练\n(")
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("第 2 行", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        save.assertIsNotEnabled()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.runOnIdle {
            assertEquals(1, backs)
            assertEquals(emptyList<String>(), writes)
        }
    }

    @Test fun failedSaveLeavesTheDraftEditableAndAnEmptyDraftCanClearTheCategory() {
        var attempts = 0
        var written: String? = null
        var backs = 0
        compose.setContent {
            MaterialExpressiveTheme {
                BlockRuleEditor(
                    target = BlockRuleTarget.FORUM,
                    savedText = "^(摄影|数码)$",
                    onSave = {
                        attempts++
                        if (attempts == 1) throw IOException("write")
                        written = it
                    },
                    onBack = { backs++ },
                )
            }
        }
        val save = compose.onNodeWithText("保存规则")
        val field = compose.onNode(hasSetTextAction())
        field.performTextReplacement("^(游戏|动漫)$")
        awaitEnabledSave()
        save.performClick()
        compose.onNodeWithText("保存失败，请重试。").assertExists()
        field.assertTextContains("^(游戏|动漫)$")
        save.assertIsEnabled()
        compose.runOnIdle { assertEquals(0, backs) }

        field.performTextReplacement("")
        awaitEnabledSave()
        save.performClick()
        compose.runOnIdle {
            assertEquals("", written)
            assertEquals(1, backs)
        }
    }
}

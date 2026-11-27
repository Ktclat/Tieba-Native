package com.huanchengfly.tieba.post.ui.page.subposts

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.InTestDialogWindow
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w320dp-h640dp-mdpi")
class SubPostRecipientActionsTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun separateRecipientKeepsSubsequentContentAndCopiesTheCompleteReply() {
        ThemeUtils.init(App.ThemeDelegate)
        val reply = SubPostList(
            id = 71,
            author = User(id = 12, name = "青禾"),
            content = listOf(
                PbContent(text = "回复 "),
                PbContent(type = 4, uid = 42, text = "@山月"),
                PbContent(text = "：傍晚再试试。"),
            ),
        )
        val renders = (reply.content.renders + TextContentRender("补一条记录。")).toImmutableList()
        val item = SubPostItemData(reply.wrapImmutable(), renders, blocked = false)
        val opened = mutableListOf<Long>()
        val copied = mutableListOf<String>()
        compose.setContent {
            InTestDialogWindow {
                TiebaLiteTheme {
                    ProvideNavigator(EmptyDestinationsNavigator) {
                        SubPostItem(item, onReplyClick = { opened += it.id }, onMenuCopyClick = { copied += it })
                    }
                }
            }
        }

        compose.onAllNodesWithText("@山月", substring = true, useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("补一条记录。", useUnmergedTree = true).performTouchInput { click() }
        compose.onNodeWithText("傍晚再试试。", useUnmergedTree = true).performTouchInput { longClick() }
        compose.onNodeWithText("复制").performClick()
        compose.onNodeWithText("补一条记录。", useUnmergedTree = true).performTouchInput { longClick() }
        compose.onNodeWithText("选词屏蔽").performClick()
        val selection = compose.onNodeWithTag("comment-phrase-selection").fetchSemanticsNode()
        assertEquals("傍晚再试试。补一条记录。", selection.config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle {
            assertEquals(listOf(71L), opened)
            assertEquals(listOf("回复 @山月：傍晚再试试。\n补一条记录。"), copied)
        }
    }
}

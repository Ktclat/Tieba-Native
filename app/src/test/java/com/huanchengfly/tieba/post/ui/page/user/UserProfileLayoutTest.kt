package com.huanchengfly.tieba.post.ui.page.user

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.user.post.PostListItemData
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostItem
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w412dp-h915dp-mdpi")
class UserProfileLayoutTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun copyUsesTheVisiblePublicIdAndMissingDetailsCollapse() {
        var user by mutableStateOf(
            User(id = 27, tieba_uid = "5986190117", name = "山月", nameShow = "贝拉拉贝拉✨",
                ip_address = "四川", tb_age = "0.9", sex = 1)
        )
        val copied = mutableListOf<String>()
        compose.setContent {
            TiebaLiteTheme {
                UserProfileDetail(user.wrapImmutable(), showBtn = false, onCopyIdClick = { copied += it })
            }
        }
        fun idText(value: String) = App.INSTANCE.getString(R.string.text_profile_user_id, value)
        compose.onNode(hasText("贝拉拉贝拉✨") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        compose.onNode(hasText(idText("5986190117")) and hasClickAction()).performClick()
        compose.onNodeWithText(App.INSTANCE.getString(R.string.text_profile_tb_age, "0.9")).assertIsDisplayed()
        compose.runOnIdle { user = user.copy(nameShow = "", tieba_uid = "", ip_address = "", tb_age = "", sex = 0) }
        compose.onNodeWithText("山月").assertIsDisplayed()
        compose.onNode(hasText(idText("27")) and hasClickAction()).performClick()
        compose.onAllNodesWithText("IP 属地", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("吧龄", substring = true).assertCountEquals(0)
        compose.runOnIdle { assertEquals(listOf("5986190117", "27"), copied) }
    }

    @Test
    @Config(qualifiers = "w411dp-h915dp-420dpi")
    fun publicIdFitsAtPhoneDensityBesideAVisibleFollowAction() {
        var user by mutableStateOf(User(id = 71409, name = "雾岛听风", nameShow = "雾岛听风", tieba_uid = "5986190117"))
        compose.setContent {
            TiebaLiteTheme {
                UserProfileDetail(user.wrapImmutable(), modifier = Modifier.width(411.dp).padding(16.dp))
            }
        }
        for (publicId in listOf("5986190117", "9223372036854775807")) {
            compose.runOnIdle { user = user.copy(tieba_uid = publicId) }
            val label = App.INSTANCE.getString(R.string.text_profile_user_id, publicId)
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout = results.single()
            val dimensions = "ID $publicId: box=${layout.size}, lines=${layout.lineCount}, " +
                "width=${layout.multiParagraph.width}, height=${layout.multiParagraph.height}"
            assertFalse(dimensions, layout.hasVisualOverflow)
            assertEquals(dimensions, 1, layout.lineCount)
            assertEquals(label.length, layout.getLineEnd(0, visibleEnd = true))
        }
    }

    @Test fun narrowPanelStacksActionWhileRequestsDisableIt() {
        val user = User(id = 89, name = "主页昵称", tb_age = "3", concern_num = 100, fans_num = 200).wrapImmutable()
        var enabled by mutableStateOf(false)
        var followCount = 0
        compose.setContent {
            TiebaLiteTheme {
                UserProfileDetail(user, modifier = Modifier.width(248.dp), actionEnabled = enabled, onBtnClick = { followCount++ })
            }
        }
        val button = compose.onNode(hasText(App.INSTANCE.getString(R.string.button_follow)) and hasClickAction())
        button.assertIsNotEnabled()
        val age = compose.onNodeWithText(App.INSTANCE.getString(R.string.text_profile_tb_age, "3"))
        assertTrue(button.fetchSemanticsNode().boundsInRoot.top >= age.fetchSemanticsNode().boundsInRoot.bottom)
        compose.runOnIdle { enabled = true }
        button.performClick()
        compose.runOnIdle { assertEquals(1, followCount) }
    }

    @Test fun profilePostCardHidesOnlyAnExactDuplicateOfItsVisibleTitle() {
        val title = "相机入门"
        val initial = PostInfoList(thread_id = 7, is_thread = 1, title = title,
            rich_abstract = listOf(PbContent(type = 0, text = title)))
        var item by mutableStateOf(initial)
        compose.setContent {
            TiebaLiteTheme {
                ProvideNavigator(EmptyDestinationsNavigator) {
                    UserPostItem(PostListItemData(item.wrapImmutable()), onAgree = {})
                }
            }
        }
        compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { item = initial.copy(rich_abstract = listOf(PbContent(text = "$title，从曝光开始。"))) }
        compose.onNodeWithText("$title\n$title，从曝光开始。", useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { item = initial.copy(is_ntitle = 1) }
        compose.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }
}

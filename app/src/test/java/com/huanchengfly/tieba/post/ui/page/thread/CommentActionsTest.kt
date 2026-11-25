package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.subPosts
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
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
class CommentActionsTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun authorLikeAndReplyTargetsRemainIndependent() {
        val reply = SubPostList(
            id = 81,
            author = User(id = 20, name = "竹影"),
            content = listOf(PbContent(text = "昨天下午拍的。")),
        )
        val post = Post(
            id = 45,
            floor = 2,
            author = User(id = 10, name = "秋山"),
            content = listOf(PbContent(text = "这张照片是什么时候拍的？")),
            sub_post_number = 1,
            sub_post_list = SubPost(sub_post_list = listOf(reply)),
        )
        val actions = mutableListOf<String>()
        compose.setContent {
            TiebaLiteTheme {
                ProvideNavigator(EmptyDestinationsNavigator) {
                    PostCard(
                        postHolder = post.wrapImmutable(),
                        contentRenders = persistentListOf(TextContentRender(post.content.single().text)),
                        subPosts = post.subPosts,
                        onUserClick = { actions += "user-${it.id}" },
                        onAgree = { actions += "like" },
                        onReplyClick = { actions += "reply-${it.id}" },
                        onOpenSubPosts = { actions += "open-$it" },
                        onSubPostReplyClick = { parent, child -> actions += "reply-${parent.id}-${child.id}" },
                        onMenuCopyClick = { actions += it },
                    )
                }
            }
        }

        fun tapText(text: String) = compose.onNodeWithText(text, substring = true, useUnmergedTree = true)
            .performTouchInput { click() }

        tapText("秋山")
        tapText("竹影")
        compose.onNodeWithContentDescription("点赞", useUnmergedTree = true).performTouchInput { click() }
        tapText(post.content.single().text)
        val previewBody = compose.onNodeWithText(reply.content.single().text, useUnmergedTree = true)
        previewBody.performTouchInput { click() }
        previewBody.performTouchInput { longClick() }
        compose.onNodeWithText("回复").performClick()
        previewBody.performTouchInput { longClick() }
        compose.onNodeWithText("复制").performClick()

        compose.runOnIdle {
            assertEquals(listOf("user-10", "user-20", "like", "reply-45", "open-81", "reply-45-81",
                reply.content.single().text), actions)
        }
    }

    @Test
    fun hidingRepliesPreservesTotalAndLeavesNoExtraDividerOrEmptyPanel() {
        runBlocking {
            App.INSTANCE.dataStore.edit { it[booleanPreferencesKey("hideBlockedContent")] = true }
        }
        val first = SubPostItemData(
            SubPostList(id = 10).wrapImmutable(), AnnotatedString("可见回复"), blocked = false,
        )
        val hidden = SubPostItemData(
            SubPostList(id = 11).wrapImmutable(), AnnotatedString("隐藏回复"), blocked = true,
        )
        var previews by mutableStateOf(persistentListOf(first))
        var total by mutableStateOf(1)
        compose.setContent {
            TiebaLiteTheme {
                ProvideNavigator(EmptyDestinationsNavigator) {
                    Box(Modifier.testTag("floor")) {
                        PostCard(
                            postHolder = Post(id = 1, floor = 2, sub_post_number = total).wrapImmutable(),
                            contentRenders = persistentListOf(TextContentRender("楼层正文")),
                            subPosts = previews,
                        )
                    }
                }
            }
        }

        fun height() = compose.onNodeWithTag("floor").fetchSemanticsNode().boundsInRoot.height
        val oneReplyHeight = height()
        compose.runOnIdle {
            previews = persistentListOf(first, hidden)
            total = 2
        }
        compose.onNodeWithText("隐藏回复", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("查看全部", substring = true).assertDoesNotExist()
        assertEquals(oneReplyHeight, height(), 0f)

        compose.runOnIdle {
            previews = persistentListOf(hidden)
            total = 3
        }
        compose.onNodeWithText("查看全部 3 条回复").assertExists()
        compose.runOnIdle { total = 1 }
        val hiddenOnlyHeight = height()
        compose.runOnIdle {
            previews = persistentListOf()
            total = 0
        }
        assertEquals(height(), hiddenOnlyHeight, 0f)
    }
}

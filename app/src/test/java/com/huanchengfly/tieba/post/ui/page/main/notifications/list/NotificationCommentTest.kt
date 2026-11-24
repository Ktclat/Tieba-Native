package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.api.models.MessageListBean.MessageInfoBean
import com.huanchengfly.tieba.post.api.models.MessageListBean.ReplyerInfoBean
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowSizeClass
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationCommentTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var view: View
    private val routes = mutableListOf<String>()
    private val reply = MessageInfoBean(
        threadId = "101", postId = "202", quotePid = "303", isFloor = "1",
        replyer = ReplyerInfoBean(id = "42", nameShow = "北窗"),
        time = "1700000000", content = "可以把阅读设置里的字号调大一级，预览会实时更新。",
        quoteContent = "小屏手机怎么看清帖子里的文字？",
    )
    private val mention = MessageInfoBean(
        threadId = "404", postId = "505", title = "分享几本周末读的书",
        time = "1700001000", content = "这里也有一份适合入门的书单。",
    )

    private fun show(vararg messages: Pair<NotificationsType, MessageInfoBean>) {
        compose.setContent {
            val local = LocalView.current
            SideEffect { view = local }
            val window = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
            CompositionLocalProvider(LocalWindowSizeClass provides window) {
                TiebaLiteTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column {
                            for ((type, info) in messages) {
                                NotificationCard(type, info, blocked = false, onNavigate = { routes += it.route })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun tapText(text: String) {
        // Tap the glyphs; the center of a merged card can lie over its separate quote action.
        compose.onNodeWithText(text, useUnmergedTree = true).performTouchInput { click(center) }
    }

    @Test fun bodyAuthorAndQuoteKeepTheirOwnDestinations() {
        show(NotificationsType.ReplyMe to reply, NotificationsType.AtMe to mention)
        val replyText = requireNotNull(reply.content)
        val quoteText = requireNotNull(reply.quoteContent)
        val author = compose.onNodeWithText("北窗", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithText(replyText, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val quote = compose.onNodeWithText(quoteText, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("author=$author, body=$body, quote=$quote", author.bottom <= body.top && body.bottom <= quote.top)
        compose.onNodeWithText("引用的回复", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("提到我的主题", useUnmergedTree = true).assertIsDisplayed()
        val time = DateTimeUtils.getRelativeTimeString(ApplicationProvider.getApplicationContext(), mention.time!!)
        compose.onNodeWithText(time, substring = true, useUnmergedTree = true).assertIsDisplayed()
        println("Notification bounds: author=$author, body=$body, quote=$quote")
        compose.captureReviewImage(view, "notifications-comments.png")

        tapText("北窗")
        tapText(replyText)
        tapText(quoteText)
        tapText(mention.content!!)
        tapText(mention.title!!)
        val replyAction = compose.onNodeWithText(requireNotNull(mention.content))
        val sourceAction = compose.onNodeWithText(requireNotNull(mention.title))
        assertEquals("查看回复", replyAction.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        assertEquals("查看原文", sourceAction.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
        replyAction.performSemanticsAction(SemanticsActions.OnClick)
        sourceAction.performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle {
            assertEquals(
                listOf(
                    UserProfilePageDestination(42L).route,
                    SubPostsPageDestination(threadId = 101L, subPostId = 202L, loadFromSubPost = true).route,
                    SubPostsPageDestination(threadId = 101L, postId = 303L, loadFromSubPost = true).route,
                    ThreadPageDestination(threadId = 404L, postId = 505L).route,
                    ThreadPageDestination(threadId = 404L).route,
                    ThreadPageDestination(threadId = 404L, postId = 505L).route,
                    ThreadPageDestination(threadId = 404L).route,
                ),
                routes,
            )
        }
    }

    @Test fun missingFloorTargetsCanStillOpenTheParentThread() {
        val partial = reply.copy(postId = "", quotePid = "not-a-pid")
        show(NotificationsType.ReplyMe to partial)
        tapText(requireNotNull(partial.content))
        tapText(requireNotNull(partial.quoteContent))
        compose.runOnIdle {
            assertEquals(List(2) { ThreadPageDestination(threadId = 101L).route }, routes)
        }
    }

    @Test fun listKeysSurviveInsertionsAndHandleRepeatedPartialNotifications() {
        val known = MessageItemData(reply, blocked = false)
        val partial = MessageItemData(mention.copy(postId = null), blocked = false)
        val keys = notificationKeys(listOf(known, partial, partial))
        assertEquals("202_42_1700000000", keys.first())
        assertEquals(3, keys.toSet().size)
        val inserted = known.copy(info = reply.copy(postId = "808"))
        assertEquals(keys, notificationKeys(listOf(inserted, known, partial, partial)).drop(1))
    }

    @Test fun malformedIdsAndTimestampLeaveReadableContentWithoutBrokenActions() {
        show(NotificationsType.ReplyMe to reply.copy(
            threadId = "not-an-id", time = "not-a-time",
            replyer = ReplyerInfoBean(id = "0", name = "保留的昵称", nameShow = " "),
        ))
        compose.onNodeWithText("保留的昵称").assertIsDisplayed()
        compose.onNodeWithText(reply.content!!).assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}

package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.navigation.NavOptionsBuilder
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DirectedReplyTest {
    @get:Rule val compose = createComposeRule()

    private fun mention(name: String, id: String = "42") = buildAnnotatedString {
        pushStringAnnotation("user", id)
        append(name)
        pop()
    }

    @Test
    fun separatesExplicitReceiverAndPreservesOtherMentionsLinksAndLineBreaks() {
        val recipient = mention("@北窗")
        val body = buildAnnotatedString {
            append("这张是昨天拍的。\n")
            append(mention("@江岸", "57"))
            append(" 看看")
            pushStringAnnotation("url", "https://example.org/photo")
            append("原图")
            pop()
        }
        for (delimiter in listOf(":", " ：", "： ")) {
            val original = AnnotatedString("回复 ") + recipient + AnnotatedString(delimiter) + body
            assertEquals(DirectedReply(42, "@北窗", body), original.splitDirectedReply())
        }
        val captionBeforeMedia = AnnotatedString("回复") + recipient + AnnotatedString(":")
        assertEquals("", captionBeforeMedia.splitDirectedReply()?.body?.text)
        val splitAtSign = AnnotatedString("回复 @") + mention("北窗") + AnnotatedString(":") + body
        assertEquals(DirectedReply(42, "@北窗", body), splitAtSign.splitDirectedReply())
    }

    @Test
    fun keepsOrdinaryMentionsUnlinkedNamesAndAmbiguousPrefixesInTheMessage() {
        val user = mention("@北窗")
        val messages = listOf(
            AnnotatedString("回复 @北窗：没有可验证的用户"),
            user + AnnotatedString(" 提到的角度很好"),
            AnnotatedString("请回复 ") + user + AnnotatedString("：明天继续"),
            AnnotatedString("回复 ") + user + AnnotatedString(" 的问题"),
            AnnotatedString("回复 ") + mention("@北窗", "0") + AnnotatedString(":早"),
            AnnotatedString("回复 ") + mention("@北窗", "bad") + AnnotatedString(":早"),
        )
        messages.forEach { assertNull(it.splitDirectedReply()) }
    }

    @Test
    fun recipientOpensTheAddressedProfile() {
        var route: String? = null
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigate(direction: Direction, onlyIfResumed: Boolean, builder: NavOptionsBuilder.() -> Unit) {
                route = direction.route
            }
        }
        compose.setContent {
            ProvideNavigator(navigator) {
                ReplyRecipient(DirectedReply(42, "@北窗", AnnotatedString("正文")))
            }
        }
        compose.onNodeWithText("回复 @北窗").performClick()
        compose.runOnIdle { assertEquals(UserProfilePageDestination(42).route, route) }
    }
}

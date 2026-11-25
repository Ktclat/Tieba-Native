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
    fun separatesPlainRecipientsWithoutInventingAProfileOrChangingTheBody() {
        val raw = AnnotatedString("回复 木偶鼹鼠 :这价格")
        assertEquals(DirectedReply(null, "木偶鼹鼠", AnnotatedString("这价格")), raw.splitDirectedReply())
        assertEquals("回复 木偶鼹鼠 :这价格", raw.text)

        val body = AnnotatedString("  看看\n") + mention("@江岸", "57")
        for (prefix in listOf("回复 @北窗： ", "回复@北窗: ", "回复\u3000@北窗 ： ")) {
            assertEquals(DirectedReply(null, "@北窗", body), (AnnotatedString(prefix) + body).splitDirectedReply())
        }
        assertEquals("", AnnotatedString("回复 北窗：").splitDirectedReply()?.body?.text)
    }

    @Test
    fun keepsOrdinaryMentionsAndAmbiguousPrefixesInTheMessage() {
        val user = mention("@北窗")
        val messages = listOf(
            AnnotatedString("昨天回复 北窗：早"),
            AnnotatedString("回复北窗：早"),
            AnnotatedString("回复 北窗的提问"),
            AnnotatedString("回复 这个问题：有两个原因"),
            AnnotatedString("回复 如下：第一条"),
            AnnotatedString("回复 你的评论：我同意"),
            AnnotatedString("回复 北窗 的问题：明天继续"),
            AnnotatedString("回复\n北窗：跨行"),
            AnnotatedString("回复 北窗\n：跨行"),
            AnnotatedString("回复 北窗，江岸：大家好"),
            AnnotatedString("回复 （北窗）：早"),
            AnnotatedString("回复 " + "窗".repeat(25) + "：早"),
            user + AnnotatedString(" 提到的角度很好"),
            AnnotatedString("请回复 ") + user + AnnotatedString("：明天继续"),
            AnnotatedString("回复 ") + user + AnnotatedString(" 的问题"),
            AnnotatedString("回复 ") + mention("@北窗", "0") + AnnotatedString(":早"),
            AnnotatedString("回复 ") + mention("@北窗", "bad") + AnnotatedString(":早"),
            AnnotatedString("回复 北") + mention("窗") + AnnotatedString(":早"),
            buildAnnotatedString {
                append("回复 ")
                pushStringAnnotation("url", "https://example.org/")
                append("北窗")
                pop()
                append("：早")
            },
        )
        messages.forEach { assertNull(it.text, it.splitDirectedReply()) }
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

package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.MessageListBean.MessageInfoBean
import com.huanchengfly.tieba.post.api.models.MessageListBean.ReplyerInfoBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.blocksNotification
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscussionBlockRulesTest {
    private fun floor(number: Int, user: User, text: String, blocked: Boolean = false) =
        PostItemData(
            post = Post(id = number.toLong(), floor = number, author = user).wrapImmutable(),
            blocked = blocked, contentRenders = persistentListOf(TextContentRender(text)), subPosts = persistentListOf(),
        )

    @Test fun ruleChangesPreserveTheTopicOriginalFloorsAndLegacyBlocks() {
        val normal = User(name = "读者")
        val rows = persistentListOf(
            floor(1, normal, "广告123"), floor(2, normal, "广告123"),
            floor(3, User(name = "bot77", nameShow = "游客"), "正常"),
            floor(4, User(name = "anonymous", nameShow = "BOT88"), "正常"),
            floor(11, normal, "正常", blocked = true), floor(15, normal, "保留"),
        )
        val rules = LocalBlockRules.fromText(user = "^bot[0-9]+$", comment = "^广告[0-9]+$")
        assertEquals(listOf(false, true, true, true, true, false), rules.displayFloors(rows, hide = false).map { it.blocked })
        assertEquals(listOf(1, 15), rules.displayFloors(rows, hide = true).map { it.post.get { floor } })
        assertEquals(listOf(1, 2, 3, 4, 15), LocalBlockRules.Empty.displayFloors(rows, true).map { it.post.get { floor } })
        assertSame(rows, LocalBlockRules.Empty.displayFloors(rows, false))
    }

    @Test fun previewAndExpandedDirectedRepliesMatchTheSameBodyIndependentlyOfRecipient() {
        val display = AnnotatedString("回复 @被提及的人：刷屏123")
        val preview = display.visibleReplyBody()
        val expanded = listOf(TextContentRender(display)).visibleReplyBody()
        assertEquals("刷屏123", preview)
        assertEquals(preview, expanded)
        val rules = LocalBlockRules.fromText(user = "被提及", comment = "^刷屏[0-9]+$")
        assertTrue(rules.blocksNestedReply(null) { expanded })
        assertFalse(rules.blocksNestedReply(User(name = "普通用户")) { "保留" })
        assertTrue(rules.blocksNestedReply(User(nameShow = "被提及的人")) { "保留" })
    }

    @Test fun notificationChecksForumSenderAndIncomingTextIndependentlyOfTopicTitleAndQuote() {
        val message = MessageInfoBean(title = "广告123", quoteContent = "广告123", content = "保留")
        val rules = LocalBlockRules.fromText(forum = "^闲聊", user = "^机器人", comment = "^广告")
        assertFalse(rules.blocksNotification(message))
        assertTrue(rules.blocksNotification(message.copy(forumName = "闲聊室")))
        assertTrue(rules.blocksNotification(message.copy(replyer = ReplyerInfoBean(nameShow = "机器人77"))))
        assertTrue(rules.blocksNotification(message.copy(content = "广告123")))
    }
}

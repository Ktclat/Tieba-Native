package com.huanchengfly.tieba.post.ui.page.user.post

import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProfilePostRulesTest {
    private val rules = LocalBlockRules.fromText(forum = "^推广$", user = "^推广号[0-9]+$", comment = "领券|代购")
    private val topic = PostInfoList(is_thread = 1, forum_name = "游戏", title = "领券分析")

    @Test fun replyRulesActOnEachReplyAndKeepItsNavigationTarget() {
        val hidden = PostContentData("私聊领券", 1, 10, false)
        val shown = PostContentData("值得一读", 2, 11, true)
        val entry = PostListItemData(ImmutableHolder(topic.copy(is_thread = 0)), contents = persistentListOf(hidden, shown))
        assertEquals(listOf(shown), rules.visibleProfilePost(entry)?.contents)
        assertEquals(listOf(hidden, shown), entry.contents)
        assertNull(rules.visibleProfilePost(entry.copy(contents = persistentListOf(hidden))))
    }

    @Test fun topicTitlesIgnoreCommentRulesAndRepostsRespectTheOriginalAuthorAndForum() {
        val plain = PostListItemData(ImmutableHolder(topic))
        assertSame(plain, rules.visibleProfilePost(plain))
        val blockedOrigins = listOf(
            OriginThreadInfo(fname = "推广"),
            OriginThreadInfo(fname = "游戏", author = User(name = "小李", nameShow = "推广号43")),
        )
        for (origin in blockedOrigins) {
            assertNull(rules.visibleProfilePost(PostListItemData(ImmutableHolder(topic.copy(origin_thread_info = origin)))))
        }
        assertNull(rules.visibleProfilePost(PostListItemData(ImmutableHolder(topic.copy(name_show = "推广号25")))))
        assertNull(rules.visibleProfilePost(PostListItemData(ImmutableHolder(topic.copy(forum_name = "推广")))))
    }
}

package com.huanchengfly.tieba.post.ui.page.forum

import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.ui.models.blocksThread
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedThreadRulesTest {
    @Test fun feedsApplyForumAndAuthorRulesToPostsAndReposts() {
        val rules = LocalBlockRules.fromText(forum = "^游戏$", user = "广告[0-9]+", comment = "摄影")
        val post = ThreadInfo(title = "摄影", forumName = "摄影")
        assertFalse(rules.blocksThread(post))
        assertTrue(rules.blocksThread(post.copy(author = User(nameShow = "广告23"))))
        assertTrue(rules.blocksThread(post.copy(forumInfo = SimpleForum(name = "游戏"))))
        assertTrue(rules.blocksThread(post.copy(origin_thread_info = OriginThreadInfo(fname = "游戏"))))
        assertTrue(rules.blocksThread(post.copy(origin_thread_info = OriginThreadInfo(author = User(name = "广告2")))))
    }
}

package com.huanchengfly.tieba.post.ui.page.search.thread

import android.app.Application
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import com.huanchengfly.tieba.post.ui.models.blocksSearchResult
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SearchResultNavigationTest {
    private val author = SearchThreadBean.UserInfoBean("author", null, "7", null)
    private val mainPost = SearchThreadBean.MainPost("thread", "", 10L, author, "0", "0", "2")
    private val parent = SearchThreadBean.PostInfo(10L, 20L, "reply", "", author)
    private val thread = SearchThreadBean.ThreadInfoBean(
        tid = "10", pid = "11", title = "thread", content = "", time = "", modifiedTime = 0,
        postNum = "2", likeNum = "0", shareNum = "0", forumId = "3", forumName = "forum",
        user = author, type = 0, forumInfo = SearchThreadBean.ForumInfo("forum", ""),
    )
    private val reply = thread.copy(pid = "21", mainPost = mainPost)
    private val subreply = reply.copy(cid = "30", postInfo = parent)

    @Test
    fun commentRulesApplyToReplySearchResultsAndQuotedRepliesOnly() {
        val rules = LocalBlockRules.fromText(comment = "引流|代购")
        assertFalse(rules.blocksSearchResult(thread.copy(content = "代购")))
        assertTrue(rules.blocksSearchResult(reply.copy(content = "请联系代购")))
        assertTrue(rules.blocksSearchResult(subreply.copy(postInfo = parent.copy(content = "引流"))))
        assertFalse(rules.blocksSearchResult(reply.copy(mainPost = mainPost.copy(content = "代购"))))
    }

    @Test
    fun hiddenAuthorsAndForumsAlsoMatchEmbeddedSearchMetadata() {
        val rules = LocalBlockRules.fromText(forum = "^摄影吧$", user = "^推广.*")
        assertTrue(rules.blocksSearchResult(thread.copy(forumInfo = thread.forumInfo.copy(forumName = "摄影"))))
        assertTrue(rules.blocksSearchResult(thread.copy(user = author.copy(showNickname = "推广账号"))))
        assertTrue(rules.blocksSearchResult(reply.copy(mainPost = mainPost.copy(user = author.copy(userName = "推广2")))))
        assertFalse(rules.blocksSearchResult(subreply))
    }

    @Test
    fun tappingCommentCountOpensRepliesAndKeepsTheMatchedFloorOrSubreply() {
        assertEquals(ThreadPageDestination(threadId = 10, scrollToReply = true).route, thread.searchDestination(true)?.route)
        assertEquals(reply.searchDestination()?.route, reply.searchDestination(true)?.route)
        assertEquals(subreply.searchDestination()?.route, subreply.searchDestination(true)?.route)
    }

    @Test
    fun resultClickOpensTheMatchingThreadReplyOrSubreply() {
        assertEquals(ThreadPageDestination(threadId = 10).route, thread.searchDestination()?.route)
        assertEquals(
            ThreadPageDestination(threadId = 10, postId = 21, scrollToReply = true).route,
            reply.searchDestination()?.route,
        )
        assertEquals(
            SubPostsPageDestination(threadId = 10, subPostId = 30, loadFromSubPost = true).route,
            subreply.searchDestination()?.route,
        )
    }

    @Test
    fun quotedReplyAndMainPostOpenTheirOwnThread() {
        assertEquals(
            ThreadPageDestination(threadId = 40, postId = 50, scrollToReply = true).route,
            parent.copy(tid = 40, pid = 50).searchDestination()?.route,
        )
        assertEquals(
            ThreadPageDestination(threadId = 40, scrollToReply = true).route,
            mainPost.copy(tid = 40).searchDestination()?.route,
        )
    }

    @Test
    fun missingSubreplyIdFallsBackToTheClosestValidPostInThatThread() {
        assertEquals(
            ThreadPageDestination(threadId = 10, postId = 21, scrollToReply = true).route,
            subreply.copy(cid = "invalid").searchDestination()?.route,
        )
        assertEquals(
            ThreadPageDestination(threadId = 10, postId = 20, scrollToReply = true).route,
            subreply.copy(cid = "0", pid = "-1").searchDestination()?.route,
        )
        assertEquals(
            ThreadPageDestination(threadId = 10).route,
            subreply.copy(cid = "", pid = "0", postInfo = parent.copy(tid = 99)).searchDestination()?.route,
        )
        assertEquals(ThreadPageDestination(threadId = 10).route, reply.copy(pid = "?").searchDestination()?.route)
    }

    @Test
    fun missingThreadIdUsesTheEmbeddedPostWhenAvailable() {
        assertEquals(
            ThreadPageDestination(threadId = 10, postId = 20, scrollToReply = true).route,
            subreply.copy(tid = "invalid").searchDestination()?.route,
        )
        assertEquals(
            ThreadPageDestination(threadId = 10, scrollToReply = true).route,
            reply.copy(tid = "0").searchDestination()?.route,
        )
        assertEquals(
            ThreadPageDestination(threadId = 10).route,
            parent.copy(pid = 0).searchDestination()?.route,
        )
        assertNull(thread.copy(tid = "-1").searchDestination())
        assertNull(parent.copy(tid = 0).searchDestination())
        assertNull(mainPost.copy(tid = -1).searchDestination())
    }
}

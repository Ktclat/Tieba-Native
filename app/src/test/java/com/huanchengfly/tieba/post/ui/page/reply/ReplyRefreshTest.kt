package com.huanchengfly.tieba.post.ui.page.reply

import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsPartialChange
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsUiState
import com.huanchengfly.tieba.post.ui.page.thread.PostItemData
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPartialChange
import com.huanchengfly.tieba.post.ui.page.thread.ThreadUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyRefreshTest {
    private fun floor(id: Long, replies: Int = 0) = PostItemData(
        post = Post(id = id, sub_post_number = replies).wrapImmutable(), blocked = false,
        contentRenders = persistentListOf(), subPosts = persistentListOf(),
    )

    @Test fun nestedSuccessRefreshesItsParentAndCountWithoutAddingOtherServerFloors() {
        val parent = floor(20)
        val neighbor = floor(21)
        val original = ThreadUiState(
            threadInfo = ThreadInfo(id = 10, replyNum = 42).wrapImmutable(),
            data = persistentListOf(parent, neighbor), latestPosts = persistentListOf(parent),
        )
        val updated = ThreadPartialChange.LoadMyLatestReply.Success(
            Anti(), listOf(floor(20, 4), floor(99)), page = 1, isContinuous = false,
            isDesc = false, hasNewPost = false, serverReplyCount = 43, updatedParentId = 20,
        ).reduce(original)
        assertEquals(listOf(20L, 21L), updated.data.map { it.post.get { id } })
        assertEquals(4, updated.data.first().post.get { sub_post_number })
        assertEquals(4, updated.latestPosts.single().post.get { sub_post_number })
        assertEquals(43, updated.threadInfo?.get { replyNum })
    }

    @Test fun topLevelSuccessAppendsTheNewFloorAndUpdatesCount() {
        val original = ThreadUiState(data = persistentListOf(floor(20)), threadInfo = ThreadInfo(id = 10).wrapImmutable())
        val updated = ThreadPartialChange.LoadMyLatestReply.Success(
            Anti(), listOf(floor(20), floor(21)), page = 1, isContinuous = true,
            isDesc = false, hasNewPost = true, serverReplyCount = 2,
        ).reduce(original)
        assertEquals(listOf(20L, 21L), updated.data.map { it.post.get { id } })
        assertEquals(2, updated.threadInfo?.get { replyNum })
    }

    @Test fun detailRefreshKeepsTheNewNestedIdAndServerPage() {
        val loaded = SubPostsPartialChange.Load.Success(
            anti = Anti(tbs = "fresh").wrapImmutable(), forum = SimpleForum(id = 1).wrapImmutable(),
            thread = ThreadInfo(id = 10).wrapImmutable(), post = Post(id = 20).wrapImmutable(),
            postContentRenders = persistentListOf(), subPosts = persistentListOf(),
            hasMore = false, currentPage = 3, totalPage = 3, totalCount = 25,
            postedReplyId = 71, loadedAccountId = "42",
        )
        val state = loaded.reduce(SubPostsUiState())
        assertEquals(71L, state.postedReplyId)
        assertEquals(25, state.totalCount)
        assertEquals(3, state.currentPage)
        assertEquals("fresh", state.anti?.get { tbs })
        assertEquals("42", state.loadedAccountId)
    }
}

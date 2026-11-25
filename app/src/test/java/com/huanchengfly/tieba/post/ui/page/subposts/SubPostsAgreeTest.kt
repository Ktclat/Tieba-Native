package com.huanchengfly.tieba.post.ui.page.subposts

import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsPartialChange.Agree as AgreeChange
import java.io.IOException
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

class SubPostsAgreeTest {
    private val floor = Post(id = 31, agree = Agree(hasAgree = 1, agreeNum = 10, disagreeNum = 2, diffAgreeNum = 8))
    private val reply = SubPostList(id = 41, agree = Agree(agreeNum = 6, disagreeNum = 1, diffAgreeNum = 5))
    private val sibling = SubPostList(id = 42, agree = Agree(hasAgree = 1, agreeNum = 2, diffAgreeNum = 2))
    private val initialState = SubPostsUiState(
        post = floor.wrapImmutable(),
        subPosts = persistentListOf(
            SubPostItemData(reply.wrapImmutable(), persistentListOf(), blocked = false),
            SubPostItemData(sibling.wrapImmutable(), persistentListOf(), blocked = false),
        ),
    )

    private fun assertRows(state: SubPostsUiState, expectedFloor: Post, expectedReply: SubPostList) {
        assertEquals(expectedFloor, state.post?.get())
        assertEquals(listOf(expectedReply, sibling), state.subPosts.map { it.subPost.get() })
    }

    @Test fun likingAReplyUpdatesOnlyThatReplyAndCountsItsAcknowledgementOnce() {
        val start = AgreeChange.Start(reply.id, true)
        val success = AgreeChange.Success(reply.id, true)
        val liked = reply.copy(agree = reply.agree!!.copy(hasAgree = 1, agreeNum = 7, diffAgreeNum = 6))

        val pending = start.reduce(initialState)
        assertRows(pending, floor, liked)
        assertRows(start.reduce(pending), floor, liked)
        val confirmed = success.reduce(pending)
        assertRows(confirmed, floor, liked)
        assertRows(success.reduce(confirmed), floor, liked)

        val rollback = AgreeChange.Failure(reply.id, false, IOException()).reduce(pending)
        assertRows(rollback, floor, reply)
    }

    @Test fun replyWithoutAgreeDataCanBeLikedAndReturnsToZeroOnUndoOrFailure() {
        val fresh = reply.copy(agree = null)
        val before = initialState.copy(subPosts = persistentListOf(
            initialState.subPosts.first().copy(subPost = fresh.wrapImmutable()),
            initialState.subPosts.last(),
        ))
        fun assertVote(state: SubPostsUiState, vote: Int) {
            val agree = state.subPosts.first().subPost.get().agree
            assertEquals(vote, agree?.hasAgree)
            assertEquals(vote.toLong(), agree?.agreeNum)
            assertEquals(vote.toLong(), agree?.diffAgreeNum)
            assertRows(state, floor, fresh.copy(agree = agree))
        }

        val pending = AgreeChange.Start(fresh.id, true).reduce(before)
        assertVote(pending, 1)
        val confirmed = AgreeChange.Success(fresh.id, true).reduce(pending)
        assertVote(confirmed, 1)
        assertVote(AgreeChange.Success(fresh.id, true).reduce(confirmed), 1)

        val undo = AgreeChange.Start(fresh.id, false).reduce(confirmed)
        assertVote(undo, 0)
        assertVote(AgreeChange.Success(fresh.id, false).reduce(undo), 0)
        assertVote(AgreeChange.Failure(fresh.id, false, IOException()).reduce(pending), 0)
    }

    @Test fun removingTheFloorLikeUsesNullChildIdAndLeavesAllRepliesIntact() {
        val start = AgreeChange.Start(null, false)
        val success = AgreeChange.Success(null, false)
        val unliked = floor.copy(agree = floor.agree!!.copy(hasAgree = 0, agreeNum = 9, diffAgreeNum = 7))

        val pending = start.reduce(initialState)
        assertRows(pending, unliked, reply)
        assertRows(start.reduce(pending), unliked, reply)
        val confirmed = success.reduce(pending)
        assertRows(confirmed, unliked, reply)
        assertRows(success.reduce(confirmed), unliked, reply)

        val rollback = AgreeChange.Failure(null, true, IOException()).reduce(pending)
        assertRows(rollback, floor, reply)
    }
}

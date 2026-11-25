package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPartialChange.AgreePost
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadReplyAgreeStateTest {
    private val floorVote = Agree(agreeNum = 12, disagreeNum = 2, diffAgreeNum = 10)
    private val replyVote = Agree(agreeNum = 8, disagreeNum = 13, diffAgreeNum = -5)
    private val likedReplyVote = replyVote.copy(hasAgree = 1, agreeNum = 9, diffAgreeNum = -4)
    private val target = SubPostList(id = 201, agree = replyVote)
    private val sibling = SubPostList(id = 202, agree = null)
    private val otherReply = target.copy(agree = Agree(hasAgree = 1, agreeNum = 21, diffAgreeNum = 21))

    @Test fun previewLikeUpdatesOnlyTheParentAndReplyPairInBothLists() {
        val first = floor(10, listOf(otherReply))
        val second = floor(20, listOf(sibling, target))
        val original = ThreadUiState(
            data = persistentListOf(first, second),
            latestPosts = persistentListOf(second, first),
        )
        fun assertVotes(state: ThreadUiState, expected: Agree) {
            for (rows in listOf(state.data, state.latestPosts)) {
                assertFloor(rows.single { it.post.get().id == 10L }, floorVote, listOf(otherReply))
                assertFloor(rows.single { it.post.get().id == 20L }, floorVote, listOf(sibling, target.copy(agree = expected)))
            }
        }

        val liked = AgreePost.Start(20, true, subPostId = target.id).reduce(original)
        assertVotes(liked, likedReplyVote)
        val confirmed = AgreePost.Success(20, true, subPostId = target.id).reduce(liked)
        assertVotes(confirmed, likedReplyVote)
        val unliked = AgreePost.Start(20, false, subPostId = target.id).reduce(confirmed)
        assertVotes(unliked, replyVote)
        val restored = AgreePost.Failure(20, true, 503, "retry", subPostId = target.id).reduce(unliked)
        assertVotes(restored, likedReplyVote)
    }

    @Test fun floorLikeOnLatestOnlyPostPreservesItsPreviews() {
        val first = floor(10, listOf(otherReply))
        val second = floor(20, listOf(target))
        val original = ThreadUiState(data = persistentListOf(first), latestPosts = persistentListOf(second))

        val liked = AgreePost.Start(20, true).reduce(original)
        val confirmed = AgreePost.Success(20, true).reduce(liked)
        val likedFloor = floorVote.copy(hasAgree = 1, agreeNum = 13, diffAgreeNum = 11)
        for (state in listOf(liked, confirmed)) {
            assertFloor(state.data.single(), floorVote, listOf(otherReply))
            assertFloor(state.latestPosts.single(), likedFloor, listOf(target))
        }
    }

    @Test fun expandedVotesReachBothListsOnlyForTheirOwnThreadAndTarget() {
        val post = floor(20, listOf(sibling, target))
        val threadId = 91L
        val original = ThreadUiState(
            threadInfo = ThreadInfo(id = threadId).wrapImmutable(),
            data = persistentListOf(post), latestPosts = persistentListOf(post),
        )
        val replyEvent = GlobalEvent.CommentVoteConfirmed(threadId, 20, target.id, true)
        val floorEvent = GlobalEvent.CommentVoteConfirmed(threadId, 20, null, true)
        val foreign = ThreadPartialChange.ConfirmedCommentVote(replyEvent.copy(threadId = 92)).reduce(original)
        assertEquals(original, foreign)

        val replyChange = ThreadPartialChange.ConfirmedCommentVote(replyEvent)
        val likedReply = replyChange.reduce(original)
        val likedChildren = listOf(sibling, target.copy(agree = likedReplyVote))
        for (rows in listOf(likedReply.data, likedReply.latestPosts)) {
            assertFloor(rows.single(), floorVote, likedChildren)
        }
        val noPreviewVote = ThreadPartialChange.ConfirmedCommentVote(replyEvent.copy(subPostId = sibling.id))
        val stillOmitted = noPreviewVote.reduce(likedReply)
        for (rows in listOf(stillOmitted.data, stillOmitted.latestPosts)) {
            assertFloor(rows.single(), floorVote, likedChildren)
        }
        val floorChange = ThreadPartialChange.ConfirmedCommentVote(floorEvent)
        val likedBoth = floorChange.reduce(replyChange.reduce(likedReply))
        val repeated = floorChange.reduce(likedBoth)
        val likedFloor = floorVote.copy(hasAgree = 1, agreeNum = 13, diffAgreeNum = 11)
        for (state in listOf(likedBoth, repeated)) {
            assertFloor(state.data.single(), likedFloor, likedChildren)
            assertFloor(state.latestPosts.single(), likedFloor, likedChildren)
        }
    }

    @Test fun floorWithoutServerCountsTracksItsFirstVoteAndKeepsTotalLikesNonnegative() {
        val withoutCounts = floor(20, listOf(target), vote = null)
        val emptyCounts = floor(30, listOf(otherReply), vote = Agree(hasAgree = 1))
        val original = ThreadUiState(data = persistentListOf(withoutCounts, emptyCounts))

        val pending = AgreePost.Start(20, true).reduce(original)
        val confirmed = AgreePost.Success(20, true).reduce(pending)
        val oneVote = Agree(agreeNum = 1, diffAgreeNum = 1, hasAgree = 1)
        assertFloor(pending.data.first(), oneVote, listOf(target))
        assertFloor(confirmed.data.first(), oneVote, listOf(target))
        val rolledBack = AgreePost.Failure(20, false, 503, "retry").reduce(pending)
        assertFloor(rolledBack.data.first(), Agree(), listOf(target))

        val unliked = AgreePost.Start(30, false).reduce(original)
        assertFloor(unliked.data.last(), Agree(agreeNum = 0, diffAgreeNum = -1), listOf(otherReply))
    }

    private fun floor(id: Long, replies: List<SubPostList>, vote: Agree? = floorVote): PostItemData {
        val post = Post(id = id, agree = vote, sub_post_list = SubPost(sub_post_list = replies))
        return PostItemData(
            post = post.wrapImmutable(), blocked = false, contentRenders = persistentListOf(),
            subPosts = replies.map {
                SubPostItemData(it.wrapImmutable(), AnnotatedString("reply ${it.id}"), blocked = false)
            }.toImmutableList(),
        )
    }

    private fun assertFloor(row: PostItemData, expectedVote: Agree, expectedReplies: List<SubPostList>) {
        assertEquals(expectedVote, row.post.get().agree)
        assertEquals(expectedReplies, row.subPosts.map { it.subPost.get() })
        assertEquals(expectedReplies, row.post.get().sub_post_list?.sub_post_list)
    }
}

package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.buildProtobufRequestBody
import com.huanchengfly.tieba.post.api.sort
import com.huanchengfly.tieba.post.api.interfaces.impls.withReplyTarget
import com.huanchengfly.tieba.post.api.models.PicInfo
import com.huanchengfly.tieba.post.api.models.PicInfoItem
import com.huanchengfly.tieba.post.api.models.UploadPictureResultBean
import com.huanchengfly.tieba.post.api.models.protos.Error
import com.huanchengfly.tieba.post.api.models.protos.PostAntiInfo
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostRequestData
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.GlobalEventFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ReplySubmissionTest {
    private val draft = ReplyRequest("正文", forumId = 2, forumName = "旅行", threadId = 10)

    @Test
    fun wireDistinguishesANewFloorFromAReplyToAFloorOrAnotherReader() {
        fun payload(floor: String?, nested: String?) = AddPostRequestData.ADAPTER.decode(
            AddPostRequestData(tid = "10", fid = "2").withReplyTarget(floor, nested, "31").encode()
        )
        val top = payload(null, null)
        assertEquals("13", top.post_from)
        assertNull(top.reply_uid)
        assertNull(top.quote_id)
        assertNull(top.repostid)
        assertNull(top.sub_post_id)

        val floor = payload("20", null)
        assertEquals("0", floor.post_from)
        assertEquals("20", floor.quote_id)
        assertEquals("20", floor.repostid)
        assertEquals("31", floor.reply_uid)
        assertNull(floor.sub_post_id)

        val nested = payload("20", "21")
        assertEquals("10", nested.tid)
        assertEquals("2", nested.fid)
        assertNull(nested.post_from)
        assertEquals("20", nested.quote_id)
        assertEquals("20", nested.repostid)
        assertEquals("21", nested.sub_post_id)
        assertEquals("31", nested.reply_uid)
        assertThrows(IllegalArgumentException::class.java) { payload(null, "21") }
    }

    @Test
    fun signingAndSortingKeepTheReplyBodySingleUse() {
        val body = buildProtobufRequestBody(AddPostRequestData(tid = "10"), needSToken = false, oneShot = true)
        assertTrue(body.isOneShot())
        assertTrue(body.sort().isOneShot())
    }

    @Test
    fun attachmentsKeepTheRecipientAndDoNotReplaceTheDraftText() {
        val pic = PicInfoItem("128", "96", "jpg", "")
        val upload = UploadPictureResultBean("0", "", "", "", "p1", PicInfo(pic, pic, pic))
        val directed = draft.copy(postId = 20, subPostId = 21, replyUserName = "阿远", replyUserPortrait = "portrait")
        assertEquals("回复 #(reply, portrait, 阿远) :正文\n#(pic,p1,128,96)", directed.contentWithImages(listOf(upload)))
        assertEquals("回复 阿远：正文", directed.copy(replyUserPortrait = null).contentWithImages(emptyList()))
        assertEquals("#(pic,p1,128,96)", draft.copy(content = "").contentWithImages(listOf(upload)))
        assertEquals("正文", directed.copy(subPostId = null).contentWithImages(emptyList()))
    }

    @Test
    fun onlyServerConfirmedPositiveIdsCanRefreshTheList() {
        val ok = AddPostResponse(data_ = AddPostResponseData(pid = "101"))
        assertEquals(GlobalEvent.ReplySuccess(10, 101), ok.confirmReply(10, null, null).event)
        assertEquals(GlobalEvent.ReplySuccess(10, 20, 20, null, 101), ok.confirmReply(10, 20, null).event)
        assertEquals(GlobalEvent.ReplySuccess(10, 20, 20, 21, 101), ok.confirmReply(10, 20, 21).event)
        for (response in listOf(
            AddPostResponse(), AddPostResponse(data_ = AddPostResponseData(pid = "0")),
            ok.copy(error = Error(error_code = 5, error_msg = "未登录")),
            AddPostResponse(data_ = AddPostResponseData(pid = "101", info = PostAntiInfo(need_vcode = "1"))),
        )) {
            assertThrows(TiebaException::class.java) { response.confirmReply(10, 20, null) }
        }
    }

    @Test
    fun doubleTapDuringSendingAndAfterSuccessPostsOnceAndFailureAllowsExplicitRetry() = runBlocking {
        val attempts = mutableListOf<ReplyRequest>()
        val confirmed = GlobalEvent.ReplySuccess(10, 101)
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val sender = ReplySubmission { request ->
            attempts += request
            if (attempts.size == 1) throw IOException("网络断开")
            started.complete(Unit)
            finish.await()
            ReplySubmissionState.Sent(confirmed, "")
        }
        val events = mutableListOf<GlobalEvent.ReplySuccess>()
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            GlobalEventFlow.filterIsInstance<GlobalEvent.ReplySuccess>().collect { events += it }
        }
        try {
            sender.submit(this, draft)
            sender.submit(this, draft)
            assertEquals("网络断开", sender.state.filterIsInstance<ReplySubmissionState.Failed>().first().errorMessage)
            assertTrue(events.isEmpty())
            sender.submit(this, draft)
            started.await()
            sender.submit(this, draft)
            finish.complete(Unit)
            assertEquals(confirmed, sender.state.filterIsInstance<ReplySubmissionState.Sent>().first().event)
            sender.submit(this, draft)
            assertEquals(listOf(draft, draft), attempts)
            assertEquals(listOf(confirmed), events)
        } finally {
            observer.cancelAndJoin()
        }
    }
}

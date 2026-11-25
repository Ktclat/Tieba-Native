package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.UploadPictureResultBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ReplyRequest(
    val content: String,
    val forumId: Long,
    val forumName: String,
    val threadId: Long,
    val tbs: String? = null,
    val postId: Long? = null,
    val subPostId: Long? = null,
    val replyUserId: Long? = null,
    val replyUserName: String? = null,
    val replyUserPortrait: String? = null,
    val imageUris: List<String> = emptyList(),
    val isOriginImage: Boolean = false,
) {
    fun contentWithImages(images: List<UploadPictureResultBean>): String = buildString {
        if ((subPostId ?: 0L) > 0 && !replyUserName.isNullOrBlank()) {
            if (replyUserPortrait.isNullOrBlank()) {
                append("回复 ").append(replyUserName).append("：")
            } else {
                append("回复 #(reply, ").append(replyUserPortrait).append(", ").append(replyUserName).append(") :")
            }
        }
        append(content)
        for (image in images) {
            if (isNotEmpty()) append('\n')
            val size = image.picInfo.originPic
            append("#(pic,${image.picId},${size.width},${size.height})")
        }
    }
}

internal sealed interface ReplySubmissionState {
    data object Idle : ReplySubmissionState
    data object Sending : ReplySubmissionState
    data class Failed(val errorCode: Int, val errorMessage: String) : ReplySubmissionState
    data class Sent(val event: GlobalEvent.ReplySuccess, val expInc: String) : ReplySubmissionState
}

internal class ReplySubmission(private val send: suspend (ReplyRequest) -> ReplySubmissionState.Sent) {
    private val status = MutableStateFlow<ReplySubmissionState>(ReplySubmissionState.Idle)
    val state = status.asStateFlow()

    fun submit(scope: CoroutineScope, request: ReplyRequest) {
        val previous = status.value
        if (previous != ReplySubmissionState.Idle && previous !is ReplySubmissionState.Failed) return
        if (!status.compareAndSet(previous, ReplySubmissionState.Sending)) return
        scope.launch {
            try {
                val sent = send(request)
                emitGlobalEventSuspend(sent.event)
                status.value = sent
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                status.value = ReplySubmissionState.Failed(error.getErrorCode(), error.getErrorMessage())
            }
        }
    }
}

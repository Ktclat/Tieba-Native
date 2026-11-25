package com.huanchengfly.tieba.post.repository

import android.net.Uri
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.components.ImageUploader
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.withContext

internal object AddPostRepository {
    suspend fun submit(request: ReplyRequest): ReplySubmissionState.Sent = withContext(Dispatchers.IO) {
        val accountId = activeAccount(request.accountId).uid
        val floor = request.postId?.takeUnless { it == 0L }
        val nested = request.subPostId?.takeUnless { it == 0L }
        if (request.threadId <= 0L || request.forumId <= 0L || (floor ?: 0L) < 0L || (nested ?: 0L) < 0L ||
            (nested != null && floor == null)
        ) {
            throw TiebaUnknownException
        }
        if (request.content.isBlank() && request.imageUris.isEmpty()) throw TiebaUnknownException
        val images = if (request.imageUris.isEmpty()) emptyList() else {
            ImageUploader(request.forumName).uploadImages(
                request.imageUris.map { FileUtil.getRealPathFromUri(App.INSTANCE, Uri.parse(it)) },
                request.isOriginImage,
            ).single()
        }
        val account = activeAccount(accountId)
        val response = TiebaApi.getInstance().addPostFlow(
            content = request.contentWithImages(images),
            forumId = request.forumId.toString(),
            forumName = request.forumName,
            threadId = request.threadId.toString(),
            tbs = request.tbs?.takeIf { it.isNotBlank() } ?: account.tbs,
            nameShow = account.nameShow,
            postId = floor?.toString(),
            subPostId = nested?.toString(),
            replyUserId = request.replyUserId?.takeIf { floor != null && it > 0L }?.toString(),
        ).single()
        response.confirmReply(request.threadId, floor, nested)
    }

    private fun activeAccount(expectedId: String?): Account {
        val account = AccountUtil.getLoginInfo()
        if (account == null || (expectedId != null && account.uid != expectedId)) {
            throw TiebaNotLoggedInException(App.INSTANCE.getString(R.string.tip_login))
        }
        return account
    }
}

internal fun AddPostResponse.confirmReply(threadId: Long, floor: Long?, nested: Long?): ReplySubmissionState.Sent {
    error?.takeIf { it.error_code != 0 }?.let {
        throw TiebaApiException(CommonResponse(it.error_code, it.user_msg.ifBlank { it.error_msg }))
    }
    val data = data_ ?: throw TiebaUnknownException
    val pid = data.pid.toLongOrNull()?.takeIf { it > 0L }
    if (pid == null || data.info?.need_vcode == "1") {
        throw TiebaApiException(CommonResponse(-1, data.msg.ifBlank { data.info?.block_content.orEmpty() }))
    }
    val event = GlobalEvent.ReplySuccess(
        threadId = threadId,
        newPostId = floor ?: pid,
        postId = floor,
        subPostId = nested,
        newSubPostId = pid.takeIf { floor != null },
    )
    return ReplySubmissionState.Sent(event, data.exp?.inc.orEmpty())
}

package com.huanchengfly.tieba.post.ui.utils

import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.collections.immutable.toImmutableList

fun getPhotoViewData(
    post: Post,
    pictures: List<PicItem>,
    index: Int,
): PhotoViewData {
    val selected = pictures[index]
    return PhotoViewData(
        data = post.from_forum?.let { forum ->
            LoadPicPageData(
                forumId = forum.id,
                forumName = forum.name,
                threadId = post.tid,
                postId = post.id,
                objType = "pb",
                picId = selected.picId,
                picIndex = 1,
                seeLz = false,
                originUrl = selected.originUrl,
            )
        },
        picItems = pictures,
        index = index,
    )
}

fun getPhotoViewData(
    threadInfo: ThreadInfo,
    index: Int
): PhotoViewData {
    return getPhotoViewData(
        medias = threadInfo.media,
        forumId = threadInfo.forumId,
        forumName = threadInfo.forumName,
        threadId = threadInfo.threadId,
        index = index
    )
}

fun getPhotoViewData(
    medias: List<Media>,
    forumId: Long,
    forumName: String,
    threadId: Long?,
    index: Int
): PhotoViewData {
    val pictures = medias.mapIndexed { mediaIndex, media ->
        val fullSizeUrl = media.originPic.ifBlank {
            media.bigPic.ifBlank { media.dynamicPic.ifBlank { media.srcPic } }
        }
        PicItem(
            picId = ImageUtil.getPicId(fullSizeUrl),
            picIndex = mediaIndex + 1,
            url = media.bigPic.ifBlank { fullSizeUrl },
            originUrl = fullSizeUrl,
            showOriginBtn = media.showOriginalBtn == 1,
            originSize = media.originSize,
            postId = media.postId,
        )
    }.toImmutableList()
    val selected = pictures[index]
    return PhotoViewData(
        data = threadId?.let { id ->
            LoadPicPageData(
                forumId = forumId,
                forumName = forumName,
                threadId = id,
                postId = medias[index].postId,
                seeLz = false,
                objType = "index",
                picId = selected.picId,
                picIndex = index + 1,
                originUrl = selected.originUrl,
            )
        },
        picItems = pictures,
        index = index
    )
}

package com.huanchengfly.tieba.post.api.models.protos

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender.Companion.appendText
import com.huanchengfly.tieba.post.ui.common.VideoContentRender
import com.huanchengfly.tieba.post.ui.common.VoiceContentRender
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.thread.SubPostItemData
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

val List<Abstract>.abstractText: String
    get() = joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            4 -> it.text

            else -> ""
        }
    }

val ThreadInfo.abstractText: String
    get() = richAbstract.joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }

            else -> ""
        }
    }

val PostInfoList.abstractText: String
    get() = rich_abstract.joinToString(separator = "") {
        when (it.type) {
            0 -> it.text.replace(Regex(" {2,}"), " ")
            2 -> {
                EmoticonManager.registerEmoticon(it.text, it.c)
                "#(${it.c})"
            }

            else -> ""
        }
    }

val ThreadInfo.hasAgree: Int
    get() = agree?.hasAgree ?: 0
val ThreadInfo.hasAgreed: Boolean
    get() = hasAgree == 1
val ThreadInfo.hasAbstract: Boolean
    get() = richAbstract.any { (it.type == 0 && it.text.isNotBlank()) || it.type == 2 }

fun ThreadInfo.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agreeNum = agreeNum + 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agreeNum = agreeNum - 1,
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    copy(
        agreeNum = if (hasAgree == 1) agreeNum + 1 else agreeNum - 1
    )
}

fun ThreadInfo.updateCollectStatus(
    newStatus: Int,
    markPostId: Long
) = if (collectStatus != newStatus) {
    this.copy(
        collectStatus = newStatus,
        collectMarkPid = markPostId.toString()
    )
} else {
    this
}

fun Post.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    this
}

fun SubPostList.updateAgreeStatus(
    hasAgree: Int
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                )
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                )
            )
        }
    } else {
        this
    }
} else {
    this
}

fun PostInfoList.updateAgreeStatus(
    hasAgree: Int,
) = if (agree != null) {
    if (hasAgree != agree.hasAgree) {
        if (hasAgree == 1) {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum + 1,
                    diffAgreeNum = agree.diffAgreeNum + 1,
                    hasAgree = 1
                ),
                agree_num = agree_num + 1
            )
        } else {
            copy(
                agree = agree.copy(
                    agreeNum = agree.agreeNum - 1,
                    diffAgreeNum = agree.diffAgreeNum - 1,
                    hasAgree = 0
                ),
                agree_num = agree_num - 1
            )
        }
    } else {
        this
    }
} else {
    this
}

private val PbContent.picUrl: String
    get() =
        ImageUtil.getUrl(
            App.INSTANCE,
            true,
            originSrc,
            bigCdnSrc,
            bigSrc,
            dynamic_,
            cdnSrc,
            cdnSrcActive,
            src
        )

private fun PbContent.mediaSize(): Pair<Int, Int> {
    val parts = bsize.split(',', limit = 2)
    val parsedWidth = parts.getOrNull(0)?.toIntOrNull()
    val parsedHeight = parts.getOrNull(1)?.toIntOrNull()
    return when {
        parsedWidth != null && parsedWidth > 0 && parsedHeight != null && parsedHeight > 0 -> parsedWidth to parsedHeight
        width > 0 && height > 0 -> width to height
        else -> 1 to 1
    }
}

val List<PbContent>.plainText: String
    get() = renders.joinToString("\n") { it.toString() }

@OptIn(ExperimentalTextApi::class)
val List<PbContent>.renders: ImmutableList<PbContentRender>
    get() {
        val renders = mutableListOf<PbContentRender>()

        forEach {
            when (it.type) {
                0, 9, 27 -> {
                    renders.appendText(it.text)
                }

                1 -> {
                    val text = buildAnnotatedString {
                        appendInlineContent("link_icon", alternateText = "🔗")
                        withAnnotation(tag = "url", annotation = it.link) {
                            withStyle(
                                SpanStyle(
                                    color = Color(
                                        ThemeUtils.getColorByAttr(
                                            App.INSTANCE,
                                            R.attr.colorNewPrimary
                                        )
                                    )
                                )
                            ) {
                                append(it.text)
                            }
                        }
                    }
                    renders.appendText(text)
                }

                2 -> {
                    EmoticonManager.registerEmoticon(
                        it.text,
                        it.c
                    )
                    val emoticonText = "#(${it.c})".emoticonString
                    renders.appendText(emoticonText)
                }

                3 -> {
                    val (width, height) = it.mediaSize()
                    renders.add(
                        PicContentRender(
                            picUrl = it.picUrl,
                            originUrl = it.originSrc,
                            showOriginBtn = it.showOriginalBtn == 1,
                            originSize = it.originSize,
                            picId = ImageUtil.getPicId(it.originSrc),
                            width = width,
                            height = height
                        )
                    )
                }

                4 -> {
                    val text = buildAnnotatedString {
                        withAnnotation(tag = "user", annotation = "${it.uid}") {
                            withStyle(
                                SpanStyle(
                                    color = Color(
                                        ThemeUtils.getColorByAttr(
                                            App.INSTANCE,
                                            R.attr.colorNewPrimary
                                        )
                                    )
                                )
                            ) {
                                append(it.text)
                            }
                        }
                    }
                    renders.appendText(text)
                }

                5 -> {
                    if (it.src.isNotBlank()) {
                        val (width, height) = it.mediaSize()
                        renders.add(
                            VideoContentRender(
                                videoUrl = it.link,
                                picUrl = it.src,
                                webUrl = it.text,
                                width = width,
                                height = height
                            )
                        )
                    } else {
                        val text = buildAnnotatedString {
                            appendInlineContent("video_icon", alternateText = "🎥")
                            withAnnotation(tag = "url", annotation = it.text) {
                                withStyle(
                                    SpanStyle(
                                        color = Color(
                                            ThemeUtils.getColorByAttr(
                                                App.INSTANCE,
                                                R.attr.colorNewPrimary
                                            )
                                        )
                                    )
                                ) {
                                    append(App.INSTANCE.getString(R.string.tag_video))
                                    append(it.text)
                                }
                            }
                        }
                        renders.appendText(text)
                    }
                }

                10 -> {
                    renders.add(VoiceContentRender(it.voiceMD5, it.duringTime))
                }

                20 -> {
                    val (width, height) = it.mediaSize()
                    renders.add(
                        PicContentRender(
                            picUrl = it.src,
                            originUrl = it.src,
                            showOriginBtn = it.showOriginalBtn == 1,
                            originSize = it.originSize,
                            picId = ImageUtil.getPicId(it.src),
                            width = width,
                            height = height
                        )
                    )
                }
            }
        }

        return renders.toImmutableList()
    }

val Post.contentRenders: ImmutableList<PbContentRender>
    get() {
        val renders = content.renders

        return renders.map {
            if (it is PicContentRender) {
                it.copy(
                    photoViewData = getPhotoViewData(
                        this,
                        it.picId,
                        it.picUrl,
                        it.originUrl,
                        it.showOriginBtn,
                        it.originSize
                    )
                )
            } else it
        }.toImmutableList()
    }

val User.bawuType: String?
    get() = if (is_bawu == 1) {
        if (bawu_type == "manager") "吧主" else "小吧主"
    } else null

val Post.subPostContents: ImmutableList<AnnotatedString>
    get() = sub_post_list?.sub_post_list?.map { it.getContentText(origin_thread_info?.author?.id) }
        ?.toImmutableList()
        ?: persistentListOf()

val Post.subPosts: ImmutableList<SubPostItemData>
    get() = sub_post_list?.sub_post_list?.map {
        SubPostItemData(
            it.wrapImmutable(),
            it.previewBody
        )
    }?.toImmutableList() ?: persistentListOf()

internal val SubPostList.previewBody: AnnotatedString
    get() = buildAnnotatedString {
        content.renders.forEach { render ->
            val text = render.toAnnotationString()
            if (text.isEmpty()) append(render.toString()) else append(text)
        }
    }

@OptIn(ExperimentalTextApi::class)
fun SubPostList.getContentText(threadAuthorId: Long? = null): AnnotatedString {
    val user = author ?: return previewBody
    if (user.name.isBlank() && user.nameShow.isBlank()) return previewBody
    val context = App.INSTANCE
    val displayName = StringUtil.getUsernameAnnotatedString(context, user.name, user.nameShow.takeIf { it.isNotBlank() })
    return buildAnnotatedString {
        withStyle(SpanStyle(color = Color(ThemeUtils.getColorByAttr(context, R.attr.colorNewPrimary)), fontWeight = FontWeight.Bold)) {
            if (user.id > 0) {
                withAnnotation("user", user.id.toString()) { append(displayName) }
            } else {
                append(displayName)
            }
        }
        if (user.id > 0 && user.id == threadAuthorId) appendInlineContent("Lz")
        append(": ")
        append(previewBody)
    }
}

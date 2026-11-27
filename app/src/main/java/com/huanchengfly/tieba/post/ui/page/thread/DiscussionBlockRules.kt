package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.ui.text.AnnotatedString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.utils.BlockRuleTarget
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun LocalBlockRules.blocksAuthor(author: User?): Boolean =
    author != null && blocksUser(author.name, author.nameShow)

internal fun LocalBlockRules.blocksFloor(post: Post, content: List<PbContentRender>): Boolean =
    blocksAuthor(post.author) || (post.floor != 1 && count(BlockRuleTarget.COMMENT) != 0 &&
        blocksComment(content.joinToString("")))

// The UI renders a directed reply's recipient in the header. Match the body the reader sees.
internal fun AnnotatedString.visibleReplyBody(): String = splitDirectedReply()?.body?.text ?: text

internal fun List<PbContentRender>.visibleReplyBody(): String = buildString {
    this@visibleReplyBody.forEachIndexed { index, render ->
        if (index == 0 && render is TextContentRender) append(render.text.visibleReplyBody())
        else append(render.toString())
    }
}

internal inline fun LocalBlockRules.blocksNestedReply(author: User?, body: () -> String): Boolean =
    blocksAuthor(author) || (count(BlockRuleTarget.COMMENT) != 0 && blocksComment(body()))

internal fun LocalBlockRules.displayFloors(items: ImmutableList<PostItemData>, hide: Boolean): ImmutableList<PostItemData> {
    if (!hide && count(BlockRuleTarget.USER) == 0 && count(BlockRuleTarget.COMMENT) == 0) return items
    return items.mapNotNull { item ->
        val blocked = item.blocked || blocksFloor(item.post.get(), item.contentRenders)
        when {
            blocked && hide -> null
            blocked != item.blocked -> item.copy(blocked = true)
            else -> item
        }
    }.toImmutableList()
}

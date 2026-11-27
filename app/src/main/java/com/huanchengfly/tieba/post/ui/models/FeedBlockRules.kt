package com.huanchengfly.tieba.post.ui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun LocalBlockRules.blocksOrigin(origin: OriginThreadInfo): Boolean =
    blocksForum(origin.fname) || blocksUser(origin.author?.name, origin.author?.nameShow)

internal fun LocalBlockRules.blocksThread(thread: ThreadInfo): Boolean =
    blocksForum(thread.forumName) || blocksForum(thread.forumInfo?.name) ||
        blocksUser(thread.author?.name, thread.author?.nameShow) ||
        thread.origin_thread_info?.let { blocksOrigin(it) } == true

internal fun LocalBlockRules.blocksSearchResult(result: SearchThreadBean.ThreadInfoBean): Boolean {
    fun blocksAuthor(user: SearchThreadBean.UserInfoBean) = blocksUser(user.userName, user.showNickname)
    val main = result.mainPost
    val quote = result.postInfo
    return blocksForum(result.forumName) || blocksForum(result.forumInfo.forumName) ||
        blocksAuthor(result.user) || (main != null && blocksAuthor(main.user)) ||
        (quote != null && (blocksAuthor(quote.user) || blocksComment(quote.content))) ||
        ((main != null || quote != null) && blocksComment(result.content))
}

/** Filter the presentation only. Page cursors, source counts and cached posts stay on the server's timeline. */
@Composable
internal fun <T> rememberVisibleItems(
    source: ImmutableList<T>,
    isBlocked: LocalBlockRules.(T) -> Boolean,
): ImmutableList<T> {
    val rules = LocalContentBlockRules.current
    return remember(source, rules, isBlocked) {
        if (rules.isEmpty) source else source.filterNot { rules.isBlocked(it) }.toImmutableList()
    }
}

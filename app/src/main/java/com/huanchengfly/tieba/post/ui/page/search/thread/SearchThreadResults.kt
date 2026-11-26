@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadList
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SearchThreadResults(
    data: ImmutableList<SearchThreadBean.ThreadInfoBean>,
    lazyListState: LazyListState,
    navigator: DestinationsNavigator,
    searchKeyword: String,
    hideForum: Boolean = false,
    onRetryPage: (() -> Unit)? = null,
    header: LazyListScope.() -> Unit = {},
) {
    fun open(destination: Direction?) {
        if (destination != null) navigator.navigate(destination)
    }

    SearchThreadList(
        data = data,
        lazyListState = lazyListState,
        onItemClick = { open(it.searchDestination()) },
        onReplyClick = { open(it.searchDestination(scrollToReply = true)) },
        onItemUserClick = { user ->
            user.userId.positiveId()?.let { navigator.navigate(UserProfilePageDestination(it)) }
        },
        onItemForumClick = { forum ->
            if (forum.forumName.isNotBlank()) navigator.navigate(ForumPageDestination(forum.forumName))
        },
        onQuotePostClick = { open(it.searchDestination()) },
        onMainPostClick = { open(it.searchDestination()) },
        hideForum = hideForum,
        searchKeyword = searchKeyword,
        header = header,
        footer = {
            if (onRetryPage != null) {
                item(key = "search-page-retry") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.text_load_failed))
                        TextButton(onClick = onRetryPage, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.button_retry)) }
                    }
                }
            }
        },
    )
}

private fun String?.positiveId(): Long? = this?.toLongOrNull()?.takeIf { it > 0 }

internal fun SearchThreadBean.ThreadInfoBean.searchDestination(scrollToReply: Boolean = false): Direction? {
    val threadId = tid.positiveId() ?: return postInfo?.searchDestination(scrollToReply) ?: mainPost?.searchDestination()
    if (postInfo != null) {
        cid.positiveId()?.let { subPostId ->
            return SubPostsPageDestination(threadId = threadId, subPostId = subPostId, loadFromSubPost = true)
        }
    }
    if (mainPost != null || postInfo != null) {
        val postId = pid.positiveId() ?: postInfo?.takeIf { it.tid == threadId && it.pid > 0 }?.pid
        if (postId != null) return ThreadPageDestination(threadId = threadId, postId = postId, scrollToReply = true)
    }
    return ThreadPageDestination(threadId = threadId, scrollToReply = scrollToReply)
}

internal fun SearchThreadBean.PostInfo.searchDestination(scrollToReply: Boolean = false): Direction? = when {
    tid <= 0 -> null
    pid > 0 -> ThreadPageDestination(threadId = tid, postId = pid, scrollToReply = true)
    else -> ThreadPageDestination(threadId = tid, scrollToReply = scrollToReply)
}

internal fun SearchThreadBean.MainPost.searchDestination(): Direction? =
    if (tid > 0) ThreadPageDestination(threadId = tid, scrollToReply = true) else null

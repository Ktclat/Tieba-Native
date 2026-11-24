package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.MessageListBean.MessageInfoBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.thread.CommentHeader
import com.huanchengfly.tieba.post.ui.page.thread.CommentLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.ramcosta.composedestinations.spec.Direction
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationsListPage(
    type: NotificationsType,
    viewModel: NotificationsListViewModel = when (type) {
        NotificationsType.ReplyMe -> pageViewModel<NotificationsListUiIntent, ReplyMeListViewModel>()
        NotificationsType.AtMe -> pageViewModel<NotificationsListUiIntent, AtMeListViewModel>()
    }
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(NotificationsListUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::hasMore,
        initial = true
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::data,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::currentPage,
        initial = 1
    )
    val rowKeys = remember(data) { notificationKeys(data) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(NotificationsListUiIntent.Refresh) }
    )
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier.pullRefresh(pullRefreshState)
    ) {
        LoadMoreLayout(
            isLoading = isLoadingMore,
            onLoadMore = { viewModel.send(NotificationsListUiIntent.LoadMore(currentPage + 1)) },
            loadEnd = !hasMore,
            lazyListState = lazyListState,
        ) {
            MyLazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                state = lazyListState,
            ) {
                itemsIndexed(data, key = { index, _ -> rowKeys[index] }) { _, (info, blocked) ->
                    NotificationCard(type, info, blocked) { navigator.navigate(it) }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.primary,
        )
    }
}

@Composable
internal fun NotificationCard(
    type: NotificationsType,
    info: MessageInfoBean,
    blocked: Boolean,
    onNavigate: (Direction) -> Unit,
) {
    Container {
        BlockableContent(
            blocked = blocked,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            blockedTip = { BlockTip { Text(stringResource(R.string.tip_blocked_message)) } },
        ) {
            val threadId = info.threadId.positiveLong()
            val postId = info.postId.positiveLong()
            val quotePid = info.quotePid.positiveLong()
            val isFloor = info.isFloor == "1"
            val author = info.replyer
            val authorId = author?.id.positiveLong()
            val time = info.time.positiveLong()
            val openReply = stringResource(R.string.notification_open_reply)
            val rowClick = if (threadId == null) Modifier else Modifier.clickable(
                role = Role.Button,
                onClickLabel = openReply,
                onClick = {
                    val target = if (isFloor && postId != null) {
                        SubPostsPageDestination(threadId = threadId, subPostId = postId, loadFromSubPost = true)
                    } else {
                        ThreadPageDestination(threadId = threadId, postId = postId ?: 0L)
                    }
                    onNavigate(target)
                },
            )
            val quotedReply = type == NotificationsType.ReplyMe && isFloor
            val quoteText = if (quotedReply) info.quoteContent else info.title
            CommentLayout(
                modifier = Modifier.fillMaxWidth().then(rowClick).padding(horizontal = 16.dp, vertical = 12.dp),
                header = if (author != null || time != null) {
                    {
                        val name = author?.nameShow?.takeIf { it.isNotBlank() }
                            ?: author?.name?.takeIf { it.isNotBlank() }
                            ?: if (authorId != null) stringResource(R.string.notification_unknown_author) else ""
                        CommentHeader(
                            name = AnnotatedString(name),
                            portrait = author?.portrait,
                            time = time,
                            onAuthorClick = if (authorId != null) {
                                { onNavigate(UserProfilePageDestination(authorId)) }
                            } else null,
                        )
                    }
                } else null,
            ) {
                if (!info.content.isNullOrBlank()) {
                    EmoticonText(
                        text = info.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else if (quoteText.isNullOrBlank()) {
                    Text(stringResource(R.string.notification_content_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!quoteText.isNullOrBlank()) {
                    val label = when {
                        quotedReply -> R.string.notification_quoted_reply
                        type == NotificationsType.ReplyMe -> R.string.notification_replied_thread
                        else -> R.string.notification_mentioned_thread
                    }
                    val quoteClick = if (threadId == null) Modifier else Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.notification_open_source),
                        onClick = {
                            val target = if (isFloor && quotePid != null) {
                                SubPostsPageDestination(threadId = threadId, postId = quotePid, loadFromSubPost = true)
                            } else ThreadPageDestination(threadId = threadId)
                            onNavigate(target)
                        },
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(
                            modifier = Modifier.then(quoteClick).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            EmoticonText(
                                text = quoteText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String?.positiveLong(): Long? = this?.toLongOrNull()?.takeIf { it > 0 }

internal fun notificationKeys(rows: List<MessageItemData>): List<String> {
    val occurrences = mutableMapOf<String, Int>()
    return rows.map { (info) ->
        val authorId = info.replyer?.id
        val base = if (info.postId.positiveLong() != null && info.time.positiveLong() != null) {
            "${info.postId}_${authorId}_${info.time}"
        } else {
            "partial-${info.hashCode()}"
        }
        // The server can repeat a notification, including ones with no post or author ID.
        val count = occurrences.getOrDefault(base, 0)
        occurrences[base] = count + 1
        if (count == 0) base else "$base#$count"
    }
}

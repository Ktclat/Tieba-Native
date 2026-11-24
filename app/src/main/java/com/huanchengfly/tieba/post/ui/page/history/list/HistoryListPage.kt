package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonParseException
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.hiltViewModel
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.HistoryUtil

@Composable
internal fun HistoryListPage(
    type: Int,
    query: String,
    refreshVersion: Long,
    onClearQuery: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: HistoryListViewModel = if (type == HistoryUtil.TYPE_THREAD) {
        hiltViewModel<ThreadHistoryListViewModel>()
    } else {
        hiltViewModel<ForumHistoryListViewModel>()
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val showMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(viewModel, query, refreshVersion) { viewModel.refresh(query) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { result ->
            val message = when (result) {
                HistoryListUiEvent.Delete.Success -> context.getString(R.string.delete_history_success)
                is HistoryListUiEvent.Delete.Failure -> context.getString(R.string.delete_history_failure, result.errorMsg)
            }
            showMessage(message)
        }
    }
    when {
        state.isRefreshing || state.query != query -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        state.todayHistoryData.isEmpty() && state.beforeHistoryData.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.error != null -> {
                        Text(stringResource(R.string.text_load_failed))
                        TextButton(onClick = { viewModel.refresh(query) }) { Text(stringResource(R.string.button_retry)) }
                    }
                    state.hasMore -> {
                        LaunchedEffect(viewModel, query, state.hasMore) { viewModel.loadMore() }
                        CircularProgressIndicator()
                    }
                    query.isNotEmpty() -> {
                        Text(stringResource(R.string.history_no_matches))
                        TextButton(onClick = onClearQuery) { Text(stringResource(R.string.history_clear_query)) }
                    }
                    else -> {
                        val label = if (type == HistoryUtil.TYPE_THREAD) R.string.history_empty_thread else R.string.history_empty_forum
                        Text(stringResource(label))
                    }
                }
            }
        }
        else -> key(query) {
            HistoryRows(
                state = state,
                onLoadMore = viewModel::loadMore,
                onDelete = { viewModel.delete(it.id) },
                onClick = { entry ->
                    if (entry.type == HistoryUtil.TYPE_FORUM) {
                        navigator.navigate(ForumPageDestination(entry.data))
                    } else {
                        val tid = entry.data.toLongOrNull()
                        if (tid != null) {
                            val extra = try {
                                entry.extras?.fromJson<ThreadHistoryInfoBean>()
                            } catch (_: JsonParseException) {
                                null
                            }
                            navigator.navigate(
                                ThreadPageDestination(
                                    threadId = tid,
                                    postId = extra?.pid?.toLongOrNull() ?: 0L,
                                    seeLz = extra?.isSeeLz ?: false,
                                    from = ThreadPageFrom.FROM_HISTORY,
                                )
                            )
                        }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRows(
    state: HistoryListUiState,
    onLoadMore: () -> Unit,
    onDelete: (History) -> Unit,
    onClick: (History) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreLayout(
        isLoading = state.isLoadingMore,
        onLoadMore = onLoadMore,
        loadEnd = !state.hasMore,
        enableLoadMore = state.error == null,
        lazyListState = listState,
        isEmpty = false,
    ) {
        MyLazyColumn(Modifier.fillMaxSize(), state = listState) {
            val groups = listOf(
                R.string.title_history_today to state.todayHistoryData,
                R.string.title_history_before to state.beforeHistoryData,
            )
            for ((label, rows) in groups) {
                if (rows.isEmpty()) continue
                stickyHeader(key = "group-$label") {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { heading() },
                    )
                }
                items(rows, key = { it.id }) { entry ->
                    HistoryItem(entry, onClick = onClick, onDelete = onDelete)
                }
            }
            if (state.error != null) {
                item {
                    TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.button_retry))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(info: History, onClick: (History) -> Unit, onDelete: (History) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val thread = info.type == HistoryUtil.TYPE_THREAD
    val title = info.title.ifBlank { info.data }
    val menuLabel = stringResource(R.string.history_item_actions, title)
    val time = DateTimeUtils.getRelativeTimeString(LocalContext.current, info.timestamp)
    ListItem(
        modifier = Modifier.combinedClickable(
            role = Role.Button,
            onClick = { onClick(info) },
            onLongClickLabel = menuLabel,
            onLongClick = { menuOpen = true },
        ),
        headlineContent = { Text(title) },
        supportingContent = {
            val subtitle = if (thread && !info.username.isNullOrBlank()) {
                stringResource(R.string.history_author_time, info.username, time)
            } else time
            Text(subtitle)
        },
        leadingContent = {
            if (info.avatar.isNullOrBlank()) {
                Icon(if (thread) Icons.Outlined.Person else Icons.Outlined.Forum, contentDescription = null)
            } else Avatar(data = info.avatar, size = Sizes.Small, contentDescription = null)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, menuLabel) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.title_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete(info)
                        },
                    )
                }
            }
        },
    )
}

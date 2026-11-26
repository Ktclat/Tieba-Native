@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.history.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId

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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
        }
        state.rows.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    state.error != null -> {
                        Text(stringResource(R.string.text_load_failed))
                        TextButton(onClick = { viewModel.refresh(query) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.button_retry)) }
                    }
                    state.hasMore -> {
                        LaunchedEffect(viewModel, query, state.hasMore) { viewModel.loadMore() }
                        LoadingIndicator()
                    }
                    query.isNotEmpty() -> {
                        Text(stringResource(R.string.history_no_matches))
                        TextButton(onClick = onClearQuery, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.history_clear_query)) }
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

@Composable
private fun rememberHistoryGroups(visits: List<History>): HistoryGroups? {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val groups by produceState<HistoryGroups?>(null, visits, locale, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val clockChanged = Channel<Unit>(Channel.CONFLATED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) { clockChanged.trySend(Unit) }
            }
            val filter = IntentFilter(Intent.ACTION_TIME_CHANGED).apply { addAction(Intent.ACTION_TIMEZONE_CHANGED) }
            // Both actions are protected system broadcasts.
            context.registerReceiver(receiver, filter)
            try {
                var timeline = HistoryTimeline(context, locale, ZoneId.systemDefault())
                while (isActive) {
                    val result = withContext(Dispatchers.Default) { timeline.group(visits, System.currentTimeMillis()) }
                    value = result
                    val waitMillis = (result.nextChange - System.currentTimeMillis()).coerceAtLeast(1)
                    if (withTimeoutOrNull(waitMillis) { clockChanged.receive() } != null) {
                        timeline = HistoryTimeline(context, locale, ZoneId.systemDefault())
                    }
                }
            } finally {
                context.unregisterReceiver(receiver)
                clockChanged.close()
            }
        }
    }
    return groups
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
    val groups = rememberHistoryGroups(state.rows) ?: return
    val leading = rememberVectorPainter(if (state.rows.first().type == HistoryUtil.TYPE_THREAD) Icons.Outlined.Person else Icons.Outlined.Forum)
    val more = rememberVectorPainter(Icons.Outlined.MoreVert)
    val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    LoadMoreLayout(
        isLoading = state.isLoadingMore,
        onLoadMore = onLoadMore,
        loadEnd = !state.hasMore,
        enableLoadMore = state.error == null,
        lazyListState = listState,
        isEmpty = false,
    ) {
        MyLazyColumn(Modifier.fillMaxSize(), state = listState) {
            val sections = listOf(R.string.title_history_today to groups.today, R.string.title_history_before to groups.earlier)
            for ((label, rows) in sections) {
                if (rows.isEmpty()) continue
                stickyHeader(key = "group-$label") {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { heading() },
                    )
                }
                items(rows, key = { it.visit.id }, contentType = { "visit" }) { entry ->
                    HistoryItem(entry, rowColors, leading, more, onClick = onClick, onDelete = onDelete)
                }
            }
            if (state.error != null) {
                item {
                    TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.button_retry))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(
    row: VisitLabel,
    colors: ListItemColors,
    leading: Painter,
    more: Painter,
    onClick: (History) -> Unit,
    onDelete: (History) -> Unit,
) {
    val info = row.visit
    // Compose the anchored popup on first use and keep it mounted for its exit transition.
    var menuOpen by remember { mutableStateOf<Boolean?>(null) }
    ListItem(
        colors = colors,
        modifier = Modifier.combinedClickable(
            role = Role.Button,
            onClick = { onClick(info) },
            onLongClickLabel = row.menu,
            onLongClick = { menuOpen = true },
        ),
        headlineContent = { Text(row.title) },
        supportingContent = { Text(row.subtitle) },
        leadingContent = {
            if (info.avatar.isNullOrBlank()) {
                Icon(leading, contentDescription = null)
            } else Avatar(data = info.avatar, size = Sizes.Small, contentDescription = null)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) { Icon(more, row.menu) }
                if (menuOpen != null) DropdownMenu(expanded = menuOpen == true, onDismissRequest = { menuOpen = false }) {
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

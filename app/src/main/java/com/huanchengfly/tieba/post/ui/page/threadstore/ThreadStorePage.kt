@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.hiltViewModel
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.FilteredPageNotice
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getUsernameAnnotatedString
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Destination(deepLinks = [DeepLink(uriPattern = "tblite://favorite")])
@Composable
fun ThreadStorePage(navigator: DestinationsNavigator) {
    val accountId = LocalAccount.current?.uid
    key(accountId) {
        val snackbarHostState = remember { SnackbarHostState() }
        FavoritesScaffold(onBack = { navigator.navigateUp() }, snackbarHostState = snackbarHostState) { content ->
            if (accountId == null) {
                Column(
                    content,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    Text(stringResource(R.string.favorite_login_required), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { navigator.navigate(LoginPageDestination) }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.button_login))
                    }
                }
            } else {
                val viewModel = hiltViewModel<ThreadStoreViewModel>(key = "favorites:$accountId")
                AccountFavorites(viewModel, navigator, snackbarHostState, content)
            }
        }
    }
}

@Composable
internal fun FavoritesScaffold(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (Modifier) -> Unit,
) {
    MyScaffold(
        backgroundColor = MaterialTheme.colorScheme.surface,
        snackbarHostState = snackbarHostState,
        topBar = {
            TitleCentredToolbar(
                title = stringResource(R.string.title_my_collect),
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
            )
        },
    ) { padding -> content(Modifier.fillMaxSize().padding(padding)) }
}

@Composable
private fun AccountFavorites(
    viewModel: ThreadStoreViewModel,
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
) {
    // A thread can be added, removed or re-bookmarked on the detail screen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ids = remember(state.data) { state.data.map { it.threadId } }
    val visits = remember(ids) {
        HistoryUtil.threadVisits(ids).catch { error ->
            if (error !is Exception || error is CancellationException) throw error
            emit(emptyList())
        }.map { rows ->
            val positions = buildMap<String, FavoriteReadingPosition> {
                for (row in rows) {
                    if (row.data !in this) {
                        FavoriteReadingPosition.fromHistory(row.extras)?.let { put(row.data, it) }
                    }
                }
            }
            ids.toSet() to positions
        }.flowOn(Dispatchers.IO)
    }
    val snapshot by visits.collectAsStateWithLifecycle(initialValue = emptySet<String>() to emptyMap())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel, context) {
        viewModel.deleteResults.collect { error ->
            val message = if (error == null) context.getString(R.string.delete_store_success)
                else context.getString(R.string.delete_store_failure, error.getErrorMessage())
            snackbarHostState.showSnackbar(message)
        }
    }
    FavoriteListContent(
        state = state,
        progress = snapshot.second,
        knownHistory = snapshot.first,
        defaultOnlyAuthor = context.appPreferences.collectThreadSeeLz,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onOpen = { info, displayedPosition ->
            if (!opening && info.isDeleted != "1") {
                opening = true
                scope.launch(Dispatchers.Main.immediate) {
                    try {
                        val position = try {
                            val latest = HistoryUtil.latestThreadVisit(info.threadId)
                            FavoriteReadingPosition.fromHistory(latest?.extras)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            displayedPosition
                        }
                        info.threadDestination(
                            position,
                            context.appPreferences.collectThreadSeeLz,
                            context.appPreferences.collectThreadDescSort,
                        )?.let { navigator.navigate(it) }
                    } finally {
                        opening = false
                    }
                }
            }
        },
        onUserClick = { navigator.navigate(UserProfilePageDestination(it)) },
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteListContent(
    state: ThreadStoreUiState,
    progress: Map<String, FavoriteReadingPosition>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (ThreadStoreBean.ThreadStoreInfo, FavoriteReadingPosition?) -> Unit,
    onUserClick: (Long) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    knownHistory: Set<String>? = null,
    defaultOnlyAuthor: Boolean = false,
) {
    var keyword by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FavoriteFilter.ALL) }
    val query = keyword.trim()
    val focusManager = LocalFocusManager.current
    val rules = LocalContentBlockRules.current
    val allowed = remember(state.data, rules) {
        if (rules.isEmpty) state.data else state.data.filterNot {
            rules.blocksForum(it.forumName) || rules.blocksUser(it.author?.name, it.author?.nameShow)
        }
    }
    val positions = remember(allowed, progress, defaultOnlyAuthor) {
        buildMap {
            for (info in allowed) {
                val position = progress[info.threadId] ?: info.serverReadingPosition(defaultOnlyAuthor)
                if (position != null) put(info.threadId, position)
            }
        }
    }
    val awaitingHistory = filter != FavoriteFilter.ALL && knownHistory != null && allowed.any { it.threadId !in knownHistory }
    val visible = remember(allowed, query, filter, positions, knownHistory) {
        allowed.filter {
            (filter == FavoriteFilter.ALL || knownHistory == null || it.threadId in knownHistory) &&
                it.matchesFavoriteQuery(query) && filter.matches(it, positions[it.threadId])
        }
    }
    val filtering = query.isNotEmpty() || filter != FavoriteFilter.ALL
    val scanning = filtering || (allowed.isEmpty() && state.nextOffset > 0)
    val scanNext = scanning && state.hasMore && !state.isRefreshing && !state.isLoadingMore && state.error == null
    LaunchedEffect(scanNext, state.nextOffset) {
        if (scanNext) onLoadMore()
    }
    val pull = rememberPullToRefreshState()
    Column(modifier = modifier) {
        SearchBox(
            keyword = keyword, onKeywordChange = { keyword = it },
            onKeywordSubmit = { focusManager.clearFocus() },
            showSubmitButton = false,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.favorite_search_hint)) },
            prependIcon = { Icon(painterResource(R.drawable.ms_search), contentDescription = null) },
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(FavoriteFilter.entries) { choice ->
                FilterChip(
                    selected = choice == filter, onClick = {
                        filter = choice
                        focusManager.clearFocus()
                    },
                    label = { Text(stringResource(choice.label)) },
                )
            }
        }
        Box(Modifier.fillMaxSize().pullToRefresh(isRefreshing = state.isRefreshing, state = pull, onRefresh = onRefresh)) {
            val lazyState = key(query, filter) { rememberLazyListState() }
            LoadMoreLayout(
                isLoading = state.isLoadingMore, onLoadMore = onLoadMore, loadEnd = !state.hasMore,
                lazyListState = lazyState, isEmpty = visible.isEmpty(),
                enableLoadMore = !scanning && !state.isRefreshing && state.error == null,
            ) {
                MyLazyColumn(modifier = Modifier.fillMaxSize(), state = lazyState) {
                    items(visible, key = { it.threadId }) { info ->
                        val position = positions[info.threadId].takeIf { knownHistory == null || info.threadId in knownHistory }
                        StoreItem(
                            info = info, position = position,
                            onUserClick = { info.author?.lzUid?.toLongOrNull()?.let(onUserClick) },
                            onClick = { onOpen(info, position) },
                            onDelete = { onDelete(info.threadId) },
                        )
                    }
                    if (state.error != null) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.error.getErrorMessage().ifBlank { stringResource(R.string.favorite_first_page_error) },
                                    style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = if (state.retryRefresh) onRefresh else onLoadMore) {
                                    Text(stringResource(R.string.button_retry))
                                }
                            }
                        }
                    } else if (visible.isEmpty() && !state.isRefreshing) {
                        item {
                            if (!filtering && allowed.isEmpty() && state.data.isNotEmpty() && !state.hasMore) {
                                FilteredPageNotice()
                            } else {
                                val message = when {
                                    (scanning && state.hasMore) || awaitingHistory -> R.string.favorite_search_loading
                                    filtering -> R.string.favorite_search_empty
                                    else -> R.string.favorite_empty
                                }
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            if (scanning && state.isLoadingMore) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            PullToRefreshDefaults.LoadingIndicator(
                isRefreshing = state.isRefreshing, state = pull, modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun StoreItem(
    info: ThreadStoreBean.ThreadStoreInfo,
    position: FavoriteReadingPosition?,
    onUserClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = remember(info) { (info.count?.toLongOrNull() ?: 0) > 0 && (info.postNo?.toLongOrNull() ?: 0) > 0 }
    val isDeleted = remember(info) { info.isDeleted == "1" }
    LongClickMenu(
        menuContent = {
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.title_collect_on)) },
                onClick = onDelete,
            )
        },
        onClick = onClick
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                avatar = {
                    Avatar(
                        data = StringUtil.getAvatarUrl(info.author?.userPortrait),
                        size = Sizes.Small,
                        contentDescription = null
                    )
                },
                name = {
                    Text(
                        text = getUsernameAnnotatedString(
                            LocalContext.current,
                            info.author?.name.orEmpty(),
                            info.author?.nameShow,
                            LocalContentColor.current
                        )
                    )
                },
                onClick = onUserClick,
                content = if (hasUpdate) {
                    {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) { Text(stringResource(R.string.tip_thread_store_update, info.postNo)) }
                    }
                } else null,
            )
            Text(
                text = info.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!info.forumName.isNullOrBlank()) {
                    Text(stringResource(R.string.title_forum, info.forumName), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (position != null) {
                    Text(
                        position.floor?.let { stringResource(R.string.favorite_read_floor, it) }
                            ?: stringResource(R.string.favorite_filter_has_progress),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

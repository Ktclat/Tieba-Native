@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockedDestination
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import com.huanchengfly.tieba.post.ui.models.blocksSearchResult
import com.huanchengfly.tieba.post.ui.models.rememberVisibleItems
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumSearchPostPageDestination
import com.huanchengfly.tieba.post.ui.page.search.SearchHistorySection
import com.huanchengfly.tieba.post.ui.page.search.SearchTopBar
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadResults
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination
@Composable
fun ForumSearchPostPage(
    forumName: String,
    forumId: Long,
    navigator: DestinationsNavigator,
    viewModel: ForumSearchPostViewModel = pageViewModel<ForumSearchPostUiIntent, ForumSearchPostViewModel>(
        listOf(ForumSearchPostUiIntent.Init(forumName))
    ),
) {
    if (LocalContentBlockRules.current.blocksForum(forumName)) {
        BlockedDestination(onBack = { navigator.navigateUp() }, message = stringResource(R.string.tip_locally_blocked_forum))
        return
    }
    val currentKeyword by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::keyword,
        initial = ""
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::error,
        initial = null
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::data,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isLoadingMore,
        initial = false
    )
    val currentSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::sortType,
        initial = ForumSearchPostSortType.NEWEST
    )
    val currentFilterType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::filterType,
        initial = ForumSearchPostFilterType.ALL
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::currentPage,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::hasMore,
        initial = false
    )
    val searchHistories by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::searchHistories,
        initial = persistentListOf()
    )

    val visible = rememberVisibleItems(data) { blocksSearchResult(it) }
    val isEmpty by remember {
        derivedStateOf { data.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }
    val isKeywordEmpty by remember {
        derivedStateOf { currentKeyword.isEmpty() }
    }
    var inputKeyword by rememberSaveable(forumName) { mutableStateOf(viewModel.uiState.value.keyword) }

    fun refresh() {
        viewModel.requestSearch(
            ForumSearchPostUiIntent.Refresh(
                currentKeyword,
                forumName,
                forumId,
                currentSortType,
                currentFilterType
            )
        )
    }

    fun loadNextPage() {
        if (hasMore && !isRefreshing && !isLoadingMore) {
            viewModel.send(ForumSearchPostUiIntent.LoadMore(
                currentKeyword, forumName, forumId, currentPage + 1, currentSortType, currentFilterType
            ))
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitKeyword(value: String) {
        val keyword = value.trim()
        inputKeyword = keyword
        keyboardController?.hide()
        viewModel.requestSearch(
            ForumSearchPostUiIntent.Refresh(
                keyword, forumName, forumId, currentSortType, currentFilterType, recordHistory = true
            )
        )
    }

    fun showHistory() {
        inputKeyword = ""
        viewModel.requestSearch(ForumSearchPostUiIntent.Refresh("", forumName, forumId, currentSortType, currentFilterType))
    }

    MyBackHandler(enabled = !isKeywordEmpty, currentScreen = ForumSearchPostPageDestination) {
        showHistory()
    }

    fun changeOptions(sortType: Int, filterType: Int) {
        if (sortType != currentSortType || filterType != currentFilterType) {
            viewModel.requestSearch(
                ForumSearchPostUiIntent.Refresh(currentKeyword, forumName, forumId, sortType, filterType)
            )
        }
    }

    MyScaffold(
        topBar = {
            TopAppBarContainer(
                topBar = {
                    SearchTopBar(
                        keyword = inputKeyword,
                        onKeywordChange = {
                            inputKeyword = it
                            if (it.isEmpty()) showHistory()
                        },
                        onKeywordSubmit = ::submitKeyword,
                        onBack = { if (isKeywordEmpty) navigator.navigateUp() else showHistory() },
                        hint = stringResource(R.string.hint_search_in_ba, forumName),
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProvideNavigator(navigator = navigator) {
                if (!isKeywordEmpty) {
                    StateScreen(
                        modifier = Modifier.fillMaxSize(),
                        isEmpty = isEmpty,
                        isError = isError,
                        isLoading = isRefreshing,
                        onReload = ::refresh,
                        clickToReload = true,
                        errorScreen = {
                            error?.item?.let {
                                ErrorScreen(error = it)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pullToRefresh(isRefreshing = isRefreshing, state = pullRefreshState, onRefresh = ::refresh)
                        ) {
                            LoadMoreLayout(
                                isLoading = isLoadingMore,
                                isEmpty = visible.isEmpty(),
                                onLoadMore = ::loadNextPage,
                                enableLoadMore = !isRefreshing,
                                preloadCount = if (error == null) 1 else 0,
                                loadEnd = !hasMore,
                                lazyListState = lazyListState,
                            ) {
                                SearchThreadResults(
                                    data = visible,
                                    filteredAll = data.isNotEmpty() && visible.isEmpty(),
                                    onLoadNext = if (hasMore && !isRefreshing && !isLoadingMore) ::loadNextPage else null,
                                    lazyListState = lazyListState,
                                    navigator = navigator,
                                    hideForum = true,
                                    searchKeyword = currentKeyword,
                                    onRetryPage = if (error == null) null else if (hasMore) ::loadNextPage else ::refresh,
                                ) {
                                    stickyHeader(key = "Sort&Filter") {
                                        ForumSearchOptions(currentSortType, currentFilterType, ::changeOptions)
                                    }
                                }
                            }

                            PullToRefreshDefaults.LoadingIndicator(
                                isRefreshing = isRefreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    }
                } else {
                    SearchHistorySection(
                        entries = searchHistories,
                        term = { it.content },
                        clearMessage = stringResource(R.string.search_history_clear_forum_message, forumName),
                        onSearch = { submitKeyword(it.content) },
                        onDelete = { viewModel.send(ForumSearchPostUiIntent.DeleteHistory(forumName, it.content)) },
                        onClear = { viewModel.send(ForumSearchPostUiIntent.ClearHistory(forumName)) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ForumSearchOptions(sortType: Int, filterType: Int, onChange: (Int, Int) -> Unit) {
    val newest = stringResource(R.string.title_search_post_sort_by_time)
    val relevant = stringResource(R.string.title_search_post_sort_by_relevant)
    var menuOpen by remember { mutableStateOf(false) }
    val onlyThreads = filterType == ForumSearchPostFilterType.ONLY_THREAD

    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            AssistChip(
                onClick = { menuOpen = true },
                label = { Text(if (sortType == ForumSearchPostSortType.NEWEST) newest else relevant) },
                trailingIcon = { Icon(ImageVector.vectorResource(R.drawable.ms_arrow_drop_down), contentDescription = null) },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(ForumSearchPostSortType.NEWEST to newest, ForumSearchPostSortType.RELATIVE to relevant)
                    .forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            trailingIcon = { if (sortType == value) Icon(ImageVector.vectorResource(R.drawable.ms_check), null) },
                            onClick = {
                                menuOpen = false
                                onChange(value, filterType)
                            },
                        )
                    }
            }
        }
        FilterChip(
            selected = onlyThreads,
            onClick = {
                onChange(sortType, if (onlyThreads) ForumSearchPostFilterType.ALL else ForumSearchPostFilterType.ONLY_THREAD)
            },
            label = { Text(stringResource(R.string.title_search_filter_only_thread)) },
            leadingIcon = if (onlyThreads) ({ Icon(ImageVector.vectorResource(R.drawable.ms_check), contentDescription = null) }) else null,
        )
    }
}

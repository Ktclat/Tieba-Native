package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchThreadPage(
    keyword: String,
    sortType: Int = SearchThreadSortType.SORT_TYPE_NEWEST,
    viewModel: SearchThreadViewModel = pageViewModel(),
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query = SearchThreadQuery(keyword, sortType)
    val matchesQuery = state.query == query
    val refresh = { viewModel.send(SearchThreadUiIntent.Refresh(query)) }
    LazyLoad(key = query, loaded = matchesQuery, onLoad = refresh)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = refresh,
    )
    val lazyListState = key(query) { rememberLazyListState() }
    val loadNextPage = { viewModel.send(SearchThreadUiIntent.LoadMore(query, state.currentPage + 1)) }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = !matchesQuery || state.data.isEmpty(),
        isError = matchesQuery && state.error != null,
        isLoading = !matchesQuery || state.isRefreshing,
        onReload = refresh,
        errorScreen = {
            state.error?.let {
                val (e) = it
                ErrorScreen(error = e)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LoadMoreLayout(
                isLoading = state.isLoadingMore,
                onLoadMore = loadNextPage,
                enableLoadMore = LocalShouldLoad.current && !state.isRefreshing,
                loadEnd = !state.hasMore,
                preloadCount = if (state.error == null) 1 else 0,
                lazyListState = lazyListState,
            ) {
                SearchThreadResults(
                    data = state.data,
                    lazyListState = lazyListState,
                    navigator = navigator,
                    searchKeyword = keyword,
                    onRetryPage = when {
                        state.error == null -> null
                        state.hasMore -> loadNextPage
                        else -> refresh
                    },
                )

                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                    contentColor = ExtendedTheme.colors.primary,
                )
            }
        }
    }
}

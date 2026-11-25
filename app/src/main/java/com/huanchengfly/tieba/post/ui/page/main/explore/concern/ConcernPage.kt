package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn

@Composable
fun ConcernPage(
    viewModel: ConcernViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ConcernUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
    val isRefreshing by viewModel.uiState.collectPartialAsState(ConcernUiState::isRefreshing)
    val isLoadingMore by viewModel.uiState.collectPartialAsState(ConcernUiState::isLoadingMore)
    val nextPageTag by viewModel.uiState.collectPartialAsState(ConcernUiState::nextPageTag)
    val data by viewModel.uiState.collectPartialAsState(ConcernUiState::data)

    val lazyListState = rememberLazyListState()
    if (LocalShouldLoad.current) {
        viewModel.bindScrollToTopEvent(lazyListState = lazyListState)
        onGlobalEvent<GlobalEvent.Refresh>(
            filter = { it.key == "concern" }
        ) {
            viewModel.send(ConcernUiIntent.Refresh)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.send(ConcernUiIntent.Refresh) },
    ) {
        LoadMoreLayout(
            isLoading = isLoadingMore,
            onLoadMore = { viewModel.send(ConcernUiIntent.LoadMore(nextPageTag)) },
            lazyListState = lazyListState,
        ) {
            MyLazyColumn(
                state = lazyListState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = data,
                    key = { item -> "${item.recommendType}_${item.threadList?.id}" },
                    contentType = { item -> item.recommendType }
                ) { item ->
                    Container {
                        if (item.recommendType == 1) {
                            FeedCard(
                                item = wrapImmutable(item.threadList!!),
                                onClick = {
                                    navigator.navigate(
                                        ThreadPageDestination(
                                            it.threadId,
                                            it.forumId,
                                            threadInfo = it
                                        )
                                    )
                                },
                                onClickReply = {
                                    navigator.navigate(
                                        ThreadPageDestination(
                                            it.threadId,
                                            it.forumId,
                                            scrollToReply = true
                                        )
                                    )
                                },
                                onAgree = {
                                    viewModel.send(
                                        ConcernUiIntent.Agree(
                                            it.threadId,
                                            it.firstPostId,
                                            it.hasAgree
                                        )
                                    )
                                },
                                onClickForum = { navigator.navigate(ForumPageDestination(it.name)) },
                                onClickUser = { navigator.navigate(UserProfilePageDestination(it.id)) },
                            )
                        } else {
                            Box {}
                        }
                    }
                }
            }
        }
    }
}
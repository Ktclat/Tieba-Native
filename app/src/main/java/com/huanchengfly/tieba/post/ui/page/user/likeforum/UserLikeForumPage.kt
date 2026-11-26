@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.user.likeforum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.api.models.UserLikeForumBean
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserLikeForumPage(
    uid: Long,
    fluid: Boolean = false,
    enablePullRefresh: Boolean = false,
    viewModel: UserLikeForumViewModel = pageViewModel(),
) {
    val navigator = LocalNavigator.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::hasMore,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::forums,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { forums.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "user_profile" }
    ) {
        viewModel.send(UserLikeForumUiIntent.Refresh(uid))
    }

    StateScreen(
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = {
            viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        },
        errorScreen = { ErrorScreen(error = error.getOrNull()) },
    ) {
        val pullRefreshState = rememberPullToRefreshState()

        val lazyListState = rememberLazyListState()

        val pullRefreshModifier =
            if (enablePullRefresh) Modifier.pullToRefresh(isRefreshing = isRefreshing, state = pullRefreshState, onRefresh = ::reload) else Modifier

        Box(modifier = pullRefreshModifier) {
            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = {
                    viewModel.send(UserLikeForumUiIntent.LoadMore(uid, currentPage))
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState
            ) {
                UserLikeForumList(
                    data = forums,
                    fluid = fluid,
                    onClickForum = { forumBean ->
                        forumBean.name?.let {
                            navigator.navigate(ForumPageDestination(it))
                        }
                    },
                    lazyListState = lazyListState
                )
            }

            PullToRefreshDefaults.LoadingIndicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun UserLikeForumList(
    data: ImmutableList<UserLikeForumBean.ForumBean>,
    onClickForum: (UserLikeForumBean.ForumBean) -> Unit,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    MyLazyColumn(state = lazyListState) {
        items(
            items = data,
            key = { it.id }
        ) {
            Container(fluid = fluid) {
                UserLikeForumItem(
                    item = it,
                    onClick = {
                        onClickForum(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun UserLikeForumItem(
    item: UserLikeForumBean.ForumBean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = item.slogan
    ListItem(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        leadingContent = { Avatar(data = item.avatar, size = Sizes.Medium, contentDescription = null) },
        headlineContent = { Text(item.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = description?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
    )
}

package com.huanchengfly.tieba.post.ui.page.search.forum

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchForumBean
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun SearchForumPage(
    keyword: String,
    viewModel: SearchForumViewModel = pageViewModel(),
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val matchesKeyword = state.keyword == keyword
    LazyLoad(key = keyword, loaded = matchesKeyword) {
        viewModel.send(SearchForumUiIntent.Refresh(keyword))
    }

    val exactMatchForum = state.exactMatchForum
    val fuzzyMatchForumList = state.fuzzyMatchForumList
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = { viewModel.send(SearchForumUiIntent.Refresh(keyword)) }
    )

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = !matchesKeyword || (exactMatchForum == null && fuzzyMatchForumList.isEmpty()),
        isError = matchesKeyword && state.error != null,
        isLoading = !matchesKeyword || state.isRefreshing,
        onReload = { viewModel.send(SearchForumUiIntent.Refresh(keyword)) },
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
            MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (exactMatchForum != null) {
                    stickyHeader(key = "ExactMatchHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(
                                text = stringResource(id = R.string.title_exact_match),
                                invertColor = true
                            )
                        }
                    }
                    item(key = "ExactMatch") {
                        SearchForumItem(
                            item = exactMatchForum,
                            onClick = {
                                navigator.navigate(ForumPageDestination(exactMatchForum.forumName.orEmpty()))
                            }
                        )
                    }
                }
                if (fuzzyMatchForumList.isNotEmpty()) {
                    stickyHeader(key = "FuzzyMatchHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(
                                text = stringResource(id = R.string.title_fuzzy_match),
                                invertColor = false
                            )
                        }
                    }
                    items(fuzzyMatchForumList) {
                        SearchForumItem(
                            item = it,
                            onClick = {
                                navigator.navigate(ForumPageDestination(it.forumName.orEmpty()))
                            }
                        )
                    }
                }
            }

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

@Composable
private fun SearchForumItem(
    item: SearchForumBean.ForumInfoBean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Avatar(
            data = item.avatar,
            size = Sizes.Medium,
            contentDescription = item.forumNameShow
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_forum, item.forumNameShow.orEmpty()),
                style = MaterialTheme.typography.subtitle1
            )
            if (!item.intro.isNullOrEmpty()) {
                Text(
                    text = item.slogan.orEmpty(),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1
                )
            }
        }
    }
}
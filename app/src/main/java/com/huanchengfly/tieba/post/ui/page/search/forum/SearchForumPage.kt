@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.search.forum

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchForumBean
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.page.search.SearchResultHeading
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FilteredPageNotice
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val rules = LocalContentBlockRules.current
    fun blocked(forum: SearchForumBean.ForumInfoBean) =
        rules.blocksForum(forum.forumName, forum.forumNameShow)
    val exactMatchForum = state.exactMatchForum?.takeUnless(::blocked)
    val fuzzyMatchForumList = remember(state.fuzzyMatchForumList, rules) {
        if (rules.isEmpty) state.fuzzyMatchForumList else state.fuzzyMatchForumList.filterNot(::blocked)
    }
    val pullRefreshState = rememberPullToRefreshState()

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = !matchesKeyword || (state.exactMatchForum == null && state.fuzzyMatchForumList.isEmpty()),
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
                .pullToRefresh(isRefreshing = state.isRefreshing, state = pullRefreshState, onRefresh = { viewModel.send(SearchForumUiIntent.Refresh(keyword)) })
        ) {
            MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (exactMatchForum == null && fuzzyMatchForumList.isEmpty()) item { FilteredPageNotice() }
                if (exactMatchForum != null) {
                    stickyHeader(key = "ExactMatchHeader") {
                        SearchResultHeading(stringResource(R.string.title_exact_match))
                    }
                    item(key = "ExactMatch") {
                        SearchForumItem(
                            item = exactMatchForum,
                            onClick = {
                                navigator.navigate(ForumPageDestination(it))
                            }
                        )
                    }
                }
                if (fuzzyMatchForumList.isNotEmpty()) {
                    stickyHeader(key = "FuzzyMatchHeader") {
                        SearchResultHeading(stringResource(R.string.title_fuzzy_match))
                    }
                    items(fuzzyMatchForumList) {
                        SearchForumItem(
                            item = it,
                            onClick = {
                                navigator.navigate(ForumPageDestination(it))
                            }
                        )
                    }
                }
            }

            PullToRefreshDefaults.LoadingIndicator(
                isRefreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

}

@Composable
private fun SearchForumItem(
    item: SearchForumBean.ForumInfoBean,
    onClick: (String) -> Unit,
) {
    val name = item.forumName?.takeIf { it.isNotBlank() } ?: item.forumNameShow?.takeIf { it.isNotBlank() }
    val description = item.slogan?.takeIf { it.isNotBlank() } ?: item.intro
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(enabled = name != null) { name?.let(onClick) },
        leadingContent = { Avatar(data = item.avatar, size = Sizes.Medium, contentDescription = null) },
        headlineContent = { Text(stringResource(R.string.title_forum, item.forumNameShow?.takeIf { it.isNotBlank() } ?: name.orEmpty()), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = description?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
    )
}

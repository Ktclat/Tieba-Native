@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.search.user

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchUserBean
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchUserPage(
    keyword: String,
    viewModel: SearchUserViewModel = pageViewModel(),
) {
    val navigator = LocalNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val matchesKeyword = state.keyword == keyword
    LazyLoad(key = keyword, loaded = matchesKeyword) {
        viewModel.send(SearchUserUiIntent.Refresh(keyword))
    }

    val exactMatch = state.exactMatch
    val fuzzyMatch = state.fuzzyMatch
    val pullRefreshState = rememberPullToRefreshState()

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = !matchesKeyword || (exactMatch == null && fuzzyMatch.isEmpty()),
        isError = matchesKeyword && state.error != null,
        isLoading = !matchesKeyword || state.isRefreshing,
        onReload = { viewModel.send(SearchUserUiIntent.Refresh(keyword)) },
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
                .pullToRefresh(isRefreshing = state.isRefreshing, state = pullRefreshState, onRefresh = { viewModel.send(SearchUserUiIntent.Refresh(keyword)) })
        ) {
            MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                if (exactMatch != null) {
                    stickyHeader(key = "ExactMatchHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(
                                text = stringResource(id = R.string.title_exact_match),
                                invertColor = true
                            )
                        }
                    }
                    item(key = "ExactMatch") {
                        SearchUserItem(
                            item = exactMatch,
                            onClick = {
                                val id = exactMatch.id?.toLongOrNull()
                                if (id != null) {
                                    navigator.navigate(UserProfilePageDestination(id))
                                }
                            }
                        )
                    }
                }
                if (fuzzyMatch.isNotEmpty()) {
                    stickyHeader(key = "FuzzyMatchHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(
                                text = stringResource(id = R.string.title_fuzzy_match_user),
                                invertColor = false
                            )
                        }
                    }
                    items(fuzzyMatch) {
                        SearchUserItem(
                            item = it,
                            onClick = {
                                val id = it.id?.toLongOrNull()
                                if (id != null) {
                                    navigator.navigate(UserProfilePageDestination(id))
                                }
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
private fun SearchUserItem(
    item: SearchUserBean.UserBean,
    onClick: () -> Unit,
) {
    val description = item.intro
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        leadingContent = { Avatar(data = StringUtil.getAvatarUrl(item.portrait), size = Sizes.Medium, contentDescription = null) },
        headlineContent = { Text(StringUtil.getUsernameAnnotatedString(LocalContext.current, item.name.orEmpty(), item.showNickname), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = description?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
    )
}

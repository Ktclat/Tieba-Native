@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import androidx.compose.material3.PrimaryTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://notifications/{initialTab}")
    ]
)
@Composable
fun NotificationsPage(
    navigator: DestinationsNavigator,
    initialTab: Int = 0,
) {
    val pages = listOf<Pair<String, (@Composable () -> Unit)>>(
        stringResource(id = R.string.title_reply_me) to @Composable {
            NotificationsListPage(type = NotificationsType.ReplyMe)
        },
        stringResource(id = R.string.title_at_me) to @Composable {
            NotificationsListPage(type = NotificationsType.AtMe)
        }
    )
    val pagerState = rememberPagerState(
        initialPage = initialTab,
    ) { pages.size }
    ProvideNavigator(navigator = navigator) {
        MyScaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TitleCentredToolbar(
                    title = { Text(text = stringResource(id = R.string.title_notifications)) },
                    navigationIcon = {
                        BackNavigationIcon {
                            navigator.navigateUp()
                        }
                    }
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        divider = {},
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        pages.forEachIndexed { index, pair ->
                            Tab(
                                text = { androidx.compose.material3.Text(text = pair.first) },
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    pagerState.requestScrollToPage(index)
                                },
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            LazyLoadHorizontalPager(
                state = pagerState,
                contentPadding = paddingValues,
                key = { pages[it].first },
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                pages[it].second()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationsPage(
    initialTab: Int = 0,
) {
    val navigator = LocalNavigator.current
    val pages = listOf<Pair<String, (@Composable () -> Unit)>>(
        stringResource(id = R.string.title_reply_me) to @Composable {
            NotificationsListPage(type = NotificationsType.ReplyMe)
        },
        stringResource(id = R.string.title_at_me) to @Composable {
            NotificationsListPage(type = NotificationsType.AtMe)
        }
    )
    val pagerState = rememberPagerState(
        initialPage = initialTab,
    ) { pages.size }
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_notifications),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.title_search)
                    ) {
                        navigator.navigate(SearchPageDestination)
                    }
                },
            ) {
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    divider = {},
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    pages.forEachIndexed { index, pair -> 
                        Tab(
                            text = { androidx.compose.material3.Text(text = pair.first) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                pagerState.requestScrollToPage(index)
                            },
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            state = pagerState,
            contentPadding = paddingValues,
            key = { pages[it].first },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].second()
        }
    }
}
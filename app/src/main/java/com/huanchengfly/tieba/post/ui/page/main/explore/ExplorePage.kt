@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabsButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch


@Immutable
data class ExplorePageItem(
    val id: String,
    val name: @Composable () -> Unit,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.ExplorePageTab(
    pagerState: PagerState,
    pages: ImmutableList<ExplorePageItem>
) {
    val coroutineScope = rememberCoroutineScope()

    PrimaryTabRow(
        selectedTabIndex = pagerState.currentPage,
        divider = {},
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(76.dp * pages.size),
    ) {
        pages.fastForEachIndexed { index, item ->
            Tab(
                text = item.name,
                selected = pagerState.currentPage == index,
                onClick = {
                    if (pagerState.currentPage == index) {
                        coroutineScope.launch { emitGlobalEvent(GlobalEvent.Refresh(item.id)) }
                    } else {
                        pagerState.requestScrollToPage(index)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorePage() {
    val account = LocalAccount.current
    val navigator = LocalNavigator.current

    val loggedIn = remember(account) { account != null }

    val pages = remember(loggedIn) {
        listOfNotNull(
            if (loggedIn) ExplorePageItem(
                "concern",
                { Text(stringResource(R.string.title_concern)) },
                { ConcernPage() }
            ) else null,
            ExplorePageItem(
                "personalized",
                { Text(stringResource(R.string.title_personalized)) },
                { PersonalizedPage() }
            ),
            ExplorePageItem(
                "hot",
                { Text(stringResource(R.string.title_hot)) },
                { HotPage() }
            ),
        ).toImmutableList()
    }
    val pagerState = rememberPagerState(initialPage = if (account != null) 1 else 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    if (LocalShouldLoad.current) {
        onGlobalEvent<GlobalEvent.Refresh>(
            filter = { it.key == "explore" }
        ) {
            coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(pages[pagerState.currentPage].id))
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_explore),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = ImageVector.vectorResource(R.drawable.ms_search),
                        contentDescription = stringResource(id = R.string.title_search)
                    ) {
                        navigator.navigate(SearchPageDestination)
                    }
                    ThreadTabsButton(navigator)
                },
            ) {
                ExplorePageTab(pagerState = pagerState, pages = pages)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            contentPadding = paddingValues,
            state = pagerState,
            key = { pages[it].id },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].content()
        }
    }
}

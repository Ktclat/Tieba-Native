@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PersonalizedPage(
    viewModel: PersonalizedViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(PersonalizedUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
    val isRefreshing by viewModel.uiState.collectPartialAsState(PersonalizedUiState::isRefreshing)
    val isLoadingMore by viewModel.uiState.collectPartialAsState(PersonalizedUiState::isLoadingMore)
    val currentPage by viewModel.uiState.collectPartialAsState(PersonalizedUiState::currentPage)
    val data by viewModel.uiState.collectPartialAsState(PersonalizedUiState::data)
    val error by viewModel.uiState.collectPartialAsState(PersonalizedUiState::error)
    val refreshPosition by viewModel.uiState.collectPartialAsState(PersonalizedUiState::refreshPosition)
    val hiddenThreadIds by viewModel.uiState.collectPartialAsState(PersonalizedUiState::hiddenThreadIds)
    val lazyListState = rememberLazyListState()
    val isEmpty by remember {
        derivedStateOf {
            data.isEmpty()
        }
    }
    val isError by remember {
        derivedStateOf {
            error != null
        }
    }
    var refreshCount by remember {
        mutableIntStateOf(0)
    }
    var showRefreshTip by remember(LocalShouldLoad.current) {
        mutableStateOf(false)
    }

    if (LocalShouldLoad.current) {
        viewModel.bindScrollToTopEvent(lazyListState = lazyListState)
        onGlobalEvent<GlobalEvent.Refresh>(
            filter = { it.key == "personalized" }
        ) {
            viewModel.send(PersonalizedUiIntent.Refresh)
        }
        viewModel.onEvent<PersonalizedUiEvent.RefreshSuccess> {
            refreshCount = it.count
            showRefreshTip = true
        }
    }

    if (showRefreshTip) {
        LaunchedEffect(Unit) {
            launch {
                delay(20)
                lazyListState.scrollToItem(0, 0)
            }
            delay(2000)
            showRefreshTip = false
        }
    }
//    if (lazyListState.isScrollInProgress) {
//        DisposableEffect(Unit) {
//            PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = true
//            onDispose {
//                PauseLoadWhenScrollingDrawableDecodeInterceptor.scrolling = false
//            }
//        }
//    }
    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = { viewModel.send(PersonalizedUiIntent.Refresh) },
        errorScreen = {
            error?.let {
                ErrorScreen(
                    error = it.get()
                )
            }
        }
    ) {
        PullToRefreshLayout(
            refreshing = isRefreshing,
            onRefresh = { viewModel.send(PersonalizedUiIntent.Refresh) },
        ) {
            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = { viewModel.send(PersonalizedUiIntent.LoadMore(currentPage + 1)) },
                loadEnd = false,
                lazyListState = lazyListState,
                isEmpty = data.isEmpty()
            ) {
                FeedList(
                    state = lazyListState,
                    dataProvider = { data },
                    refreshPositionProvider = { refreshPosition },
                    hiddenThreadIdsProvider = { hiddenThreadIds },
                    onItemClick = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.id,
                                it.forumId,
                                threadInfo = it
                            )
                        )
                    },
                    onItemReplyClick = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.id,
                                it.forumId,
                                scrollToReply = true
                            )
                        )
                    },
                    onAgree = {
                        viewModel.send(
                            PersonalizedUiIntent.Agree(
                                it.threadId,
                                it.firstPostId,
                                it.agree?.hasAgree ?: 0
                            )
                        )
                    },
                    onDislike = { item, clickTime, reasons ->
                        viewModel.send(
                            PersonalizedUiIntent.Dislike(
                                item.forumInfo?.id ?: 0,
                                item.threadId,
                                reasons,
                                clickTime
                            )
                        )
                    },
                    onRefresh = { viewModel.send(PersonalizedUiIntent.Refresh) },
                    onOpenForum = { navigator.navigate(ForumPageDestination(it)) },
                    onClickUser = { navigator.navigate(UserProfilePageDestination(it.id)) }
                )
            }

            AnimatedVisibility(
                visible = showRefreshTip,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                RefreshTip(refreshCount = refreshCount)
            }
        }
    }
}

@Composable
private fun BoxScope.RefreshTip(refreshCount: Int) {
    Surface(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Text(
            text = stringResource(R.string.toast_feed_refresh, refreshCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FeedList(
    state: LazyListState,
    dataProvider: () -> ImmutableList<ThreadItemData>,
    refreshPositionProvider: () -> Int,
    hiddenThreadIdsProvider: () -> ImmutableList<Long>,
    onItemClick: (ThreadInfo) -> Unit,
    onItemReplyClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    onDislike: (ThreadInfo, Long, ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
    onRefresh: () -> Unit,
    onOpenForum: (forumName: String) -> Unit = {},
    onClickUser: (User) -> Unit = {},
) {
    val data = dataProvider()
    val refreshPosition = refreshPositionProvider()
    val hiddenIds = hiddenThreadIdsProvider()
    val hiddenThreadIds = remember(hiddenIds) { hiddenIds.toHashSet() }

    MyLazyColumn(
        state = state,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(
            items = data,
            key = { _, (item) -> "${item.get { id }}" },
            contentType = { _, (item) ->
                when {
                    item.get { videoInfo } != null -> "Video"
                    item.get { media }.size == 1 -> "SingleMedia"
                    item.get { media }.size > 1 -> "MultiMedia"
                    else -> "PlainText"
                }
            }
        ) { index, (item, blocked, personalized, hidden) ->
            val isHidden = hidden || item.get { threadId } in hiddenThreadIds
            val isRefreshPosition = index + 1 == refreshPosition
            Container {
                AnimatedVisibility(
                    visible = !isHidden,
                    enter = EnterTransition.None,
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        BlockableContent(
                            blocked = blocked,
                            blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                        ) {
                            FeedCard(
                                item = item,
                                onClick = onItemClick,
                                onClickReply = onItemReplyClick,
                                onAgree = onAgree,
                                onClickForum = remember {
                                    {
                                        onOpenForum(it.name)
                                    }
                                },
                                onClickUser = onClickUser,
                            ) {
                                if (personalized != null) {
                                    Dislike(
                                        personalized = personalized,
                                        onDislike = { clickTime, reasons ->
                                            onDislike(item.get(), clickTime, reasons)
                                        }
                                    )
                                }
                            }
                        }
                        if (isRefreshPosition) {
                            RefreshTip(onRefresh)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshTip(
    onRefresh: () -> Unit
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onRefresh, shapes = ButtonDefaults.shapes()) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ms_refresh),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.tip_refresh))
        }
    }
}

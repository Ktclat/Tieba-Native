package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import com.huanchengfly.tieba.post.ui.models.blocksThread
import com.huanchengfly.tieba.post.ui.models.rememberVisibleItems
import com.huanchengfly.tieba.post.ui.widgets.compose.FilteredPageNotice
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.frsPage.Classify
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.models.ThreadItemData
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumRuleDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private fun getFirstLoadIntent(forumName: String, isGood: Boolean, sortType: Int): ForumThreadListUiIntent {
    return if (isGood) ForumThreadListUiIntent.Refresh(forumName, -1, 0)
    else ForumThreadListUiIntent.FirstLoad(forumName, sortType, null)
}

private fun getRefreshIntent(
    forumName: String,
    isGood: Boolean,
    sortType: Int,
    goodClassifyId: Int?,
): ForumThreadListUiIntent {
    return if (isGood) ForumThreadListUiIntent.Refresh(forumName, -1, goodClassifyId ?: 0)
    else ForumThreadListUiIntent.Refresh(forumName, sortType, null)
}

private enum class ItemType {
    Top, PlainText, SingleMedia, MultiMedia, Video
}

@Composable
internal fun GoodClassifyTabs(
    goodClassifyHolders: ImmutableList<ImmutableHolder<Classify>>,
    selectedItem: Int?,
    onSelected: (Int) -> Unit,
) {
    if (goodClassifyHolders.isEmpty()) return

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = goodClassifyHolders,
            key = { it.get { "${class_id}_$class_name" } }
        ) { holder ->
            val (classify) = holder
            FilterChip(
                label = { Text(classify.class_name) },
                selected = selectedItem == classify.class_id,
                onClick = { if (selectedItem != classify.class_id) onSelected(classify.class_id) },
            )
        }
    }
}

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String = stringResource(id = R.string.content_top),
) {
    ListItem(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) { Text(type, style = MaterialTheme.typography.labelSmall) }
        },
        headlineContent = {
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}

@Composable
private fun ThreadList(
    state: LazyListState,
    items: ImmutableList<ThreadItemData>,
    filteredAll: Boolean,
    onLoadMore: (() -> Unit)?,
    onItemClicked: (ThreadInfo) -> Unit,
    onItemReplyClicked: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    forumRuleTitle: String? = null,
    onOpenForumRule: (() -> Unit)? = null,
    onOriginThreadClicked: (threadId: Long, forumId: Long) -> Unit,
    onUserClicked: (User) -> Unit = {},
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val itemFraction = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 0.5f
        else -> 1f
    }
    MyLazyColumn(
        state = state,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        if (!forumRuleTitle.isNullOrEmpty()) {
            item(key = "ForumRule") {
                TopThreadItem(
                    title = forumRuleTitle,
                    onClick = {
                        onOpenForumRule?.invoke()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    type = stringResource(id = R.string.desc_forum_rule)
                )
            }
        }
        if (filteredAll) item { FilteredPageNotice(onLoadMore) }
        itemsIndexed(
            items = items,
            key = { index, (holder) ->
                val (item) = holder
                "${index}_${item.id}"
            },
            contentType = { _, (holder) ->
                val (item) = holder
                if (item.isTop == 1) ItemType.Top
                else {
                    if (item.media.isNotEmpty())
                        if (item.media.size == 1) ItemType.SingleMedia else ItemType.MultiMedia
                    else if (item.videoInfo != null)
                        ItemType.Video
                    else ItemType.PlainText
                }
            }
        ) { index, (holder, blocked) ->
            BlockableContent(
                blocked = blocked,
                blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_thread)) }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                val (item) = holder
                Column(
                    modifier = Modifier.fillMaxWidth(itemFraction)
                ) {
                    if (item.isTop == 1) {
                        val title = item.title.takeUnless { it.isBlank() } ?: item.abstractText
                        TopThreadItem(
                            title = title,
                            onClick = { onItemClicked(item) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        if (index > 0 && items[index - 1].thread.get { isTop } == 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        FeedCard(
                            item = holder,
                            onClick = onItemClicked,
                            onClickReply = onItemReplyClicked,
                            onAgree = onAgree,
                            onClickOriginThread = onOriginThreadClicked,
                            onClickUser = onUserClicked
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumThreadListPage(
    forumId: Long,
    forumName: String,
    isGood: Boolean = false,
    sortType: Int,
    onRefreshingChanged: (Boolean) -> Unit,
    viewModel: ForumThreadListViewModel = if (isGood) pageViewModel<GoodThreadListViewModel>() else pageViewModel<LatestThreadListViewModel>()
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current

    val lazyListState = rememberLazyListState()

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(getFirstLoadIntent(forumName, isGood, sortType))
        viewModel.initialized = true
    }
    val goodClassifyId by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifyId,
        initial = null
    )
    onGlobalEvent<ForumThreadListUiEvent.Refresh>(
        filter = { it.isGood == isGood },
    ) {
        viewModel.send(getRefreshIntent(forumName, isGood, it.sortType, goodClassifyId))
    }
    onGlobalEvent<ForumThreadListUiEvent.BackToTop>(
        filter = { it.isGood == isGood },
    ) {
        lazyListState.animateScrollToItem(0)
    }
    viewModel.onEvent<ForumThreadListUiEvent.AgreeFail> {
        val snackbarResult = snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.snackbar_agree_fail,
                it.errorCode,
                it.errorMsg
            ),
            actionLabel = context.getString(R.string.button_retry)
        )

        if (snackbarResult == SnackbarResult.ActionPerformed) {
            viewModel.send(
                ForumThreadListUiIntent.Agree(
                    it.threadId,
                    it.postId,
                    it.hasAgree
                )
            )
        }
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isRefreshing,
        initial = false
    )
    LaunchedEffect(isRefreshing) { onRefreshingChanged(isRefreshing) }
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::currentPage,
        initial = 1
    )
    val forumRuleTitle by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::forumRuleTitle,
        initial = null
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadList,
        initial = persistentListOf()
    )
    val visible = rememberVisibleItems(threadList) { blocksThread(it.thread.get()) }
    val threadListIds by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threadListIds,
        initial = persistentListOf()
    )
    val goodClassifies by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::goodClassifies,
        initial = persistentListOf()
    )
    val loadNextPage = {
        viewModel.send(ForumThreadListUiIntent.LoadMore(
            forumId, forumName, currentPage, threadListIds,
            sortType = if (isGood) 0 else sortType,
            goodClassifyId = if (isGood) goodClassifyId ?: 0 else null,
        ))
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isGood) {
                GoodClassifyTabs(
                    goodClassifyHolders = goodClassifies,
                    selectedItem = goodClassifyId,
                    onSelected = {
                        viewModel.send(
                            getRefreshIntent(
                                forumName,
                                isGood = true,
                                sortType = sortType,
                                goodClassifyId = it
                            )
                        )
                    }
                )
            }

            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = loadNextPage,
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = visible.isEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                ThreadList(
                    state = lazyListState,
                    items = visible,
                    filteredAll = threadList.isNotEmpty() && visible.isEmpty(),
                    onLoadMore = if (hasMore && !isLoadingMore && !isRefreshing) loadNextPage else null,
                    onItemClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                threadInfo = it
                            )
                        )
                    },
                    onItemReplyClicked = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.threadId,
                                forumId = it.forumId,
                                scrollToReply = true
                            )
                        )
                    },
                    onAgree = {
                        viewModel.send(
                            ForumThreadListUiIntent.Agree(
                                it.threadId,
                                it.firstPostId,
                                it.agree?.hasAgree ?: 0
                            )
                        )
                    },
                    forumRuleTitle = forumRuleTitle,
                    onOpenForumRule = {
                        navigator.navigate(ForumRuleDetailPageDestination(forumId))
                    },
                    onOriginThreadClicked = { threadId, sourceForumId ->
                        navigator.navigate(ThreadPageDestination(threadId, forumId = sourceForumId))
                    }
                ) { navigator.navigate(UserProfilePageDestination(it.id)) }
            }
        }

    }
}

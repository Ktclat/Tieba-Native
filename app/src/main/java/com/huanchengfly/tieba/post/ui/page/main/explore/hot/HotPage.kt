@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HotTopicListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.itemsIndexed
import com.huanchengfly.tieba.post.ui.widgets.compose.loadingPlaceholder
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun HotPage(
    viewModel: HotViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(HotUiIntent.Load)
        viewModel.initialized = true
    }
    if (LocalShouldLoad.current) {
        onGlobalEvent<GlobalEvent.Refresh>(
            filter = { it.key == "hot" }
        ) {
            viewModel.send(HotUiIntent.Load)
        }
    }
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(HotUiState::isRefreshing)
    val topicList by viewModel.uiState.collectPartialAsState(HotUiState::topicList)
    val threadList by viewModel.uiState.collectPartialAsState(HotUiState::threadList)
    val tabList by viewModel.uiState.collectPartialAsState(HotUiState::tabList)
    val currentTabCode by viewModel.uiState.collectPartialAsState(HotUiState::currentTabCode)
    val isLoadingThreadList by viewModel.uiState.collectPartialAsState(HotUiState::isLoadingThreadList)
    PullToRefreshLayout(
        refreshing = isLoading,
        onRefresh = { viewModel.send(HotUiIntent.Load) },
    ) {
        MyLazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            if (topicList.isNotEmpty()) {
                item(key = "TopicHeader") {
                    Container {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hot_topic_rank),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
                item(key = "TopicList") {
                    Container {
                        VerticalGrid(
                            column = 2,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = topicList,
                            ) { index, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    RankNumber(index)
                                    Text(
                                        text = item.get { topicName },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    when (item.get { tag }) {
                                        2 -> Badge(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                        ) { Text(stringResource(R.string.topic_tag_hot)) }
                                        1 -> Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        ) { Text(stringResource(R.string.topic_tag_new)) }
                                    }
                                }
                            }
                            item {
                                TextButton(
                                    onClick = { navigator.navigate(HotTopicListPageDestination) },
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    Text(stringResource(R.string.tip_more_topic))
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ms_chevron_right),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "TopicDivider") {
                    Container {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 8.dp)
                                .padding(horizontal = 16.dp),
                            thickness = 1.dp
                        )
                    }
                }
            }
            if (threadList.isNotEmpty()) {
                if (tabList.isNotEmpty()) {
                    item(key = "ThreadTabs") {
                        Container {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = currentTabCode == "all",
                                    onClick = { viewModel.send(HotUiIntent.RefreshThreadList("all")) },
                                    label = { Text(stringResource(R.string.tab_all_hot_thread)) },
                                )
                                tabList.forEach { tab ->
                                    val code = tab.get { tabCode }
                                    FilterChip(
                                        selected = currentTabCode == code,
                                        onClick = { viewModel.send(HotUiIntent.RefreshThreadList(code)) },
                                        label = { Text(tab.get { tabName }) },
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "ThreadListTip") {
                    Container(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.hot_thread_rank_rule),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (isLoadingThreadList) {
                    items(10) { ThreadListItemPlaceholder() }
                } else {
                    itemsIndexed(
                        items = threadList,
                        key = { _, item -> "Thread_${item.get { threadId }}" }
                    ) { index, item ->
                        Container {
                            FeedCard(
                                item = item,
                                onClick = {
                                    navigator.navigate(
                                        ThreadPageDestination(
                                            threadId = it.id,
                                            threadInfo = it
                                        )
                                    )
                                },
                                onClickReply = {
                                    navigator.navigate(
                                        ThreadPageDestination(
                                            threadId = it.id,
                                            scrollToReply = true
                                        )
                                    )
                                },
                                onAgree = {
                                    viewModel.send(
                                        HotUiIntent.Agree(
                                            threadId = it.threadId,
                                            postId = it.firstPostId,
                                            hasAgree = it.hasAgree
                                        )
                                    )
                                },
                                onClickForum = { navigator.navigate(ForumPageDestination(it.name)) },
                                onClickUser = { navigator.navigate(UserProfilePageDestination(it.id)) },
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    RankNumber(index)
                                    Text(
                                        text = stringResource(
                                            id = R.string.hot_num,
                                            item.get { hotNum }.getShortNumString()
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadListItemPlaceholder() {
    Row(modifier = Modifier.padding(all = 16.dp)) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .wrapContentSize()
                    .loadingPlaceholder(visible = true)
                    .padding(vertical = 1.dp, horizontal = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .loadingPlaceholder(visible = true)
                )
                Text(
                    text = stringResource(id = R.string.hot_num, "666"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.loadingPlaceholder(visible = true)
                )
            }
        }
    }
}

@Composable
private fun RankNumber(index: Int) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when (index) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        2 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> Color.Transparent to colors.onSurfaceVariant
    }
    Badge(containerColor = background, contentColor = foreground) {
        Text((index + 1).toString(), style = MaterialTheme.typography.labelMedium)
    }
}

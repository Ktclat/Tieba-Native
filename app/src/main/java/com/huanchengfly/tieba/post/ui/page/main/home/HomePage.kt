package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.ui.widgets.compose.loadingPlaceholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import androidx.compose.material3.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import androidx.compose.material3.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import kotlinx.coroutines.launch

private val ListSingleKey = booleanPreferencesKey("listSingle")
private val ShowHistoryForumKey = booleanPreferencesKey("homePageShowHistoryForum")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SearchBox(modifier: Modifier = Modifier, onClick: () -> Unit) {
    SearchBar(
        state = rememberSearchBarState(),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        inputField = {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = SearchBarDefaults.InputFieldHeight)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Text(
                    stringResource(R.string.hint_search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false
) {
    Chip(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp)
            .then(modifier),
        invertColor = invert
    )
}

@Composable
private fun ForumItemPlaceholder(
    showAvatar: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (showAvatar) {
            Image(
                painter = rememberDrawablePainter(
                    drawable = ImageUtil.getPlaceHolder(
                        LocalContext.current,
                        0
                    )
                ),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .align(CenterVertically)
                    .loadingPlaceholder(visible = true),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            color = MaterialTheme.colorScheme.onSurface,
            text = "",
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically)
                .loadingPlaceholder(visible = true),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
                .loadingPlaceholder(visible = true)
        ) {
            Text(
                text = "0",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Center)
            )
        }
    }
}

@Composable
private fun ForumItemMenuContent(
    menuState: MenuState,
    isTopForum: Boolean,
    onDeleteTopForum: () -> Unit,
    onAddTopForum: () -> Unit,
    onCopyName: () -> Unit,
    onUnfollow: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(if (isTopForum) R.string.menu_top_del else R.string.menu_top)) },
        onClick = {
            if (isTopForum) onDeleteTopForum() else onAddTopForum()
            menuState.expanded = false
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.title_copy_forum_name)) },
        onClick = {
            onCopyName()
            menuState.expanded = false
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.button_unfollow)) },
        onClick = {
            onUnfollow()
            menuState.expanded = false
        },
    )
}

@Composable
private fun ForumItemContent(
    item: HomeUiState.Forum,
    showAvatar: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AnimatedVisibility(visible = showAvatar) {
            Row {
                Avatar(data = item.avatar, size = 40.dp, contentDescription = null)
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        Text(
            color = MaterialTheme.colorScheme.onSurface,
            text = item.forumName,
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
        ) {
            Row(
                modifier = Modifier.align(Center),
            ) {
                Text(
                    text = "Lv.${item.levelId}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(CenterVertically)
                )
                if (item.isSign) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(id = R.string.tip_signed),
                        modifier = Modifier
                            .size(12.dp)
                            .align(CenterVertically),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumItem(
    item: HomeUiState.Forum,
    showAvatar: Boolean,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onAddTopForum: (HomeUiState.Forum) -> Unit,
    onDeleteTopForum: (HomeUiState.Forum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            ForumItemMenuContent(
                menuState = menuState,
                isTopForum = isTopForum,
                onDeleteTopForum = { onDeleteTopForum(item) },
                onAddTopForum = { onAddTopForum(item) },
                onCopyName = {
                    TiebaUtil.copyText(context, item.forumName)
                },
                onUnfollow = { onUnfollow(item) }
            )
        },
        menuState = menuState,
        onClick = {
            onClick(item)
        }
    ) {
        ForumItemContent(item = item, showAvatar = showAvatar)
    }
}

@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel<HomeUiIntent, HomeViewModel>(listOf(HomeUiIntent.Refresh)),
    canOpenExplore: Boolean = false,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(HomeUiState::isLoading)
    val forums by viewModel.uiState.collectPartialAsState(HomeUiState::forums)
    val topForums by viewModel.uiState.collectPartialAsState(HomeUiState::topForums)
    val historyForums by viewModel.uiState.collectPartialAsState(HomeUiState::historyForums)
    val expandHistoryForum by viewModel.uiState.collectPartialAsState(HomeUiState::expandHistoryForum)
    val error by viewModel.uiState.collectPartialAsState(HomeUiState::error)
    val isLoggedIn = remember(account) { account != null }
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }
    val preferences = context.dataStore
    val scope = rememberCoroutineScope()
    val historyEnabled by preferences.collectPreferenceAsState(
        ShowHistoryForumKey, defaultValue = true, initialValue = false,
    )
    val showHistoryForum = historyEnabled && historyForums.isNotEmpty()
    val listSingle by preferences.collectPreferenceAsState(ListSingleKey, defaultValue = false)
    val isError by remember { derivedStateOf { error != null } }
    val gridCells = remember(listSingle) { if (listSingle) GridCells.Fixed(1) else GridCells.Adaptive(180.dp) }

    val active = LocalShouldLoad.current
    if (active) {
        onGlobalEvent<GlobalEvent.Refresh>(
            filter = { it.key == "home" }
        ) {
            viewModel.send(HomeUiIntent.Refresh)
        }
    }

    var unfollowForum by remember { mutableStateOf<HomeUiState.Forum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = {
            unfollowForum?.let {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            }
        },
    ) {
        Text(
            text = stringResource(
                id = R.string.title_dialog_unfollow_forum,
                unfollowForum?.forumName.orEmpty()
            )
        )
    }

    var refreshHistoryOnEntry by remember { mutableStateOf(viewModel.initialized) }
    LaunchedEffect(active) {
        if (active) {
            if (refreshHistoryOnEntry) viewModel.send(HomeUiIntent.RefreshHistory)
            refreshHistoryOnEntry = true
        }
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_main),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                        contentDescription = stringResource(id = R.string.title_oksign)
                    ) {
                        TiebaUtil.startSign(context)
                    }
                    ActionItem(
                        icon = Icons.Outlined.ViewAgenda,
                        contentDescription = stringResource(id = R.string.title_switch_list_single)
                    ) {
                        scope.launch {
                            preferences.edit { it[ListSingleKey] = !(it[ListSingleKey] ?: false) }
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { contentPaddings ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.send(HomeUiIntent.Refresh) },
            modifier = Modifier
                .padding(contentPaddings),
        ) {
            Column {
                SearchBox(modifier = Modifier.padding(bottom = 4.dp)) {
                    navigator.navigate(SearchPageDestination)
                }
                StateScreen(
                    isEmpty = isEmpty,
                    isError = isError,
                    isLoading = isLoading,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onReload = {
                        viewModel.send(HomeUiIntent.Refresh)
                    },
                    emptyScreen = {
                        EmptyScreen(
                            loggedIn = isLoggedIn,
                            canOpenExplore = canOpenExplore,
                            onOpenExplore = onOpenExplore
                        )
                    },
                    loadingScreen = {
                        HomePageSkeletonScreen(listSingle = listSingle, gridCells = gridCells)
                    },
                    errorScreen = {
                        error?.let { ErrorScreen(error = it) }
                    }
                ) {
                    MyLazyVerticalGrid(
                        columns = gridCells,
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (showHistoryForum) {
                            item(key = "HistoryForums", span = { GridItemSpan(maxLineSpan) }) {
                                val rotate by animateFloatAsState(
                                    targetValue = if (expandHistoryForum) 90f else 0f,
                                    label = "rotate"
                                )
                                Column {
                                    Row(
                                        verticalAlignment = CenterVertically,
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                viewModel.send(
                                                    HomeUiIntent.ToggleHistory(
                                                        expandHistoryForum
                                                    )
                                                )
                                            }
                                            .padding(vertical = 8.dp)
                                            .padding(end = 16.dp)
                                    ) {
                                        Header(
                                            text = stringResource(id = R.string.title_history_forum),
                                            invert = false
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = stringResource(id = R.string.desc_show),
                                            modifier = Modifier
                                                .size(24.dp)
                                                .rotate(rotate)
                                        )
                                    }
                                    AnimatedVisibility(visible = expandHistoryForum) {
                                        LazyRow(
                                            contentPadding = PaddingValues(bottom = 8.dp),
                                        ) {
                                            item(key = "Spacer1") {
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            items(
                                                historyForums,
                                                key = { it.data }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .padding(horizontal = 4.dp)
                                                        .height(IntrinsicSize.Min)
                                                        .clip(RoundedCornerShape(100))
                                                        .background(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                                                        .clickable {
                                                            navigator.navigate(
                                                                ForumPageDestination(
                                                                    it.data
                                                                )
                                                            )
                                                        }
                                                        .padding(4.dp),
                                                    verticalAlignment = CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Avatar(
                                                        data = it.avatar,
                                                        contentDescription = null,
                                                        size = 24.dp,
                                                        shape = CircleShape
                                                    )
                                                    Text(
                                                        text = it.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                }
                                            }
                                            item(key = "Spacer2") {
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (hasTopForum) {
                            item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Header(
                                        text = stringResource(id = R.string.title_top_forum),
                                        invert = true
                                    )
                                }
                            }
                            items(
                                items = topForums,
                                key = { "Top${it.forumId}" }
                            ) { item ->
                                ForumItem(
                                    item,
                                    listSingle,
                                    onClick = {
                                        navigator.navigate(ForumPageDestination(it.forumName))
                                    },
                                    onUnfollow = {
                                        unfollowForum = it
                                        confirmUnfollowDialog.show()
                                    },
                                    onAddTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Add(it))
                                    },
                                    onDeleteTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                    },
                                    isTopForum = true
                                )
                            }
                        }
                        if (showHistoryForum || hasTopForum) {
                            item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Header(text = stringResource(id = R.string.forum_list_title))
                                }
                            }
                        }
                        items(
                            items = forums,
                            key = { it.forumId }
                        ) { item ->
                            ForumItem(
                                item,
                                listSingle,
                                onClick = {
                                    navigator.navigate(ForumPageDestination(it.forumName))
                                },
                                onUnfollow = {
                                    unfollowForum = it
                                    confirmUnfollowDialog.show()
                                },
                                onAddTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Add(it))
                                },
                                onDeleteTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePageSkeletonScreen(
    listSingle: Boolean,
    gridCells: GridCells
) {
    MyLazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 12.dp),
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item(key = "TopForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Header(
                    text = stringResource(id = R.string.title_top_forum),
                    modifier = Modifier.loadingPlaceholder(visible = true),
                    invert = true
                )
            }
        }
        items(6, key = { "TopPlaceholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }
        item(
            key = "Spacer",
            span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(
                    16.dp
                )
            )
        }
        item(key = "ForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(
                    text = stringResource(id = R.string.forum_list_title),
                    modifier = Modifier.loadingPlaceholder(visible = true),
                    invert = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(12, key = { "Placeholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }
    }
}

@Composable
fun EmptyScreen(
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavigator.current
    TipScreen(
        title = {
            if (!loggedIn) {
                Text(text = stringResource(id = R.string.title_empty_login))
            } else {
                Text(text = stringResource(id = R.string.title_empty))
            }
        },
        image = {
            Box(
                modifier = Modifier.fillMaxWidth().height(156.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (!loggedIn) {
                Button(
                    onClick = {
                        navigator.navigate(LoginPageDestination)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                TextButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}
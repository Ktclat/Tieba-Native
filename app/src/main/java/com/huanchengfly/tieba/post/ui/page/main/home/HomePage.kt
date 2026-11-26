package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalShouldLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
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
    ListItem(
        headlineContent = {
            Text(item.forumName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = if (showAvatar) {
            { Avatar(data = item.avatar, size = 40.dp, contentDescription = null) }
        } else null,
        trailingContent = {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text("Lv.${item.levelId}")
                if (item.isSign) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.Check, stringResource(R.string.tip_signed), Modifier.size(12.dp))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val forums = state.forums
    val topForums = state.topForums
    val historyForums = state.historyForums
    val expandHistoryForum = state.expandHistoryForum
    val isLoading = state.isLoading
    val error = state.error
    val isLoggedIn = account != null
    val isEmpty = forums.isEmpty()
    val hasTopForum = topForums.isNotEmpty()
    val preferences = context.dataStore
    val scope = rememberCoroutineScope()
    val historyEnabled by preferences.collectPreferenceAsState(
        ShowHistoryForumKey, defaultValue = true, initialValue = false,
    )
    val showHistoryForum = historyEnabled && historyForums.isNotEmpty()
    val listSingle by preferences.collectPreferenceAsState(ListSingleKey, defaultValue = false)
    val isError = error != null
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
        PullToRefreshLayout(
            refreshing = isLoading && !isEmpty,
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
                                            .clickable(role = Role.Button) {
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
                                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            items(historyForums, key = { it.data }) { forum ->
                                                AssistChip(
                                                    onClick = { navigator.navigate(ForumPageDestination(forum.data)) },
                                                    label = { Text(forum.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                    leadingIcon = {
                                                        Avatar(data = forum.avatar, contentDescription = null, size = 20.dp)
                                                    },
                                                )
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
                                contentType = { "forum" },
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
                            contentType = { "forum" },
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
                        .clip(MaterialTheme.shapes.extraLarge)
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
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                TextButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.widgets.compose.loadingPlaceholder
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import androidx.compose.material3.PrimaryTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.GsonUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    var addBlockCategory by remember { mutableStateOf(Block.CATEGORY_BLACK_LIST) }
    val dialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.send(
                BlockListUiIntent.Add(
                    category = addBlockCategory,
                    keywords = it.split(" ")
                )
            )
        },
        dialogState = dialogState,
        title = {
            Text(
                text = if (addBlockCategory == Block.CATEGORY_WHITE_LIST) stringResource(id = R.string.title_add_white)
                else stringResource(id = R.string.title_add_black)
            )
        }
    ) {
        Text(text = stringResource(id = R.string.tip_add_block))
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState { 2 }
    val blackList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::blackList,
        initial = emptyList()
    )
    val whiteList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::whiteList,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::isLoading,
        initial = false
    )
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_list),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
                content = {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        divider = {},
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(84.dp * 2),
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { pagerState.requestScrollToPage(0) },
                            text = { androidx.compose.material3.Text(text = stringResource(id = R.string.title_black_list)) },
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { pagerState.requestScrollToPage(1) },
                            text = { androidx.compose.material3.Text(text = stringResource(id = R.string.title_white_list)) },
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (category in listOf(Block.CATEGORY_BLACK_LIST, Block.CATEGORY_WHITE_LIST)) {
                    val black = category == Block.CATEGORY_BLACK_LIST
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            addBlockCategory = category
                            dialogState.show()
                        },
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(if (black) Icons.Outlined.Block else Icons.Outlined.CheckCircle, null,
                            modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(if (black) R.string.title_add_black else R.string.title_add_white))
                    }
                }
            }
        }
    ) { paddingValues ->
        val snackbarHostState = LocalSnackbarHostState.current
        viewModel.onEvent<BlockListUiEvent.Success> {
            snackbarHostState.showSnackbar(
                when (it) {
                    is BlockListUiEvent.Success.Add -> context.getString(R.string.toast_add_success)
                    is BlockListUiEvent.Success.Delete -> context.getString(R.string.toast_delete_success)
                }
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            contentPadding = paddingValues,
            verticalAlignment = Alignment.Top
        ) { position ->
            val items by remember {
                derivedStateOf {
                    if (position == 0) blackList else whiteList
                }
            }
            StateScreen(
                isEmpty = items.isEmpty(),
                isError = false,
                isLoading = isLoading,
                loadingScreen = {
                    MyLazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(4) {
                            BlockItemPlaceholder()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                MyLazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) {
                        LongClickMenu(menuContent = {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_delete)) },
                                onClick = { viewModel.send(BlockListUiIntent.Delete(it.id)) },
                            )
                        }) {
                            BlockItem(item = it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockItemPlaceholder() {
    ListItem(
        leadingContent = { Icon(Icons.Outlined.Block, null, Modifier.loadingPlaceholder(visible = true)) },
        headlineContent = { Text(stringResource(R.string.title_block_settings), Modifier.loadingPlaceholder(visible = true)) },
    )
}

@Composable
private fun BlockItem(
    item: Block,
) {
    val isUser = item.type == Block.TYPE_USER
    val title = if (isUser) item.username.orEmpty() else remember(item.keywords) {
        runCatching {
            GsonUtil.getGson().fromJson<List<String>>(item.keywords ?: "[]", object : TypeToken<List<String>>() {}.type)
                .orEmpty().joinToString(" ")
        }.getOrDefault("")
    }
    ListItem(
        leadingContent = {
            Icon(
                imageVector = if (isUser) Icons.Outlined.AccountCircle else ImageVector.vectorResource(R.drawable.ic_comment_new),
                contentDescription = stringResource(if (isUser) R.string.block_type_user else R.string.block_type_keywords),
            )
        },
        headlineContent = { Text(title) },
        supportingContent = if (isUser) {{ Text("UID: ${item.uid}") }} else null,
    )
}

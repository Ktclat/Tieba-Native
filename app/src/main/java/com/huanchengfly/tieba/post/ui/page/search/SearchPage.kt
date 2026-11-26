@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList

internal enum class SearchTab(val title: Int) {
    Forum(R.string.title_search_forum),
    Thread(R.string.title_search_thread),
    User(R.string.title_search_user),
}

private val threadSortLabels = mapOf(
    SearchThreadSortType.SORT_TYPE_NEWEST to R.string.title_search_order_new,
    SearchThreadSortType.SORT_TYPE_OLDEST to R.string.title_search_order_old,
    SearchThreadSortType.SORT_TYPE_RELATIVE to R.string.title_search_order_relevant,
)

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://search")
    ]
)
@Composable
fun SearchPage(
    navigator: DestinationsNavigator,
    viewModel: SearchViewModel = pageViewModel<SearchUiIntent, SearchViewModel>(
        listOf(SearchUiIntent.Init)
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var keyword by rememberSaveable { mutableStateOf("") }
    var input by rememberSaveable { mutableStateOf("") }
    var showingResults by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun edit(value: String) {
        input = value
        showingResults = false
        viewModel.send(SearchUiIntent.KeywordInputChanged(value))
    }

    fun submit(value: String) {
        keyword = value.trim()
        input = keyword
        showingResults = keyword.isNotEmpty()
        focusManager.clearFocus()
        viewModel.send(SearchUiIntent.SubmitKeyword(keyword))
    }

    fun goBack() {
        when {
            showingResults -> submit("")
            keyword.isNotEmpty() -> {
                input = keyword
                showingResults = true
                focusManager.clearFocus()
                viewModel.send(SearchUiIntent.KeywordInputChanged(""))
            }
            input.isNotEmpty() -> edit("")
            else -> navigator.navigateUp()
        }
    }

    // Restoring an unfinished draft should also restore its suggestions.
    LaunchedEffect(Unit) {
        if (input.isNotBlank() && !showingResults) {
            viewModel.send(SearchUiIntent.KeywordInputChanged(input))
        }
    }
    MyBackHandler(
        enabled = keyword.isNotEmpty() || input.isNotEmpty(),
        currentScreen = SearchPageDestination,
        onBack = ::goBack,
    )

    var sortType by rememberSaveable { mutableIntStateOf(SearchThreadSortType.SORT_TYPE_NEWEST) }
    val pagerState = rememberPagerState { SearchTab.entries.size }
    MyScaffold(
        topBar = {
            TopAppBarContainer(
                topBar = {
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SearchTopBar(
                            keyword = input,
                            onKeywordChange = ::edit,
                            onKeywordSubmit = ::submit,
                            onBack = ::goBack,
                            focusRequester = focusRequester,
                        )
                    }
                },
            ) {
                if (showingResults) {
                    SearchTabs(
                        current = SearchTab.entries[pagerState.currentPage],
                        sortType = sortType,
                        onTabSelected = { pagerState.requestScrollToPage(it.ordinal) },
                        onSortSelected = { sortType = it },
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (showingResults) {
                ProvideNavigator(navigator = navigator) {
                    LazyLoadHorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (SearchTab.entries[page]) {
                            SearchTab.Forum -> SearchForumPage(keyword)
                            SearchTab.Thread -> SearchThreadPage(keyword, sortType)
                            SearchTab.User -> SearchUserPage(keyword)
                        }
                    }
                }
            } else if (input.isNotBlank() && state.suggestionKeyword == input.trim() && state.suggestions.isNotEmpty()) {
                SearchSuggestionList(
                    suggestions = state.suggestions,
                    onItemClick = ::submit,
                    onFill = {
                        edit(it)
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                )
            } else {
                Container {
                    SearchHistorySection(
                        entries = state.searchHistories,
                        term = { it.content },
                        clearMessage = stringResource(R.string.search_history_clear_global_message),
                        onSearch = { submit(it.content) },
                        onDelete = { viewModel.send(SearchUiIntent.DeleteSearchHistory(it.id)) },
                        onClear = { viewModel.send(SearchUiIntent.ClearSearchHistory) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SearchSuggestionList(
    suggestions: ImmutableList<String>,
    onItemClick: (String) -> Unit,
    onFill: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxWidth()) {
        items(suggestions, key = { it }) { term ->
            ListItem(
                modifier = Modifier.clickable(role = Role.Button) { onItemClick(term) },
                headlineContent = { Text(term) },
                leadingContent = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onFill(term) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.search_fill_suggestion, term))
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTabs(
    current: SearchTab,
    sortType: Int,
    onTabSelected: (SearchTab) -> Unit,
    onSortSelected: (Int) -> Unit,
) {
    val sortDescription = stringResource(R.string.search_current_sort, stringResource(threadSortLabels.getValue(sortType)))
    PrimaryTabRow(selectedTabIndex = current.ordinal) {
        SearchTab.entries.forEach { tab ->
            val selected = current == tab
            val canSort = selected && tab == SearchTab.Thread
            var expanded by remember { mutableStateOf(false) }
            Box {
                Tab(
                    selected = selected,
                    modifier = if (canSort) Modifier.semantics { stateDescription = sortDescription } else Modifier,
                    onClick = { if (canSort) expanded = true else onTabSelected(tab) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(tab.title))
                            if (canSort) Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp))
                        }
                    },
                )
                if (canSort) {
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        threadSortLabels.forEach { (type, label) ->
                            DropdownMenuItem(
                                modifier = Modifier.semantics { this.selected = type == sortType },
                                text = { Text(stringResource(label)) },
                                trailingIcon = {
                                    if (type == sortType) Icon(Icons.Rounded.Check, null)
                                },
                                onClick = {
                                    expanded = false
                                    onSortSelected(type)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchTopBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {},
    hint: String = stringResource(R.string.hint_search),
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    SearchBox(
        keyword = keyword,
        onKeywordChange = onKeywordChange,
        modifier = Modifier.fillMaxSize(),
        onKeywordSubmit = onKeywordSubmit,
        focusRequester = focusRequester,
        placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        prependIcon = {
            IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.button_back))
            }
        },
    )
}

@Preview("SearchBox")
@Composable
fun PreviewSearchBox() {
    var keyword by remember { mutableStateOf("") }
    TiebaLiteTheme {
        Box(
            modifier = Modifier
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            SearchTopBar(
                keyword = keyword,
                onKeywordChange = { keyword = it }
            )
        }
    }
}

@Preview("Search history")
@Composable
private fun PreviewSearchHistory() {
    TiebaLiteTheme {
        SearchHistorySection(
            entries = (1..7).map { "搜索记录$it" },
            term = { it },
            clearMessage = stringResource(R.string.search_history_clear_global_message),
            onSearch = {},
            onDelete = {},
            onClear = {},
        )
    }
}

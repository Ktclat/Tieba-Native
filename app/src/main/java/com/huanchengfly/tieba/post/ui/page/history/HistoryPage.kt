package com.huanchengfly.tieba.post.ui.page.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination(deepLinks = [DeepLink(uriPattern = "tblite://history")])
@Composable
fun HistoryPage(navigator: DestinationsNavigator) {
    val pages = listOf(R.string.title_history_thread, R.string.title_history_forum)
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val refreshVersion by HistoryUtil.clearVersion.collectAsStateWithLifecycle()

    fun back() {
        if (searching) {
            searching = false
            query = ""
        } else navigator.navigateUp()
    }
    BackHandler(enabled = searching, onBack = ::back)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_history)) },
                    navigationIcon = {
                        IconButton(onClick = ::back) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.button_back))
                        }
                    },
                    actions = {
                        if (!searching) {
                            IconButton(onClick = { searching = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.history_search))
                            }
                        }
                        IconButton(onClick = { confirmClear = true }, enabled = !clearing) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.title_history_delete))
                        }
                    },
                )
                if (searching) {
                    val requester = remember { FocusRequester() }
                    val focus = LocalFocusManager.current
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).focusRequester(requester),
                        label = { Text(stringResource(R.string.history_search)) },
                        placeholder = {
                            val hint = if (pager.currentPage == 0) R.string.history_thread_hint else R.string.history_forum_hint
                            Text(stringResource(hint))
                        },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Close, stringResource(R.string.history_clear_query))
                                }
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    )
                    LaunchedEffect(Unit) { requester.requestFocus() }
                }
                TabRow(selectedTabIndex = pager.currentPage) {
                    pages.forEachIndexed { index, title ->
                        Tab(
                            selected = pager.currentPage == index,
                            onClick = { scope.launch { pager.animateScrollToPage(index) } },
                            text = { Text(stringResource(title)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        ProvideNavigator(navigator) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
                verticalAlignment = Alignment.Top,
            ) { page ->
                HistoryListPage(
                    type = if (page == 0) HistoryUtil.TYPE_THREAD else HistoryUtil.TYPE_FORUM,
                    query = query.trim(),
                    refreshVersion = refreshVersion,
                    onClearQuery = { query = "" },
                    onMessage = { scope.launch { snackbar.showSnackbar(it) } },
                )
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.title_history_delete)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.button_cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    if (!clearing) {
                        clearing = true
                        scope.launch {
                            val message = try {
                                withContext(Dispatchers.IO) { HistoryUtil.deleteAll() }
                                context.getString(R.string.toast_clear_success)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                context.getString(R.string.toast_clear_failure, error.localizedMessage.orEmpty())
                            } finally {
                                clearing = false
                            }
                            snackbar.showSnackbar(message)
                        }
                    }
                }) { Text(stringResource(R.string.button_clear)) }
            },
        )
    }
}

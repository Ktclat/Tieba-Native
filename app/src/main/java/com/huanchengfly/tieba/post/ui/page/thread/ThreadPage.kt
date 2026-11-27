@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TonalToggleButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.displayPortrait
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.ProvideCommentLinkColor
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.openReply
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.QuotedThread
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StateIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.TextWithMinWidth
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.max
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Composable
fun PostAgreeBtn(
    hasAgreed: Boolean,
    agreeNum: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (hasAgreed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "postAgreeBtnColor"
    )
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 112.dp)
            .semantics { selected = hasAgreed },
        shapes = ButtonDefaults.shapes(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(if (hasAgreed) R.drawable.ms_favorite_filled else R.drawable.ms_favorite),
                contentDescription = stringResource(id = R.string.title_agree),
                tint = animatedColor,
                modifier = Modifier.size(16.dp)
            )
            if (agreeNum > 0) {
                Text(
                    text = agreeNum.getShortNumString(),
                    color = animatedColor,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThreadMenuToggle(
    text: @Composable () -> Unit,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    FilterChip(
        selected = checked,
        onClick = onClick,
        label = text,
        leadingIcon = icon,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = null,
    )
}

object ThreadPageFrom {
    const val FROM_FORUM = "forum"

    // 收藏
    const val FROM_STORE = "store_thread"
    const val FROM_PERSONALIZED = "personalized"
    const val FROM_HISTORY = "history"
}

@Serializable
sealed interface ThreadPageExtra

@Serializable
data object ThreadPageNoExtra : ThreadPageExtra

@Serializable
data class ThreadPageFromStoreExtra(
    val maxPid: Long,
    val maxFloor: Int,
) : ThreadPageExtra

@Composable
private fun ThreadLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        LoadingIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://thread/{threadId}"),
    ]
)
@Composable
fun ThreadPage(
    threadId: Long,
    navigator: DestinationsNavigator,
    forumId: Long? = null,
    postId: Long = 0,
    seeLz: Boolean = false,
    sortType: Int = 0,
    from: String = "",
    extra: ThreadPageExtra? = null,
    threadInfo: ThreadInfo? = null,
    scrollToReply: Boolean = false,
    viewModel: ThreadViewModel = pageViewModel(),
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            ThreadUiIntent.Init(
                threadId,
                forumId,
                postId,
                threadInfo,
                seeLz,
                sortType
            )
        )
        viewModel.send(
            ThreadUiIntent.Load(
                threadId,
                page = 0,
                postId = postId,
                forumId = forumId,
                seeLz = seeLz,
                sortType = sortType,
                from = from
            )
        )
        viewModel.initialized = true
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::author,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::threadInfo,
        initial = null
    )
    val firstPost by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPost,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::forum,
        initial = null
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::user,
        initial = wrapImmutable(User())
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::anti,
        initial = null
    )
    val firstPostContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPostContentRenders,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isLoadingMore,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isError,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::error,
        initial = null
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasMore,
        initial = true
    )
    val nextPagePostId by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::nextPagePostId,
        initial = 0L
    )
    val hasPrevious by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasPrevious,
        initial = true
    )
    val currentPageMax by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::currentPageMax,
        initial = 0
    )
    val totalPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::totalPage,
        initial = 0
    )
    val isSeeLz by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::seeLz,
        initial = seeLz
    )
    val curSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::sortType,
        initial = sortType
    )
    val isImmersiveMode by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isImmersiveMode,
        initial = false
    )
    val latestPosts by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::latestPosts,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { data.isEmpty() && firstPost == null }
    }
    val enablePullRefresh by remember {
        derivedStateOf {
            hasPrevious || curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMoreEnd by remember {
        derivedStateOf {
            !hasMore && curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMorePreloadCount by remember {
        derivedStateOf {
            if (hasMore) {
                1
            } else {
                0
            }
        }
    }
    val isCollected = remember(thread) {
        thread?.get { collectStatus != 0 } == true
    }
    val hasThreadAgreed = remember(thread) {
        thread?.get { agree?.hasAgree == 1 } == true
    }
    val threadAgreeNum = remember(thread) {
        thread?.get { agree?.diffAgreeNum } ?: 0L
    }
    val threadTitle = remember(thread) {
        thread?.get { title } ?: ""
    }
    val curForumId = remember(forumId, forum) {
        forum?.get { id }?.takeIf { it > 0 } ?: forumId?.takeIf { it > 0 }
    }
    val curForumName = remember(forum) { forum?.get { name } }
    val curTbs = remember(anti) { anti?.get { tbs } }
    val currentAccount = LocalAccount.current
    fun openReply(args: ReplyArgs) = navigator.openReply(
        args.copy(tbs = curTbs.takeIf { user.get { id }.toString() == currentAccount?.uid }),
        currentAccount?.uid,
    )
    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = key(threadId) { rememberLazyListState() }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val firstFloor = remember(firstPost) {
        firstPost?.get { ThreadFloorKey(id, floor) }?.takeIf { it.postId > 0L }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    fun closeBottomSheet() {
        coroutineScope.launch { bottomSheetState.hide() }.invokeOnCompletion {
            if (!bottomSheetState.isVisible) menuOpen = false
        }
    }

    viewModel.onEvent<ThreadUiEvent.ScrollToFirstReply> {
        lazyListState.scrollToDiscussionItem(1)
    }
    viewModel.onEvent<ThreadUiEvent.ScrollToLatestReply> {
        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            lazyListState.scrollToDiscussionItem(2 + data.size)
        } else {
            lazyListState.scrollToDiscussionItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.LoadSuccess> {
        if (it.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
            waitLoadSuccessAndScrollToFirstReply = false
            lazyListState.scrollToDiscussionItem(1)
        }
    }
    viewModel.onEvent<ThreadUiEvent.AddFavoriteSuccess> {
        snackbarHostState.showSnackbar(
            context.getString(R.string.message_add_favorite_success, it.floor)
        )
    }
    viewModel.onEvent<ThreadUiEvent.RemoveFavoriteSuccess> {
        snackbarHostState.showSnackbar(
            context.getString(R.string.message_remove_favorite_success)
        )
    }

    var markBeforeBack by rememberSaveable(threadId) { mutableStateOf<ThreadFloorKey?>(null) }
    fun leaveThread() {
        val position = if (isCollected) lazyListState.lastVisibleFloor(firstFloor) else null
        if (position == null) navigator.navigateUp() else markBeforeBack = position
    }
    MyBackHandler(
        enabled = isCollected && !menuOpen,
        currentScreen = ThreadPageDestination,
        onBack = ::leaveThread,
    )
    markBeforeBack?.let { position ->
        ThreadReadingMarkDialog(
            position = position,
            saveMark = { postIdToMark ->
                val response = TiebaApi.getInstance().addStoreFlow(threadId, postIdToMark).first()
                if (response.errorCode != 0) throw TiebaApiException(response)
                context.toastShort(R.string.message_update_collect_mark_success)
            },
            onDismiss = { markBeforeBack = null },
            onLeave = {
                markBeforeBack = null
                navigator.navigateUp()
            },
        )
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deletePost by remember { mutableStateOf<ImmutableHolder<Post>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            curForumId ?: return@ConfirmDialog
            if (deletePost == null) {
                val isSelfThread = author?.get { id } == user.get { id }
                viewModel.send(
                    ThreadUiIntent.DeleteThread(
                        forumId = curForumId,
                        forumName = curForumName.orEmpty(),
                        threadId = threadId,
                        deleteMyThread = isSelfThread,
                        tbs = curTbs
                    )
                )
            } else {
                val isSelfPost = deletePost!!.get { author_id } == user.get { id }
                viewModel.send(
                    ThreadUiIntent.DeletePost(
                        forumId = curForumId,
                        forumName = curForumName.orEmpty(),
                        threadId = threadId,
                        postId = deletePost!!.get { id },
                        deleteMyPost = isSelfPost,
                        tbs = curTbs
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deletePost == null) stringResource(id = R.string.this_thread)
                else stringResource(id = R.string.tip_post_floor, deletePost!!.get { floor })
            )
        )
    }

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.send(
                ThreadUiIntent.Load(
                    threadId = threadId,
                    forumId = forum?.get { id } ?: forumId,
                    page = it.toInt(),
                    seeLz = isSeeLz,
                    sortType = curSortType
                )
            )
        },
        dialogState = jumpToPageDialogState,
        onValueChange = { newVal, _ -> "^[0-9]*$".toRegex().matches(newVal) },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            Text(
                text = stringResource(
                    id = R.string.tip_jump_page,
                    currentPageMax,
                    totalPage
                )
            )
        }
    )

    LaunchedEffect(Unit) {
        if (from == ThreadPageFrom.FROM_STORE && extra is ThreadPageFromStoreExtra && extra.maxPid != postId) {
            val result = snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = extra.maxPid,
                        forumId = forumId,
                        seeLz = seeLz,
                        sortType = sortType
                    )
                )
            }
        }
    }

    if (threadTitle.isNotBlank()) {
        TrackThreadReadingPosition(threadId, lazyListState, firstFloor, onlyAuthor = isSeeLz) { position ->
            HistoryUtil.saveHistory(
                History(
                    title = threadTitle,
                    data = threadId.toString(),
                    type = HistoryUtil.TYPE_THREAD,
                    extras = ThreadHistoryInfoBean(
                        isSeeLz = isSeeLz,
                        pid = position.postId.toString(),
                        forumName = curForumName,
                        floor = position.floor.toString(),
                    ).toJson(),
                    avatar = StringUtil.getAvatarUrl(author?.get { commentAvatarPortrait }),
                    username = author?.get { commentDisplayName(context) },
                ),
            )
        }
    }

    val refreshPosts = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    forumId,
                    isSeeLz,
                    curSortType
                )
            )
        }
    val pullRefreshState = rememberPullToRefreshState()

    @Composable
    fun PostCard(
        item: ImmutableHolder<Post>,
        contentRenders: ImmutableList<PbContentRender>,
        subPosts: ImmutableList<SubPostItemData>,
        blocked: Boolean,
    ) {
        PostCard(
            postHolder = item,
            contentRenders = contentRenders,
            subPosts = subPosts,
            threadAuthorId = author?.get { id } ?: 0L,
            blocked = blocked,
            canDelete = { it.author_id == user.get { id } },
            immersiveMode = isImmersiveMode,
            isCollected = { it.id == thread?.get { collectMarkPid.toLongOrNull() } },
            onUserClick = {
                navigator.navigate(UserProfilePageDestination(it.id))
            },
            onAgree = {
                val postHasAgreed =
                    item.get { agree?.hasAgree == 1 }
                viewModel.send(
                    ThreadUiIntent.AgreePost(
                        threadId = threadId,
                        postId = item.get { id },
                        agree = !postHasAgreed
                    )
                )
            },
            onSubPostAgree = { reply ->
                viewModel.send(
                    ThreadUiIntent.AgreePost(
                        threadId = threadId,
                        postId = item.get { id },
                        agree = reply.agree?.hasAgree != 1,
                        subPostId = reply.id,
                    )
                )
            },
            onReplyClick = {
                openReply(
                    ReplyArgs(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = it.id,
                        replyUserId = it.author?.id?.takeIf { id -> id > 0L } ?: it.author_id.takeIf { id -> id > 0L },
                        replyUserName = it.author?.commentDisplayName(context),
                        replyUserPortrait = it.author?.commentMentionPortrait,
                    )
                )
            },
            onSubPostReplyClick = { post, subPost ->
                openReply(
                    ReplyArgs(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = post.id,
                        subPostId = subPost.id,
                        replyUserId = subPost.author?.id?.takeIf { id -> id > 0L } ?: subPost.author_id.takeIf { id -> id > 0L },
                        replyUserName = subPost.author?.commentDisplayName(context),
                        replyUserPortrait = subPost.author?.commentMentionPortrait,
                    )
                )
            },
            onOpenSubPosts = {
                if (curForumId != null) {
                    navigator.navigate(
                        SubPostsPageDestination(
                            forumId = curForumId,
                            threadId = threadId,
                            postId = item.get { id },
                            subPostId = it,
                            loadFromSubPost = it > 0L
                        )
                    )
                }
            },
            onMenuCopyClick = {
                navigator.navigate(
                    CopyTextDialogPageDestination(it)
                )
            },
            onMenuFavoriteClick = {
                val isPostCollected =
                    it.id == thread?.get { collectMarkPid.toLongOrNull() }
                val fid = forum?.get { id } ?: forumId
                val tbs = anti?.get { tbs }
                if (fid != null) {
                    if (isPostCollected) {
                        viewModel.send(
                            ThreadUiIntent.RemoveFavorite(
                                threadId = threadId,
                                forumId = fid,
                                tbs = tbs
                            )
                        )
                    } else {
                        viewModel.send(
                            ThreadUiIntent.AddFavorite(
                                threadId = threadId,
                                postId = it.id,
                                floor = it.floor
                            )
                        )
                    }
                }
            },
            onMenuDeleteClick = {
                deletePost = it.wrapImmutable()
                confirmDeleteDialogState.show()
            },
        )
    }

    fun LazyListScope.replyFloors(posts: ImmutableList<PostItemData>, latest: Boolean = false) {
        itemsIndexed(
            items = posts,
            key = { _, item -> item.post.get { ThreadFloorKey(id, floor, latest) } },
            contentType = { _, _ -> "reply-floor" },
        ) { index, (post, blocked, renders, replies) ->
            Container {
                Column {
                    FloorDivider(posts.getOrNull(index - 1)?.post?.get(), post.get())
                    PostCard(post, renders, replies, blocked)
                }
            }
        }
    }

    fun LazyListScope.latestPosts(desc: Boolean) {
        if (latestPosts.isNotEmpty()) {
            if (!desc) {
                item("LatestPostsTip") {
                    Container {
                        Text(
                            text = stringResource(id = R.string.below_is_latest_post),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            replyFloors(latestPosts, latest = true)
            if (desc) {
                item("LatestPostsTip") {
                    Container {
                        Text(
                            text = stringResource(id = R.string.above_is_latest_post),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = isEmpty,
            isError = isError,
            isLoading = isRefreshing,
            errorScreen = {
                error?.let {
                    val (e) = it
                    ErrorScreen(error = e)
                }
            },
            onReload = {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = postId,
                        forumId = forumId,
                        seeLz = seeLz,
                        sortType = sortType
                    )
                )
            }
        ) {
            MyScaffold(
                snackbarHostState = snackbarHostState,
                topBar = {
                    ThreadTopBar(
                        forum = forum,
                        onBack = ::leaveThread,
                        onForumClick = {
                            val forumName = forum?.get { name }
                            if (forumName != null) navigator.navigate(
                                ForumPageDestination(
                                    forumName
                                )
                            )
                        }
                    )
                },
                bottomBar = {
                    ThreadBottomBar(
                        user = user,
                        signedIn = currentAccount != null,
                        onClickReply = {
                            openReply(
                                ReplyArgs(
                                    forumId = curForumId ?: 0,
                                    forumName = forum?.get { name }.orEmpty(),
                                    threadId = threadId,
                                )
                            )
                        },
                        onAgree = {
                            val firstPostId =
                                thread?.get { firstPostId }.takeIf { it != 0L }
                                    ?: firstPost?.get { id }
                                    ?: 0L
                            if (firstPostId != 0L) viewModel.send(
                                ThreadUiIntent.AgreeThread(
                                    threadId,
                                    firstPostId,
                                    !hasThreadAgreed
                                )
                            )
                        },
                        onClickMore = { menuOpen = true },
                        hasAgreed = hasThreadAgreed,
                        agreeNum = threadAgreeNum,
                    )
                },
            ) { paddingValues ->
                if (menuOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { menuOpen = false },
                        sheetState = bottomSheetState,
                    ) {
                        ThreadMenu(
                            isSeeLz = isSeeLz,
                            isCollected = isCollected,
                            isImmersiveMode = isImmersiveMode,
                            isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                            canDelete = { author?.get { id } == user.get { id } },
                            onSeeLzClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        forumId,
                                        !isSeeLz,
                                        curSortType
                                    )
                                )
                                closeBottomSheet()
                            },
                            onCollectClick = {
                                if (isCollected) {
                                    val fid = forum?.get { id } ?: forumId
                                    val tbs = anti?.get { tbs }
                                    if (fid != null) {
                                        viewModel.send(
                                            ThreadUiIntent.RemoveFavorite(
                                                threadId,
                                                fid,
                                                tbs
                                            )
                                        )
                                    }
                                } else {
                                    val readItem = lazyListState.lastVisibleFloor(firstFloor)
                                    if (readItem != null) {
                                        viewModel.send(
                                            ThreadUiIntent.AddFavorite(
                                                threadId,
                                                readItem.postId,
                                                readItem.floor
                                            )
                                        )
                                    }
                                }
                                closeBottomSheet()
                            },
                            onImmersiveModeClick = {
                                if (!isImmersiveMode && !isSeeLz) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadFirstPage(
                                            threadId,
                                            forumId,
                                            true,
                                            curSortType
                                        )
                                    )
                                }
                                viewModel.send(ThreadUiIntent.ToggleImmersiveMode(!isImmersiveMode))
                                closeBottomSheet()
                            },
                            onDescClick = {
                                viewModel.send(
                                    ThreadUiIntent.LoadFirstPage(
                                        threadId,
                                        forumId,
                                        isSeeLz,
                                        if (curSortType != ThreadSortType.SORT_TYPE_DESC) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT
                                    )
                                )
                                closeBottomSheet()
                            },
                            onJumpPageClick = {
                                closeBottomSheet()
                                jumpToPageDialogState.show()
                            },
                            onShareClick = {
                                TiebaUtil.shareText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId",
                                    threadTitle
                                )
                            },
                            onCopyLinkClick = {
                                TiebaUtil.copyText(
                                    context,
                                    "https://tieba.baidu.com/p/$threadId?see_lz=${isSeeLz.booleanToString()}"
                                )
                            },
                            onReportClick = {
                                val firstPostId =
                                    thread?.get { firstPostId }.takeIf { it != 0L }
                                        ?: firstPost?.get { id }
                                        ?: 0L
                                coroutineScope.launch {
                                    TiebaUtil.reportPost(
                                        context,
                                        navigator,
                                        firstPostId.toString()
                                    )
                                }
                            },
                            onDeleteClick = {
                                deletePost = null
                                confirmDeleteDialogState.show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                        .pullToRefresh(
                            isRefreshing = isRefreshing, state = pullRefreshState,
                            enabled = enablePullRefresh, onRefresh = refreshPosts,
                        )
                ) {
                    LoadMoreLayout(
                        isLoading = isLoadingMore,
                        onLoadMore = {
                            if (hasMore) {
                                viewModel.send(
                                    ThreadUiIntent.LoadMore(
                                        threadId = threadId,
                                        page = if (curSortType == ThreadSortType.SORT_TYPE_DESC) totalPage - currentPageMax
                                        else currentPageMax + 1,
                                        forumId = forumId,
                                        postId = nextPagePostId,
                                        seeLz = isSeeLz,
                                        sortType = curSortType,
                                        postIds = data.map { it.post.get { id } }
                                    )
                                )
                            } else if (data.isNotEmpty() && curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                viewModel.send(
                                    ThreadUiIntent.LoadLatestPosts(
                                        threadId = threadId,
                                        curLatestPostId = data.last().post.get { id },
                                        forumId = curForumId,
                                        seeLz = isSeeLz,
                                        sortType = curSortType
                                    )
                                )
                            }
                        },
                        loadEnd = loadMoreEnd,
                        indicator = { isLoading, loadMoreEnd, willLoad ->
                            ThreadLoadMoreIndicator(
                                isLoading,
                                loadMoreEnd,
                                willLoad,
                                hasMore
                            )
                        },
                        lazyListState = lazyListState,
                        isEmpty = data.isEmpty(),
                        preloadCount = loadMorePreloadCount,
                    ) {
                        MyLazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item(key = FirstPostKey, contentType = "thread-topic") {
                                if (firstPost != null) {
                                    Container {
                                        TopicSurface {
                                            PostCard(
                                                postHolder = firstPost!!,
                                                contentRenders = firstPostContentRenders,
                                                discussionRole = DiscussionRole.Topic,
                                                threadTitle = threadTitle,
                                                threadAuthorId = author?.get { id } ?: 0L,
                                                canDelete = { it.author_id == user.get { id } },
                                                immersiveMode = isImmersiveMode,
                                                isCollected = {
                                                    it.id == thread?.get { collectMarkPid }
                                                        ?.toLongOrNull()
                                                },
                                                showSubPosts = false,
                                                onUserClick = {
                                                    navigator.navigate(
                                                        UserProfilePageDestination(
                                                            it.id
                                                        )
                                                    )
                                                },
                                                onReplyClick = {
                                                    openReply(
                                                        ReplyArgs(
                                                            forumId = curForumId ?: 0,
                                                            forumName = forum?.get { name }
                                                                .orEmpty(),
                                                            threadId = threadId,
                                                        )
                                                    )
                                                },
                                                onMenuCopyClick = {
                                                    navigator.navigate(
                                                        CopyTextDialogPageDestination(it)
                                                    )
                                                },
                                                onMenuFavoriteClick = {
                                                    viewModel.send(
                                                        ThreadUiIntent.AddFavorite(
                                                            threadId,
                                                            it.id,
                                                            it.floor
                                                        )
                                                    )
                                                },
                                            ) {
                                                deletePost = null
                                                confirmDeleteDialogState.show()
                                            }

                                            thread?.getNullableImmutable { origin_thread_info }
                                                .takeIf { thread?.get { is_share_thread } == 1 }
                                                ?.let {
                                                    QuotedThread(
                                                        thread = it,
                                                        onOpen = { id, fid ->
                                                            navigator.navigate(ThreadPageDestination(id, forumId = fid))
                                                        },
                                                        modifier = Modifier
                                                            .padding(horizontal = 16.dp)
                                                            .padding(bottom = 16.dp),
                                                    )
                                                }

                                        }
                                    }
                                } else if (threadTitle.isNotBlank()) {
                                    Container {
                                        TopicSurface {
                                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TopicHeadline(threadTitle)
                                                if (!isImmersiveMode && author != null) {
                                                    CommentHeader(
                                                        author = author?.get(), threadAuthorId = author?.get { id },
                                                        time = thread?.get { createTime.toLong() }, onUserClick = {
                                                            navigator.navigate(UserProfilePageDestination(it.id))
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stickyHeader(key = "ThreadHeader") {
                                Container {
                                    ThreadRepliesHeader(
                                        replyCount = ((thread?.get { replyNum } ?: 1) - 1).coerceAtLeast(0),
                                        seeLz = isSeeLz,
                                        onFilterChange = { onlyAuthor ->
                                            viewModel.send(
                                                ThreadUiIntent.LoadFirstPage(
                                                    threadId = threadId, forumId = forumId,
                                                    seeLz = onlyAuthor, sortType = curSortType,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                            if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                                latestPosts(true)
                            }
                            item(key = "LoadPreviousBtn") {
                                if (hasPrevious) {
                                    Container {
                                        TextButton(
                                            onClick = {
                                                viewModel.send(
                                                    ThreadUiIntent.LoadPrevious(
                                                        threadId,
                                                        max(currentPageMax - 1, 1),
                                                        forumId,
                                                        postId = data
                                                            .first()
                                                            .post
                                                            .get { id },
                                                        seeLz = isSeeLz,
                                                        sortType = curSortType,
                                                        postIds = data.map { it.post.get { id } }
                                                    )
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shapes = ButtonDefaults.shapes(),
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ms_align_vertical_top),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = stringResource(id = R.string.btn_load_previous)
                                            )
                                        }
                                    }
                                }
                            }
                            if (!isRefreshing && data.isEmpty()) {
                                item(key = "EmptyTip") {
                                    Container {
                                        TipScreen(
                                            title = { Text(text = stringResource(id = R.string.title_empty)) },
                                            image = {
                                                StateIllustration(ImageVector.vectorResource(R.drawable.ms_chat))
                                            },
                                            actions = {
                                                if (canReload) {
                                                    Button(onClick = { reload() }, shapes = ButtonDefaults.shapes()) {
                                                        Text(text = stringResource(id = R.string.btn_refresh))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            scrollable = false
                                        )
                                    }
                                }
                            } else {
                                replyFloors(data)
                            }
                            if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                latestPosts(false)
                            }
                        }
                    }

                    PullToRefreshDefaults.LoadingIndicator(
                        isRefreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ThreadTopBar(
    forum: ImmutableHolder<SimpleForum>?,
    onBack: () -> Unit,
    onForumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TitleCentredToolbar(
        title = {
            forum?.let {
                if (forum.get { name }.isNotBlank()) {
                    AssistChip(
                        onClick = onForumClick,
                        label = {
                            Text(
                                stringResource(R.string.title_forum, it.get { name }),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Avatar(
                                data = it.get { avatar }, contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = null,
                    )
                }
            }
        },
        navigationIcon = {
            BackNavigationIcon(onBack)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ThreadRepliesHeader(replyCount: Int, seeLz: Boolean, onFilterChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surface) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.title_thread_header, replyCount.toString()),
                modifier = Modifier.align(Alignment.CenterVertically).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                listOf(false, true).forEach { onlyAuthor ->
                    val isSelected = seeLz == onlyAuthor
                    TonalToggleButton(
                        checked = isSelected,
                        onCheckedChange = { if (it) onFilterChange(onlyAuthor) },
                        shapes = if (onlyAuthor) ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .height(ButtonDefaults.ExtraSmallContainerHeight)
                            .sizeIn(minWidth = 48.dp)
                            .semantics {
                                role = Role.RadioButton
                                selected = isSelected
                            },
                    ) {
                        Text(
                            stringResource(if (onlyAuthor) R.string.title_see_lz else R.string.text_all),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThreadBottomBar(
    user: ImmutableHolder<User>,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
    onClickMore: () -> Unit,
    modifier: Modifier = Modifier,
    hasAgreed: Boolean = false,
    agreeNum: Long = 0,
    signedIn: Boolean = user.get { is_login } == 1,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!LocalContext.current.appPreferences.hideReply) {
                    ReplyPrompt(signedIn, user.get { displayPortrait }, onClickReply, Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                PostAgreeBtn(hasAgreed, agreeNum, onClick = onAgree)
                IconButton(onClick = onClickMore, shapes = IconButtonDefaults.shapes()) {
                    Icon(ImageVector.vectorResource(R.drawable.ms_more_vert), contentDescription = stringResource(R.string.btn_more))
                }
            }
            Spacer(
                Modifier.requiredHeightIn(min = if (LocalContext.current.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
internal fun ReplyPrompt(signedIn: Boolean, portrait: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shapes = ButtonDefaults.shapes(),
    ) {
        if (signedIn) {
            Avatar(data = StringUtil.getAvatarUrl(portrait.orEmpty()), size = Sizes.Tiny, contentDescription = null)
        }
        Text(
            text = stringResource(if (signedIn) R.string.tip_reply_thread else R.string.login_to_reply),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

internal enum class DiscussionRole { Topic, Reply, ParentFloor }

@Composable
internal fun ThreadReadingMarkDialog(
    position: ThreadFloorKey,
    saveMark: suspend (Long) -> Unit,
    onDismiss: () -> Unit,
    onLeave: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var saving by remember(position) { mutableStateOf(false) }
    var error by remember(position) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.message_update_collect_mark, position.floor))
                error?.let { Text(stringResource(R.string.message_update_collect_mark_failed, it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                if (saving) return@TextButton
                saving = true
                error = null
                scope.launch {
                    try {
                        saveMark(position.postId)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = exception.getErrorMessage()
                        saving = false
                        return@launch
                    }
                    onLeave()
                }
            }, shapes = ButtonDefaults.shapes()) {
                if (saving) LoadingIndicator(Modifier.size(20.dp))
                else Text(stringResource(R.string.save_reading_mark_and_return))
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onLeave, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.return_without_saving_reading_mark))
            }
        },
    )
}

@Composable
internal fun TopicSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

@Composable
private fun TopicHeadline(title: String) {
    Text(
        title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun PostCard(
    postHolder: ImmutableHolder<Post>,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    threadAuthorId: Long = 0L,
    blocked: Boolean = false,
    canDelete: (Post) -> Boolean = { false },
    immersiveMode: Boolean = false,
    isCollected: (Post) -> Boolean = { false },
    showSubPosts: Boolean = true,
    onUserClick: (User) -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (Post) -> Unit = {},
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onSubPostAgree: ((SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuFavoriteClick: ((Post) -> Unit)? = null,
    discussionRole: DiscussionRole = DiscussionRole.Reply,
    threadTitle: String? = null,
    onMenuDeleteClick: ((Post) -> Unit)? = null,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val post = remember(postHolder) { postHolder.get() }
    val topicTitle = if (discussionRole == DiscussionRole.Topic && post.is_ntitle != 1) {
        threadTitle?.takeIf { it.isNotBlank() } ?: post.title.takeIf { it.isNotBlank() }
    } else null
    val isContext = discussionRole != DiscussionRole.Reply
    val backdrop = if (isContext) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface
    val hasAgreed = remember(postHolder) {
        post.agree?.hasAgree == 1
    }
    val agreeNum = remember(postHolder) {
        post.agree?.diffAgreeNum ?: 0L
    }
    val menuState = rememberMenuState()
    val hideBlockedContent = context.appPreferences.hideBlockedContent || immersiveMode
    val visibleSubPosts = remember(subPosts, hideBlockedContent) {
        if (hideBlockedContent) subPosts.filterNot { it.blocked } else subPosts
    }
    val moreReplies = post.sub_post_number > subPosts.size
    BlockableContent(
        blocked = blocked,
        blockedTip = {
            BlockTip {
                Text(
                    text = stringResource(id = R.string.tip_blocked_post, postHolder.get { floor }),
                )
            }
        },
        hideBlockedContent = hideBlockedContent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            onClick = {
                onReplyClick(post)
            }.takeIf { !context.appPreferences.hideReply },
            menuContent = {
                if (!context.appPreferences.hideReply) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.btn_reply)) },
                        onClick = {
                            onReplyClick(post)
                            menuState.expanded = false
                        }
                    )
                }
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.menu_copy)) },
                        onClick = {
                            onMenuCopyClick(post.content.plainText)
                            menuState.expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.title_report)) },
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, post.id.toString())
                        }
                        menuState.expanded = false
                    }
                )
                if (onMenuFavoriteClick != null) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(if (isCollected(post)) R.string.title_collect_on else R.string.title_collect_floor))
                        },
                        onClick = {
                            onMenuFavoriteClick(post)
                            menuState.expanded = false
                        }
                    )
                }
                if (canDelete(post) && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.title_delete)) },
                        onClick = {
                            onMenuDeleteClick(post)
                            menuState.expanded = false
                        }
                    )
                }
            }
        ) {
            CommentLayout(
                modifier = Modifier.fillMaxWidth().background(backdrop)
                    .padding(horizontal = 16.dp, vertical = if (isContext) 16.dp else 12.dp),
                background = backdrop,
                header = {
                    if (topicTitle != null) TopicHeadline(topicTitle)
                    if (!immersiveMode) {
                        CommentHeader(
                            author = post.author,
                            threadAuthorId = threadAuthorId,
                            time = post.time.toLong(),
                            floor = post.floor,
                            nameStyle = MaterialTheme.typography.titleMedium,
                            authorHeading = topicTitle == null,
                            onUserClick = onUserClick,
                            action = if (post.floor > 1 && post.id > 0) {
                                { PostAgreeBtn(hasAgreed = hasAgreed, agreeNum = agreeNum, onClick = onAgree) }
                            } else null,
                        )
                    }
                },
                content = {
                    if (isCollected(post)) {
                        Chip(
                            text = stringResource(id = R.string.title_collected_floor),
                            invertColor = true,
                            prefixIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ms_star_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }

                    contentRenders.fastForEach { it.Render() }

                    if (showSubPosts && !immersiveMode && (visibleSubPosts.isNotEmpty() || moreReplies)) {
                        ReplyPreviewGroup {
                            visibleSubPosts.fastForEach { item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = {
                                        Text(
                                            text = stringResource(R.string.tip_blocked_sub_post),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                    },
                                ) {
                                    SubPostItem(
                                        subPostList = item.subPost,
                                        subPostContent = item.subPostContent,
                                        threadAuthorId = threadAuthorId,
                                        onUserClick = onUserClick,
                                        onAgree = onSubPostAgree,
                                        onReplyClick = onSubPostReplyClick?.let { reply -> { reply(post, it) } },
                                        onOpenSubPosts = onOpenSubPosts,
                                        onMenuCopyClick = onMenuCopyClick?.let { copy -> { copy(it.content.plainText) } },
                                    )
                                }
                            }
                            if (moreReplies) {
                                TextButton(
                                    onClick = { onOpenSubPosts(0) },
                                    shapes = ButtonDefaults.shapes(),
                                    modifier = Modifier.padding(start = 4.dp),
                                ) {
                                    Text(stringResource(R.string.open_all_sub_posts, post.sub_post_number))
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ReplyPreviewGroup(content: @Composable ColumnScope.() -> Unit) {
    val background = MaterialTheme.colorScheme.surfaceContainer
    Surface(modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        shape = MaterialTheme.shapes.medium, color = background) {
        ProvideCommentLinkColor(background) { Column(content = content) }
    }
}

@Composable
internal fun SubPostItem(
    subPostList: ImmutableHolder<SubPostList>,
    subPostContent: AnnotatedString,
    threadAuthorId: Long,
    onUserClick: (User) -> Unit,
    onReplyClick: ((SubPostList) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: ((SubPostList) -> Unit)?,
    onAgree: ((SubPostList) -> Unit)? = null,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()
    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (!context.appPreferences.hideReply && onReplyClick != null) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.title_reply)) },
                    onClick = {
                        onReplyClick(subPostList.get())
                        menuState.expanded = false
                    }
                )
            }
            if (onMenuCopyClick != null) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.menu_copy)) },
                    onClick = {
                        onMenuCopyClick(subPostList.get())
                        menuState.expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.title_report)) },
                onClick = {
                    coroutineScope.launch {
                        TiebaUtil.reportPost(context, navigator, subPostList.get { id }.toString())
                    }
                    menuState.expanded = false
                }
            )
        },
        shape = RoundedCornerShape(0),
        onClick = {
            onOpenSubPosts(subPostList.get { id })
        }
    ) {
        val author = subPostList.get { author }
        val directed = remember(subPostContent) { subPostContent.splitDirectedReply() }
        val body = directed?.body ?: subPostContent
        val reply = subPostList.get()
        val agree = reply.agree
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            CommentHeader(
                author = author, threadAuthorId = threadAuthorId, onUserClick = onUserClick,
                compact = true, time = null, showMetadata = false,
                recipient = directed?.let { { ReplyRecipient(it) } },
                action = if (reply.id > 0 && agree != null && onAgree != null) {
                    { PostAgreeBtn(agree.hasAgree == 1, agree.diffAgreeNum, onClick = { onAgree(reply) }) }
                } else null,
            )
            if (body.isNotEmpty()) {
                PbContentText(
                    text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis, maxLines = 4,
                )
            }
            CommentMetadata(time = reply.time.toLong(), ipAddress = author?.ip_address)
        }
    }
}

@Composable
internal fun ThreadMenu(
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    canDelete: () -> Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onJumpPageClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .fillMaxWidth(0.25f)
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
        VerticalGrid(
            column = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            rowModifier = Modifier.height(IntrinsicSize.Min),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                ThreadMenuToggle(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(id = R.string.title_see_lz),
                            minLength = 4
                        )
                    },
                    checked = isSeeLz,
                    onClick = onSeeLzClick,
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (isSeeLz) R.drawable.ms_face_6 else R.drawable.ms_face_retouching_off),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ThreadMenuToggle(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(
                                id = if (isCollected) R.string.title_collected else R.string.title_uncollected
                            ),
                            minLength = 4
                        )
                    },
                    checked = isCollected,
                    onClick = onCollectClick,
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (isCollected) R.drawable.ms_star_filled else R.drawable.ms_star),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ThreadMenuToggle(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(
                                id = R.string.title_pure_read
                            ),
                            minLength = 4
                        )
                    },
                    checked = isImmersiveMode,
                    onClick = onImmersiveModeClick,
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(if (isImmersiveMode) R.drawable.ms_chrome_reader_mode_filled else R.drawable.ms_chrome_reader_mode),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ThreadMenuToggle(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(
                                id = R.string.title_sort
                            ),
                            minLength = 4
                        )
                    },
                    checked = isDesc,
                    onClick = onDescClick,
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ms_sort),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column {
            ListMenuItem(
                icon = ImageVector.vectorResource(R.drawable.ms_rocket_launch),
                text = stringResource(id = R.string.title_jump_page),
                iconColor = MaterialTheme.colorScheme.onSurface,
                onClick = onJumpPageClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = ImageVector.vectorResource(R.drawable.ms_share),
                text = stringResource(id = R.string.title_share),
                iconColor = MaterialTheme.colorScheme.onSurface,
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = ImageVector.vectorResource(R.drawable.ms_content_copy),
                text = stringResource(id = R.string.title_copy_link),
                iconColor = MaterialTheme.colorScheme.onSurface,
                onClick = onCopyLinkClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = ImageVector.vectorResource(R.drawable.ms_report),
                text = stringResource(id = R.string.title_report),
                iconColor = MaterialTheme.colorScheme.onSurface,
                onClick = onReportClick,
                modifier = Modifier.fillMaxWidth(),
            )
            if (canDelete()) {
                ListMenuItem(
                    icon = ImageVector.vectorResource(R.drawable.ms_delete),
                    text = stringResource(id = R.string.title_delete),
                    iconColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

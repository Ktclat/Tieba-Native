package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.ReplyDialog
import com.huanchengfly.tieba.post.ui.page.thread.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.page.thread.CommentHeader
import com.huanchengfly.tieba.post.ui.page.thread.CommentLayout
import com.huanchengfly.tieba.post.ui.page.thread.CommentMetadata
import com.huanchengfly.tieba.post.ui.page.thread.ReplyRecipient
import com.huanchengfly.tieba.post.ui.page.thread.splitDirectedReply
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val account = LocalAccount.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId.takeIf { loadFromSubPost } ?: 0L
            )
        )
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::anti,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::forum,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::thread,
        initial = null
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::totalCount,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = true
    )

    val lazyListState = rememberLazyListState()

    PositionSubPostOnOpen(lazyListState, subPostId, subPosts)

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<ImmutableHolder<SubPostList>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost = post?.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        deleteMyPost = isSelfPost,
                        tbs = anti?.get { tbs },
                    )
                )
            } else {
                val isSelfSubPost =
                    deleteSubPost!!.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = postId,
                        subPostId = deleteSubPost!!.get { id },
                        deleteMyPost = isSelfSubPost,
                        tbs = anti?.get { tbs },
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deleteSubPost == null && post != null) stringResource(
                    id = R.string.tip_post_floor,
                    post!!.get { floor })
                else stringResource(id = R.string.this_reply)
            )
        )
    }

    val replyDialogState = rememberDialogState()
    var currentReplyArgs by remember { mutableStateOf<ReplyArgs?>(null) }
    if (currentReplyArgs != null) {
        ReplyDialog(args = currentReplyArgs!!, state = replyDialogState)
    }

//    onGlobalEvent<GlobalEvent.ReplySuccess>(
//        filter = { it.threadId == threadId && it.postId == postId }
//    ) { event ->
//        viewModel.send(
//            SubPostsUiIntent.Load(
//                forumId,
//                threadId,
//                postId,
//                subPostId.takeIf { loadFromSubPost } ?: 0L
//            )
//        )
//    }

    fun showReplyDialog(args: ReplyArgs) {
        currentReplyArgs = args
        replyDialogState.show()
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(text = post?.let {
                            stringResource(
                                id = R.string.title_sub_posts,
                                it.get { floor })
                        } ?: stringResource(id = R.string.title_sub_posts_default),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navigator.navigate(
                                ThreadPageDestination(
                                    forumId = forumId,
                                    threadId = threadId,
                                    postId = postId
                                )
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = stringResource(id = R.string.btn_open_origin_thread)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (account != null && !LocalContext.current.appPreferences.hideReply) {
                    Column(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Avatar(
                                data = StringUtil.getAvatarUrl(account.portrait),
                                size = Sizes.Tiny,
                                contentDescription = account.name,
                            )
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable {
                                        val fid = forum?.get { id } ?: forumId
                                        val forumName = forum?.get { name }
                                        if (!forumName.isNullOrEmpty()) {
                                            showReplyDialog(
                                                ReplyArgs(
                                                    forumId = fid,
                                                    forumName = forumName,
                                                    threadId = threadId,
                                                    postId = postId,
                                                )
                                            )
                                        }
                                    }
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tip_reply_thread),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .requiredHeightIn(min = if (LocalContext.current.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LoadMoreLayout(
                modifier = Modifier.padding(paddingValues),
                isLoading = isLoading,
                onLoadMore = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(
                            forumId,
                            threadId,
                            postId,
//                            subPostId,
                            page = currentPage + 1,
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = post == null && subPosts.isEmpty(),
            ) {
                MyLazyColumn(state = lazyListState) {
                    item(key = "Post$postId") {
                        post?.let {
                            Column {
                                PostCard(
                                    postHolder = it,
                                    contentRenders = postContentRenders,
                                    threadAuthorId = thread?.get { author?.id } ?: 0L,
                                    canDelete = { it.author_id == account?.uid?.toLongOrNull() },
                                    showSubPosts = false,
                                    onUserClick = {
                                        navigator.navigate(UserProfilePageDestination(it.id))
                                    },
                                    onAgree = {
                                        val hasAgreed = it.get { agree?.hasAgree == 1 }
                                        viewModel.send(
                                            SubPostsUiIntent.Agree(
                                                forumId,
                                                threadId,
                                                it.get { id },
                                                agree = !hasAgreed
                                            )
                                        )
                                    },
                                    onReplyClick = {
                                        showReplyDialog(
                                            ReplyArgs(
                                                forumId = forumId,
                                                forumName = forum?.get { name } ?: "",
                                                threadId = threadId,
                                                postId = postId,
                                                replyUserId = it.author?.id ?: it.author_id,
                                                replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                                    ?: it.author?.name,
                                                replyUserPortrait = it.author?.portrait,
                                            )
                                        )
                                    },
                                    onMenuCopyClick = {
                                        navigator.navigate(
                                            CopyTextDialogPageDestination(it)
                                        )
                                    },
                                ) {
                                    deleteSubPost = null
                                    confirmDeleteDialogState.show()
                                }
                            }
                        }
                    }
                    stickyHeader(key = "SubPostsHeader") { SubPostsHeading(totalCount) }
                    items(subPosts, key = { it.id }) { item ->
                        SubPostItem(
                            item = item,
                            canDelete = { it.author_id == account?.uid?.toLongOrNull() },
                            threadAuthorId = thread?.get { author?.id },
                            onUserClick = {
                                navigator.navigate(UserProfilePageDestination(it.id))
                            },
                            onAgree = { reply ->
                                val parentId = post?.get { id } ?: 0L
                                if (parentId > 0L) {
                                    viewModel.send(
                                        SubPostsUiIntent.Agree(
                                            forumId,
                                            threadId,
                                            postId = parentId,
                                            subPostId = reply.id,
                                            agree = reply.agree?.hasAgree != 1,
                                        )
                                    )
                                }
                            },
                            onReplyClick = {
                                showReplyDialog(
                                    ReplyArgs(
                                        forumId = forumId,
                                        forumName = forum?.get { name } ?: "",
                                        threadId = threadId,
                                        postId = postId,
                                        subPostId = it.id,
                                        replyUserId = it.author?.id ?: it.author_id,
                                        replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                            ?: it.author?.name,
                                        replyUserPortrait = it.author?.portrait,
                                    )
                                )
                            },
                            onMenuCopyClick = {
                                navigator.navigate(
                                    CopyTextDialogPageDestination(it)
                                )
//                                TiebaUtil.copyText(context, it)
                            },
                            onMenuDeleteClick = {
                                deleteSubPost = it.wrapImmutable()
                                confirmDeleteDialogState.show()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubPostsHeading(totalCount: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), color = colors.surfaceContainerLow) {
        Text(
            text = stringResource(R.string.title_sub_posts_header, totalCount),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics { heading() },
        )
    }
}

@Composable
internal fun PositionSubPostOnOpen(list: LazyListState, targetId: Long, replies: List<SubPostItemData>) {
    var located by rememberSaveable(targetId) { mutableStateOf(false) }
    val replyIndex = remember(targetId, replies) {
        if (targetId > 0L) replies.indexOfFirst { it.id == targetId } else -1
    }
    LaunchedEffect(list, targetId, replyIndex) {
        if (!located && replyIndex >= 0) {
            val index = replyIndex + 2 // Parent floor and pinned replies heading.
            list.scrollToItem(index)
            val heading = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "SubPostsHeader" }
            if (heading != null) list.scrollToItem(index, -heading.size)
            located = true
        }
    }
}

@Composable
internal fun SubPostItem(
    item: SubPostItemData,
    threadAuthorId: Long? = null,
    canDelete: (SubPostList) -> Boolean = { false },
    onUserClick: (User) -> Unit = {},
    onAgree: (SubPostList) -> Unit = {},
    onReplyClick: (SubPostList) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostList) -> Unit)? = null,
) {
    val (subPost, contentRenders, blocked) = item
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val hasAgreed = remember(subPost) {
        subPost.get { agree?.hasAgree == 1 }
    }
    val agreeNum = remember(subPost) {
        subPost.get { agree?.diffAgreeNum ?: 0L }
    }
    val menuState = rememberMenuState()
    val directed = remember(contentRenders) {
        (contentRenders.firstOrNull() as? TextContentRender)?.text?.splitDirectedReply()
    }
    val rowPadding = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    BlockableContent(
        blocked = blocked,
        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_sub_post)) }) },
        modifier = rowPadding,
    ) {
        LongClickMenu(
            modifier = Modifier.fillMaxWidth(),
            menuState = menuState,
            indication = null,
            menuContent = {
                if (!context.appPreferences.hideReply) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.btn_reply)) },
                        onClick = {
                            onReplyClick(subPost.get())
                            menuState.expanded = false
                        }
                    )
                }
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.menu_copy)) },
                        onClick = {
                            onMenuCopyClick(contentRenders.joinToString("\n") { it.toString() })
                            menuState.expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(id = R.string.title_report)) },
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, subPost.get { id }.toString())
                        }
                        menuState.expanded = false
                    }
                )
                if (canDelete(subPost.get()) && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.title_delete)) },
                        onClick = {
                            onMenuDeleteClick(subPost.get())
                            menuState.expanded = false
                        }
                    )
                }
            },
            onClick = { onReplyClick(subPost.get()) }.takeUnless { context.appPreferences.hideReply }
        ) {
            CommentLayout(
                modifier = rowPadding,
                header = {
                    CommentHeader(
                        author = subPost.get { author },
                        threadAuthorId = threadAuthorId,
                        time = null,
                        showMetadata = false,
                        onUserClick = onUserClick,
                        recipient = if (directed != null) { { ReplyRecipient(directed) } } else null,
                        action = if (item.id > 0) {
                            {
                                PostAgreeBtn(
                                    hasAgreed = hasAgreed,
                                    agreeNum = agreeNum,
                                    onClick = { onAgree(subPost.get()) },
                                )
                            }
                        } else null,
                    )
                },
            ) {
                contentRenders.fastForEachIndexed { index, render ->
                    if (index == 0 && directed != null) {
                        if (directed.body.isNotEmpty()) TextContentRender(directed.body).Render()
                    } else {
                        render.Render()
                    }
                }
                CommentMetadata(
                    time = subPost.get { time.toLong() },
                    ipAddress = subPost.get { author?.ip_address },
                )
            }
        }
    }
}

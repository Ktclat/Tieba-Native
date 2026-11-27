@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.forum

import com.huanchengfly.tieba.post.collectPreferenceAsState
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.constrainHeight
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.ui.widgets.compose.loadingPlaceholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.ForumHistoryExtra
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumSearchPostPageDestination
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import androidx.compose.material3.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import androidx.compose.material3.PrimaryScrollableTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


private val LoadDistance = 70.dp

internal fun forumSortPreference(
    context: Context,
    forumName: String,
): Flow<Int> {
    val key = intPreferencesKey("${forumName}_sort_type")
    val defaultSortKey = stringPreferencesKey("default_sort_type")
    return context.dataStore.data.map { prefs ->
        prefs[key] ?: prefs[defaultSortKey]?.toIntOrNull() ?: 0
    }.distinctUntilChanged()
}

suspend fun setSortType(
    context: Context,
    forumName: String,
    sortType: Int,
) {
    context.dataStore.edit {
        it[intPreferencesKey("${forumName}_sort_type")] = sortType
    }
}

@Composable
private fun ForumHeaderPlaceholder(
    forumName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarPlaceholder(size = Sizes.Large)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.title_forum, forumName),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (LocalAccount.current != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .loadingPlaceholder(
                            visible = true,
                        )
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                ) {
                    Text(text = stringResource(id = R.string.button_sign_in), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ForumHeader(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>,
    onOpenForumInfo: () -> Unit,
    onBtnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (forum) = forumInfoImmutableHolder
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.avatar,
                size = Sizes.Large,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenForumInfo
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum, forum.name),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
//                    Icon(
//                        imageVector = Icons.Rounded.KeyboardArrowRight,
//                        contentDescription = null,
//                        modifier = Modifier.size(16.dp)
//                    )
                }
                AnimatedVisibility(visible = forum.is_like == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { max(
                                0F,
                                min(
                                    1F,
                                    forum.cur_score * 1.0F / (max(1.0F, forum.levelup_score * 1.0F))
                                )
                            ) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        Text(
                            text = stringResource(
                                id = R.string.tip_forum_header_liked,
                                forum.user_level.toString(),
                                forum.level_name
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            val btnEnabled =
                (forum.is_like != 1) || (forum.sign_in_info?.user_info?.is_sign_in != 1)
            if (LocalAccount.current != null) {
                Button(
                    onClick = onBtnClick,
                    elevation = null,
                    shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(100)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    enabled = btnEnabled
                ) {
                    val text = when {
                        forum.is_like != 1 -> stringResource(id = R.string.button_follow)
                        forum.sign_in_info?.user_info?.is_sign_in == 1 -> stringResource(
                            id = R.string.button_signed_in,
                            forum.sign_in_info.user_info.cont_sign_num
                        )

                        else -> stringResource(id = R.string.button_sign_in)
                    }
                    Text(text = text, fontSize = 13.sp)
                }
            }
        }
//        Row(
//            modifier = Modifier
//                .clip(RoundedCornerShape(8.dp))
//                .background(color = MaterialTheme.colorScheme.surfaceContainer)
//                .padding(vertical = 20.dp),
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            StatCardItem(
//                statNum = forum.member_num,
//                statText = stringResource(id = R.string.text_stat_follow)
//            )
//            StatCardItem(
//                statNum = forum.thread_num,
//                statText = stringResource(id = R.string.text_stat_threads)
//            )
//            StatCardItem(
//                statNum = forum.post_num,
//                statText = stringResource(id = R.string.title_stat_posts_num)
//            )
//        }
    }
}

private fun shareForum(context: Context, forumName: String) {
    TiebaUtil.shareText(
        context,
        Uri.parse("https://tieba.baidu.com/f").buildUpon().appendQueryParameter("kw", forumName).toString(),
        context.getString(R.string.title_forum, forumName)
    )
}

private suspend fun sendToDesktop(
    context: Context,
    forum: ForumInfo,
    onSuccess: () -> Unit = {},
    onFailure: (String) -> Unit = {}
) {
    requestPinShortcut(
        context,
        "forum_${forum.id}",
        forum.avatar,
        context.getString(R.string.title_forum, forum.name),
        Intent(Intent.ACTION_VIEW, Uri.parse("tblite://forum").buildUpon().appendPath(forum.name).build()),
        onSuccess = onSuccess,
        onFailure = onFailure
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://forum/{forumName}")
    ]
)
@Composable
fun ForumPage(
    forumName: String,
    viewModel: ForumViewModel = pageViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val sortPreference = remember(context, forumName) { forumSortPreference(context, forumName) }
    val currentSortType = sortPreference.collectAsState(initial = null).value
    if (currentSortType != null) {
        LazyLoad(loaded = viewModel.initialized) {
            viewModel.send(ForumUiIntent.Load(forumName, currentSortType))
            viewModel.initialized = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    viewModel.onEvent<ForumUiEvent.SignIn.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_success,
                "${it.signBonusPoint}",
                "${it.userSignRank}"
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.SignIn.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_success,
                it.memberSum,
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_success
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_failed,
                it.errorMsg
            )
        )
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForumContent(forumName, currentSortType, state, navigator, viewModel::send, snackbarHostState)
}

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
internal fun ForumContent(
    forumName: String,
    currentSortType: Int?,
    state: ForumUiState,
    navigator: DestinationsNavigator,
    onIntent: (ForumUiIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val context = LocalContext.current
    val forumInfo = state.forum
    val account = LocalAccount.current
    val pagerState = rememberPagerState { 2 }

    val currentPage by remember {
        derivedStateOf {
            pagerState.currentPage
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current

    var heightOffset by rememberSaveable { mutableFloatStateOf(0f) }
    var headerHeight by rememberSaveable {
        mutableFloatStateOf(
            with(density) {
                (Sizes.Large + 16.dp * 2).toPx()
            }
        )
    }

    val isShowTopBarArea by remember {
        derivedStateOf {
            heightOffset.absoluteValue < headerHeight
        }
    }

    val unlikeDialogState = rememberDialogState()

    LaunchedEffect(forumInfo) {
        if (forumInfo != null) {
            val (forum) = forumInfo
            HistoryUtil.saveHistory(
                History(
                    title = context.getString(R.string.title_forum, forum.name),
                    timestamp = System.currentTimeMillis(),
                    avatar = forum.avatar,
                    type = HistoryUtil.TYPE_FORUM,
                    data = forum.name,
                    extras = Json.encodeToString(ForumHistoryExtra(forum.id))
                ),
                true
            )
        }
    }

    if (account != null && forumInfo != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = {
                onIntent(
                    ForumUiIntent.Unlike(forumInfo.get { id }, forumName, state.tbs ?: account.tbs)
                )
            },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.title_dialog_unfollow_forum,
                        forumName
                    )
                )
            }
        )
    }

    ProvideNavigator(navigator = navigator) {
        if (currentSortType == null) {
            LoadingPlaceholder(forumName)
            return@ProvideNavigator
        }
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = forumInfo == null,
            isError = state.isError,
            isLoading = state.isLoading,
            onReload = {
                onIntent(
                    ForumUiIntent.Load(
                        forumName,
                        currentSortType
                    )
                )
            },
            loadingScreen = { LoadingPlaceholder(forumName) },
            errorScreen = {
                MyScaffold(topBar = { ForumToolbar(forumName, showTitle = true) }) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        Text(stringResource(R.string.text_load_failed))
                        Button(onClick = ::reload, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.button_retry))
                        }
                    }
                }
            },
        ) {
            MyScaffold(
                snackbarHostState = snackbarHostState,
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    ForumToolbar(
                        forumName = forumName,
                        showTitle = !isShowTopBarArea,
                        menuContent = {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.title_share)) },
                                onClick = {
                                    shareForum(context, forumName)
                                    dismiss()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.title_send_to_desktop)) },
                                onClick = {
                                    if (forumInfo != null) {
                                        val (forum) = forumInfo
                                        coroutineScope.launch {
                                            sendToDesktop(
                                                context,
                                                forum,
                                                onSuccess = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_success
                                                            )
                                                        )
                                                    }
                                                },
                                                onFailure = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_failed,
                                                                it
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    dismiss()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = stringResource(id = R.string.title_unfollow)) },
                                onClick = {
                                    unlikeDialogState.show()
                                    dismiss()
                                }
                            )
                        },
                        forumId = forumInfo?.get { id }
                    )
                },
                floatingActionButton = {
                    val forumAction by context.dataStore.collectPreferenceAsState(
                        key = stringPreferencesKey("forumFabFunction"),
                        defaultValue = "post",
                    )
                    if (forumAction != "hide") {
                        FloatingActionButton(
                            onClick = {
                                when (forumAction) {
                                    "refresh" -> {
                                        coroutineScope.launch {
                                            emitGlobalEventSuspend(
                                                ForumThreadListUiEvent.BackToTop(
                                                    currentPage == 1
                                                )
                                            )
                                            emitGlobalEventSuspend(
                                                ForumThreadListUiEvent.Refresh(
                                                    currentPage == 1,
                                                    currentSortType
                                                )
                                            )
                                        }
                                    }

                                    "back_to_top" -> {
                                        coroutineScope.launch {
                                            emitGlobalEvent(
                                                ForumThreadListUiEvent.BackToTop(
                                                    currentPage == 1
                                                )
                                            )
                                        }
                                    }

                                    else -> {
                                        context.toastShort(R.string.toast_feature_unavailable)
                                    }
                                }
                            },
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            Icon(
                                imageVector = when (forumAction) {
                                    "refresh" -> Icons.Rounded.Refresh
                                    "back_to_top" -> Icons.Rounded.VerticalAlignTop
                                    else -> Icons.Rounded.Add
                                },
                                contentDescription = stringResource(
                                    when (forumAction) {
                                        "refresh" -> R.string.btn_refresh
                                        "back_to_top" -> R.string.btn_back_to_top
                                        else -> R.string.btn_post
                                    }
                                )
                            )
                        }
                    }
                }
            ) { contentPadding ->
                val refreshingTabs = remember(forumName) { mutableStateMapOf<Boolean, Boolean>() }
                PullToRefreshLayout(
                    refreshing = refreshingTabs[currentPage == 1] == true,
                    onRefresh = {
                        coroutineScope.emitGlobalEvent(
                            ForumThreadListUiEvent.Refresh(
                                currentPage == 1,
                                currentSortType
                            )
                        )
                    }
                ) {
                    val headerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y < 0) {
                                    val prevHeightOffset = heightOffset
                                    heightOffset = max(heightOffset + available.y, -headerHeight)
                                    return Offset(0f, heightOffset - prevHeightOffset)
                                }

                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource,
                            ): Offset {
                                if (available.y > 0f) {
                                    // Adjust the height offset in case the consumed delta Y is less than what was
                                    // recorded as available delta Y in the pre-scroll.
                                    val prevHeightOffset = heightOffset
                                    heightOffset = min(heightOffset + available.y, 0f)
                                    return Offset(0f, heightOffset - prevHeightOffset)
                                }

                                return Offset.Zero
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .nestedScroll(headerNestedScrollConnection)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clipToBounds()
                                    .layout { measurable, constraints ->
                                        val header = measurable.measure(constraints.copy(minHeight = 0))
                                        val visibleHeight = constraints.constrainHeight((header.height + heightOffset).roundToInt())
                                        layout(header.width, visibleHeight) {
                                            header.placeRelative(0, visibleHeight - header.height)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentHeight(
                                            align = Alignment.Bottom,
                                            unbounded = true
                                        )
                                        .onSizeChanged {
                                            headerHeight = it.height.toFloat()
                                            heightOffset = heightOffset.coerceIn(-headerHeight, 0f)
                                        }
                                ) {
                                    forumInfo?.let { holder ->
                                        ForumHeader(
                                            forumInfoImmutableHolder = holder,
                                            onOpenForumInfo = {
                                                navigator.navigate(
                                                    ForumDetailPageDestination(
                                                        forumId = holder.get { this.id })
                                                )
                                            },
                                            onBtnClick = {
                                                val (forum) = holder
                                                when {
                                                    forum.is_like != 1 -> onIntent(
                                                        ForumUiIntent.Like(
                                                            forum.id,
                                                            forum.name,
                                                            state.tbs ?: account!!.tbs
                                                        )
                                                    )

                                                    forum.sign_in_info?.user_info?.is_sign_in != 1 -> {
                                                        onIntent(
                                                            ForumUiIntent.SignIn(
                                                                forum.id,
                                                                forum.name,
                                                                state.tbs ?: account!!.tbs
                                                            )
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }

                            val tabTextStyle = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.sp
                            )

                            PrimaryScrollableTabRow(
                                selectedTabIndex = currentPage,
                                divider = {},
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                edgePadding = 0.dp,
                                modifier = Modifier
                                    .wrapContentWidth(align = Alignment.Start)
                                    .align(Alignment.Start)
                            ) {
                                TabClickMenu(
                                    selected = currentPage == 0,
                                    onClick = {
                                        pagerState.requestScrollToPage(0)
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(id = R.string.tab_forum_latest),
                                            style = tabTextStyle
                                        )
                                    },
                                    menuContent = {
                                        ListSinglePicker(
                                            itemTitles = persistentListOf(
                                                stringResource(id = R.string.title_sort_by_reply),
                                                stringResource(id = R.string.title_sort_by_send)
                                            ),
                                            itemValues = persistentListOf(0, 1),
                                            selectedPosition = currentSortType,
                                            onItemSelected = { _, _, value, changed ->
                                                if (changed) {
                                                    coroutineScope.launch {
                                                        setSortType(context, forumName, value)
                                                        emitGlobalEvent(
                                                            ForumThreadListUiEvent.Refresh(
                                                                currentPage == 1,
                                                                value
                                                            )
                                                        )
                                                    }
                                                }
                                                dismiss()
                                            }
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Tab(
                                    selected = currentPage == 1,
                                    onClick = {
                                        pagerState.requestScrollToPage(1)
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.tab_forum_good),
                                            style = tabTextStyle
                                        )
                                    }
                                }
                            }

                            if (forumInfo != null) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    key = { it },
                                    verticalAlignment = Alignment.Top,
                                    userScrollEnabled = true,
                                ) { tab ->
                                    ForumThreadListPage(
                                        forumId = forumInfo.get { id },
                                        forumName = forumInfo.get { name },
                                        isGood = tab == 1,
                                        sortType = currentSortType,
                                        onRefreshingChanged = { refreshingTabs[tab == 1] = it },
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
fun LoadingPlaceholder(
    forumName: String
) {
    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            ForumToolbar(
                forumName = forumName,
                showTitle = false,
                menuContent = {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.title_share)) },
                        onClick = {
                            shareForum(context, forumName)
                            dismiss()
                        }
                    )
                }
            )
        }
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            ForumHeaderPlaceholder(
                forumName = forumName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Row(modifier = Modifier.height(48.dp)) {
                persistentListOf(
                    stringResource(id = R.string.tab_forum_latest),
                    stringResource(id = R.string.tab_forum_good),
                ).fastForEach {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.loadingPlaceholder(
                                visible = true,
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            repeat(4) {
                FeedCardPlaceholder()
            }
        }
    }
}

@Composable
private fun BackNavigationIconPlaceholder() {
    IconButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.alpha(0f),
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null
        )
    }
}

@Composable
private fun ForumToolbar(
    forumName: String,
    showTitle: Boolean,
    menuContent: @Composable (MenuScope.() -> Unit)? = null,
    forumId: Long? = null,
) {
    val navigator = LocalNavigator.current
    Toolbar(
        title = {
            if (showTitle) Text(
                text = stringResource(
                    id = R.string.title_forum,
                    forumName
                )
            )
        },
        navigationIcon = { BackNavigationIcon(onBackPressed = { navigator.navigateUp() }) },
        actions = {
            if (forumId != null) {
                IconButton(
                    onClick = {
                        navigator.navigate(ForumSearchPostPageDestination(forumName, forumId))
                    },
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.btn_search_in_forum)
                    )
                }
            }
            Box {
                if (menuContent != null) {
                    val menuState = rememberMenuState()
                    ClickMenu(
                        menuContent = menuContent,
                        menuState = menuState,
                        triggerShape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.btn_more)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun RowScope.StatCardItem(
    statNum: Int,
    statText: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumString(),
            fontSize = 20.sp,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

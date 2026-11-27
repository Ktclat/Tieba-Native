@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.buildAnnotatedStringWithUser
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlin.math.min
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
private fun QuotePostCard(
    quotePostInfo: SearchThreadBean.PostInfo,
    mainPost: SearchThreadBean.MainPost,
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit,
    onClick: () -> Unit,
    medias: ImmutableList<SearchThreadBean.MediaInfo> = persistentListOf(),
    keyword: String? = null,
) {
    val quoteContentString = remember(quotePostInfo) {
        buildAnnotatedStringWithUser(
            quotePostInfo.user.userId,
            quotePostInfo.user.userName ?: "",
            quotePostInfo.user.showNickname,
            quotePostInfo.content
        )
    }
    MaterialCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HighlightText(
                text = quoteContentString,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList(),
            )
            MainPostCard(
                mainPost = mainPost,
                onClick = { onMainPostClick(mainPost) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                medias = medias,
                keyword = keyword,
            )
        }
    }
}

@Composable
private fun MainPostCard(
    mainPost: SearchThreadBean.MainPost,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    medias: ImmutableList<SearchThreadBean.MediaInfo> = persistentListOf(),
    keyword: String? = null,
) {
    val titleString = remember(mainPost) {
        buildAnnotatedStringWithUser(
            mainPost.user.userId,
            mainPost.user.userName ?: "",
            mainPost.user.showNickname,
            mainPost.title
        )
    }
    MaterialCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HighlightText(
                text = titleString,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList()
            )
            if (mainPost.content.isNotBlank()) {
                PbContentText(
                    text = mainPost.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SearchMedia(medias = medias)
        }
    }
}

@Composable
fun SearchThreadList(
    data: ImmutableList<SearchThreadBean.ThreadInfoBean>,
    lazyListState: LazyListState,
    onItemClick: (SearchThreadBean.ThreadInfoBean) -> Unit,
    onItemUserClick: (SearchThreadBean.UserInfoBean) -> Unit,
    onItemForumClick: (SearchThreadBean.ForumInfo) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (SearchThreadBean.PostInfo) -> Unit = {},
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
    onReplyClick: (SearchThreadBean.ThreadInfoBean) -> Unit = onItemClick,
    footer: LazyListScope.() -> Unit = {},
    header: LazyListScope.() -> Unit = {},
) {
    MyLazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        header()
        items(data) { item ->
            SearchThreadItem(
                item = item,
                onClick = onItemClick,
                onReplyClick = onReplyClick,
                onUserClick = onItemUserClick,
                onForumClick = onItemForumClick,
                onQuotePostClick = onQuotePostClick,
                onMainPostClick = onMainPostClick,
                hideForum = hideForum,
                searchKeyword = searchKeyword
            )
        }
        footer()
    }
}

@Composable
fun SearchThreadUserHeader(
    user: SearchThreadBean.UserInfoBean,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(user.portrait),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    user.userName.orEmpty(),
                    user.showNickname,
                    color = LocalContentColor.current
                )
            )
        },
        desc = {
            Text(
                text = DateTimeUtils.getRelativeTimeString(LocalContext.current, time)
            )
        },
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun SearchThreadItem(
    item: SearchThreadBean.ThreadInfoBean,
    onClick: (SearchThreadBean.ThreadInfoBean) -> Unit,
    onUserClick: (SearchThreadBean.UserInfoBean) -> Unit,
    onForumClick: (SearchThreadBean.ForumInfo) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (SearchThreadBean.PostInfo) -> Unit = {},
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
    onReplyClick: (SearchThreadBean.ThreadInfoBean) -> Unit = onClick,
) {
    Card(
        modifier = modifier,
        header = {
            SearchThreadUserHeader(
                user = item.user,
                time = item.time,
                onClick = { onUserClick(item.user) }
            )
        },
        content = {
            ThreadContent(
                title = item.title,
                abstractText = item.content,
                showTitle = item.mainPost == null && item.title.isNotBlank(),
                showAbstract = item.content.isNotBlank(),
                maxLines = 2,
                highlightKeywords = (searchKeyword?.split(" ") ?: emptyList()).toImmutableList(),
            )
            if (item.mainPost != null) {
                if (item.postInfo != null) {
                    QuotePostCard(
                        quotePostInfo = item.postInfo,
                        mainPost = item.mainPost,
                        onMainPostClick = onMainPostClick,
                        onClick = { onQuotePostClick(item.postInfo) },
                        medias = item.media.toImmutableList(),
                        keyword = searchKeyword
                    )
                } else {
                    MainPostCard(
                        mainPost = item.mainPost,
                        onClick = { onMainPostClick(item.mainPost) },
                        medias = item.media.toImmutableList(),
                        keyword = searchKeyword
                    )
                }
            } else {
                SearchMedia(medias = item.media.toImmutableList())
            }
            if (!hideForum && item.forumName.isNotEmpty()) {
                ForumInfoChip(
                    imageUriProvider = { item.forumInfo.avatar },
                    nameProvider = { item.forumName }
                ) {
                    onForumClick(item.forumInfo)
                }
            }
        },
        action = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadReplyBtn(
                    replyNum = item.postNum.toIntOrNull() ?: 0,
                    onClick = { onReplyClick(item) },
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = false,
                    agreeNum = item.likeNum.toIntOrNull() ?: 0,
                    modifier = Modifier.weight(1f)
                )

                ThreadShareBtn(
                    shareNum = item.shareNum.toLongOrNull() ?: 0L,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        onClick = { onClick(item) },
    )
}

@Composable
fun SearchMedia(
    medias: ImmutableList<SearchThreadBean.MediaInfo>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val pics = medias.filter { it.type == "pic" }
//    val video = medias.filter { it.type == "flash" }

    val picCount = remember(pics) {
        pics.size
    }
    val hasPhoto = remember(picCount) { picCount > 0 }
    val isSinglePhoto = remember(picCount) { picCount == 1 }
    val hideMedia = context.appPreferences.hideMedia

    val windowWidthSizeClass = BaseComposeActivity.LocalWindowSizeClass.current.widthSizeClass
    val singleMediaFraction = remember(windowWidthSizeClass) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact)
            1f
        else 0.5f
    }

    if (hasPhoto && !hideMedia) {
        val mediaWidthFraction = remember(isSinglePhoto, singleMediaFraction) {
            if (isSinglePhoto) singleMediaFraction else 1f
        }
        val mediaAspectRatio = remember(isSinglePhoto) {
            if (isSinglePhoto) 2f else 3f
        }
        val showMediaCount = remember(pics) { min(pics.size, 3) }
        val hasMoreMedia = remember(pics) { pics.size > 3 }
        val showMedias = remember(pics) { pics.subList(0, showMediaCount) }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(mediaWidthFraction)
                        .aspectRatio(mediaAspectRatio)
                        .clip(RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    showMedias.fastForEach { pic ->
                        NetworkImage(
                            imageUri = pic.bigPic ?: pic.smallPic ?: pic.waterPic ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentScale = ContentScale.Crop,
                            enablePreview = true
                        )
                    }
                }
                if (hasMoreMedia) {
                    MediaCountBadge(
                        count = medias.size,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onKeywordSubmit: (String) -> Unit = {},
    placeholder: @Composable () -> Unit = {},
    prependIcon: @Composable () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    showSubmitButton: Boolean = true,
) {
    val state = rememberTextFieldState(keyword)
    val externalKeyword by rememberUpdatedState(keyword)
    val change by rememberUpdatedState(onKeywordChange)
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(keyword) {
        if (state.text.toString() != keyword) state.setTextAndPlaceCursorAtEnd(keyword)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { if (it != externalKeyword) change(it) }
    }

    SearchBarDefaults.InputField(
        state = state,
        onSearch = onKeywordSubmit,
        expanded = focused,
        onExpandedChange = {},
        modifier = modifier.focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = placeholder,
        leadingIcon = prependIcon,
        trailingIcon = if (keyword.isEmpty()) null else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (focused || !showSubmitButton) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onKeywordChange("") }) {
                            M3Icon(ImageVector.vectorResource(R.drawable.ms_close), stringResource(R.string.button_clear))
                        }
                    }
                    if (showSubmitButton && keyword.isNotBlank()) {
                        IconButton(shapes = IconButtonDefaults.shapes(), onClick = { onKeywordSubmit(keyword) }) {
                            M3Icon(ImageVector.vectorResource(R.drawable.ms_search), stringResource(R.string.button_search))
                        }
                    }
                }
            }
        },
    )
}

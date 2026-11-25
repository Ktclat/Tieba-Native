package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.displayPortrait
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.ui.common.ProvideCommentLinkColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.Util

private val User.hidesCommentIdentity: Boolean get() = userhide != 0 || is_mask != 0

internal fun User.commentDisplayName(context: Context): String? =
    nameShow.trim().takeIf { it.isNotEmpty() }
        ?: if (hidesCommentIdentity) context.getString(R.string.comment_anonymous_user)
        else name.trim().takeIf { it.isNotEmpty() }

internal val User.commentAvatarPortrait: String? get() = displayPortrait.takeUnless { hidesCommentIdentity }

internal val User.commentMentionPortrait: String? get() = portrait.takeUnless { hidesCommentIdentity }

@Composable
internal fun CommentLayout(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideCommentLinkColor(background) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            header?.invoke()
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        }
    }
}

@Composable
internal fun CommentAuthorRow(
    action: (@Composable () -> Unit)?,
    author: @Composable () -> Unit,
) {
    if (action == null) {
        author()
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).align(Alignment.CenterVertically)) { author() }
        action()
    }
}

@Composable
internal fun CommentHeader(
    author: User?,
    threadAuthorId: Long?,
    time: Long?,
    floor: Int? = null,
    showMetadata: Boolean = true,
    authorHeading: Boolean = false,
    nameStyle: TextStyle = MaterialTheme.typography.titleSmall,
    compact: Boolean = false,
    recipient: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onUserClick: (User) -> Unit,
) {
    val hidden = author?.hidesCommentIdentity == true
    val authorId = author?.id?.takeIf { it > 0L && !hidden }
    val isThreadAuthor = authorId != null && authorId == threadAuthorId
    val openProfile: (() -> Unit)? = if (author != null && authorId != null) {
        { onUserClick(author) }
    } else null
    val context = LocalContext.current
    val name = if (hidden) AnnotatedString(author?.commentDisplayName(context).orEmpty()) else
        StringUtil.getUsernameAnnotatedString(context, author?.name.orEmpty().trim(), author?.nameShow?.trim()?.takeIf { it.isNotEmpty() })
    CommentHeader(
        name = name,
        showUnnamed = true,
        nameStyle = nameStyle,
        avatarSize = if (compact) Sizes.Tiny else Sizes.Small,
        authorHeading = authorHeading,
        portrait = author?.commentAvatarPortrait,
        time = time,
        onAuthorClick = openProfile,
        floor = floor,
        ipAddress = author?.ip_address,
        userLevel = if (compact) 0 else author?.level_id ?: 0,
        isLz = isThreadAuthor,
        bawuType = author?.bawuType?.takeUnless { compact },
        authorId = authorId,
        recipient = recipient,
        action = action,
        showMetadata = showMetadata,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommentHeader(
    name: AnnotatedString,
    portrait: String?,
    time: Long?,
    onAuthorClick: (() -> Unit)? = null,
    floor: Int? = null,
    ipAddress: String? = null,
    userLevel: Int = 0,
    isLz: Boolean = false,
    bawuType: String? = null,
    authorId: Long? = null,
    recipient: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    showMetadata: Boolean = true,
    avatarSize: Dp = Sizes.Small,
    showUnnamed: Boolean = false,
    authorHeading: Boolean = false,
    nameStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val hasAuthor = showUnnamed || name.isNotBlank() || !portrait.isNullOrBlank() || onAuthorClick != null ||
        userLevel > 0 || isLz || !bawuType.isNullOrBlank()
    if (!hasAuthor && recipient == null) {
        CommentAuthorRow(action) { if (showMetadata) CommentMetadata(time, floor, ipAddress) }
        return
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CommentAuthorRow(action) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (hasAuthor) {
                    val profile = if (onAuthorClick == null) Modifier else Modifier
                        .clickable(role = Role.Button, onClick = onAuthorClick)
                        .heightIn(min = 48.dp)
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically).then(profile),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CommentAvatar(name = name.text, portrait = portrait, authorId = authorId, size = avatarSize)
                        CommentAuthorName(
                            if (showUnnamed && name.isBlank()) AnnotatedString(stringResource(R.string.user)) else name,
                            userLevel = userLevel, isLz = isLz, bawuType = bawuType,
                            nameStyle = nameStyle, authorHeading = authorHeading,
                        )
                    }
                }
                if (recipient != null) Box(Modifier.align(Alignment.CenterVertically)) { recipient() }
            }
        }
        if (showMetadata) CommentMetadata(time, floor, ipAddress)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommentMetadata(time: Long?, floor: Int? = null, ipAddress: String? = null) {
    val ip = ipAddress?.trim().orEmpty()
    val postedAt = time?.takeIf { it > 0 }
    val visibleFloor = floor?.takeIf { it > 1 }
    if (postedAt == null && visibleFloor == null && ip.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    val secondary = scheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelMedium
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (postedAt != null) {
            Text(DateTimeUtils.getRelativeTimeString(LocalContext.current, postedAt), style = labelStyle, color = secondary)
        }
        if (visibleFloor != null) {
            Text(stringResource(R.string.tip_post_floor, visibleFloor), style = labelStyle, color = secondary)
        }
        if (ip.isNotEmpty()) {
            Text(stringResource(R.string.text_profile_ip_location, ip), style = labelStyle, color = secondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommentAuthorName(
    userName: AnnotatedString,
    userLevel: Int = 0,
    modifier: Modifier = Modifier,
    isLz: Boolean = false,
    bawuType: String? = null,
    nameStyle: TextStyle = MaterialTheme.typography.titleSmall,
    authorHeading: Boolean = false,
) {
    val bawu = bawuType?.trim().orEmpty()
    if (userName.isBlank() && userLevel <= 0 && !isLz && bawu.isEmpty()) return

    val scheme = MaterialTheme.colorScheme
    val textColor = scheme.onSurface
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (userName.isNotBlank()) {
            Text(
                userName,
                modifier = Modifier.align(Alignment.CenterVertically).then(
                    if (authorHeading) Modifier.semantics { heading() } else Modifier
                ),
                style = nameStyle, color = textColor,
            )
        }
        if (userLevel > 0) {
            CommentBadge(
                "Lv.$userLevel",
                Color(Util.getIconColorByLevel(userLevel.toString())),
                textColor,
            )
        }
        val authorLabel = stringResource(R.string.tip_lz)
        if (bawu.isNotEmpty() && !(isLz && bawu == authorLabel)) {
            CommentBadge(bawu, scheme.primary, textColor)
        }
        if (isLz) {
            Text(
                authorLabel,
                modifier = Modifier.background(scheme.tertiaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                color = scheme.onTertiaryContainer,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun CommentBadge(text: String, tint: Color, textColor: Color) {
    Text(
        text,
        modifier = Modifier.background(tint.copy(alpha = 0.18f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
    )
}

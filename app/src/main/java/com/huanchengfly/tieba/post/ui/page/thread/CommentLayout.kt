package com.huanchengfly.tieba.post.ui.page.thread

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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.Util

@Composable
internal fun CommentLayout(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        header?.invoke()
        content()
        if (action != null) {
            Box(Modifier.align(Alignment.End)) { action() }
        }
    }
}

@Composable
internal fun CommentHeader(
    author: User?,
    threadAuthorId: Long?,
    time: Long?,
    floor: Int? = null,
    onUserClick: (User) -> Unit,
) {
    val authorId = author?.id ?: 0L
    val isThreadAuthor = authorId > 0 && authorId == threadAuthorId
    val openProfile: (() -> Unit)? = if (author != null && authorId > 0) {
        { onUserClick(author) }
    } else null
    val name = StringUtil.getUsernameAnnotatedString(
        LocalContext.current,
        author?.name.orEmpty().trim(),
        author?.nameShow?.trim()?.takeIf { it.isNotEmpty() },
    )
    CommentHeader(
        name = name,
        portrait = author?.portrait,
        time = time,
        onAuthorClick = openProfile,
        floor = floor,
        ipAddress = author?.ip_address,
        userLevel = author?.level_id ?: 0,
        isLz = isThreadAuthor,
        bawuType = author?.bawuType,
    )
}

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
) {
    val avatar = portrait?.trim().orEmpty()
    val hasAuthor = name.isNotBlank() || avatar.isNotEmpty() || onAuthorClick != null ||
        userLevel > 0 || isLz || !bawuType.isNullOrBlank()
    if (!hasAuthor) {
        CommentMetadata(time, floor, ipAddress)
        return
    }

    val colors = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    val secondary = colors.textSecondary.takeOrElse { scheme.onSurfaceVariant }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val profileAction = if (onAuthorClick == null) Modifier else Modifier
            .clickable(role = Role.Button, onClick = onAuthorClick)
            .heightIn(min = 48.dp)
        Row(
            modifier = Modifier.fillMaxWidth().then(profileAction),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val avatarDescription = if (name.isBlank()) stringResource(R.string.user) else null
            if (avatar.isNotEmpty()) {
                Avatar(
                    data = StringUtil.getAvatarUrl(avatar), size = Sizes.Small,
                    contentDescription = avatarDescription,
                )
            } else {
                Icon(
                    Icons.Rounded.AccountCircle, contentDescription = avatarDescription,
                    modifier = Modifier.size(Sizes.Small), tint = secondary,
                )
            }
            CommentAuthorName(
                userName = name,
                userLevel = userLevel,
                modifier = Modifier.weight(1f),
                isLz = isLz,
                bawuType = bawuType,
            )
        }
        CommentMetadata(time, floor, ipAddress)
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
    val secondary = ExtendedTheme.colors.textSecondary.takeOrElse { scheme.onSurfaceVariant }
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
) {
    val bawu = bawuType?.trim().orEmpty()
    if (userName.isBlank() && userLevel <= 0 && !isLz && bawu.isEmpty()) return

    val colors = ExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    val textColor = colors.text.takeOrElse { scheme.onSurface }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (userName.isNotBlank()) {
            Text(
                userName, modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.titleSmall, color = textColor,
            )
        }
        if (userLevel > 0) {
            CommentBadge(
                "Lv.$userLevel",
                Color(Util.getIconColorByLevel(userLevel.toString())),
                textColor,
            )
        }
        if (bawu.isNotEmpty()) {
            CommentBadge(bawu, colors.primary.takeOrElse { scheme.primary }, textColor)
        }
        if (isLz) {
            CommentBadge(
                stringResource(R.string.tip_lz),
                colors.accent.takeOrElse { scheme.secondary },
                textColor,
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

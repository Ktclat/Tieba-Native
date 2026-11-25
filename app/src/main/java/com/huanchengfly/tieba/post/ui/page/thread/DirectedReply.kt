package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination

internal data class DirectedReply(val userId: Long?, val recipient: String, val body: AnnotatedString)

private val replyIntro = Regex("""回复[\t\p{Zs}]*""")
private val replySeparator = Regex("""[\t\p{Zs}]*[:：] ?""")
private val plainName = Regex("""@?[\p{L}\p{N}_][\p{L}\p{M}\p{N}_-]{0,23}""")
private val proseSubject = Regex("""如下|(?:这个|你的|我的)(?:问题|评论|消息)""")

// A type-4 user annotation alone is an ordinary mention. Only separate the explicit
// opening "回复 <user> :" from the display text. The original comment is kept for copying.
internal fun AnnotatedString.splitDirectedReply(): DirectedReply? {
    val start = replyIntro.matchAt(text, 0)?.value?.length ?: return null
    if (start >= length) return null
    // Some bodies put '@' in the preceding plain-text fragment instead of the user fragment.
    val lookupEnd = if (text[start] == '@') (start + 2).coerceAtMost(length) else start + 1
    val mention = getStringAnnotations("user", start, lookupEnd).singleOrNull()
    val id: Long?
    val end: Int
    if (mention != null) {
        if (mention.start !in start until lookupEnd || mention.end <= mention.start) return null
        id = mention.item.toLongOrNull()?.takeIf { it > 0 } ?: return null
        end = mention.end
    } else {
        // Plain-text protobuf replies have no profile target. Require a separate name token.
        if (start == "回复".length && text[start] != '@') return null
        val candidate = plainName.matchAt(text, start) ?: return null
        if (proseSubject.matches(candidate.value)) return null
        id = null
        end = candidate.range.last + 1
    }
    val name = text.substring(start, end).trim()
    if (name.removePrefix("@").isBlank() || '\n' in name || '\r' in name) return null
    val separator = replySeparator.matchAt(text, end) ?: return null
    val bodyStart = separator.range.last + 1
    if (getStringAnnotations(0, bodyStart).any { it != mention }) return null
    return DirectedReply(id, name, subSequence(bodyStart, length))
}

@Composable
internal fun ReplyRecipient(reply: DirectedReply) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.hint_reply, reply.recipient)
    val nameStart = label.lastIndexOf(reply.recipient)
    val userId = reply.userId
    val modifier = if (userId != null) {
        val navigator = LocalNavigator.current
        Modifier.clickable(
            onClickLabel = stringResource(R.string.open_reply_recipient_profile),
            role = Role.Button,
        ) { navigator.navigate(UserProfilePageDestination(userId)) }
    } else Modifier
    Text(
        text = buildAnnotatedString {
            append(label)
            if (nameStart >= 0) {
                val nameColor = if (userId != null) colors.primary else colors.onSurface
                addStyle(SpanStyle(color = nameColor), nameStart, nameStart + reply.recipient.length)
            }
        },
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
    )
}

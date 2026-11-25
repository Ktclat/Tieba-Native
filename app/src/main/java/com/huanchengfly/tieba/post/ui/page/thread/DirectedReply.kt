package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination

internal data class DirectedReply(val userId: Long, val recipient: String, val body: AnnotatedString)

private val replyIntro = Regex("""回复[\t\p{Zs}]*""")
private val replySeparator = Regex("""[\t\p{Zs}]*[:：] ?""")

// A type-4 user annotation alone is an ordinary mention. Only separate the explicit
// opening "回复 <user> :" from the display text. The original comment is kept for copying.
internal fun AnnotatedString.splitDirectedReply(): DirectedReply? {
    val start = replyIntro.matchAt(text, 0)?.value?.length ?: return null
    if (start >= length) return null
    // Some bodies put '@' in the preceding plain-text fragment instead of the user fragment.
    val lookupEnd = if (text[start] == '@') (start + 2).coerceAtMost(length) else start + 1
    val mention = getStringAnnotations("user", start, lookupEnd).singleOrNull() ?: return null
    if (mention.start !in start until lookupEnd || mention.end <= mention.start) return null
    val id = mention.item.toLongOrNull()?.takeIf { it > 0 } ?: return null
    val name = text.substring(start, mention.end).trim()
    if (name.removePrefix("@").isBlank() || '\n' in name || '\r' in name) return null
    val separator = replySeparator.matchAt(text, mention.end) ?: return null
    return DirectedReply(id, name, subSequence(separator.range.last + 1, length))
}

@Composable
internal fun ReplyRecipient(reply: DirectedReply) {
    val navigator = LocalNavigator.current
    Text(
        text = stringResource(R.string.hint_reply, reply.recipient),
        modifier = Modifier
            .clickable(onClickLabel = stringResource(R.string.open_reply_recipient_profile), role = Role.Button) {
                navigator.navigate(UserProfilePageDestination(reply.userId))
            }
            .padding(vertical = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

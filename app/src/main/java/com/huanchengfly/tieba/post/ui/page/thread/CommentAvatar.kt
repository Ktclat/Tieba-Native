package com.huanchengfly.tieba.post.ui.page.thread

import android.icu.text.BreakIterator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.compose.rememberAsyncImageState
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.StringUtil
import java.util.Locale

internal fun commentAvatarMonogram(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return ""
    val characters = BreakIterator.getCharacterInstance(Locale.ROOT)
    characters.setText(trimmed)
    return trimmed.substring(0, characters.next())
}

internal fun commentPortraitUrl(portrait: String?): String? =
    StringUtil.getAvatarUrl(portrait).ifEmpty { null }

internal fun commentAvatarPaletteIndex(name: String, imageUrl: String?, authorId: Long?): Int {
    val identity = authorId?.takeIf { it > 0 }?.toString() ?: imageUrl ?: name.trim()
    return Math.floorMod(identity.hashCode(), 3)
}

@Composable
internal fun CommentAvatar(
    name: String,
    portrait: String?,
    authorId: Long? = null,
    size: Dp = Sizes.Small,
) {
    val monogram = remember(name) { commentAvatarMonogram(name) }
    val imageUrl = remember(portrait) { commentPortraitUrl(portrait) }
    val scheme = MaterialTheme.colorScheme
    val (container, ink) = when (commentAvatarPaletteIndex(name, imageUrl, authorId)) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    val description = if (monogram.isEmpty()) stringResource(R.string.user) else null
    Box(
        Modifier.size(size).clip(CircleShape).background(container).clearAndSetSemantics {
            if (description != null) contentDescription = description
        },
        contentAlignment = Alignment.Center,
    ) {
        key(imageUrl) {
            val state = if (imageUrl != null) rememberAsyncImageState() else null
            val loaded = state?.result is DisplayResult.Success
            if (imageUrl != null && state != null) {
                val context = LocalContext.current
                val request = remember(context, imageUrl) { DisplayRequest(context, imageUrl) }
                AsyncImage(
                    request = request,
                    state = state,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (!loaded) {
                Box(
                    modifier = Modifier.matchParentSize().background(container),
                    contentAlignment = Alignment.Center,
                ) {
                    if (monogram.isEmpty()) {
                        Icon(
                            Icons.Rounded.Person, contentDescription = null,
                            modifier = Modifier.size(size * 0.6f), tint = ink,
                        )
                    } else {
                        val fontSize = with(LocalDensity.current) { (size * 0.48f).toSp() }
                        Text(
                            monogram, color = ink, style = MaterialTheme.typography.titleSmall,
                            fontSize = fontSize, lineHeight = fontSize, maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

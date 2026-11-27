package com.huanchengfly.tieba.post.ui.page.threadstore

import com.google.gson.JsonParseException
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFromStoreExtra
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.ramcosta.composedestinations.spec.Direction

internal data class FavoriteReadingPosition(val postId: Long, val floor: Long?, val onlyAuthor: Boolean) {
    companion object {
        fun fromHistory(extras: String?): FavoriteReadingPosition? {
            val visit = try {
                extras?.fromJson<ThreadHistoryInfoBean?>()
            } catch (_: JsonParseException) {
                null
            } ?: return null
            val postId = visit.pid?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            return FavoriteReadingPosition(postId, visit.floor?.toLongOrNull()?.takeIf { it > 0 }, visit.isSeeLz)
        }
    }
}

internal enum class FavoriteFilter(val label: Int) {
    ALL(R.string.favorite_filter_all),
    IN_PROGRESS(R.string.favorite_filter_partial),
    HAS_PROGRESS(R.string.favorite_filter_has_progress),
    NO_PROGRESS(R.string.favorite_filter_no_progress);

    fun matches(info: ThreadStoreInfo, progress: FavoriteReadingPosition?): Boolean = when (this) {
        ALL -> true
        HAS_PROGRESS -> progress != null
        NO_PROGRESS -> progress == null
        IN_PROGRESS -> {
            val lastPost = info.maxPid?.toLongOrNull()?.takeIf { it > 0 }
            val lastFloor = info.postNo?.toLongOrNull()?.takeIf { it > 0 }
            when {
                progress == null || progress.postId == lastPost -> false
                progress.floor != null && lastFloor != null -> progress.floor < lastFloor
                lastPost != null -> progress.postId < lastPost
                else -> false
            }
        }
    }
}

internal fun ThreadStoreInfo.matchesFavoriteQuery(query: String): Boolean {
    if (query.isEmpty()) return true
    val forum = forumName.orEmpty()
    return title.orEmpty().contains(query, ignoreCase = true) ||
        author?.name?.contains(query, ignoreCase = true) == true ||
        author?.nameShow?.contains(query, ignoreCase = true) == true ||
        (forum.isNotBlank() && (forum + "吧").contains(query, ignoreCase = true))
}

// Older installations may only have the server's bookmark. A default bookmark on the
// first post does not establish that the reader has made progress in the thread.
internal fun ThreadStoreInfo.serverReadingPosition(onlyAuthor: Boolean): FavoriteReadingPosition? {
    val mark = validServerMark() ?: return null
    val first = minPid?.toLongOrNull()?.takeIf { it > 0 } ?: return null
    val last = maxPid?.toLongOrNull()?.takeIf { it > 0 }
    if (mark == first) return null
    return FavoriteReadingPosition(mark, if (mark == last) postNo?.toLongOrNull()?.takeIf { it > 0 } else null, onlyAuthor)
}

private fun ThreadStoreInfo.validServerMark(): Long? {
    val mark = markPid?.toLongOrNull()?.takeIf { it > 0 } ?: return null
    val first = minPid?.toLongOrNull()?.takeIf { it > 0 }
    val last = maxPid?.toLongOrNull()?.takeIf { it > 0 }
    return mark.takeUnless { (first != null && it < first) || (last != null && it > last) }
}

internal fun ThreadStoreInfo.resumePostId(position: FavoriteReadingPosition?): Long =
    position?.postId ?: validServerMark() ?: 0L

internal fun ThreadStoreInfo.threadDestination(
    position: FavoriteReadingPosition?,
    defaultOnlyAuthor: Boolean,
    descending: Boolean,
): Direction? {
    val id = threadId?.toLongOrNull()?.takeIf { it > 0 } ?: return null
    if (isDeleted == "1") return null
    val maxPost = maxPid?.toLongOrNull()?.takeIf { it > 0 }
    val maxFloor = postNo?.toIntOrNull()?.takeIf { it > 0 }
    return ThreadPageDestination(
        threadId = id,
        postId = resumePostId(position),
        seeLz = position?.onlyAuthor ?: defaultOnlyAuthor,
        sortType = if (descending) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT,
        from = ThreadPageFrom.FROM_STORE,
        extra = if (maxPost != null && maxFloor != null) ThreadPageFromStoreExtra(maxPost, maxFloor) else null,
    )
}

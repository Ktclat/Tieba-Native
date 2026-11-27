package com.huanchengfly.tieba.post.ui.page.thread.tabs

import android.os.Parcelable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.FirstPostKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ThreadTab(
    val threadId: Long,
    val title: String = "",
    val forumName: String = "",
    val forumId: Long? = null,
    val postId: Long = 0,
    val floor: Int = 0,
    val onlyAuthor: Boolean = false,
    val sortType: Int = 0,
    val scrollOffset: Int = 0,
    val authorName: String = "",
    val authorAlias: String = "",
    val generation: String = "",
    val latest: Boolean = false,
) : Parcelable {
    fun direction() = ThreadPageDestination(
        threadId = threadId, postId = postId, forumId = forumId, seeLz = onlyAuthor,
        sortType = sortType, from = ThreadPageFrom.FROM_TABS,
    )

    fun visibleWith(rules: LocalBlockRules) =
        !rules.blocksForum(forumName) && !rules.blocksUser(authorName, authorAlias)
}

internal class ThreadTabs(initialTabs: List<ThreadTab> = emptyList()) {
    var entries by mutableStateOf(initialTabs.filter { it.threadId > 0 }.distinctBy { it.threadId }
        .map { if (it.generation.isEmpty()) it.copy(generation = UUID.randomUUID().toString()) else it })
        private set

    fun find(id: Long) = entries.firstOrNull { it.threadId == id }

    fun activate(tab: ThreadTab): ThreadTab {
        val current = find(tab.threadId)
        val entry = current ?: tab.copy(generation = UUID.randomUUID().toString())
        entries = listOf(entry) + entries.filterNot { it.threadId == tab.threadId }
        return entry
    }

    private fun update(id: Long, generation: String?, change: (ThreadTab) -> ThreadTab) {
        val index = entries.indexOfFirst { it.threadId == id && it.generation == generation }
        if (index < 0) return
        val updated = change(entries[index])
        if (updated != entries[index]) entries = entries.toMutableList().also { it[index] = updated }
    }

    fun describe(tab: ThreadTab, generation: String?) = update(tab.threadId, generation) {
        it.copy(
            title = tab.title.ifBlank { it.title }, forumName = tab.forumName.ifBlank { it.forumName },
            forumId = tab.forumId ?: it.forumId, authorName = tab.authorName.ifBlank { it.authorName },
            authorAlias = tab.authorAlias.ifBlank { it.authorAlias }, onlyAuthor = tab.onlyAuthor, sortType = tab.sortType,
        )
    }

    fun mark(id: Long, generation: String?, position: ThreadFloorKey, offset: Int) = update(id, generation) {
        it.copy(postId = position.postId, floor = position.floor, scrollOffset = offset, latest = position.latest)
    }

    fun close(id: Long) { entries = entries.filterNot { it.threadId == id } }

    // Bound Android's saved-state bundle. Open tabs in the running activity have no such limit.
    internal fun savedEntries() = ArrayList(entries.take(100).map {
        it.copy(title = it.title.take(160), forumName = it.forumName.take(96),
            authorName = it.authorName.take(96), authorAlias = it.authorAlias.take(96))
    })

    companion object {
        val saver = Saver<ThreadTabs, ArrayList<ThreadTab>>(
            save = { it.savedEntries() },
            restore = { ThreadTabs(it) },
        )
    }
}

internal val LocalThreadTabs = staticCompositionLocalOf<ThreadTabs?> { null }

@Composable
internal fun ProvideThreadTabs(state: ThreadTabs, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThreadTabs provides state, content = content)
}

@Composable
internal fun rememberThreadTabs(): ThreadTabs = rememberSaveable(saver = ThreadTabs.saver) { ThreadTabs() }

private fun LazyListState.tabAnchor(firstFloor: ThreadFloorKey?): Pair<ThreadFloorKey, Int>? {
    val layout = layoutInfo
    val first = layout.visibleItemsInfo.firstOrNull {
        it.size > 0 && it.offset + it.size > layout.viewportStartOffset &&
            (it.key is ThreadFloorKey || (it.key == FirstPostKey && firstFloor != null))
    } ?: return null
    val floor = first.key as? ThreadFloorKey ?: firstFloor ?: return null
    return floor to (layout.viewportStartOffset - first.offset)
}

// The topic and sticky filter are followed by an optional descending latest block,
// the previous-page control, the ordinary replies and the ascending latest block.
internal fun threadTabAnchorIndex(
    postId: Long,
    topicPostId: Long?,
    replyIds: List<Long>,
    latestIds: List<Long> = emptyList(),
    descending: Boolean = false,
    preferLatest: Boolean = false,
): Int? {
    if (postId <= 0) return null
    if (postId == topicPostId) return 0
    val replyStart = 3 + if (descending && latestIds.isNotEmpty()) latestIds.size + 1 else 0
    val latestStart = if (descending) 2 else 4 + replyIds.size
    val reply = replyIds.indexOf(postId).takeIf { it >= 0 }?.let { replyStart + it }
    val latest = latestIds.indexOf(postId).takeIf { it >= 0 }?.let { latestStart + it }
    return if (preferLatest) latest ?: reply else reply ?: latest
}

internal suspend fun LazyListState.restoreThreadTabAnchor(targetIndex: Int, scrollOffset: Int) {
    snapshotFlow { layoutInfo.totalItemsCount }.first { it > targetIndex }
    scrollToItem(targetIndex, scrollOffset)
}

@Composable
internal fun TrackThreadTab(
    tab: ThreadTab,
    list: LazyListState,
    firstFloor: ThreadFloorKey?,
    recordPosition: Boolean = true,
    onExpired: () -> Unit,
) {
    val tabs = LocalThreadTabs.current ?: return
    if (tab.threadId <= 0) return
    var generation by rememberSaveable(tab.threadId) { mutableStateOf<String?>(null) }
    val latest by rememberUpdatedState(tab)
    val topic by rememberUpdatedState(firstFloor)
    val canRecord by rememberUpdatedState(recordPosition)
    val expire by rememberUpdatedState(onExpired)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun capture(anchor: Pair<ThreadFloorKey, Int>?) {
        if (anchor != null && canRecord) tabs.mark(tab.threadId, generation, anchor.first, anchor.second)
    }
    LifecycleResumeEffect(tabs, tab.threadId) {
        val existing = tabs.find(tab.threadId)
        if (generation != null && generation != existing?.generation) {
            // A covered route can return to the foreground after its tab was explicitly closed.
            expire()
        } else {
            generation = tabs.activate(latest).generation
            tabs.describe(latest, generation)
        }
        onPauseOrDispose { capture(list.tabAnchor(topic)) }
    }
    SideEffect {
        if (lifecycle.currentState == Lifecycle.State.RESUMED) tabs.describe(tab, generation)
    }
    LaunchedEffect(tabs, tab.threadId, list, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { if (list.isScrollInProgress || !canRecord) null else list.tabAnchor(topic) }
                .collect(::capture)
        }
    }
}

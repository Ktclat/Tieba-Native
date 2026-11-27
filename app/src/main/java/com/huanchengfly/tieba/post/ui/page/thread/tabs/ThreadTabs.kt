package com.huanchengfly.tieba.post.ui.page.thread.tabs

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.FirstPostKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFromTabsExtra
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
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
    val savedOrder: Long = 0,
) {
    fun direction() = ThreadPageDestination(
        threadId = threadId, postId = postId, forumId = forumId, seeLz = onlyAuthor,
        sortType = sortType, from = ThreadPageFrom.FROM_TABS,
        extra = ThreadPageFromTabsExtra(scrollOffset, latest, floor),
    )

    fun visibleWith(rules: LocalBlockRules) =
        !rules.blocksForum(forumName) && !rules.blocksUser(authorName, authorAlias)
}

// All mutations are issued by the foreground UI. Restoring the preferences happens off main.
internal class ThreadTabs(private val archive: ThreadTabArchive? = null) {
    var entries by mutableStateOf(emptyList<ThreadTab>())
        private set
    var ready by mutableStateOf(archive == null)
        private set

    private val loading = Mutex()

    suspend fun load() {
        loading.withLock {
            if (!ready && archive != null) {
                entries = withContext(Dispatchers.IO) { archive.read() }
                ready = true
            }
        }
    }

    fun find(id: Long) = entries.firstOrNull { it.threadId == id }

    // Only the New Tab action creates a record or moves it to the front.
    fun save(tab: ThreadTab) {
        if (!ready || tab.threadId <= 0) return
        val current = find(tab.threadId)
        val position = if (tab.floor == 0 && current != null) current else tab
        val entry = tab.copy(
            title = tab.title.ifBlank { current?.title.orEmpty() },
            forumName = tab.forumName.ifBlank { current?.forumName.orEmpty() },
            forumId = tab.forumId ?: current?.forumId,
            authorName = tab.authorName.ifBlank { current?.authorName.orEmpty() },
            authorAlias = tab.authorAlias.ifBlank { current?.authorAlias.orEmpty() },
            postId = position.postId,
            floor = position.floor,
            scrollOffset = position.scrollOffset,
            latest = position.latest,
            generation = current?.generation ?: UUID.randomUUID().toString(),
            savedOrder = (entries.firstOrNull()?.savedOrder ?: 0) + 1,
        )
        archive?.put(entry)
        entries = listOf(entry) + entries.filterNot { it.threadId == tab.threadId }
    }

    private fun update(id: Long, generation: String, change: (ThreadTab) -> ThreadTab) {
        val index = entries.indexOfFirst { it.threadId == id && it.generation == generation }
        if (index < 0) return
        val updated = change(entries[index])
        if (updated != entries[index]) {
            archive?.put(updated)
            entries = entries.toMutableList().also { it[index] = updated }
        }
    }

    fun describe(tab: ThreadTab, generation: String) = update(tab.threadId, generation) {
        it.copy(
            title = tab.title.ifBlank { it.title }, forumName = tab.forumName.ifBlank { it.forumName },
            forumId = tab.forumId ?: it.forumId, authorName = tab.authorName.ifBlank { it.authorName },
            authorAlias = tab.authorAlias.ifBlank { it.authorAlias }, onlyAuthor = tab.onlyAuthor, sortType = tab.sortType,
        )
    }

    fun mark(id: Long, generation: String, position: ThreadFloorKey, offset: Int) = update(id, generation) {
        it.copy(postId = position.postId, floor = position.floor, scrollOffset = offset, latest = position.latest)
    }

    fun close(id: Long) {
        if (find(id) == null) return
        archive?.remove(id)
        entries = entries.filterNot { it.threadId == id }
    }

    fun closeAll() {
        if (!ready) return
        archive?.clear()
        entries = emptyList()
    }
}

internal val LocalThreadTabs = staticCompositionLocalOf<ThreadTabs?> { null }

@Composable
internal fun ProvideThreadTabs(state: ThreadTabs, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThreadTabs provides state, content = content)
}

@Composable
internal fun rememberThreadTabs(accountId: String?): ThreadTabs {
    val app = LocalContext.current.applicationContext
    val tabs = remember(app, accountId) { ThreadTabArchive.session(app, accountId) }
    LaunchedEffect(tabs) { tabs.load() }
    return tabs
}

private fun LazyListState.tabAnchor(firstFloor: ThreadFloorKey?): Pair<ThreadFloorKey, Int>? {
    val layout = layoutInfo
    val first = layout.visibleItemsInfo.firstOrNull {
        it.size > 0 && it.offset + it.size > layout.viewportStartOffset &&
            (it.key is ThreadFloorKey || (it.key == FirstPostKey && firstFloor != null))
    } ?: return null
    val floor = first.key as? ThreadFloorKey ?: firstFloor ?: return null
    return floor to (layout.viewportStartOffset - first.offset)
}

internal fun ThreadTab.withReadingPosition(list: LazyListState, firstFloor: ThreadFloorKey?): ThreadTab {
    val (position, offset) = list.tabAnchor(firstFloor) ?: return this
    return copy(postId = position.postId, floor = position.floor, scrollOffset = offset, latest = position.latest)
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
) {
    val tabs = LocalThreadTabs.current ?: return
    val savedGeneration by remember(tabs, tab.threadId) {
        derivedStateOf { tabs.find(tab.threadId)?.generation }
    }
    val generation = savedGeneration ?: return
    val latest by rememberUpdatedState(tab)
    val topic by rememberUpdatedState(firstFloor)
    val canRecord by rememberUpdatedState(recordPosition)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun capture(anchor: Pair<ThreadFloorKey, Int>?) {
        if (anchor != null && canRecord) tabs.mark(tab.threadId, generation, anchor.first, anchor.second)
    }
    LifecycleResumeEffect(tabs, tab.threadId, generation) {
        if (canRecord) tabs.describe(latest, generation)
        onPauseOrDispose { capture(list.tabAnchor(topic)) }
    }
    SideEffect {
        if (recordPosition && lifecycle.currentState == Lifecycle.State.RESUMED) tabs.describe(tab, generation)
    }
    LaunchedEffect(tabs, tab.threadId, generation, list, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { if (list.isScrollInProgress || !canRecord) null else list.tabAnchor(topic) }
                .collect(::capture)
        }
    }
}

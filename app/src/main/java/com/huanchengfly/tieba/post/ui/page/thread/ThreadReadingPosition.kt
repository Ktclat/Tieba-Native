package com.huanchengfly.tieba.post.ui.page.thread

import android.os.Parcelable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

internal const val FirstPostKey = "FirstPost"

@Parcelize
internal data class ThreadFloorKey(
    val postId: Long,
    val floor: Int,
    val latest: Boolean = false,
) : Parcelable

internal fun LazyListState.lastVisibleFloor(firstPost: ThreadFloorKey?): ThreadFloorKey? {
    val visibleItems = layoutInfo.visibleItemsInfo
    for (index in visibleItems.indices.reversed()) {
        val item = visibleItems[index]
        if (item.size == 0) continue
        val key = item.key
        if (key is ThreadFloorKey && key.postId > 0L) return key
        if (key == FirstPostKey) return firstPost
    }
    return null
}

@Composable
internal fun TrackThreadReadingPosition(
    threadId: Long,
    list: LazyListState,
    firstPost: ThreadFloorKey?,
    onRead: (ThreadFloorKey) -> Unit,
) {
    val currentFirstPost by rememberUpdatedState(firstPost)
    val record by rememberUpdatedState(onRead)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    DisposableEffect(threadId, list, lifecycle) {
        var lastPostId: Long? = null
        fun save(position: ThreadFloorKey?) {
            if (position != null && position.postId != lastPostId) {
                record(position)
                lastPostId = position.postId
            }
        }

        val job = scope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                snapshotFlow {
                    if (list.isScrollInProgress) null else list.lastVisibleFloor(currentFirstPost)
                }.collect(::save)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) save(list.lastVisibleFloor(currentFirstPost))
        }
        lifecycle.addObserver(observer)
        onDispose {
            job.cancel()
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                save(list.lastVisibleFloor(currentFirstPost))
            }
        }
    }
}

internal suspend fun LazyListState.scrollToDiscussionItem(index: Int) {
    // Animate within one viewport, even when the destination is hundreds of floors away.
    val window = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    val distance = index - firstVisibleItemIndex
    if (distance > window || distance < -window) {
        scrollToItem(if (distance > 0) index - window else index + window)
    }
    animateScrollToItem(index)
}

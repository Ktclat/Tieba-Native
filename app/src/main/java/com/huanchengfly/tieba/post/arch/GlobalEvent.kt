package com.huanchengfly.tieba.post.arch

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

sealed interface GlobalEvent : UiEvent {
    data object AccountSwitched : GlobalEvent

    data object ScrollToTop : GlobalEvent

    data class Refresh(val key: String) : GlobalEvent

    data class StartSelectImages(
        val id: String,
        val maxCount: Int,
        val mediaType: PickMediasRequest.MediaType
    ) : GlobalEvent

    data class SelectedImages(
        val id: String,
        val images: List<Uri>,
    ) : GlobalEvent

    data class ReplySuccess(
        val threadId: Long,
        val newPostId: Long,
        val postId: Long? = null,
        val subPostId: Long? = null,
        val newSubPostId: Long? = null,
    ) : GlobalEvent

    data class CommentVoteConfirmed(
        val threadId: Long,
        val postId: Long,
        val subPostId: Long?,
        val hasAgree: Boolean,
    ) : GlobalEvent

    data class StartActivityForResult(
        val requesterId: String,
        val intent: Intent,
    ) : GlobalEvent

    data class ActivityResult(
        val requesterId: String,
        val resultCode: Int,
        val intent: Intent?,
    ) : GlobalEvent
}

private val globalEventSharedFlow: MutableSharedFlow<UiEvent> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    MutableSharedFlow(0, 2, BufferOverflow.DROP_OLDEST)
}

val GlobalEventFlow = globalEventSharedFlow.asSharedFlow()

fun CoroutineScope.emitGlobalEvent(event: UiEvent) {
    launch {
        globalEventSharedFlow.emit(event)
    }
}

suspend fun emitGlobalEventSuspend(event: UiEvent) {
    globalEventSharedFlow.emit(event)
}

inline fun <reified Event : UiEvent> CoroutineScope.onGlobalEvent(
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
): Job {
    return launch {
        GlobalEventFlow
            .filterIsInstance<Event>()
            .filter {
                filter(it)
            }
            .cancellable()
            .collect { listener(it) }
    }
}

@Composable
inline fun <reified Event : UiEvent> onGlobalEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    noinline filter: (Event) -> Boolean = { true },
    noinline listener: suspend (Event) -> Unit,
) {
    val currentFilter = rememberUpdatedState(filter)
    val currentListener = rememberUpdatedState(listener)
    DisposableEffect(coroutineScope, Event::class) {
        val job = coroutineScope.onGlobalEvent<Event>(
            filter = { event -> currentFilter.value(event) },
            listener = { event -> currentListener.value(event) },
        )
        onDispose {
            job.cancel()
        }
    }
}

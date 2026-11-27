package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import com.huanchengfly.tieba.post.utils.AccountUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class ThreadStoreViewModel @Inject constructor() : ViewModel() {
    private val state = MutableStateFlow(ThreadStoreUiState())
    val uiState = state.asStateFlow()
    private val changes = Mutex()
    private val deleteEvents = Channel<Throwable?>(Channel.BUFFERED)
    val deleteResults = deleteEvents.receiveAsFlow()

    private val account = AccountUtil.getUid()
    private fun isCurrentAccount() = account != null && account == AccountUtil.getUid()

    fun refresh() {
        if (state.value.isRefreshing) return
        viewModelScope.launch {
            changes.withLock { load(refresh = true) }
        }
    }

    fun loadMore() {
        val request = state.value
        if (request.isRefreshing || request.isLoadingMore || !request.hasMore) return
        viewModelScope.launch {
            changes.withLock {
                val now = state.value
                if (now.nextOffset == request.nextOffset && now.hasMore) load(refresh = false)
            }
        }
    }

    private suspend fun load(refresh: Boolean) {
        if (!isCurrentAccount()) return
        val before = state.value
        state.value = before.copy(isRefreshing = refresh, isLoadingMore = !refresh, error = null)
        try {
            val offset = if (refresh) 0 else before.nextOffset
            val response = TiebaApi.getInstance().threadStoreAtOffsetFlow(offset).first()
            if (!isCurrentAccount()) {
                state.value = ThreadStoreUiState()
                return
            }
            state.value = before.withPage(response.favoriteRows(), refresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.value = before.withLoadError(e, refresh)
        }
    }

    fun delete(threadId: String) {
        viewModelScope.launch {
            changes.withLock {
                try {
                    if (!isCurrentAccount()) return@withLock
                    val result = TiebaApi.getInstance().removeStoreFlow(threadId).first()
                    check(result.errorCode == 0) { result.errorMsg }
                    state.value = state.value.withoutFavorite(threadId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    deleteEvents.send(e)
                    return@withLock
                }
                deleteEvents.send(null)
            }
        }
    }
}

internal fun ThreadStoreBean.favoriteRows(): List<ThreadStoreInfo> {
    val codes = listOfNotNull(errorCode, error?.errorCode).filter(String::isNotBlank)
    val failing = codes.firstOrNull { it != "0" }
    val message = error?.errorMsg?.takeIf(String::isNotBlank)
    check(failing == null && !(codes.isEmpty() && message != null)) { message ?: "加载收藏失败 ($failing)" }
    return storeThread.orEmpty()
}

data class ThreadStoreUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
    val data: List<ThreadStoreInfo> = emptyList(),
    val error: Throwable? = null,
    val retryRefresh: Boolean = false,
    val seenPages: Set<Set<String>> = emptySet(),
) {
    internal fun withLoadError(failure: Exception, refresh: Boolean) =
        copy(isRefreshing = false, isLoadingMore = false, error = failure, retryRefresh = refresh)

    internal fun withPage(page: List<ThreadStoreInfo>, refresh: Boolean): ThreadStoreUiState {
        val rows = LinkedHashMap<String, ThreadStoreInfo>()
        if (!refresh) data.associateByTo(rows) { it.threadId }
        page.filter { (it.threadId?.toLongOrNull() ?: 0) > 0 }.associateByTo(rows) { it.threadId }
        val ids = page.mapTo(mutableSetOf()) { it.threadId }
        val previousPages = if (refresh) emptySet() else seenPages
        return ThreadStoreUiState(
            nextOffset = (if (refresh) 0 else nextOffset) + page.size,
            hasMore = ids.isNotEmpty() && ids !in previousPages,
            data = rows.values.toList(),
            seenPages = if (ids.isEmpty()) previousPages else previousPages + setOf(ids),
        )
    }

    internal fun withoutFavorite(id: String): ThreadStoreUiState {
        if (data.none { it.threadId == id }) return this
        // Deleting an already fetched item moves every following server offset back by one.
        return copy(data = data.filterNot { it.threadId == id }, nextOffset = (nextOffset - 1).coerceAtLeast(0))
    }
}

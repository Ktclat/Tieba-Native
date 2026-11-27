package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.HistoryCursor
import com.huanchengfly.tieba.post.utils.HistoryUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

abstract class HistoryListViewModel(private val type: Int) : ViewModel() {
    private val state = MutableStateFlow(HistoryListUiState())
    val uiState = state.asStateFlow()
    private val deleteEvents = Channel<HistoryListUiEvent.Delete>(Channel.BUFFERED)
    val events = deleteEvents.receiveAsFlow()

    private val operations = Mutex()
    private var reader: Job? = null
    private var next: HistoryCursor? = null

    fun refresh(query: String) {
        reader?.cancel()
        next = null
        state.value = HistoryListUiState(query = query.trim(), isRefreshing = true)
        readPage(null)
    }

    fun loadMore() {
        val cursor = next ?: return
        if (state.value.isRefreshing || state.value.isLoadingMore) return
        state.value = state.value.copy(isLoadingMore = true, error = null)
        readPage(cursor)
    }

    private fun readPage(after: HistoryCursor?) {
        val query = state.value.query
        reader = viewModelScope.launch {
            // Complete each read and publish its rows before applying a queued deletion.
            operations.withLock {
                try {
                    val page = withContext(Dispatchers.IO) { HistoryUtil.search(type, query, after) }
                    val old = state.value
                    next = page.next
                    state.value = old.copy(
                        rows = old.rows + page.rows,
                        hasMore = next != null,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = null,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    state.value = state.value.copy(isRefreshing = false, isLoadingMore = false, error = error)
                }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            operations.withLock {
                try {
                    withContext(Dispatchers.IO) { HistoryUtil.delete(type, id) }
                    val old = state.value
                    val remaining = old.rows.filterNot { it.id == id }
                    state.value = old.copy(rows = remaining)
                    deleteEvents.trySend(HistoryListUiEvent.Delete.Success)
                    if (remaining.isEmpty() && old.hasMore) loadMore()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    deleteEvents.trySend(HistoryListUiEvent.Delete.Failure(error.getErrorMessage()))
                }
            }
        }
    }
}

@HiltViewModel
class ThreadHistoryListViewModel @Inject constructor() : HistoryListViewModel(HistoryUtil.TYPE_THREAD)

@HiltViewModel
class ForumHistoryListViewModel @Inject constructor() : HistoryListViewModel(HistoryUtil.TYPE_FORUM)

data class HistoryListUiState(
    val query: String = "",
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val rows: List<History> = emptyList(),
    val error: Throwable? = null,
)

sealed interface HistoryListUiEvent {
    sealed interface Delete : HistoryListUiEvent {
        data object Success : Delete
        data class Failure(val errorMsg: String) : Delete
    }
}

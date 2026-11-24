package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val SUBMITTED_SEARCH = "forum-submitted-search"

internal fun SavedStateHandle.saveForumSearch(request: ForumSearchPostUiIntent.Refresh) {
    this[SUBMITTED_SEARCH] = request.takeIf { it.keyword.isNotBlank() }?.copy(recordHistory = false)
}

internal fun SavedStateHandle.restoreForumSearch(forumName: String): ForumSearchPostUiIntent.Refresh? =
    get<ForumSearchPostUiIntent.Refresh>(SUBMITTED_SEARCH)?.takeIf { it.forumName == forumName }

@HiltViewModel
class ForumSearchPostViewModel @Inject constructor(private val savedState: SavedStateHandle) :
    BaseViewModel<ForumSearchPostUiIntent, ForumSearchPostPartialChange, ForumSearchPostUiState, UiEvent>() {
    override fun createInitialState(): ForumSearchPostUiState = ForumSearchPostUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<ForumSearchPostUiIntent, ForumSearchPostPartialChange, ForumSearchPostUiState> =
        ForumSearchPostPartialChangeProducer(restore = { forumName ->
            savedState.restoreForumSearch(forumName)
        })

    fun requestSearch(request: ForumSearchPostUiIntent.Refresh) {
        savedState.saveForumSearch(request)
        send(request)
    }

    override fun dispatchEvent(partialChange: ForumSearchPostPartialChange): UiEvent? =
        when (partialChange) {
            is ForumSearchPostPartialChange.DeleteHistory.Failure -> CommonUiEvent.Toast(
                App.INSTANCE.getString(
                    R.string.toast_delete_failure,
                    partialChange.error.getErrorMessage()
                )
            )

            is ForumSearchPostPartialChange.ClearHistory.Success -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_clear_success)
            )

            is ForumSearchPostPartialChange.ClearHistory.Failure -> CommonUiEvent.Toast(
                App.INSTANCE.getString(
                    R.string.toast_clear_failure,
                    partialChange.error.getErrorMessage()
                )
            )

            else -> null
        }

    internal class ForumSearchPostPartialChangeProducer(
        private val restore: (String) -> ForumSearchPostUiIntent.Refresh? = { null },
        private val search: (ForumSearchPostUiIntent.Refresh, Int) -> Flow<SearchThreadBean> = { query, page ->
            TiebaApi.getInstance().searchPostFlow(
                query.keyword, query.forumName, query.forumId, query.sortType, query.filterType, page
            )
        },
    ) :
        PartialChangeProducer<ForumSearchPostUiIntent, ForumSearchPostPartialChange, ForumSearchPostUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<ForumSearchPostUiIntent>): Flow<ForumSearchPostPartialChange> =
            merge(
                historyChanges(intentFlow),
                searchChanges(intentFlow),
            )

        @OptIn(ExperimentalCoroutinesApi::class)
        fun searchChanges(intentFlow: Flow<ForumSearchPostUiIntent>): Flow<ForumSearchPostPartialChange> =
            intentFlow.mapNotNull {
                when (it) {
                    is ForumSearchPostUiIntent.Refresh -> it.copy(keyword = it.keyword.trim())
                    is ForumSearchPostUiIntent.Init -> restore(it.forumName)
                    else -> null
                }
            }.flatMapLatest { query ->
                if (query.keyword.isEmpty()) return@flatMapLatest flowOf<ForumSearchPostPartialChange>(ForumSearchPostPartialChange.Reset)
                var nextPage: Int? = 1
                intentFlow.filterIsInstance<ForumSearchPostUiIntent.LoadMore>()
                    .filter {
                        it.keyword == query.keyword && it.forumName == query.forumName && it.forumId == query.forumId &&
                            it.sortType == query.sortType && it.filterType == query.filterType
                    }
                    .map { it.page }
                    .conflate()
                    .onStart { emit(1) }
                    .filter { it == nextPage }
                    .transform<Int, ForumSearchPostPartialChange> { page ->
                        emit(if (page == 1) ForumSearchPostPartialChange.Refresh.Start(
                            query.keyword, query.sortType, query.filterType
                        ) else ForumSearchPostPartialChange.LoadMore.Start)
                        emitAll(search(query, page).map<SearchThreadBean, ForumSearchPostPartialChange> {
                            val hasMore = it.data.hasMore == 1
                            nextPage = if (hasMore) page + 1 else null
                            val rows = it.data.postList.toImmutableList()
                            if (page == 1) ForumSearchPostPartialChange.Refresh.Success(
                                query.keyword, rows, hasMore, query.sortType, query.filterType
                            ) else ForumSearchPostPartialChange.LoadMore.Success(rows, hasMore, page)
                        }.catch { error ->
                            emit(if (page == 1) ForumSearchPostPartialChange.Refresh.Failure(error)
                                else ForumSearchPostPartialChange.LoadMore.Failure(error))
                        })
                    }
            }

        // Process history operations in intent order, independently of network requests.
        @OptIn(ExperimentalCoroutinesApi::class)
        fun historyChanges(intentFlow: Flow<ForumSearchPostUiIntent>): Flow<ForumSearchPostPartialChange> =
            intentFlow.flatMapConcat { intent ->
                when (intent) {
                    is ForumSearchPostUiIntent.Init -> loadHistory {
                        ForumSearchHistoryStore.list(intent.forumName)
                    }
                    is ForumSearchPostUiIntent.Refresh -> if (intent.recordHistory && intent.keyword.isNotBlank()) {
                        loadHistory { ForumSearchHistoryStore.record(intent.forumName, intent.keyword) }
                    } else emptyFlow()
                    is ForumSearchPostUiIntent.DeleteHistory -> intent.deleteHistory()
                    is ForumSearchPostUiIntent.ClearHistory -> intent.clearHistory()
                    is ForumSearchPostUiIntent.LoadMore -> emptyFlow()
                }
            }

        private fun loadHistory(read: () -> List<SearchPostHistory>): Flow<ForumSearchPostPartialChange.Init> =
            flow<ForumSearchPostPartialChange.Init> {
                emit(ForumSearchPostPartialChange.Init.Success(read()))
            }.catch {
                emit(ForumSearchPostPartialChange.Init.Failure(it))
            }

        private fun ForumSearchPostUiIntent.DeleteHistory.deleteHistory(): Flow<ForumSearchPostPartialChange.DeleteHistory> =
            flow<ForumSearchPostPartialChange.DeleteHistory> {
                ForumSearchHistoryStore.delete(forumName, content)
                emit(ForumSearchPostPartialChange.DeleteHistory.Success(content))
            }.catch {
                emit(ForumSearchPostPartialChange.DeleteHistory.Failure(it))
            }

        private fun ForumSearchPostUiIntent.ClearHistory.clearHistory(): Flow<ForumSearchPostPartialChange.ClearHistory> =
            flow<ForumSearchPostPartialChange.ClearHistory> {
                ForumSearchHistoryStore.clear(forumName)
                emit(ForumSearchPostPartialChange.ClearHistory.Success)
            }.catch {
                emit(ForumSearchPostPartialChange.ClearHistory.Failure(it))
            }
    }
}

sealed interface ForumSearchPostUiIntent : UiIntent {
    data class Init(val forumName: String) : ForumSearchPostUiIntent

    @Parcelize
    data class Refresh(
        val keyword: String,
        val forumName: String,
        val forumId: Long,
        val sortType: Int,
        val filterType: Int,
        val recordHistory: Boolean = false,
    ) : ForumSearchPostUiIntent, Parcelable

    data class LoadMore(
        val keyword: String,
        val forumName: String,
        val forumId: Long,
        val page: Int,
        val sortType: Int,
        val filterType: Int,
    ) : ForumSearchPostUiIntent

    data class DeleteHistory(val forumName: String, val content: String) : ForumSearchPostUiIntent

    data class ClearHistory(val forumName: String) : ForumSearchPostUiIntent
}

sealed interface ForumSearchPostPartialChange : PartialChange<ForumSearchPostUiState> {
    data object Reset : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState) = ForumSearchPostUiState(
            searchHistories = oldState.searchHistories,
            sortType = oldState.sortType,
            filterType = oldState.filterType,
        )
    }

    sealed class Init : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState): ForumSearchPostUiState =
            when (this) {
                is Success -> oldState.copy(
                    searchHistories = searchHistories.toImmutableList()
                )

                is Failure -> oldState
            }

        data class Success(
            val searchHistories: List<SearchPostHistory>,
        ) : Init()

        data class Failure(
            val error: Throwable,
        ) : Init()
    }

    sealed class Refresh : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState): ForumSearchPostUiState =
            when (this) {
                is Start -> {
                    val sameQuery = keyword == oldState.keyword && sortType == oldState.sortType &&
                        filterType == oldState.filterType
                    oldState.copy(
                        isRefreshing = true,
                        isLoadingMore = false,
                        error = null,
                        keyword = keyword,
                        data = if (sameQuery) oldState.data else persistentListOf(),
                        currentPage = if (sameQuery) oldState.currentPage else 0,
                        hasMore = false,
                        sortType = sortType,
                        filterType = filterType,
                    )
                }

                is Success -> oldState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = null,
                    currentPage = 1,
                    hasMore = hasMore,
                    keyword = keyword,
                    data = data.distinctBy { Triple(it.tid, it.pid, it.cid) }.toImmutableList(),
                    sortType = sortType,
                    filterType = filterType,
                )

                is Failure -> oldState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = error.wrapImmutable()
                )
            }

        data class Start(
            val keyword: String,
            val sortType: Int,
            val filterType: Int,
        ) : Refresh()

        data class Success(
            val keyword: String,
            val data: ImmutableList<SearchThreadBean.ThreadInfoBean>,
            val hasMore: Boolean,
            val sortType: Int,
            val filterType: Int,
        ) : Refresh()

        data class Failure(
            val error: Throwable,
        ) : Refresh()
    }

    sealed class LoadMore : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState): ForumSearchPostUiState =
            when (this) {
                is Start -> oldState.copy(
                    isRefreshing = false,
                    isLoadingMore = true,
                    error = null,
                )

                is Success -> oldState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = null,
                    currentPage = page,
                    hasMore = hasMore,
                    data = (oldState.data + data).distinctBy { Triple(it.tid, it.pid, it.cid) }.toImmutableList(),
                )

                is Failure -> oldState.copy(
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = error.wrapImmutable()
                )
            }

        data object Start : LoadMore()

        data class Success(
            val data: ImmutableList<SearchThreadBean.ThreadInfoBean>,
            val hasMore: Boolean,
            val page: Int,
        ) : LoadMore()

        data class Failure(
            val error: Throwable,
        ) : LoadMore()
    }

    sealed class DeleteHistory : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState): ForumSearchPostUiState =
            when (this) {
                is Success -> oldState.copy(
                    searchHistories = oldState.searchHistories.filterNot { it.content == content }
                        .toImmutableList()
                )

                is Failure -> oldState
            }

        data class Success(val content: String) : DeleteHistory()

        data class Failure(
            val error: Throwable,
        ) : DeleteHistory()
    }

    sealed class ClearHistory : ForumSearchPostPartialChange {
        override fun reduce(oldState: ForumSearchPostUiState): ForumSearchPostUiState =
            when (this) {
                is Success -> oldState.copy(
                    searchHistories = persistentListOf()
                )

                is Failure -> oldState
            }

        data object Success : ClearHistory()

        data class Failure(
            val error: Throwable,
        ) : ClearHistory()
    }
}

data class ForumSearchPostUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,
    val searchHistories: ImmutableList<SearchPostHistory> = persistentListOf(),
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val keyword: String = "",
    val data: ImmutableList<SearchThreadBean.ThreadInfoBean> = persistentListOf(),
    val sortType: Int = ForumSearchPostSortType.NEWEST,
    val filterType: Int = ForumSearchPostFilterType.ALL,
) : UiState

object ForumSearchPostSortType {
    const val NEWEST = 1
    const val RELATIVE = 2
}

object ForumSearchPostFilterType {
    const val ONLY_THREAD = 1
    const val ALL = 2
}
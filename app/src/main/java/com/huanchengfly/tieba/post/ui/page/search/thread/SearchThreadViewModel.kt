package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchThreadViewModel @Inject constructor() :
    BaseViewModel<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState, SearchThreadUiEvent>() {
    override fun createInitialState() = SearchThreadUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState> =
        SearchThreadPartialChangeProducer { query, page ->
            TiebaApi.getInstance().searchThreadFlow(query.keyword, page, query.sortType)
        }
}

internal class SearchThreadPartialChangeProducer(
    private val search: (SearchThreadQuery, Int) -> Flow<SearchThreadBean>,
) : PartialChangeProducer<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<SearchThreadUiIntent>): Flow<SearchThreadPartialChange> =
        intentFlow.filterIsInstance<SearchThreadUiIntent.Refresh>().flatMapLatest { (query) ->
            // A new search or pull-to-refresh also cancels pagination for the previous result set.
            var nextPage: Int? = 1
            intentFlow.filterIsInstance<SearchThreadUiIntent.LoadMore>()
                .filter { it.query == query }
                .map { it.page }
                .conflate()
                .onStart { emit(1) }
                .filter { it == nextPage }
                .transform { page ->
                    emit(SearchThreadPartialChange.Start(query, page))
                    emitAll(
                        search(query, page).map<SearchThreadBean, SearchThreadPartialChange> {
                            val hasMore = it.data.hasMore == 1
                            nextPage = if (hasMore) page + 1 else null
                            SearchThreadPartialChange.Success(page, it.data.postList, hasMore)
                        }.catch { emit(SearchThreadPartialChange.Failure(it)) }
                    )
                }
        }
}

data class SearchThreadQuery(val keyword: String, val sortType: Int)

sealed interface SearchThreadUiIntent : UiIntent {
    data class Refresh(val query: SearchThreadQuery) : SearchThreadUiIntent
    data class LoadMore(val query: SearchThreadQuery, val page: Int) : SearchThreadUiIntent
}

sealed interface SearchThreadPartialChange : PartialChange<SearchThreadUiState> {
    data class Start(val query: SearchThreadQuery, val page: Int) : SearchThreadPartialChange
    data class Success(
        val page: Int,
        val data: List<SearchThreadBean.ThreadInfoBean>,
        val hasMore: Boolean,
    ) : SearchThreadPartialChange
    data class Failure(val error: Throwable) : SearchThreadPartialChange

    override fun reduce(oldState: SearchThreadUiState): SearchThreadUiState = when (this) {
        is Start -> when {
            query != oldState.query -> SearchThreadUiState(query = query)
            page == 1 -> oldState.copy(isRefreshing = true, isLoadingMore = false, hasMore = false, error = null)
            else -> oldState.copy(isLoadingMore = true, error = null)
        }
        is Success -> {
            val rows = if (page == 1) data else oldState.data + data
            oldState.copy(
                data = rows.distinctBy { Triple(it.tid, it.pid, it.cid) }.toImmutableList(),
                currentPage = page,
                hasMore = hasMore,
                isRefreshing = false,
                isLoadingMore = false,
                error = null,
            )
        }
        is Failure -> oldState.copy(isRefreshing = false, isLoadingMore = false, error = error.wrapImmutable())
    }
}

data class SearchThreadUiState(
    val query: SearchThreadQuery? = null,
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val data: ImmutableList<SearchThreadBean.ThreadInfoBean> = persistentListOf(),
) : UiState

sealed interface SearchThreadUiEvent : UiEvent

object SearchThreadSortType {
    const val SORT_TYPE_NEWEST = 5
    const val SORT_TYPE_OLDEST = 0
    const val SORT_TYPE_RELATIVE = 2
}

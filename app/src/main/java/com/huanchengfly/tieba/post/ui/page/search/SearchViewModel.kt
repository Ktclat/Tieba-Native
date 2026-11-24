package com.huanchengfly.tieba.post.ui.page.search

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.models.database.SearchHistory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.deleteAll
import org.litepal.extension.find

class SearchViewModel :
    BaseViewModel<SearchUiIntent, SearchPartialChange, SearchUiState, UiEvent>() {
    override fun createInitialState() = SearchUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<SearchUiIntent, SearchPartialChange, SearchUiState> =
        SearchPartialChangeProducer { keyword ->
            TiebaApi.getInstance().searchSuggestionsFlow(keyword).map { it.data_?.list.orEmpty() }
        }

    override fun dispatchEvent(partialChange: SearchPartialChange): UiEvent? =
        when (partialChange) {
            SearchPartialChange.HistoryCleared -> CommonUiEvent.Toast(
                App.INSTANCE.getString(R.string.toast_clear_success)
            )
            is SearchPartialChange.HistoryError -> CommonUiEvent.Toast(
                App.INSTANCE.getString(partialChange.messageRes, partialChange.errorMessage)
            )
            else -> null
        }
}

internal class SearchPartialChangeProducer(private val suggestionsFor: (String) -> Flow<List<String>>) :
    PartialChangeProducer<SearchUiIntent, SearchPartialChange, SearchUiState> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun toPartialChangeFlow(intentFlow: Flow<SearchUiIntent>): Flow<SearchPartialChange> {
        // The same ordered stream owns reads and writes so an old snapshot cannot undo a deletion.
        val history = intentFlow.filterNot { it is SearchUiIntent.KeywordInputChanged }
            .flatMapConcat { historyChanges(it) }
            .flowOn(Dispatchers.IO)
        val suggestions = intentFlow
            .filter { it is SearchUiIntent.KeywordInputChanged || it is SearchUiIntent.SubmitKeyword }
            .flatMapLatest {
                suggestionChanges((it as? SearchUiIntent.KeywordInputChanged)?.keyword?.trim().orEmpty())
            }
            .flowOn(Dispatchers.IO)
        return merge(history, suggestions)
    }

    private fun suggestionChanges(draft: String) = flow {
        emit(SearchPartialChange.Suggestions(draft, emptyList()))
        if (draft.isEmpty()) return@flow
        delay(500)
        suggestionsFor(draft).collect { words ->
            val uniqueWords = words.map(String::trim).filter(String::isNotEmpty).distinct()
            emit(SearchPartialChange.Suggestions(draft, uniqueWords))
        }
    }.catch { emit(SearchPartialChange.Suggestions(draft, emptyList())) }

    private fun historyChanges(intent: SearchUiIntent): Flow<SearchPartialChange> =
        flow<SearchPartialChange> {
            when (intent) {
                SearchUiIntent.Init -> emit(SearchPartialChange.HistoryLoaded(loadHistory()))
                is SearchUiIntent.SubmitKeyword -> {
                    val keyword = intent.keyword.trim()
                    if (keyword.isNotEmpty() && SearchHistory(keyword).saveOrUpdate("content = ?", keyword)) {
                        emit(SearchPartialChange.HistoryLoaded(loadHistory()))
                    }
                }
                is SearchUiIntent.DeleteSearchHistory -> {
                    LitePal.delete<SearchHistory>(intent.id)
                    emit(SearchPartialChange.HistoryLoaded(loadHistory()))
                }
                SearchUiIntent.ClearSearchHistory -> {
                    LitePal.deleteAll<SearchHistory>()
                    emit(SearchPartialChange.HistoryCleared)
                }
                is SearchUiIntent.KeywordInputChanged -> Unit
            }
        }.catch { error ->
            when (intent) {
                is SearchUiIntent.DeleteSearchHistory ->
                    emit(SearchPartialChange.HistoryError(R.string.toast_delete_failure, error.getErrorMessage()))
                SearchUiIntent.ClearSearchHistory ->
                    emit(SearchPartialChange.HistoryError(R.string.toast_clear_failure, error.getErrorMessage()))
                else -> Unit
            }
        }

    private fun loadHistory() = LitePal.order("timestamp DESC, id DESC").find<SearchHistory>()
}

sealed interface SearchUiIntent : UiIntent {
    data object Init : SearchUiIntent
    data object ClearSearchHistory : SearchUiIntent
    data class DeleteSearchHistory(val id: Long) : SearchUiIntent
    data class SubmitKeyword(val keyword: String) : SearchUiIntent
    data class KeywordInputChanged(val keyword: String) : SearchUiIntent
}

sealed interface SearchPartialChange : PartialChange<SearchUiState> {
    data class HistoryLoaded(val rows: List<SearchHistory>) : SearchPartialChange {
        override fun reduce(oldState: SearchUiState) = oldState.copy(searchHistories = rows.toImmutableList())
    }

    data object HistoryCleared : SearchPartialChange {
        override fun reduce(oldState: SearchUiState) = oldState.copy(searchHistories = persistentListOf())
    }

    data class HistoryError(val messageRes: Int, val errorMessage: String) : SearchPartialChange {
        override fun reduce(oldState: SearchUiState) = oldState
    }

    data class Suggestions(val keyword: String, val words: List<String>) : SearchPartialChange {
        override fun reduce(oldState: SearchUiState) =
            oldState.copy(suggestionKeyword = keyword, suggestions = words.toImmutableList())
    }
}

data class SearchUiState(
    val searchHistories: ImmutableList<SearchHistory> = persistentListOf(),
    val suggestionKeyword: String = "",
    val suggestions: ImmutableList<String> = persistentListOf(),
) : UiState

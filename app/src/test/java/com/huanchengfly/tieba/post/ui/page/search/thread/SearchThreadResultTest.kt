package com.huanchengfly.tieba.post.ui.page.search.thread

import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchThreadResultTest {
    private val newest = SearchThreadQuery("first", SearchThreadSortType.SORT_TYPE_NEWEST)

    @Test
    fun changingSortOrKeywordCancelsPendingPagination() = runBlocking {
        withTimeout(5_000) {
            val cancelled = mutableListOf<SearchThreadQuery>()
            val loadingPageTwo = MutableStateFlow<SearchThreadQuery?>(null)
            val producer = SearchThreadPartialChangeProducer { query, page ->
                flow {
                    if (page == 1) {
                        emit(response(page, true, row(query.keyword)))
                    } else {
                        try {
                            loadingPageTwo.value = query
                            awaitCancellation()
                        } finally {
                            cancelled.add(query)
                        }
                    }
                }
            }
            val intents = MutableSharedFlow<SearchThreadUiIntent>()
            val state = MutableStateFlow(SearchThreadUiState())
            val collection = launch(Dispatchers.Unconfined) {
                producer.toPartialChangeFlow(intents).collect { state.value = it.reduce(state.value) }
            }

            intents.subscriptionCount.first { it > 0 }
            val oldest = newest.copy(sortType = SearchThreadSortType.SORT_TYPE_OLDEST)
            val second = oldest.copy(keyword = "second")
            for (query in listOf(newest, oldest, second)) {
                intents.emit(SearchThreadUiIntent.Refresh(query))
                val result = state.first { it.query == query && !it.isRefreshing }
                assertEquals(listOf(query.keyword), result.data.map { it.pid })
                if (query != second) {
                    intents.subscriptionCount.first { it == 2 }
                    intents.emit(SearchThreadUiIntent.LoadMore(query, 2))
                    loadingPageTwo.first { it == query }
                }
            }
            assertEquals(listOf(newest, oldest), cancelled)
            assertEquals(1, state.value.currentPage)
            assertFalse(state.value.isLoadingMore)
            collection.cancelAndJoin()
        }
    }

    @Test
    fun onlyTheCurrentNextPageAppendsAndDistinctRepliesSurviveOverlap() = runBlocking {
        withTimeout(5_000) {
            val pages = mutableListOf<Int>()
            val releasePageTwo = CompletableDeferred<Unit>()
            val first = row("1")
            val reply = row("2")
            val subreply = reply.copy(cid = "3")
            val producer = SearchThreadPartialChangeProducer { _, page ->
                flow {
                    pages.add(page)
                    if (page == 1) {
                        emit(response(page, true, first))
                    } else {
                        releasePageTwo.await()
                        emit(response(page, false, first, reply, subreply, subreply))
                    }
                }
            }
            val intents = MutableSharedFlow<SearchThreadUiIntent>()
            val state = MutableStateFlow(SearchThreadUiState())
            val collection = launch(Dispatchers.Unconfined) {
                producer.toPartialChangeFlow(intents).collect { state.value = it.reduce(state.value) }
            }

            intents.subscriptionCount.first { it > 0 }
            intents.emit(SearchThreadUiIntent.Refresh(newest))
            state.first { it.currentPage == 1 }
            intents.subscriptionCount.first { it == 2 }
            intents.emit(SearchThreadUiIntent.LoadMore(newest.copy(keyword = "stale"), 2))
            intents.emit(SearchThreadUiIntent.LoadMore(newest, 3))
            assertEquals(listOf(1), pages)

            val next = SearchThreadUiIntent.LoadMore(newest, 2)
            intents.emit(next)
            state.first { it.isLoadingMore }
            intents.emit(next)
            releasePageTwo.complete(Unit)
            val result = state.first { it.currentPage == 2 }
            assertEquals(listOf(first, reply, subreply), result.data)
            assertFalse(result.hasMore)
            intents.emit(next)
            intents.emit(SearchThreadUiIntent.LoadMore(newest, 3))
            assertEquals(listOf(1, 2), pages)
            collection.cancelAndJoin()
        }
    }

    @Test
    fun failedPageCanBeRetriedWithoutLosingOrDuplicatingRows() = runBlocking {
        withTimeout(5_000) {
            val pages = mutableListOf<Int>()
            val first = row("1")
            val second = row("2")
            val producer = SearchThreadPartialChangeProducer { _, page ->
                flow {
                    val retry = page in pages
                    pages.add(page)
                    when {
                        page == 1 -> emit(response(page, true, first))
                        !retry -> throw java.io.IOException("Page unavailable")
                        else -> emit(response(page, false, second))
                    }
                }
            }
            val intents = MutableSharedFlow<SearchThreadUiIntent>()
            val state = MutableStateFlow(SearchThreadUiState())
            val collection = launch(Dispatchers.Unconfined) {
                producer.toPartialChangeFlow(intents).collect { state.value = it.reduce(state.value) }
            }
            intents.subscriptionCount.first { it > 0 }
            intents.emit(SearchThreadUiIntent.Refresh(newest))
            state.first { it.currentPage == 1 }
            intents.subscriptionCount.first { it == 2 }

            val next = SearchThreadUiIntent.LoadMore(newest, 2)
            intents.emit(next)
            val failed = state.first { it.error != null }
            assertEquals(listOf(first), failed.data)
            assertEquals(1, failed.currentPage)
            assertTrue(failed.hasMore)
            assertFalse(failed.isLoadingMore)

            intents.emit(next)
            val recovered = state.first { it.currentPage == 2 }
            assertEquals(listOf(first, second), recovered.data)
            assertNull(recovered.error)
            assertFalse(recovered.isLoadingMore)
            assertFalse(recovered.hasMore)
            assertEquals(listOf(1, 2, 2), pages)
            collection.cancelAndJoin()
        }
    }

    private fun response(page: Int, hasMore: Boolean, vararg rows: SearchThreadBean.ThreadInfoBean) =
        SearchThreadBean(0, "", SearchThreadBean.DataBean(if (hasMore) 1 else 0, page, rows.toList()))

    private fun row(pid: String) = SearchThreadBean.ThreadInfoBean(
        tid = "thread", pid = pid, title = "", content = "", time = "", modifiedTime = 0,
        postNum = "0", likeNum = "0", shareNum = "0", forumId = "1", forumName = "forum",
        user = SearchThreadBean.UserInfoBean(null, null, "1", null), type = 0,
        forumInfo = SearchThreadBean.ForumInfo("forum", ""),
    )
}

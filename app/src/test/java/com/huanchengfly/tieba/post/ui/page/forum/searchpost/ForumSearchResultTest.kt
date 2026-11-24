package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumSearchResultTest {
    private val submitted = ForumSearchPostUiIntent.Refresh(
        "查询", "甲", 1L, ForumSearchPostSortType.RELATIVE, ForumSearchPostFilterType.ONLY_THREAD
    )
    private val post = SearchThreadBean.ThreadInfoBean(
        tid = "thread", pid = "1", title = "", content = "", time = "", modifiedTime = 0L,
        postNum = "0", likeNum = "0", shareNum = "0", forumId = "1", forumName = "甲",
        user = SearchThreadBean.UserInfoBean(null, null, "1", null), type = 0,
        forumInfo = SearchThreadBean.ForumInfo("甲", ""),
    )

    @Test fun changedCriteriaDropStaleRowsEvenWhenTheNextRequestFails() {
        val original = ForumSearchPostUiState(
            keyword = submitted.keyword, sortType = submitted.sortType, filterType = submitted.filterType,
            data = persistentListOf(post), currentPage = 3, hasMore = true,
        )
        val start = ForumSearchPostPartialChange.Refresh.Start(original.keyword, original.sortType, original.filterType)
        val same = start.reduce(original)
        assertEquals(original.data, same.data)
        assertEquals(3, same.currentPage)
        for (change in listOf(start.copy(keyword = "新词"), start.copy(sortType = 1), start.copy(filterType = 2))) {
            val failed = ForumSearchPostPartialChange.Refresh.Failure(IllegalStateException()).reduce(change.reduce(original))
            assertTrue(failed.data.isEmpty())
            assertEquals(0, failed.currentPage)
            assertFalse(failed.isRefreshing || failed.hasMore)
            assertNotNull(failed.error)
        }
    }

    @Test fun restoredSearchLoadsFirstPageAndOnlyTheNextPageCanAppendOrRetry() = runBlocking {
        withTimeout(5_000) {
            val pages = mutableListOf<Int>()
            var failed = false
            val reply = post.copy(cid = "2")
            val producer = ForumSearchPostViewModel.ForumSearchPostPartialChangeProducer(
                restore = { submitted },
                search = { query, page ->
                    flow {
                        assertEquals(submitted, query)
                        pages += page
                        if (page == 2 && !failed) {
                            failed = true
                            error("page failed")
                        }
                        val rows = if (page == 1) listOf(post, post) else listOf(post, reply)
                        emit(SearchThreadBean(0, "", SearchThreadBean.DataBean(if (page == 1) 1 else 0, page, rows)))
                    }
                },
            )
            val intents = MutableSharedFlow<ForumSearchPostUiIntent>()
            val state = MutableStateFlow(ForumSearchPostUiState())
            val collector = launch(Dispatchers.Unconfined) {
                producer.searchChanges(intents).collect { state.value = it.reduce(state.value) }
            }
            intents.subscriptionCount.first { it > 0 }
            intents.emit(ForumSearchPostUiIntent.Init("甲"))
            assertEquals(listOf(post), state.first { it.currentPage == 1 }.data)
            intents.subscriptionCount.first { it == 2 }
            val next = ForumSearchPostUiIntent.LoadMore(
                submitted.keyword, submitted.forumName, submitted.forumId, 2, submitted.sortType, submitted.filterType
            )
            intents.emit(next.copy(page = 4))
            intents.emit(next.copy(forumName = "乙"))
            intents.emit(next)
            val error = state.first { it.error != null }
            assertEquals(1, error.currentPage)
            assertTrue(error.hasMore)
            assertEquals(listOf(post), error.data)
            intents.emit(next)
            val result = state.first { it.currentPage == 2 }
            assertEquals(listOf(post, reply), result.data)
            assertFalse(result.hasMore)
            assertNull(result.error)
            intents.emit(next)
            intents.emit(next.copy(page = 3))
            assertEquals(listOf(1, 2, 2), pages)
            collector.cancelAndJoin()
        }
    }
}

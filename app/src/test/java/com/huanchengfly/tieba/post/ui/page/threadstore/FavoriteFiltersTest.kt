package com.huanchengfly.tieba.post.ui.page.threadstore

import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal val exampleFavorite = ThreadStoreBean.ThreadStoreInfo(
    threadId = "1", title = "拍摄月亮", forumName = "天文", author = ThreadStoreBean.AuthorInfo(
        name = "Astro_J", nameShow = "夏夜",
    ), media = emptyList(), isDeleted = "0", lastTime = "0", type = "1", status = "0",
    maxPid = "150", minPid = "101", markPid = "101", markStatus = "0",
    postNo = "20", postNoMsg = "", count = "0",
)

class FavoriteFiltersTest {
    private val thread = exampleFavorite

    @Test fun searchIncludesLiteralTextAuthorAliasAndForum() {
        listOf("", "月亮", "STRO_j", "夏夜", "天文吧").forEach { assertTrue(thread.matchesFavoriteQuery(it)) }
        assertFalse(thread.matchesFavoriteQuery(".*"))
    }

    @Test fun validateHistoryAndRecoverLegacyPostWithoutFloor() {
        listOf(null, "", "null", "{bad", """{"pid":"-9"}""", """{"floor":"7"}""").forEach {
            assertNull(FavoriteReadingPosition.fromHistory(it))
        }
        assertEquals(FavoriteReadingPosition(113, null, true),
            FavoriteReadingPosition.fromHistory("""{"pid":"113","isSeeLz":true}"""))
        assertEquals(FavoriteReadingPosition(113, 8, false),
            FavoriteReadingPosition.fromHistory("""{"pid":113,"floor":8}"""))
    }

    @Test fun classifyLocalProgressIncludingOldRecordsAndNewReplies() {
        val halfway = FavoriteReadingPosition(125, 10, false)
        assertTrue(FavoriteFilter.IN_PROGRESS.matches(thread, halfway))
        assertTrue(FavoriteFilter.IN_PROGRESS.matches(thread, halfway.copy(floor = null)))
        assertFalse(FavoriteFilter.IN_PROGRESS.matches(thread.copy(maxPid = "125"), halfway))
        assertFalse(FavoriteFilter.IN_PROGRESS.matches(thread.copy(maxPid = "0", postNo = "0"), halfway))
        assertTrue(FavoriteFilter.HAS_PROGRESS.matches(thread, halfway))
        assertTrue(FavoriteFilter.NO_PROGRESS.matches(thread, null))
    }

    @Test fun resumeLocalThenServerBookmarkWithKnownBounds() {
        val local = FavoriteReadingPosition(119, 7, true)
        val marked = thread.copy(markPid = "127")
        assertEquals(119L, marked.resumePostId(local))
        assertEquals(127L, marked.resumePostId(null))
        assertEquals(FavoriteReadingPosition(127, null, false), marked.serverReadingPosition(false))
        assertNull(thread.serverReadingPosition(true))
        assertEquals(FavoriteReadingPosition(150, 20, true), marked.copy(markPid = "150").serverReadingPosition(true))
        listOf("", "abc", "99", "151").forEach { mark ->
            assertEquals(0L, thread.copy(markPid = mark).resumePostId(null))
            assertNull(thread.copy(markPid = mark).serverReadingPosition(true))
        }
        val unspecifiedBounds = marked.copy(minPid = "", maxPid = "")
        assertEquals(127L, unspecifiedBounds.resumePostId(null))
        assertNull(unspecifiedBounds.serverReadingPosition(true))
    }

    @Test fun mergeOverlappingPagesUpdateRowsAndKeepOffsetAfterDeleting() {
        val second = thread.copy(threadId = "2")
        val updated = second.copy(title = "新标题")
        val third = thread.copy(threadId = "3")
        val state = ThreadStoreUiState().withPage(listOf(thread, second), true)
            .withPage(listOf(updated, third), false)
        assertEquals(listOf(thread, updated, third), state.data)
        assertEquals(4, state.nextOffset)
        val removed = state.withoutFavorite("2")
        assertEquals(3, removed.nextOffset)
        assertEquals(listOf(thread, third), removed.data)
        assertEquals(removed, removed.withoutFavorite("2"))
        assertFalse(removed.withPage(emptyList(), false).hasMore)
        assertEquals(listOf(updated), removed.withPage(listOf(updated), true).data)
    }

    @Test fun cyclicAndReorderedResponsesStopScanning() {
        val next = thread.copy(threadId = "2")
        val state = ThreadStoreUiState().withPage(listOf(thread, next), true)
            .withPage(listOf(thread.copy(threadId = "3")), false)
        assertFalse(state.withPage(listOf(next, thread), false).hasMore)
    }

    @Test fun errorsRetainMatchingRowsAndAllowRetryAtSameOffset() {
        val success = ThreadStoreUiState().withPage(listOf(thread), true)
        val message = ThreadStoreBean.ErrorInfo("0", "登录失效")
        val failure = runCatching { ThreadStoreBean(errorCode = "4", error = message).favoriteRows() }.exceptionOrNull()
        assertEquals("登录失效", failure?.message)
        assertTrue(runCatching { ThreadStoreBean(error = message.copy(errorCode = null)).favoriteRows() }.isFailure)
        val failedPage = success.withLoadError(IllegalStateException(), false)
        assertEquals(success.data, failedPage.data)
        assertEquals(success.nextOffset, failedPage.nextOffset)
        assertFalse(failedPage.retryRefresh)
        assertTrue(success.withLoadError(IllegalStateException(), true).retryRefresh)
        assertNull(failedPage.withPage(listOf(thread.copy(threadId = "2")), false).error)
    }
}

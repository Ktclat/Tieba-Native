package com.huanchengfly.tieba.post.ui.page.history

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.page.history.list.ThreadHistoryListViewModel
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HistorySearchTest {
    private val thread = HistoryUtil.TYPE_THREAD
    private val forum = HistoryUtil.TYPE_FORUM

    @Before
    fun initializeHistory() {
        LitePal.initialize(ApplicationProvider.getApplicationContext())
        HistoryUtil.deleteAll()
    }

    private fun seedThreads(count: Int) {
        LitePal.saveAll((1..count).map { index ->
            History(title = "Thread $index", data = "$index", type = thread, count = 1, timestamp = 123)
        })
    }

    @Test
    fun filtersDatabaseBeforePagingByTitleAuthorAndBothForumNames() {
        val term = "百分%_'"
        val title = History(title = "A ${term}BOX", data = "11", type = thread, timestamp = 1)
        val author = History(title = "Different", username = "${term}BoX", data = "12", type = thread, timestamp = 1)
        val bar = History(title = "曾用名", data = "${term}Box", type = forum)
        val photography = History(title = "摄影吧", data = "摄影", type = forum)
        val literalName = History(title = "胶片%_吧", data = "胶片%_", type = forum)
        val similar = History(title = "胶片相机吧", data = "胶片相机", type = forum)
        val decoy = History(title = "百分 many_'Box", data = "13", type = thread)
        LitePal.saveAll(listOf(title, author, bar, photography, literalName, similar, decoy))
        seedThreads(105)

        val query = " ${term}box "
        val results = HistoryUtil.search(thread, query, null)
        assertEquals(setOf("11", "12"), results.rows.map { it.data }.toSet())
        assertNull(results.next)
        assertEquals(listOf(bar.data), HistoryUtil.search(forum, query, null).rows.map { it.data })
        assertEquals(listOf(bar.data), HistoryUtil.search(forum, "曾用名", null).rows.map { it.data })
        for (name in listOf("摄影", "摄影吧")) {
            assertEquals(listOf("摄影"), HistoryUtil.search(forum, name, null).rows.map { it.data })
        }
        assertEquals(listOf("胶片%_"), HistoryUtil.search(forum, "%_吧", null).rows.map { it.data })
        assertTrue(HistoryUtil.search(thread, "missing", null).rows.isEmpty())
    }

    @Test
    fun numericIdIsAnExactMatchAlongsideTitleAndAuthorMatches() {
        LitePal.saveAll(listOf(
            History(type = thread, title = "general", data = "812"),
            History(type = thread, title = "longer ID only", data = "8129"),
            History(type = thread, title = "Topic 8123", data = "45"),
            History(type = thread, username = "abc812", data = "46"),
            History(type = forum, title = "812吧", data = "812"),
        ))
        assertEquals(setOf("812", "45", "46"), HistoryUtil.search(thread, "812", null).rows.map { it.data }.toSet())
    }

    @Test
    fun deletingAResultAndItsCursorDoesNotSkipRowsWithTiedSortKeys() {
        val visits = (1..203).map { index ->
            History(type = thread, title = "visit", data = "$index", timestamp = (index % 3).toLong(), count = index % 5)
        }
        LitePal.saveAll(visits)
        val expected = visits.sortedWith(
            compareByDescending<History> { it.timestamp }.thenByDescending { it.count }.thenByDescending { it.data.toInt() }
        ).map { it.data }
        val first = HistoryUtil.search(thread, "", null)
        assertEquals(100, first.rows.size)
        assertNotNull(first.next)
        HistoryUtil.delete(thread, first.rows.first().id)
        HistoryUtil.delete(thread, first.rows.last().id)
        val second = HistoryUtil.search(thread, "", first.next)
        val third = HistoryUtil.search(thread, "", second.next)
        val visited = first.rows + second.rows + third.rows
        assertEquals(expected, visited.map { it.data })
        assertNull(third.next)
    }

    @Test
    fun visitsPreserveSeparateNumericForumAndThreadIdentitiesAndResumeData() {
        val bar = History(type = forum, title = "42吧", data = "42", extras = "bar")
        val post = History(type = thread, title = "First", data = "42", username = "LZ", extras = "floor 1")
        HistoryUtil.saveHistory(bar)
        HistoryUtil.saveHistory(post)
        HistoryUtil.saveHistory(post.copy(title = "Latest", extras = "floor 20"), async = false)

        val latest = HistoryUtil.search(thread, "LZ", null).rows.single()
        assertEquals("Latest", latest.title)
        assertEquals("floor 20", latest.extras)
        assertEquals(2, latest.count)
        assertEquals("bar", HistoryUtil.search(forum, "42", null).rows.single().extras)
        HistoryUtil.delete(forum, latest.id)
        assertEquals(latest.id, HistoryUtil.search(thread, "LZ", null).rows.single().id)
        assertEquals(listOf("42"), runBlocking { HistoryUtil.getFlow(forum, 0).first() }.map { it.data })

        HistoryUtil.saveHistory(post)
        val clearVersion = HistoryUtil.clearVersion.value
        HistoryUtil.deleteAll()
        assertEquals(clearVersion + 1, HistoryUtil.clearVersion.value)
        assertTrue(HistoryUtil.search(thread, "", null).rows.isEmpty())
        assertTrue(HistoryUtil.search(forum, "", null).rows.isEmpty())
    }

    @Test
    fun changingQueriesDuringPaginationAndDeletionCannotRestoreOldResults() {
        seedThreads(130)
        val viewModel = ThreadHistoryListViewModel()
        viewModel.refresh("Thread")
        waitFor { !viewModel.uiState.value.isRefreshing }
        val first = viewModel.uiState.value
        val firstRows = first.todayHistoryData + first.beforeHistoryData
        assertEquals(100, firstRows.size)
        assertTrue(first.hasMore)
        val target = firstRows.single { it.data == "130" }
        viewModel.loadMore()
        viewModel.refresh("Thread 130")
        viewModel.delete(target.id)
        viewModel.refresh("Thread 13")
        waitFor {
            val state = viewModel.uiState.value
            val rows = state.todayHistoryData + state.beforeHistoryData
            !state.isRefreshing && state.query == "Thread 13" && rows.map { it.data } == listOf("13")
        }
        val state = viewModel.uiState.value
        assertFalse(state.hasMore)
        assertNull(state.error)
        assertTrue(HistoryUtil.search(thread, "Thread 130", null).rows.isEmpty())
    }

    private fun waitFor(condition: () -> Boolean) {
        repeat(500) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("Timed out waiting for history")
    }
}

package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import android.app.Application
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ForumSearchHistoryTest {
    private val history = ForumSearchHistoryStore

    @Before fun initializeDatabase() {
        LitePal.initialize(ApplicationProvider.getApplicationContext<Application>())
    }

    @Test fun existingAndNewEntriesAreIsolatedByForumThroughUpsertDeleteAndClear() {
        val keyword = "100%'s"
        assertTrue(SearchPostHistory(keyword, "甲", 1L).save())
        assertTrue(SearchPostHistory("旧词", "甲", 2L).save())
        assertTrue(SearchPostHistory(keyword, "乙", 3L).save())

        val refreshed = history.record("甲", "  $keyword  ")
        assertEquals(listOf(keyword, "旧词"), refreshed.map { it.content })
        assertTrue(refreshed.all { it.forumName == "甲" && it.id > 0 })
        assertEquals(3L, history.list("乙").single().timestamp)

        history.record("甲", "   ")
        history.delete("甲", keyword)
        assertEquals(listOf("旧词"), history.list("甲").map { it.content })
        assertEquals(keyword, history.list("乙").single().content)
        history.clear("甲")
        assertTrue(history.list("甲").isEmpty())
        assertEquals(keyword, history.list("乙").single().content)
    }

    @Test fun savedSubmissionSurvivesParcelingWithoutRecordingAgain() {
        val request = ForumSearchPostUiIntent.Refresh(
            "查询", "甲", 1L, ForumSearchPostSortType.RELATIVE, ForumSearchPostFilterType.ONLY_THREAD,
            recordHistory = true,
        )
        val handle = SavedStateHandle()
        handle.saveForumSearch(request)
        val parcel = Parcel.obtain()
        try {
            val saved = handle.savedStateProvider().saveState()
            parcel.writeBundle(saved)
            parcel.setDataPosition(0)
            val bundle = parcel.readBundle(request.javaClass.classLoader)!!
            val restored = SavedStateHandle.createHandle(bundle, null)
            assertEquals(request.copy(recordHistory = false), restored.restoreForumSearch("甲"))
            assertEquals(null, restored.restoreForumSearch("乙"))
            restored.saveForumSearch(request.copy(keyword = ""))
            assertEquals(null, restored.restoreForumSearch("甲"))
        } finally {
            parcel.recycle()
        }
    }

    @Test fun onlyExplicitSubmissionsEnterHistoryAndResetPreservesIt() = runBlocking {
        val submit = ForumSearchPostUiIntent.Refresh(
            "  查询  ", "甲", 1L, ForumSearchPostSortType.NEWEST, ForumSearchPostFilterType.ALL,
            recordHistory = true,
        )
        val intents = listOf(
            ForumSearchPostUiIntent.Init("甲"),
            submit,
            submit.copy(keyword = "重载", recordHistory = false),
            submit.copy(keyword = "排序", sortType = ForumSearchPostSortType.RELATIVE, recordHistory = false),
            submit.copy(keyword = "   "),
        )
        val loaded = ForumSearchPostViewModel.ForumSearchPostPartialChangeProducer()
            .historyChanges(intents.asFlow())
            .fold(ForumSearchPostUiState()) { state, change -> change.reduce(state) }

        assertEquals(listOf("查询"), loaded.searchHistories.map { it.content })
        assertEquals(listOf("查询"), history.list("甲").map { it.content })
        val refresh = ForumSearchPostPartialChange.Refresh.Start(
            "查询", ForumSearchPostSortType.RELATIVE, ForumSearchPostFilterType.ONLY_THREAD
        ).reduce(loaded)
        assertTrue(refresh.isRefreshing)
        val reset = ForumSearchPostPartialChange.Reset.reduce(refresh)
        assertEquals("", reset.keyword)
        assertFalse(reset.isRefreshing || reset.isLoadingMore || reset.hasMore)
        assertTrue(reset.data.isEmpty())
        assertEquals(loaded.searchHistories, reset.searchHistories)
        assertEquals(refresh.sortType, reset.sortType)
        assertEquals(refresh.filterType, reset.filterType)
    }
}

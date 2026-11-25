package com.huanchengfly.tieba.post.ui.page.main.home

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HomeRefreshTest {
    @Test
    fun guestRefreshShowsLocalForumHistoryWithoutNetwork() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        LitePal.initialize(application)
        AccountUtil.init(application)
        assertFalse(AccountUtil.isLoggedIn())
        HistoryUtil.deleteAll()
        History(type = HistoryUtil.TYPE_FORUM, data = "摄影").saveThrows()

        val changes = runBlocking {
            HomeViewModel.HomePartialChangeProducer.toPartialChangeFlow(flowOf(HomeUiIntent.Refresh)).toList()
        }
        assertEquals(HomePartialChange.Refresh.Start, changes.first())
        val state = changes.fold(HomeUiState()) { previous, change -> change.reduce(previous) }
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.forums.isEmpty())
        assertTrue(state.topForums.isEmpty())
        assertEquals(listOf("摄影"), state.historyForums.map { it.data })
    }
}

package com.huanchengfly.tieba.post.ui.page.threadstore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.HistoryUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class FavoriteHistoryTest {
    @Test fun findOldFavoriteAcrossFullHistoryAndWaitForLatestQueuedWrite() = runBlocking {
        LitePal.initialize(ApplicationProvider.getApplicationContext())
        HistoryUtil.deleteAll()
        val ids = (1..520).map(Int::toString)
        val visits = ids.map { History(data = it, timestamp = it.toLong(),
            extras = """{"pid":"$it"}""", type = HistoryUtil.TYPE_THREAD) }
        LitePal.saveAll(visits)
        val forum = visits.first().copy(type = HistoryUtil.TYPE_FORUM, extras = "unrelated")
        forum.saveThrows()
        val found = HistoryUtil.threadVisits(ids).first()
        assertEquals(520, found.size)
        assertEquals(visits.first().extras, found.single { it.data == "1" }.extras)
        val newExtras = """{"pid":"88","floor":"8"}"""
        HistoryUtil.saveHistory(visits.first().copy(extras = newExtras))
        assertEquals(newExtras, HistoryUtil.latestThreadVisit("1")?.extras)
        HistoryUtil.deleteAll()
        assertNull(HistoryUtil.latestThreadVisit("1"))
    }
}

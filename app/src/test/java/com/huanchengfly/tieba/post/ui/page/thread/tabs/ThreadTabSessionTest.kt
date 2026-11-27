package com.huanchengfly.tieba.post.ui.page.thread.tabs

import android.app.Application
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ThreadTabSessionTest {
    @Test fun openingFreshArchivesRestoresPositionAndKeepsAccountsAndGuestsSeparate() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        suspend fun reopen(account: String?) = ThreadTabs(ThreadTabArchive.open(app, account)).also {
            assertFalse(it.ready)
            it.load()
            assertTrue(it.ready)
        }
        val first = reopen("alice")
        first.save(ThreadTab(10, "第一帖", forumName = "喜欢的吧", onlyAuthor = true, sortType = 1))
        first.save(ThreadTab(11, "第二帖"))
        val generation = first.entries.last().generation
        first.mark(10, generation, ThreadFloorKey(18, 4, latest = true), -75)
        reopen("bob").save(ThreadTab(10, "另一账号"))
        reopen(null).save(ThreadTab(12, "游客"))

        val restored = reopen("alice")
        assertEquals(first.entries, restored.entries)
        assertEquals(listOf(11L, 10L), restored.entries.map { it.threadId })
        assertEquals(first.entries.last().direction().route, restored.entries.last().direction().route)

        restored.close(11)
        val afterRemoval = reopen("alice")
        assertEquals(listOf(10L), afterRemoval.entries.map { it.threadId })
        afterRemoval.closeAll()
        afterRemoval.mark(10, generation, ThreadFloorKey(22, 5), 85)
        assertTrue(reopen("alice").entries.isEmpty())
        assertEquals("另一账号", reopen("bob").entries.single().title)
        assertEquals("游客", reopen(null).entries.single().title)
    }

    @Test fun simultaneousWindowsShareTheSameAccountSession() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val first = ThreadTabArchive.session(context, "shared")
        val second = ThreadTabArchive.session(context, "shared")
        assertSame(first, second)
        first.load()
        first.save(ThreadTab(10, "刚保存"))
        second.closeAll()
        assertTrue(first.entries.isEmpty())
        assertTrue(ThreadTabArchive.open(context, "shared").read().isEmpty())
    }
}

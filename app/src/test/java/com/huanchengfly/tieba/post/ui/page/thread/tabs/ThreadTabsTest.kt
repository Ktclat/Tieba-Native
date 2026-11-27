package com.huanchengfly.tieba.post.ui.page.thread.tabs

import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadTabsTest {
    @Test fun reselectMovesOneTabToFrontAndKeepsItsReadingPosition() {
        val tabs = ThreadTabs()
        val original = tabs.activate(ThreadTab(101, title = "山野", forumName = "徒步", forumId = 15))
        tabs.mark(101, original.generation, ThreadFloorKey(1009, 9), -38)
        tabs.describe(original.copy(onlyAuthor = true, sortType = 1), original.generation)
        tabs.activate(ThreadTab(202))
        val reopened = tabs.activate(ThreadTab(101))
        assertEquals(listOf(101L, 202L), tabs.entries.map { it.threadId })
        assertEquals(original.copy(postId = 1009, floor = 9, scrollOffset = -38, onlyAuthor = true, sortType = 1), reopened)
        val restored = ThreadTabs(tabs.savedEntries())
        assertEquals(tabs.entries, restored.entries)
        assertEquals(reopened.direction().route, restored.entries.first().direction().route)
    }

    @Test fun oldEntryCannotOverwriteOrResurrectAClosedTab() {
        val tabs = ThreadTabs()
        val old = tabs.activate(ThreadTab(404, title = "旧帖"))
        tabs.close(old.threadId)
        tabs.mark(404, old.generation, ThreadFloorKey(44, 4), 0)
        assertTrue(tabs.entries.isEmpty())
        val replacement = tabs.activate(ThreadTab(404, title = "重开"))
        tabs.describe(old, old.generation)
        tabs.mark(404, old.generation, ThreadFloorKey(44, 4), 0)
        assertEquals(replacement, tabs.find(404))
        assertNotEquals(old.generation, replacement.generation)
    }

    @Test fun partialResponsesPreserveMetadataAndNewRulesHideItInTheSwitcher() {
        val tabs = ThreadTabs()
        val original = tabs.activate(ThreadTab(201, "星轨", "摄影", 5, authorName = "星野", authorAlias = "远山"))
        tabs.describe(ThreadTab(201, onlyAuthor = true), original.generation)
        val tab = tabs.entries.single()
        assertEquals(original.copy(onlyAuthor = true), tab)
        assertFalse(tab.visibleWith(LocalBlockRules.fromText(user = "远山")))
        assertFalse(tab.visibleWith(LocalBlockRules.fromText(forum = "^摄影$")))
        assertTrue(tab.visibleWith(LocalBlockRules.fromText(comment = "星轨")))
    }

    @Test fun anchorIndexFollowsSortOrderLatestBlocksAndRemovedReplies() {
        val replies = listOf(12L, 15L)
        val latest = listOf(15L, 18L)
        assertEquals(0, threadTabAnchorIndex(10, 10, replies, latest))
        assertEquals(4, threadTabAnchorIndex(15, 10, replies, latest))
        assertEquals(6, threadTabAnchorIndex(15, 10, replies, latest, preferLatest = true))
        assertEquals(7, threadTabAnchorIndex(15, 10, replies, latest, descending = true))
        assertEquals(2, threadTabAnchorIndex(15, 10, replies, latest, descending = true, preferLatest = true))
        assertEquals(5, threadTabAnchorIndex(18, 10, emptyList(), latest))
        assertNull(threadTabAnchorIndex(16, 10, replies, latest))
    }

    @Test fun savedStateBoundsMetadataAndKeepsTheMostRecentlyOpenedTabs() {
        val tabs = ThreadTabs()
        (1L..160L).forEach { tabs.activate(ThreadTab(it, "图".repeat(1000))) }
        assertEquals(160, tabs.entries.size)
        val snapshot = tabs.savedEntries()
        assertEquals(100, snapshot.size)
        assertEquals(160L, snapshot.first().threadId)
        assertTrue(snapshot.all { it.title.length <= 160 })
    }
}

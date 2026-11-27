package com.huanchengfly.tieba.post.ui.page.thread.tabs

import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadTabsTest {
    @Test fun readingOnlyUpdatesSavedThreadsAndKeepsTheirExplicitSaveOrder() {
        val tabs = ThreadTabs()
        val trail = ThreadTab(101, "山野", "徒步", 15)
        tabs.describe(trail, "")
        tabs.mark(101, "", ThreadFloorKey(1009, 9), 72)
        assertTrue(tabs.entries.isEmpty())

        tabs.save(trail)
        val original = requireNotNull(tabs.find(101))
        tabs.save(ThreadTab(202))
        tabs.mark(101, original.generation, ThreadFloorKey(1009, 9, latest = true), -38)
        tabs.describe(trail.copy(onlyAuthor = true, sortType = 1), original.generation)
        assertEquals(listOf(202L, 101L), tabs.entries.map { it.threadId })
        val snapshot = requireNotNull(tabs.find(101))
        assertEquals(original.copy(postId = 1009, floor = 9, scrollOffset = -38, latest = true,
            onlyAuthor = true, sortType = 1), snapshot)

        // An explicit save from a partially loaded page keeps its previous reading anchor.
        tabs.save(ThreadTab(101, onlyAuthor = true, sortType = 1))
        assertEquals(listOf(101L, 202L), tabs.entries.map { it.threadId })
        assertEquals(snapshot.copy(savedOrder = 3), tabs.entries.first())
    }

    @Test fun closingOneOrAllRejectsStaleReadingUpdates() {
        val tabs = ThreadTabs()
        tabs.save(ThreadTab(404, title = "旧帖"))
        val old = requireNotNull(tabs.find(404))
        tabs.close(old.threadId)
        tabs.mark(404, old.generation, ThreadFloorKey(44, 4), 0)
        assertTrue(tabs.entries.isEmpty())
        tabs.save(ThreadTab(404, title = "重开"))
        val replacement = requireNotNull(tabs.find(404))
        tabs.describe(old, old.generation)
        tabs.mark(404, old.generation, ThreadFloorKey(44, 4), 0)
        assertEquals(replacement, tabs.find(404))
        assertNotEquals(old.generation, replacement.generation)

        tabs.save(ThreadTab(505))
        tabs.closeAll()
        tabs.describe(replacement, replacement.generation)
        tabs.mark(404, replacement.generation, ThreadFloorKey(49, 9), 1)
        assertTrue(tabs.entries.isEmpty())
    }

    @Test fun partialResponsesPreserveMetadataAndNewRulesHideItInTheSwitcher() {
        val tabs = ThreadTabs()
        tabs.save(ThreadTab(201, "星轨", "摄影", 5, authorName = "星野", authorAlias = "远山"))
        val original = requireNotNull(tabs.find(201))
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
}

package com.huanchengfly.tieba.post.ui.page.main

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainNavigationPreferencesTest {
    private val tabs = MainNavigationPreferences
    private val hideExplore = booleanPreferencesKey("hideExplore")

    @Test fun freshInstallShowsAllTabsInNavigationOrder() {
        assertEquals(listOf("home", "explore", "notification", "user"), tabs.visibleIds(emptyPreferences()))
    }

    @Test fun settingsAlwaysLeaveAPathBackToMyPage() {
        assertEquals(listOf("user"), tabs.visibleIds(emptySet()))
        assertEquals(listOf("user"), tabs.visibleIds(setOf("user", "unknown")))
        val restored = preferencesOf(tabs.key to linkedSetOf("notification", "explore"))
        assertEquals(listOf("explore", "notification", "user"), tabs.visibleIds(restored))
    }

    @Test fun previousHideExploreSettingSurvivesUpgrade() = runBlocking {
        for (hidden in listOf(false, true)) {
            val previous = preferencesOf(hideExplore to hidden)
            assertTrue(tabs.shouldMigrate(previous))
            val migrated = tabs.migrate(previous)
            val expected = if (hidden) listOf("home", "notification", "user")
                else listOf("home", "explore", "notification", "user")
            assertEquals(expected, tabs.visibleIds(migrated))
            assertFalse(tabs.shouldMigrate(migrated))
        }
    }

    @Test fun migrationKeepsAnExplicitEmptySelection() = runBlocking {
        val previous = preferencesOf(hideExplore to true, tabs.key to emptySet<String>())
        val migrated = tabs.migrate(previous)
        assertEquals(listOf("user"), tabs.visibleIds(migrated))
        assertFalse(tabs.shouldMigrate(migrated))
    }
}

package com.huanchengfly.tieba.post.ui.page.main

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.huanchengfly.tieba.post.R

internal object MainNavigationPreferences : DataMigration<Preferences> {
    val key = stringSetPreferencesKey("visibleMainTabs")
    val options = mapOf(
        "home" to R.string.title_main,
        "explore" to R.string.title_explore,
        "notification" to R.string.title_notifications,
    )
    private val legacyHideExplore = booleanPreferencesKey("hideExplore")

    fun visibleIds(preferences: Preferences): List<String> = visibleIds(preferences[key] ?: options.keys)

    fun visibleIds(enabled: Set<String>): List<String> = options.keys.filter { it in enabled } + "user"

    override suspend fun shouldMigrate(currentData: Preferences) = legacyHideExplore in currentData

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            if (remove(legacyHideExplore) == true && key !in this) {
                this[key] = options.keys - "explore"
            }
        }.toPreferences()

    override suspend fun cleanUp() {}
}

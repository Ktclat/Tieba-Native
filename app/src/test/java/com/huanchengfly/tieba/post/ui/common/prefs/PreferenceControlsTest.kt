package com.huanchengfly.tieba.post.ui.common.prefs

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.CheckBoxPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.MultiSelectListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PreferenceControlsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val directory = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope) {
            File(directory.root, "settings.preferences_pb")
        }
    }

    @After fun closeStore() = scope.cancel()

    private fun waitForPreference(predicate: (Preferences) -> Boolean) {
        val saved = scope.async { store.data.first { predicate(it) } }
        compose.waitUntil(timeoutMillis = 5_000) { saved.isCompleted }
        runBlocking { saved.await() }
    }

    @Test fun checkboxAndSwitchHaveOneActionPerRowAndUseExistingBooleanKeys() {
        val checkboxKey = booleanPreferencesKey("checkbox")
        val switchKey = booleanPreferencesKey("switch")
        runBlocking { store.edit { it[checkboxKey] = true } }
        val changes = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPrefsDataStore provides store) {
                    Column {
                        CheckBoxPref("checkbox", "复选", onCheckedChange = { changes.add("check=$it") })
                        SwitchPref("switch", "开关", summary = "通知", onCheckedChange = { changes.add("switch=$it") })
                    }
                }
            }
        }

        val checkbox = compose.onNodeWithText("复选")
        val switch = compose.onNodeWithText("开关")
        compose.waitUntil {
            checkbox.fetchSemanticsNode().config[SemanticsProperties.ToggleableState] == ToggleableState.On
        }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(2)
        checkbox.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOn().performClick()
        switch.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsOff().performClick()
        waitForPreference { it[checkboxKey] == false && it[switchKey] == true }
        compose.waitUntil { changes.size == 2 }
        assertEquals(setOf("check=false", "switch=true"), changes.toSet())
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test fun singleChoiceClosesTheDialogAndUpdatesTheStoredSummary() {
        val selectedKey = stringPreferencesKey("order")
        runBlocking { store.edit { it[selectedKey] = "old" } }
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPrefsDataStore provides store) {
                    ListPref(
                        key = "order",
                        title = "排序",
                        useSelectedAsSummary = true,
                        entries = linkedMapOf("old" to "最早", "new" to "最新"),
                    )
                }
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("最早")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("排序").performClick()
        val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        compose.onAllNodes(radio, useUnmergedTree = true).assertCountEquals(2)
        compose.onNode(radio and hasText("最早")).assertIsSelected()
        compose.onNode(radio and hasText("最新")).performClick()
        waitForPreference { it[selectedKey] == "new" }
        compose.onAllNodes(radio).assertCountEquals(0)
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("排序") and hasText("最新")).fetchSemanticsNodes().size == 1
        }
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Test fun eachMultiSelectOptionHasOneToggleAndKeepsOtherSelections() {
        val selectedKey = stringSetPreferencesKey("feed")
        runBlocking { store.edit { it[selectedKey] = setOf("a") } }
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPrefsDataStore provides store) {
                    MultiSelectListPref("feed", "推荐内容", entries = linkedMapOf("a" to "已关注", "b" to "热门"))
                }
            }
        }
        compose.onNodeWithText("推荐内容").performClick()
        val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        compose.onAllNodes(isCheckbox, useUnmergedTree = true).assertCountEquals(2)
        compose.onNodeWithText("热门").assertIsOff().performClick()
        waitForPreference { it[selectedKey] == setOf("a", "b") }
        compose.onNodeWithText("已关注").assertIsOn().performClick()
        waitForPreference { it[selectedKey] == setOf("b") }
    }
}

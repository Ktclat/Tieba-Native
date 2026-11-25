package com.huanchengfly.tieba.post.arch

import android.app.Application
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.huanchengfly.tieba.post.ui.page.main.MainUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
@OptIn(ExperimentalTestApi::class)
class StateFlowPartialStateTest {
    // Queue frames on the test scheduler when the upstream IO collector emits.
    @get:Rule val compose = createComposeRule(StandardTestDispatcher())

    @Test fun cachedCountIsRenderedFromTheFirstCompositionOnEachEntry() {
        val uiState = MutableStateFlow(MainUiState(messageCount = 7))
        val rendered = mutableListOf<Int>()
        var visible by mutableStateOf(true)
        compose.setContent {
            if (visible) {
                val count = uiState.collectPartialAsState(MainUiState::messageCount).value
                BasicText(count.toString())
                SideEffect { rendered.add(count) }
            }
        }

        compose.runOnIdle {
            assertEquals(listOf(7), rendered.distinct())
            uiState.value = MainUiState(messageCount = 11)
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("11").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle {
            assertEquals(listOf(7, 11), rendered.distinct())
            visible = false
        }
        compose.onNodeWithText("11").assertDoesNotExist()

        compose.runOnIdle {
            rendered.clear()
            uiState.value = MainUiState(messageCount = 23)
            visible = true
        }
        compose.runOnIdle { assertEquals(listOf(23), rendered.distinct()) }
    }

    @Test fun inactiveTabStopsCollectingAndRestoresRememberedProjectionOnFirstFrame() {
        val uiState = MutableStateFlow(MainUiState(messageCount = 5))
        val visibleFrames = mutableListOf<Int>()
        var active by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalMainTabActive provides active) {
                val count by uiState.collectPartialAsState(MainUiState::messageCount)
                val rememberedCount by remember { derivedStateOf { count } }
                BasicText(rememberedCount.toString())
                SideEffect { if (active) visibleFrames.add(rememberedCount) }
            }
        }

        compose.waitUntil(5_000) { uiState.subscriptionCount.value == 1 }
        compose.runOnIdle { active = false }
        compose.waitUntil(5_000) { uiState.subscriptionCount.value == 0 }
        compose.runOnIdle {
            uiState.value = MainUiState(messageCount = 17)
            visibleFrames.clear()
        }
        compose.onNodeWithText("5").assertExists()
        compose.runOnIdle { active = true }
        compose.runOnIdle { assertEquals(listOf(17), visibleFrames.distinct()) }

        compose.waitUntil(5_000) { uiState.subscriptionCount.value == 1 }
        compose.runOnIdle { uiState.value = MainUiState(messageCount = 23) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("23").fetchSemanticsNodes().isNotEmpty() }
    }
}

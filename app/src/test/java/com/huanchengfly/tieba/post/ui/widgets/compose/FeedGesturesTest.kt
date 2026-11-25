package com.huanchengfly.tieba.post.ui.widgets.compose

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
class FeedGesturesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pullsAtOppositeEndsRefreshAndLoadExactlyOnce() {
        var refreshes = 0
        var loads = 0
        var end by mutableStateOf(false)
        compose.setContent {
            val list = rememberLazyListState()
            PullToRefreshLayout(refreshing = false, onRefresh = { refreshes++ }) {
                LoadMoreLayout(
                    modifier = Modifier.height(360.dp),
                    lazyListState = list,
                    isLoading = false,
                    preloadCount = 0,
                    loadEnd = end,
                    onLoadMore = { loads++ },
                ) {
                    LazyColumn(Modifier.testTag("feed"), state = list) {
                        items(80) { Box(Modifier.fillMaxWidth().height(60.dp)) }
                    }
                }
            }
        }
        val feed = compose.onNodeWithTag("feed")
        feed.performTouchInput {
            swipe(Offset(centerX, height * 0.1f), Offset(centerX, height * 0.9f), 400)
        }
        compose.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(0, loads)
        }

        feed.performScrollToIndex(10)
        feed.performTouchInput {
            swipe(Offset(centerX, height * 0.8f), Offset(centerX, height * 0.2f), 400)
        }
        compose.runOnIdle { assertEquals(0, loads) }

        feed.performScrollToIndex(79)
        feed.performTouchInput {
            swipe(Offset(centerX, height * 0.8f), Offset(centerX, height * 0.2f), 400)
        }
        compose.runOnIdle {
            assertEquals(1, loads)
            end = true
        }
        feed.performTouchInput {
            swipe(Offset(centerX, height * 0.8f), Offset(centerX, height * 0.2f), 400)
        }
        compose.runOnIdle {
            assertEquals(1, loads)
            assertEquals(1, refreshes)
        }
    }

    @Test fun preloadsEachVisibleEndOnceAndPausesForHiddenTabs() {
        var count by mutableIntStateOf(2)
        var active by mutableStateOf(false)
        var loading by mutableStateOf(false)
        var loads = 0
        compose.setContent {
            val list = rememberLazyListState()
            ProvideShouldLoad(active) {
                LoadMoreLayout(isLoading = loading, lazyListState = list, onLoadMore = { loads++ }) {
                    LazyColumn(state = list) {
                        items(count) { Box(Modifier.fillMaxWidth().height(32.dp)) }
                    }
                }
            }
        }
        compose.runOnIdle { assertEquals(0, loads); active = true }
        compose.runOnIdle { assertEquals(1, loads); loading = true }
        compose.runOnIdle { loading = false }
        compose.runOnIdle { assertEquals(1, loads); active = false; count++ }
        compose.runOnIdle { assertEquals(1, loads); active = true }
        compose.runOnIdle { assertEquals(2, loads) }
    }
}

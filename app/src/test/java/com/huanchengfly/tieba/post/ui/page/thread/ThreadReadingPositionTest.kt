package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w360dp-h720dp-mdpi")
class ThreadReadingPositionTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var list: LazyListState
    private lateinit var scope: CoroutineScope

    @Test
    fun recordsOnlySettledFloorsAndFlushesOnExitDuringScroll() {
        val topic = ThreadFloorKey(10, 1)
        val second = ThreadFloorKey(21, 2)
        val third = ThreadFloorKey(121, 3)
        val latest = ThreadFloorKey(17, 4, latest = true)
        val keys = listOf(FirstPostKey, second, third, latest, latest.copy(latest = false), "footer")
        val saved = mutableListOf<ThreadFloorKey>()
        val tracking = mutableStateOf(true)
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(Modifier.height(64.dp), state = list) {
                items(keys.size, key = { keys[it] }) {
                    Box(Modifier.fillMaxWidth().height(64.dp))
                }
            }
            if (tracking.value) TrackThreadReadingPosition(1, list, topic, saved::add)
        }
        compose.runOnIdle { assertEquals(listOf(topic), saved) }

        // A single gesture passes floor 2 and comes to rest on floor 3.
        val release = CompletableDeferred<Unit>()
        compose.runOnIdle {
            scope.launch {
                list.scroll {
                    scrollBy(128f)
                    release.await()
                }
            }
        }
        compose.runOnIdle {
            assertEquals(third, list.lastVisibleFloor(topic))
            assertEquals(listOf(topic), saved)
            release.complete(Unit)
        }
        compose.runOnIdle { assertEquals(listOf(topic, third), saved) }

        // Leave the thread mid-gesture with an item from the separate latest-replies section.
        val exit = CompletableDeferred<Unit>()
        compose.runOnIdle {
            scope.launch {
                list.scroll {
                    scrollBy(64f)
                    exit.await()
                }
            }
        }
        compose.runOnIdle { tracking.value = false }
        compose.runOnIdle {
            assertEquals(listOf(topic, third, latest), saved)
            exit.complete(Unit)
        }
    }

    @Test
    fun deduplicatesSamePostAcrossSectionsAndIgnoresFooter() {
        val floor = ThreadFloorKey(212, 25)
        val saved = mutableListOf<ThreadFloorKey>()
        val keys = listOf(floor, floor.copy(latest = true), "footer")
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            TrackThreadReadingPosition(1, list, null, saved::add)
            LazyColumn(Modifier.height(64.dp), state = list) {
                items(keys.size, key = { keys[it] }) {
                    Box(Modifier.fillMaxWidth().height(64.dp))
                }
            }
        }
        for (index in 1..2) {
            compose.runOnIdle { scope.launch { list.scrollToItem(index) } }
        }
        compose.runOnIdle {
            assertNull(list.lastVisibleFloor(null))
            assertEquals(listOf(floor), saved)
        }
    }

    @Test
    fun backgroundingMidGestureFlushesAndBackgroundChangesDoNotOverwriteTheBookmark() {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        val saved = mutableListOf<Int>()
        val tracking = mutableStateOf(true)
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                if (tracking.value) TrackThreadReadingPosition(1, list, null) { saved += it.floor }
            }
            LazyColumn(Modifier.height(64.dp), state = list) {
                items(5, key = { ThreadFloorKey(it + 1L, it + 1) }) {
                    Box(Modifier.fillMaxWidth().height(64.dp))
                }
            }
        }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        val stop = CompletableDeferred<Unit>()
        compose.runOnIdle {
            scope.launch {
                list.scroll {
                    scrollBy(64f)
                    stop.await()
                }
            }
        }
        compose.runOnIdle {
            assertEquals(listOf(1), saved)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            stop.complete(Unit)
        }
        compose.runOnIdle { scope.launch { list.scrollToItem(3) } }
        compose.runOnIdle { tracking.value = false }
        compose.runOnIdle { assertEquals(listOf(1, 2), saved) }
    }

    @Test
    fun distantJumpSkipsInterveningViewportsInBothDirections() {
        val composed = mutableSetOf<Int>()
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(Modifier.height(64.dp), state = list) {
                items(120) { index ->
                    composed.add(index)
                    Box(Modifier.fillMaxWidth().height(64.dp))
                }
            }
        }
        compose.runOnIdle { scope.launch { list.scrollToDiscussionItem(106) } }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(106, list.firstVisibleItemIndex) }
        compose.runOnIdle { scope.launch { list.scrollToDiscussionItem(3) } }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(3, list.firstVisibleItemIndex)
            assertEquals(emptySet<Int>(), composed.filter { it in 15..95 }.toSet())
        }
    }
}

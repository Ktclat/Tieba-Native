package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.capturePhoneReview
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.NavGraphs
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.MainPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadTabSearchPageDestination
import com.huanchengfly.tieba.post.ui.page.search.thread.openThreadTabSearchResult
import com.huanchengfly.tieba.post.ui.page.thread.tabs.LocalThreadTabs
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ProvideThreadTabs
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTab
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabs
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabsButton
import com.huanchengfly.tieba.post.ui.page.thread.tabs.TrackThreadTab
import com.huanchengfly.tieba.post.ui.page.thread.tabs.leaveClosedThread
import com.huanchengfly.tieba.post.ui.page.thread.tabs.restoreThreadTabAnchor
import com.huanchengfly.tieba.post.ui.page.thread.tabs.threadTabAnchorIndex
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelThreadTabsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()

    @Test fun openTheTabWindowFromTheRealHomeAndFindANewThread() {
        val activity = compose.activity
        val open = hasTestTag("thread-tabs-open")
        compose.waitUntil(15_000) { compose.onAllNodes(open).fetchSemanticsNodes().isNotEmpty() }
        val button = compose.onNode(open).assertIsDisplayed()
        val bounds = button.fetchSemanticsNode().boundsInRoot
        val metrics = activity.resources.displayMetrics
        assertTrue(bounds.center.x > metrics.widthPixels * .75f && bounds.top < metrics.density * 100)
        compose.onNodeWithTag("thread-tabs-new").assertDoesNotExist()
        button.performClick()
        compose.onNodeWithText(activity.getString(R.string.thread_tabs_empty)).assertIsDisplayed()
        compose.capturePhoneReview("thread-tabs-empty")
        compose.onNodeWithTag("thread-tabs-new").performClick()
        val hint = activity.getString(R.string.hint_search_thread_tab)
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(hint), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(hint, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription(activity.getString(R.string.button_back)).performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(open).fetchSemanticsNodes().isNotEmpty() }
        button.assertIsDisplayed()
    }

    private val trail = ThreadTab(311, "山脊步道的路线记录", "徒步", 51)
    private val camera = ThreadTab(422, "如何给山景照片测光", "摄影", 62)

    @Test fun switchTwoThreadsResumeTheirOwnFloorsAndCloseTheLastTab() {
        val activity = compose.activity
        val tabs = ThreadTabs()
        lateinit var controller: NavHostController
        compose.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(activity)) {
                    TiebaLiteTheme {
                        ProvideThreadTabs(tabs) {
                            controller = rememberNavController()
                            DestinationsNavHost(navGraph = NavGraphs.root, navController = controller) {
                                composable(MainPageDestination) {
                                    Column {
                                        ThreadTabsButton(destinationsNavigator)
                                        TextButton(onClick = {
                                            destinationsNavigator.navigate(ThreadPageDestination(
                                                trail.threadId, seeLz = true, sortType = ThreadSortType.SORT_TYPE_DESC,
                                            ))
                                        }) { Text(trail.title) }
                                    }
                                }
                                composable(ThreadTabSearchPageDestination) {
                                    TextButton(onClick = { destinationsNavigator.openThreadTabSearchResult(controller, ThreadPageDestination(camera.threadId)) }) {
                                        Text(camera.title)
                                    }
                                }
                                composable(ThreadPageDestination) {
                                    val sample = if (navArgs.threadId == trail.threadId) trail else camera
                                    SampleThread(sample, navArgs, destinationsNavigator)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun tag(value: String) = compose.onNodeWithTag(value)
        fun clickTag(value: String) {
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(value)).fetchSemanticsNodes().size == 1 }
            tag(value).assertIsDisplayed().performClick()
        }
        fun currentArgs() = ThreadPageDestination.argsFrom(checkNotNull(controller.currentBackStackEntry).arguments)
        fun awaitRoute(route: String) = compose.waitUntil(10_000) {
            compose.runOnUiThread { controller.currentDestination?.route == route }
        }
        fun read(tab: ThreadTab, floor: Int) {
            val index = fixtureFloors(tab == trail).indexOf(floor) + 3
            tag("sample-floors-${tab.threadId}").performScrollToIndex(index)
            compose.waitUntil(10_000) { compose.runOnUiThread { tabs.find(tab.threadId)?.floor == floor } }
        }
        compose.onNodeWithText(trail.title).performClick()
        awaitRoute(ThreadPageDestination.route)
        read(trail, 14)
        tag("sample-floors-311").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 36f) }
        compose.waitUntil(10_000) { compose.runOnUiThread { (tabs.find(trail.threadId)?.scrollOffset ?: 0) > 0 } }
        val offset = compose.runOnIdle { checkNotNull(tabs.find(trail.threadId)).scrollOffset }
        val adjacentTop = tag("sample-floor-311-13").fetchSemanticsNode().boundsInRoot.top
        clickTag("thread-tabs-open")
        clickTag("thread-tabs-new")
        awaitRoute(ThreadTabSearchPageDestination.route)
        compose.onNodeWithText(camera.title).performClick()
        awaitRoute(ThreadPageDestination.route)
        read(camera, 9)
        clickTag("thread-tabs-open")
        tag("thread-tab-422").assertIsSelected()
        tag("thread-tab-311").assertIsDisplayed()
        compose.capturePhoneReview("thread-tabs-two-posts")
        clickTag("thread-tab-311")
        compose.waitUntil(10_000) { compose.runOnUiThread { currentArgs().threadId == trail.threadId } }
        compose.runOnIdle {
            val args = currentArgs()
            assertEquals(311014L, args.postId)
            assertEquals(ThreadPageFrom.FROM_TABS, args.from)
            assertTrue(args.seeLz)
            assertEquals(ThreadSortType.SORT_TYPE_DESC, args.sortType)
            assertEquals(setOf(311L, 422L), tabs.entries.map { it.threadId }.toSet())
        }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("sample-floor-311-13")).fetchSemanticsNodes().size == 1 &&
                compose.onAllNodes(hasTestTag("sample-floors-422")).fetchSemanticsNodes().isEmpty()
        }
        tag("sample-floor-311-14").assertIsDisplayed()
        assertEquals(adjacentTop, tag("sample-floor-311-13").fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.runOnIdle { assertEquals(offset, tabs.find(trail.threadId)?.scrollOffset) }
        compose.capturePhoneReview("thread-tabs-restored-floor")
        clickTag("thread-tabs-open")
        clickTag("thread-tab-close-311")
        compose.waitUntil(10_000) { compose.runOnUiThread { currentArgs().threadId == camera.threadId } }
        compose.runOnIdle { assertEquals(422009L, currentArgs().postId) }
        tag("sample-floor-422-9").assertIsDisplayed()
        clickTag("thread-tabs-open")
        tag("thread-tab-311").assertDoesNotExist()
        clickTag("thread-tab-close-422")
        awaitRoute(MainPageDestination.route)
        compose.runOnIdle { assertTrue(tabs.entries.isEmpty()) }
        clickTag("thread-tabs-open")
        compose.onNodeWithText(activity.getString(R.string.thread_tabs_empty)).assertIsDisplayed()
    }

    private fun fixtureFloors(descending: Boolean) = (if (descending) 45 downTo 2 else 2..45).toList()

    @Composable
    private fun SampleThread(sample: ThreadTab, args: ThreadPageDestination.NavArgs, navigator: DestinationsNavigator) {
        val list = rememberLazyListState()
        val first = ThreadFloorKey(sample.threadId * 1000 + 1, 1)
        val floors = remember(args.sortType) { fixtureFloors(args.sortType == ThreadSortType.SORT_TYPE_DESC) }
        val tabs = checkNotNull(LocalThreadTabs.current)
        val offset = rememberSaveable(args.threadId, args.postId) { tabs.find(args.threadId)?.scrollOffset ?: 0 }
        var restoring by rememberSaveable(args.threadId, args.postId) {
            mutableStateOf(args.from == ThreadPageFrom.FROM_TABS && args.postId > 0)
        }
        TrackThreadTab(sample.copy(onlyAuthor = args.seeLz, sortType = args.sortType), list, first, recordPosition = !restoring) {
            navigator.leaveClosedThread()
        }
        LaunchedEffect(restoring) {
            if (restoring) {
                val replyIds = floors.map { sample.threadId * 1000 + it }
                val index = checkNotNull(threadTabAnchorIndex(args.postId, first.postId, replyIds,
                    descending = args.sortType == ThreadSortType.SORT_TYPE_DESC))
                list.restoreThreadTabAnchor(index, offset)
                restoring = false
            }
        }
        ProvideNavigator(navigator) {
            MyScaffold(topBar = {
                ThreadTopBar(SimpleForum(name = sample.forumName).wrapImmutable(), { navigator.navigateUp() }, {},
                    actions = { ThreadTabsButton(navigator, sample.threadId) })
            }) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("sample-floors-${sample.threadId}"), state = list) {
                    item(key = FirstPostKey) { Text(sample.title, Modifier.padding(16.dp)) }
                    item { ThreadRepliesHeader(floors.size, args.seeLz, onFilterChange = {}) }
                    item { Spacer(Modifier.height(1.dp)) }
                    items(floors.size, key = { ThreadFloorKey(sample.threadId * 1000 + floors[it], floors[it]) }) { index ->
                        val floor = floors[index]
                        val post = remember(sample.threadId, index) {
                            val comment = if (sample.threadId == trail.threadId) "那段上坡石阶有些湿滑，经过时可以走慢些。" else "让画面保留一些山脊阴影，天空的云层会更清楚。"
                            DiscussionSample.floor.copy(id = sample.threadId * 1000 + floor, floor = floor,
                                content = listOf(PbContent(text = comment)), sub_post_list = null, sub_post_number = 0)
                        }
                        Box(Modifier.testTag("sample-floor-${sample.threadId}-$floor")) {
                            PostCard(post.wrapImmutable(), post.contentRenders, showSubPosts = false)
                        }
                    }
                }
            }
        }
    }
}

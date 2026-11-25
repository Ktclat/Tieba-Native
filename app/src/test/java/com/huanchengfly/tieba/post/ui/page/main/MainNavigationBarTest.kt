@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.main

import android.app.Application
import android.graphics.BitmapFactory
import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import kotlinx.collections.immutable.toImmutableList
import java.io.File
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainNavigationBarTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View
    private val pageAtActivation = mutableListOf<Int>()
    private val measures = mutableMapOf<Int, Int>()
    private val compositions = mutableListOf<Int>()
    private val disposals = mutableListOf<Int>()
    private val requests = mutableListOf<Int>()
    private val backTargets = mutableListOf<Int>()
    private val pageFocus = mutableMapOf<Int, Boolean>()
    private val focusRequests = mutableMapOf<Int, FocusRequester>()
    private lateinit var tabSelection: MainTabSelection
    private val tabLifecycles = mutableMapOf<Int, LifecycleOwner>()
    private lateinit var backDispatcher: OnBackPressedDispatcher
    private val measuredAtActivation = mutableListOf<Boolean>()
    private var hideFraction by mutableFloatStateOf(0f)
    private val refreshedPages = mutableListOf<Int>()
    private val titles = listOf("首页", "动态", "消息", "我")
    private val rowLabels = listOf("帖子", "动态", "消息", "收藏")

    @OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalFoundationApi::class)
    private fun show(layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
        compose.setContent {
            val currentView = LocalView.current
            SideEffect { view = currentView }
            val distance = with(LocalDensity.current) { 132.dp.toPx() }
            val observer = remember(distance) {
                object : NestedScrollConnection {
                    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                        hideFraction = updateNavigationHideFraction(hideFraction, consumed.y, distance)
                        return Offset.Zero
                    }
                }
            }
            val selection = rememberMainTabSelection("0")
            SideEffect { tabSelection = selection }
            backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            LaunchedEffect(selection.id) { hideFraction = 0f }
            val items = remember {
                listOf(
                    R.string.title_main to R.drawable.ic_animated_rounded_inventory_2,
                    R.string.title_explore to R.drawable.ic_animated_toy_fans,
                    R.string.title_notifications to R.drawable.ic_animated_rounded_notifications,
                    R.string.title_user to R.drawable.ic_animated_rounded_person,
                ).mapIndexed { index, (title, icon) ->
                    NavigationItem(
                        id = index.toString(),
                        icon = { AnimatedImageVector.animatedVectorResource(icon) },
                        title = { stringResource(title) },
                        onClick = {
                            pageAtActivation += selection.id.toInt()
                            measuredAtActivation += index in measures
                        },
                        content = { FeedPage(index) },
                    )
                }.toImmutableList()
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialExpressiveTheme {
                    MyScaffold(
                        modifier = Modifier.fillMaxSize().nestedScroll(observer).testTag("window"),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        bottomBar = {
                            val visible = 1f - hideFraction
                            MainNavigationBarSlot(
                                visibleFraction = visible,
                                navigationInsets = WindowInsets(bottom = 24.dp),
                            ) {
                                MainNavigationBar(
                                    currentPosition = selection.id.toInt(),
                                    onSelect = { index -> selection.activate(items[index]) { refreshedPages += index } },
                                    navigationItems = items,
                                    modifier = Modifier.testTag("navigation-bar"),
                                )
                            }
                        },
                    ) { padding ->
                        MainTabHost(
                            selectedId = selection.id,
                            navigationItems = items,
                            modifier = Modifier.fillMaxSize().padding(padding),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun FeedPage(index: Int) {
        val owner = LocalLifecycleOwner.current
        val requester = remember { FocusRequester() }
        DisposableEffect(index) {
            focusRequests[index] = requester
            compositions += index
            tabLifecycles[index] = owner
            onDispose { disposals += index }
        }
        var loaded by remember { mutableStateOf(false) }
        LazyLoad(loaded) {
            requests += index
            loaded = true
        }
        BackHandler { backTargets += index }
        Column(Modifier.fillMaxSize().layout { measurable, constraints ->
            measures[index] = measures.getOrDefault(index, 0) + 1
            val child = measurable.measure(constraints)
            layout(child.width, child.height) { child.place(0, 0) }
        }.testTag("page-$index").focusRequester(requester)
            .onFocusChanged { pageFocus[index] = it.isFocused }.focusable()) {
            Text(titles[index], Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxSize().testTag("feed")) {
                items(50) { row ->
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("${rowLabels[index]} ${row + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("贴吧讨论", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    private fun dragFeed(deltaY: Float) {
        compose.onNodeWithTag("feed").performTouchInput {
            down(center)
            moveBy(Offset(0f, deltaY), delayMillis = 100)
            advanceEventTime(400)
            up()
        }
        compose.waitForIdle()
    }

    @Test fun scrollTranslatesTheWholeNavigationBarAndRevealsIt() {
        show()
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        val feed = compose.onNodeWithTag("feed")
        val bar = compose.onNodeWithTag("navigation-bar")
        val visible = bar.getUnclippedBoundsInRoot()
        assertTrue(visible.height >= 48.dp)
        assertEquals(window.width, visible.width)
        assertEquals(window.bottom - 24.dp - visible.height, feed.getUnclippedBoundsInRoot().bottom)
        compose.captureReviewImage(view, "nav-visible.png")

        dragFeed(-82f)
        val partial = compose.runOnIdle { hideFraction }
        assertEquals("Expected half hide", 0.5f, partial, 0.01f)
        val translated = bar.getUnclippedBoundsInRoot()
        val safeBottom = window.bottom - 24.dp
        assertEquals(visible.height, translated.height)
        assertEquals(visible.width, translated.width)
        assertEquals(visible.left, translated.left)
        assertEquals(visible.top.value + visible.height.value * partial, translated.top.value, 1f)
        assertTrue(translated.bottom > safeBottom)
        val visiblePixels = bar.fetchSemanticsNode().boundsInRoot
        assertEquals(safeBottom.value, visiblePixels.bottom, 1f)
        compose.captureReviewImage(view, "nav-partial.png")
        // The same point in the visible surface retains its color while moving.
        val sampleX = visible.left.value.toInt() + 40
        assertEquals(
            screenshotPixel("nav-visible.png", sampleX, visible.top.value.toInt() + 12),
            screenshotPixel("nav-partial.png", sampleX, translated.top.value.toInt() + 12),
        )

        dragFeed(-220f)
        compose.runOnIdle { assertEquals(1f, hideFraction) }
        assertEquals(window.bottom - 24.dp, feed.getUnclippedBoundsInRoot().bottom)
        bar.assertDoesNotExist()
        compose.onAllNodes(hasClickAction() and hasText(titles[0])).assertCountEquals(0)
        compose.captureReviewImage(view, "nav-hidden.png")

        dragFeed(240f)
        compose.runOnIdle { assertEquals(0f, hideFraction) }
        assertEquals(visible, bar.getUnclippedBoundsInRoot())
        compose.captureReviewImage(view, "nav-revealed.png")
    }

    private fun screenshotPixel(name: String, x: Int, y: Int): Int {
        val path = File("build/reports/ui-screenshots", name)
        val bitmap = BitmapFactory.decodeFile(path.absolutePath)
        return try {
            bitmap.getPixel(x, y)
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun confirmedTapCommitsInTheInputEventAndDisplaysTheDestinationOnTheNextFrame() {
        show()
        val destination = compose.onNode(hasText(titles[3]) and hasClickAction())
        val viewport = compose.onNodeWithTag("page-0").getUnclippedBoundsInRoot()
        compose.mainClock.autoAdvance = false

        destination.performTouchInput { down(center) }
        compose.onNodeWithTag("page-0").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(pageAtActivation.isEmpty())
            assertTrue("The destination must start cold", 3 !in measures)
        }
        destination.performTouchInput { up() }

        // NavigationItem.onClick runs inside the actual tab activation, before a new frame.
        compose.runOnIdle {
            assertEquals(listOf(3), pageAtActivation)
            assertEquals("Cold page measurement must stay out of the tap handler", listOf(false), measuredAtActivation)
        }
        compose.mainClock.advanceTimeByFrame()
        destination.assertIsSelected()
        assertEquals(viewport, compose.onNodeWithTag("page-3").assertIsDisplayed().getUnclippedBoundsInRoot())
        compose.onNodeWithText("收藏 1").assertIsDisplayed()
        compose.onNodeWithTag("page-0").assertIsNotDisplayed()
    }

    @Test fun revisitingAnOpenedTabKeepsItsListAndOnlyTheVisibleTabHandlesBack() {
        show()
        fun open(index: Int) = compose.onNode(hasText(titles[index]) and hasClickAction()).performClick()
        compose.onNodeWithTag("feed").performScrollToIndex(12)
        compose.onNodeWithText("帖子 13").assertIsDisplayed()
        open(3)
        compose.onNodeWithText("收藏 1").assertIsDisplayed()
        compose.onNodeWithText("帖子 13").assertDoesNotExist()
        val measurements = compose.runOnIdle { measures.toMap() }
        compose.runOnIdle { hideFraction = 0.25f }
        compose.runOnIdle {
            assertEquals(measurements[0], measures[0])
            assertTrue(measures.getValue(3) > measurements.getValue(3))
            hideFraction = 0f
        }
        compose.runOnIdle {
            assertEquals(Lifecycle.State.CREATED, tabLifecycles.getValue(0).lifecycle.currentState)
            backDispatcher.onBackPressed()
        }
        open(0)
        compose.onNodeWithText("帖子 13").assertIsDisplayed()
        compose.onNodeWithText("收藏 1").assertDoesNotExist()
        compose.runOnIdle {
            backDispatcher.onBackPressed()
            assertEquals(listOf(3, 0), backTargets)
            assertEquals(listOf(0, 3), compositions)
            assertEquals(listOf(0, 3), requests)
            assertTrue(disposals.isEmpty())
            assertEquals(setOf(0, 3), measures.keys)
        }
    }

    @Test fun switchingFromAFocusedPageReleasesItAndRejectsFocusOnItsHiddenChildren() {
        show()
        compose.runOnIdle { focusRequests.getValue(0).requestFocus() }
        compose.runOnIdle {
            assertEquals(true, pageFocus[0])
            tabSelection.select("3")
        }
        compose.onNodeWithText("收藏 1").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(false, pageFocus[0])
            assertTrue(!focusRequests.getValue(0).requestFocus())
            assertTrue(focusRequests.getValue(3).requestFocus())
        }
    }

    @Test fun restoresSelectedTabByIdEvenIfExploreWasRemoved() {
        val restoration = StateRestorationTester(compose)
        var includeExplore by mutableStateOf(true)
        lateinit var selection: MainTabSelection
        restoration.setContent {
            selection = rememberMainTabSelection("home")
            val tabs = if (includeExplore) listOf("home", "explore", "notification", "user")
                else listOf("home", "notification", "user")
            Text("${selection.id} at ${tabs.indexOf(selection.id)}")
        }
        compose.runOnIdle { selection.select("notification") }
        compose.onNodeWithText("notification at 2").assertExists()
        compose.runOnIdle { includeExplore = false }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("notification at 1").assertExists()
    }

    @Test fun changingAccountClearsThePreviousTabsModelsAndRememberedContent() {
        var account by mutableStateOf("first")
        val models = mutableListOf<AccountTabProbe>()
        val remembered = mutableListOf<Any>()
        compose.setContent {
            val main: MainViewModel = viewModel()
            MainTabAccountScope(main, account) {
                val probe: AccountTabProbe = viewModel()
                val content = remember { Any() }
                DisposableEffect(probe) {
                    models += probe
                    remembered += content
                    onDispose {}
                }
            }
        }
        compose.runOnIdle {
            models.single().handle["accountNote"] = "first account only"
            account = "second"
        }
        compose.runOnIdle {
            assertEquals(2, models.size)
            assertTrue(models.first().cleared)
            assertTrue(!models.last().cleared)
            assertEquals(null, models.last().handle.get<String>("accountNote"))
            assertTrue(remembered[0] !== remembered[1])
        }
    }

    class AccountTabProbe(val handle: SavedStateHandle) : ViewModel() {
        var cleared = false
        override fun onCleared() { cleared = true }
    }

    @Test fun rapidTapsUseTheLatestPageAndCancellationDoesNotSelect() {
        show()
        val second = compose.onNode(hasText(titles[1]) and hasClickAction())
        val third = compose.onNode(hasText(titles[2]) and hasClickAction())
        val secondCenter = second.fetchSemanticsNode().boundsInRoot.center
        val thirdCenter = third.fetchSemanticsNode().boundsInRoot.center
        compose.mainClock.autoAdvance = false

        second.performTouchInput {
            down(center)
            cancel()
        }
        compose.runOnIdle { assertTrue(pageAtActivation.isEmpty()) }
        compose.onRoot().performTouchInput {
            // Keep the three real down/up pairs in the same frame; click() inserts a dwell time.
            down(secondCenter)
            up()
            down(thirdCenter)
            up()
            down(thirdCenter)
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf(1, 2, 2), pageAtActivation)
            assertEquals(listOf(2), refreshedPages)
        }
        compose.mainClock.advanceTimeByFrame()
        third.assertIsSelected()
        compose.onNodeWithTag("page-2").assertIsDisplayed()
        compose.onNodeWithText("消息 1").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(0, 2), compositions)
            assertEquals(setOf(0, 2), measures.keys)
        }
    }

    @Test fun physicalLeftDragAdvancesTabInRtl() {
        show(LayoutDirection.Rtl)
        val first = compose.onNode(hasText(titles[0]) and hasClickAction())
        val second = compose.onNode(hasText(titles[1]) and hasClickAction())
        assertTrue(first.getUnclippedBoundsInRoot().left > second.getUnclippedBoundsInRoot().left)
        first.performTouchInput { swipe(center, center - Offset(width.toFloat() * 1.2f, 0f), 400) }
        second.assertIsSelected()
        second.performTouchInput { swipe(center, center + Offset(width.toFloat() * 1.2f, 0f), 400) }
        first.assertIsSelected()
    }

    @Test fun tapDragAndReselectUseTheDisplayedTabs() {
        show()
        val second = compose.onNode(hasText(titles[1]) and hasClickAction())
        second.performTouchInput { click() }
        second.assertIsSelected()
        second.performTouchInput { swipe(center, center + Offset(width.toFloat() * 1.2f, 0f), 400) }
        val third = compose.onNode(hasText(titles[2]) and hasClickAction())
        third.assertIsSelected()
        third.performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(2), refreshedPages) }
        third.performTouchInput { swipe(center, center - Offset(width.toFloat() * 2.2f, 0f), 400) }
        compose.onNode(hasText(titles[0]) and hasClickAction()).assertIsSelected()
    }

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Config(qualifiers = "zh-rCN-w960dp-h720dp-mdpi")
    @Test fun adaptiveNavigationPreservesSelectedPageWhenTheScreenWidens() {
        var layout by mutableStateOf(MainNavigationType.NAVIGATION_RAIL)
        var selected by mutableStateOf(0)
        val pageSessions = mutableListOf<Unit>()
        compose.setContent {
            val hostView = LocalView.current
            SideEffect { view = hostView }
            MaterialExpressiveTheme {
                CompositionLocalProvider(LocalNavigator provides EmptyDestinationsNavigator) {
                    val icons = listOf(
                        R.drawable.ic_animated_rounded_inventory_2, R.drawable.ic_animated_toy_fans,
                        R.drawable.ic_animated_rounded_notifications, R.drawable.ic_animated_rounded_person,
                    )
                    val items = titles.mapIndexed { index, title ->
                        NavigationItem(
                            id = index.toString(),
                            title = { title },
                            icon = { AnimatedImageVector.animatedVectorResource(icons[index]) },
                            badge = index == 2,
                            badgeText = "7",
                        )
                    }.toImmutableList()
                    NavigationWrapper(selected, { selected = it }, items, layout, MainNavigationContentPosition.CENTER) {
                        DisposableEffect(Unit) {
                            pageSessions.add(Unit)
                            onDispose { }
                        }
                        Text(titles[selected], modifier = Modifier.padding(24.dp).testTag("active-page"))
                    }
                }
            }
        }
        compose.onNode(hasText("动态") and hasClickAction()).performClick().assertIsSelected()
        compose.captureReviewImage(view, "nav-rail.png")
        compose.runOnIdle { layout = MainNavigationType.PERMANENT_NAVIGATION_DRAWER }
        compose.onNode(hasText("动态") and hasClickAction()).assertIsSelected()
        compose.onNodeWithTag("active-page").assertTextEquals("动态")
        compose.captureReviewImage(view, "nav-drawer.png")
        compose.runOnIdle { assertEquals(1, pageSessions.size) }
    }

    @Test fun officialBarProvidesOneSelectedAccessibleDestinationAfterReversal() {
        show()
        val from = compose.onNode(hasText(titles[0]) and hasClickAction())
        val to = compose.onNode(hasText(titles[2]) and hasClickAction())
        compose.onAllNodes(isSelectable()).assertCountEquals(titles.size)
        titles.forEach { title ->
            val bounds = compose.onNode(hasText(title) and hasClickAction()).getUnclippedBoundsInRoot()
            assertTrue("Navigation targets must be at least 48 dp", bounds.width >= 48.dp && bounds.height >= 48.dp)
        }
        to.performClick()
        to.assertIsSelected()
        from.performClick()
        from.assertIsSelected()
        compose.onAllNodes(isSelected()).assertCountEquals(1)
    }
}

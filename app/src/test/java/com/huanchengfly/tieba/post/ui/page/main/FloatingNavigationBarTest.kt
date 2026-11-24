package com.huanchengfly.tieba.post.ui.page.main

import android.app.Application
import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import kotlinx.collections.immutable.toImmutableList
import java.io.File
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
class FloatingNavigationBarTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View
    private var selected by mutableIntStateOf(0)
    private var hideFraction by mutableFloatStateOf(0f)
    private var reselected = -1
    private val titles = listOf("首页", "动态", "消息", "我")

    @OptIn(ExperimentalAnimationGraphicsApi::class)
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
                    )
                }.toImmutableList()
            }
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme {
                    val palette = MaterialTheme.colorScheme
                    val colors = ExtendedColors(
                        theme = "default", isNightMode = false,
                        accent = palette.primary, bottomBar = palette.surfaceContainer,
                        text = palette.onSurface, textSecondary = palette.onSurfaceVariant,
                    )
                    MyScaffold(
                        modifier = Modifier.fillMaxSize().nestedScroll(observer).testTag("window"),
                        backgroundColor = palette.background,
                        bottomBar = {
                            val visible = 1f - hideFraction
                            FloatingNavigationBarSlot(
                                visibleFraction = visible,
                                navigationInsets = WindowInsets(bottom = 24.dp),
                            ) {
                                FloatingBottomNavigation(
                                    currentPosition = selected,
                                    onChangePosition = {
                                        selected = it
                                        hideFraction = 0f
                                    },
                                    onReselected = { reselected = it },
                                    navigationItems = items,
                                    themeColors = colors,
                                    modifier = Modifier.testTag("capsule"),
                                )
                            }
                        },
                    ) { padding ->
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Text(titles[selected], Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
                            HorizontalDivider()
                            LazyColumn(Modifier.fillMaxSize().testTag("feed")) {
                                items(50) { index ->
                                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text("帖子 ${index + 1}", style = MaterialTheme.typography.titleMedium)
                                        Text("贴吧讨论", color = palette.onSurfaceVariant)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
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

    @Test fun scrollHidesAndRevealsTheFloatingBar() {
        show()
        val window = compose.onNodeWithTag("window").getUnclippedBoundsInRoot()
        val feed = compose.onNodeWithTag("feed")
        val capsule = compose.onNodeWithTag("capsule")
        val visible = capsule.getUnclippedBoundsInRoot()
        assertEquals(64.dp, visible.height)
        assertEquals(window.bottom - 98.dp, feed.getUnclippedBoundsInRoot().bottom)
        compose.captureReviewImage(view, "nav-visible.png")

        dragFeed(-82f)
        val partial = compose.runOnIdle { hideFraction }
        assertEquals("Expected half hide", 0.5f, partial, 0.01f)
        val translated = capsule.getUnclippedBoundsInRoot()
        val safeBottom = window.bottom - 24.dp
        assertEquals(visible.height, translated.height)
        assertEquals(visible.width, translated.width)
        assertEquals(visible.left, translated.left)
        assertEquals(visible.top.value + 74f * partial, translated.top.value, 1f)
        assertTrue(translated.bottom > safeBottom)
        val visiblePixels = capsule.fetchSemanticsNode().boundsInRoot
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
        capsule.assertDoesNotExist()
        compose.onAllNodes(hasClickAction() and hasText(titles[0])).assertCountEquals(0)
        compose.captureReviewImage(view, "nav-hidden.png")

        dragFeed(240f)
        compose.runOnIdle { assertEquals(0f, hideFraction) }
        assertEquals(visible, capsule.getUnclippedBoundsInRoot())
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
        compose.runOnIdle { assertEquals(2, reselected) }
        third.performTouchInput { swipe(center, center - Offset(width.toFloat() * 2.2f, 0f), 400) }
        compose.onNode(hasText(titles[0]) and hasClickAction()).assertIsSelected()
    }
}

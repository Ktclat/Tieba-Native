package com.huanchengfly.tieba.post.ui

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelSearchNavigationTest {
    @get:Rule(order = 0) val compose = createAndroidComposeRule<MainActivityV2>()
    @get:Rule(order = 1) val failureScreen = object : TestWatcher() {
        override fun failed(e: Throwable, description: Description) {
            runCatching { capture("unexpected-${description.methodName}") }
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val output get() = File(context.filesDir, "phone-review").apply { mkdirs() }
    private val density get() = context.resources.displayMetrics.density
    private val selectable = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
    private val back get() = context.getString(R.string.button_back)

    private fun await(matcher: SemanticsMatcher) {
        compose.waitUntil(15_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(matcher: SemanticsMatcher) {
        await(matcher)
        compose.onNode(matcher).assertIsDisplayed().performTouchInput { click() }
    }

    private fun clickTab(label: String) {
        val tab = hasText(label) and selectable and hasClickAction()
        click(tab)
        compose.onNode(tab).assertIsSelected()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val raw = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val image = if (raw.config == Bitmap.Config.HARDWARE) {
            raw.copy(Bitmap.Config.ARGB_8888, false).also { raw.recycle() }
        } else raw
        try {
            File(output, "$name.png").outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            image.recycle()
        }
    }

    @Test fun editAndSubmitChineseFromTheTopRightSearchButton() {
        await(hasText("随便看看"))
        clickTab("动态")
        await(hasText("热榜"))
        val searchAction = hasContentDescription(context.getString(R.string.title_search)) and hasClickAction()
        val button = compose.onNode(searchAction).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Search entry must be in the top-right corner", button.center.x > context.resources.displayMetrics.widthPixels * .75f && button.top < 100 * density)
        click(searchAction)

        val input = compose.onNode(hasSetTextAction())
        await(hasSetTextAction())
        val hint = context.getString(R.string.hint_search)
        val placeholder = compose.onNodeWithText(hint, useUnmergedTree = true).assertIsDisplayed()
        val layout = mutableListOf<TextLayoutResult>()
        placeholder.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertTrue("The entire Chinese placeholder should fit", !layout.single().hasVisualOverflow)
        val field = input.fetchSemanticsNode().boundsInRoot
        assertTrue("Search field is shorter than 56dp: $field", field.height >= 56 * density - 1)
        capture("search-empty")

        val query = "北京周末徒步路线与公共交通换乘攻略"
        input.performTouchInput { click() }
        input.performTextInput(query)
        fun value() = input.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        compose.waitUntil(10_000) { value() == query }
        capture("search-chinese")
        click(hasContentDescription(context.getString(R.string.button_clear)))
        compose.waitUntil(10_000) { value().isEmpty() }
        compose.onNodeWithText(hint, useUnmergedTree = true).assertIsDisplayed()
        input.performTextInput(query)
        input.performImeAction()
        val resultTab = hasText(context.getString(R.string.title_search_thread)) and selectable
        await(resultTab)
        compose.onNode(resultTab).assertIsDisplayed()
        assertEquals(query, value())
        click(hasContentDescription(back))
        compose.waitUntil(10_000) { value().isEmpty() }
        click(hasContentDescription(back))
        await(hasText("热榜"))
    }

    @Test fun switchBetweenStandardAndFloatingNavigationInSettings() {
        val toggleTitle = context.getString(R.string.title_floating_navigation_bar)
        val toggle = hasText(toggleTitle) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)
        fun openPreference() {
            clickTab("我")
            click(hasText("设置") and hasClickAction())
            click(hasText("个性化") and hasClickAction())
            await(hasText("字体大小"))
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(toggle)
            compose.onNode(toggle).assertIsDisplayed()
        }
        fun returnHome() {
            click(hasContentDescription(back))
            await(hasText("使用习惯"))
            click(hasContentDescription(back))
            await(hasText("请先登录"))
            clickTab("首页")
            await(hasText("随便看看"))
        }
        fun bounds(label: String) = compose.onNode(hasText(label) and selectable).fetchSemanticsNode().boundsInRoot

        await(hasText("随便看看"))
        val standardFirst = bounds("首页")
        val standardLast = bounds("我")
        capture("navigation-standard")
        openPreference()
        compose.onNode(toggle).assertIsOff()
        click(toggle)
        compose.onNode(toggle).assertIsOn()
        capture("navigation-setting")
        returnHome()

        val floatingFirst = bounds("首页")
        val floatingLast = bounds("我")
        assertTrue("Floating navigation needs margins on both sides",
            floatingFirst.left > standardFirst.left + 8 * density && floatingLast.right < standardLast.right - 8 * density)
        assertTrue("Floating navigation must be centered",
            abs((floatingFirst.left + floatingLast.right) - (standardFirst.left + standardLast.right)) < 4 * density)
        assertTrue("Floating navigation must clear the system navigation area",
            floatingFirst.bottom < standardFirst.bottom - 8 * density)
        capture("navigation-floating-home")
        clickTab("消息")
        await(hasText("登录后查看消息"))
        clickTab("我")
        await(hasText("请先登录"))
        capture("navigation-floating-profile")
        openPreference()
        click(toggle)
        compose.onNode(toggle).assertIsOff()
        returnHome()
        assertEquals(standardFirst.left, bounds("首页").left, 1f)
    }
}

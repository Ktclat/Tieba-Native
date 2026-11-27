package com.huanchengfly.tieba.post.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            runCatching { compose.capturePhoneReview("unexpected-${description.methodName}") }
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
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
        val layouts = mutableListOf<TextLayoutResult>()
        placeholder.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertFalse(layout.didOverflowHeight)
        assertFalse(layout.isLineEllipsized(0))
        assertEquals(hint.length, layout.getLineEnd(0, visibleEnd = true))
        assertTrue("Last character must fit in the hint bounds", layout.getLineRight(0) <= layout.size.width)
        val field = input.fetchSemanticsNode().boundsInRoot
        assertTrue("Search field is shorter than 56dp: $field", field.height >= 56 * density - 1)
        compose.capturePhoneReview("search-empty")

        val query = "北京周末徒步路线与公共交通换乘攻略"
        input.performTouchInput { click() }
        input.performTextInput(query)
        fun value() = input.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        compose.waitUntil(10_000) { value() == query }
        compose.capturePhoneReview("search-chinese")
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

    @Test fun saveReopenAndClearIndependentLocalBlockRules() {
        val forum = context.getString(R.string.block_rules_forum)
        val user = context.getString(R.string.block_rules_user)
        val comment = context.getString(R.string.block_rules_comment)
        val rules = mapOf(
            forum to "^(摄影|数码)$\n^游戏",
            user to "^推广号[0-9]+$",
            comment to "加群|代练\n免邮",
        )
        val field = hasSetTextAction()
        val input = compose.onNode(field)
        val save = hasText(context.getString(R.string.block_rules_save)) and hasClickAction()
        val blockedForum = hasText(context.getString(R.string.tip_locally_blocked_forum))
        fun visitForum() {
            val uri = Uri.parse("com.baidu.tieba://unidispatch/frs").buildUpon()
                .appendQueryParameter("kw", "摄影").build()
            compose.activityRule.scenario.onActivity { activity ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(activity.packageName))
            }
        }
        fun openEditor(label: String) {
            click(hasText(label) and hasClickAction())
            await(field)
        }
        fun waitForSettings() {
            compose.waitUntil(10_000) { compose.onAllNodes(field).fetchSemanticsNodes().isEmpty() }
            await(hasText(forum) and hasClickAction())
        }
        fun expectInput(text: String) {
            compose.waitUntil(10_000) {
                input.fetchSemanticsNode().config[SemanticsProperties.EditableText].text == text
            }
        }

        await(hasText("随便看看"))
        clickTab("我")
        click(hasText("设置") and hasClickAction())
        click(hasText(context.getString(R.string.title_block_settings)) and hasClickAction())
        for ((label, expression) in rules) {
            openEditor(label)
            expectInput("")
            input.performTextReplacement(expression)
            click(save and isEnabled())
            waitForSettings()
        }
        compose.capturePhoneReview("local-block-settings")
        visitForum()
        await(blockedForum)
        compose.onNode(blockedForum).assertIsDisplayed()
        compose.capturePhoneReview("local-block-forum")
        click(hasContentDescription(back))
        waitForSettings()

        openEditor(comment)
        expectInput(rules.getValue(comment))
        compose.capturePhoneReview("local-block-comment")
        input.performTextReplacement("加群\n[")
        await(hasText("第 2 行的正则有误", substring = true))
        compose.onNode(save).assertIsNotEnabled()
        compose.capturePhoneReview("local-block-invalid")
        click(hasContentDescription(back))
        waitForSettings()

        for ((label, expression) in rules) {
            openEditor(label)
            expectInput(expression)
            input.performTextReplacement("")
            click(save and isEnabled())
            waitForSettings()
        }
        openEditor(comment)
        expectInput("")
        click(hasContentDescription(back))
        waitForSettings()
        visitForum()
        await(hasText(context.getString(R.string.button_retry)))
        compose.onNodeWithText(context.getString(R.string.title_forum, "摄影")).assertIsDisplayed()
        compose.onNode(blockedForum).assertDoesNotExist()
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
        compose.capturePhoneReview("navigation-standard")
        openPreference()
        compose.onNode(toggle).assertIsOff()
        click(toggle)
        compose.onNode(toggle).assertIsOn()
        compose.capturePhoneReview("navigation-setting")
        returnHome()

        val floatingFirst = bounds("首页")
        val floatingLast = bounds("我")
        assertTrue("Floating navigation needs margins on both sides",
            floatingFirst.left > standardFirst.left + 8 * density && floatingLast.right < standardLast.right - 8 * density)
        assertTrue("Floating navigation must be centered",
            abs((floatingFirst.left + floatingLast.right) - (standardFirst.left + standardLast.right)) < 4 * density)
        assertTrue("Floating navigation must clear the system navigation area",
            floatingFirst.bottom < standardFirst.bottom - 8 * density)
        compose.capturePhoneReview("navigation-floating-home")
        clickTab("动态")
        await(hasText("热榜"))
        compose.capturePhoneReview("navigation-floating-explore")
        clickTab("消息")
        await(hasText("登录后查看消息"))
        clickTab("我")
        await(hasText("请先登录"))
        compose.capturePhoneReview("navigation-floating-profile")
        openPreference()
        click(toggle)
        compose.onNode(toggle).assertIsOff()
        returnHome()
        assertEquals(standardFirst.left, bounds("首页").left, 1f)
    }
}


internal fun ComposeContentTestRule.capturePhoneReview(name: String): File {
    waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val directory = File(instrumentation.targetContext.filesDir, "phone-review").apply { mkdirs() }
    val raw = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
    val image = if (raw.config == Bitmap.Config.HARDWARE) {
        raw.copy(Bitmap.Config.ARGB_8888, false).also { raw.recycle() }
    } else raw
    val target = File(directory, "$name.png")
    try {
        target.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    } finally {
        image.recycle()
    }
    return target
}

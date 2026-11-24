package com.huanchengfly.tieba.post.ui.page.history

import android.app.Application
import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = App::class, qualifiers = "w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryPageTest {
    @get:Rule val compose = createComposeRule()
    private val restore = StateRestorationTester(compose)
    private lateinit var view: View

    @Before fun seedHistory() {
        LitePal.initialize(ApplicationProvider.getApplicationContext<Application>())
        HistoryUtil.deleteAll()
        val now = System.currentTimeMillis()
        listOf("如何设置阅读字号", "平板上的分栏阅读", "怎样查看收藏的帖子").forEachIndexed { index, title ->
            check(History(title, "${1000 + index}", HistoryUtil.TYPE_THREAD, now - index * 1000L, username = "示例作者").save())
        }
        check(History("平板吧", "平板", HistoryUtil.TYPE_FORUM, now).save())
    }

    private fun show() {
        restore.setContent {
            val local = LocalView.current
            SideEffect { view = local }
            TiebaLiteTheme {
                HistoryPage(EmptyDestinationsNavigator)
            }
        }
        waitForText("如何设置阅读字号")
    }

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun firstGroupAndRowStartBelowTabs() {
        show()
        val tab = compose.onNodeWithText("贴子记录").fetchSemanticsNode().boundsInRoot
        val group = compose.onNodeWithText("今天早些时候").fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithText("如何设置阅读字号").fetchSemanticsNode().boundsInRoot
        assertTrue("tab=$tab, group=$group, row=$row", group.top >= tab.bottom && row.top >= group.bottom)
        compose.captureReviewImage(view, "browsing-history-after.png")
        println("Browsing history bounds: tab=$tab, first group=$group, first row=$row")
    }

    @Test fun filtersBothTabsRestoresTheDraftAndClearsAnUnmatchedQuery() {
        show()
        compose.onNodeWithContentDescription("搜索浏览记录").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("平板")
        waitForText("平板上的分栏阅读")
        compose.onNodeWithText("如何设置阅读字号").assertDoesNotExist()
        compose.onNodeWithText("经过贴吧").performClick()
        waitForText("平板吧")
        restore.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("平板")
        waitForText("平板吧")
        compose.captureReviewImage(view, "browsing-history-forum-search.png")
        compose.onNode(hasSetTextAction()).performTextInput("找不到")
        waitForText("没有找到匹配的浏览记录")
        compose.onNodeWithText("清除搜索词").performClick()
        waitForText("平板吧")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test fun clearingRequiresConfirmationAndRefreshesBothTabs() {
        show()
        val clear = compose.onNodeWithContentDescription("清空浏览记录")
        clear.performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("如何设置阅读字号").assertIsDisplayed()
        clear.performClick()
        compose.onNodeWithText("清除").performClick()
        waitForText("暂无帖子浏览记录")
        compose.onNodeWithText("经过贴吧").performClick()
        waitForText("暂无贴吧浏览记录")
    }
}

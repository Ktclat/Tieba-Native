package com.huanchengfly.tieba.post.ui.page.search

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.captureReviewImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w320dp-h568dp")
class SearchHistorySectionTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private val entries = listOf(
        "深色模式", "Compose", "字号设置", "平板布局", "收藏", "阅读历史", "动态配色", "导航",
    ).toMutableStateList()
    private lateinit var renderingView: View
    private val searched = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val resources get() = RuntimeEnvironment.getApplication().resources

    private fun show() {
        ThemeUtils.init(App.ThemeDelegate)
        compose.setContent {
            val view = LocalView.current
            SideEffect { renderingView = view }
            TiebaLiteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SearchHistorySection(
                        entries = entries,
                        term = { it },
                        clearMessage = resources.getString(R.string.search_history_clear_forum_message, "测试"),
                        onSearch = { searched.add(it) },
                        onDelete = { deleted.add(it); entries.remove(it) },
                        onClear = { entries.clear() },
                    )
                }
            }
        }
    }

    private fun clickListText(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    @Test fun expandsAndCollapsesRecentTermsAndSearchesTheSelectedTerm() {
        show()
        val eighth = entries.last()
        clickListText(resources.getString(R.string.button_expand_more_history))
        clickListText(eighth)
        compose.runOnIdle { assertEquals(listOf(eighth), searched) }
        clickListText(resources.getString(R.string.button_expand_less_history))
        compose.onNodeWithText(eighth).assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun manageExposesNamedDeleteActionsAndLongPressStillDeletes() {
        show()
        val first = entries[0]
        val second = entries[1]
        val deleteLabel = resources.getString(R.string.search_history_delete_term, first)
        compose.onNodeWithContentDescription(deleteLabel).assertDoesNotExist()
        clickListText(resources.getString(R.string.search_history_manage))
        compose.onNodeWithText(resources.getString(R.string.button_clear_all)).assertIsDisplayed()
        val deleteAction = compose.onNodeWithContentDescription(deleteLabel).assertIsDisplayed()
        compose.captureReviewImage(renderingView, "search-history-manage.png")
        deleteAction.performClick()
        compose.onNodeWithText(first).assertDoesNotExist()
        clickListText(resources.getString(R.string.button_finish))
        compose.onNodeWithText(second).performSemanticsAction(SemanticsActions.OnLongClick)
        compose.runOnIdle {
            assertEquals(listOf(first, second), deleted)
            assertEquals(emptyList<String>(), searched)
        }
    }

    @Test fun clearShowsForumScopeAndCancellationPreservesHistory() {
        show()
        val initial = entries.toList()
        clickListText(resources.getString(R.string.search_history_manage))
        val clear = resources.getString(R.string.button_clear_all)
        clickListText(clear)
        compose.onNodeWithText(resources.getString(R.string.search_history_clear_forum_message, "测试"))
            .assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.button_cancel)).performClick()
        compose.runOnIdle { assertEquals(initial, entries.toList()) }
        clickListText(clear)
        compose.onNodeWithText(resources.getString(R.string.button_clear)).performClick()
        compose.onNodeWithText(resources.getString(R.string.search_history_empty)).assertIsDisplayed()
        compose.onNodeWithText(clear).assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.search_history_manage)).assertDoesNotExist()
    }
}

package com.huanchengfly.tieba.post.ui.page.search

import android.app.Application
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.forum.searchpost.ForumSearchOptions
import com.huanchengfly.tieba.post.ui.page.forum.searchpost.ForumSearchPostFilterType
import com.huanchengfly.tieba.post.ui.page.forum.searchpost.ForumSearchPostSortType
import com.huanchengfly.tieba.post.ui.page.main.home.SearchBox
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchControlsTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private val resources get() = RuntimeEnvironment.getApplication().resources

    @Test fun suggestionCanBeEditedBeforeSubmittingOrOpenedDirectly() {
        val submissions = mutableListOf<String>()
        val term = "安卓手机"
        compose.setContent {
            var draft by remember { mutableStateOf("") }
            MaterialTheme {
                Column {
                    SearchTopBar(draft, { draft = it }, { submissions.add(it) })
                    SearchSuggestionList(persistentListOf(term), { submissions.add(it) }, { draft = it })
                }
            }
        }
        val input = compose.onNode(hasSetTextAction())
        input.performClick().performTextInput("安")
        compose.onNodeWithContentDescription(resources.getString(R.string.search_fill_suggestion, term)).performClick()
        input.assertTextContains(term).performTextInput("推荐")
        input.performImeAction()
        compose.onNodeWithText(term).performClick()
        compose.runOnIdle { assertEquals(listOf("安卓手机推荐", term), submissions) }
    }

    @Test fun globalSearchRendersTheCompleteHintAndDraft() {
        val entry = mutableStateOf("")
        lateinit var host: View
        compose.setContent {
            val view = LocalView.current
            SideEffect { host = view }
            TiebaLiteTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                    SearchTopBar(entry.value, { entry.value = it })
                }
            }
        }
        compose.captureReviewImage(host, "search-placeholder.png")
        compose.runOnIdle { entry.value = "摄影吧" }
        compose.onNode(hasSetTextAction()).assertTextContains("摄影吧")
        compose.captureReviewImage(host, "search-input.png")
    }

    @Test fun forumSearchFieldFitsChineseHintAndInputAndKeepsFocusAfterClearing() {
        val hint = resources.getString(R.string.hint_search_in_ba, "安卓手机")
        val term = "安卓真机体验"
        compose.setContent {
            var draft by remember { mutableStateOf("") }
            MaterialTheme { SearchTopBar(draft, { draft = it }, hint = hint) }
        }

        val input = compose.onNode(hasSetTextAction())
        assertTrue(input.getUnclippedBoundsInRoot().height >= 56.dp)
        val hintNode = compose.onNodeWithText(hint, useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        hintNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
        input.performClick().performTextInput(term)
        input.assertTextContains(term)
        compose.onNodeWithContentDescription(resources.getString(R.string.button_clear)).performClick()
        input.assertIsFocused()
        hintNode.assertExists()
    }

    @Test fun homeSearchCanOpenAgainAfterReturning() {
        var openCount = 0
        compose.setContent { MaterialTheme { SearchBox { openCount++ } } }
        val hint = compose.onNodeWithText(resources.getString(R.string.hint_search))
        repeat(2) {
            hint.performTouchInput { click() }
            compose.waitForIdle()
        }
        compose.runOnIdle { assertEquals(2, openCount) }
    }

    @Test fun changingForumSortPreservesTheFilter() {
        val changes = mutableListOf<Pair<Int, Int>>()
        compose.setContent {
            var sort by remember { mutableIntStateOf(ForumSearchPostSortType.NEWEST) }
            var filter by remember { mutableIntStateOf(ForumSearchPostFilterType.ALL) }
            MaterialTheme {
                ForumSearchOptions(sort, filter) { newSort, newFilter ->
                    sort = newSort
                    filter = newFilter
                    changes.add(newSort to newFilter)
                }
            }
        }
        val onlyThreads = compose.onNodeWithText(resources.getString(R.string.title_search_filter_only_thread))
        onlyThreads.performClick()
        compose.onNodeWithText(resources.getString(R.string.title_search_post_sort_by_time)).performClick()
        compose.onNodeWithText(resources.getString(R.string.title_search_post_sort_by_relevant)).performClick()
        onlyThreads.performClick()
        compose.runOnIdle {
            assertEquals(listOf(
                ForumSearchPostSortType.NEWEST to ForumSearchPostFilterType.ONLY_THREAD,
                ForumSearchPostSortType.RELATIVE to ForumSearchPostFilterType.ONLY_THREAD,
                ForumSearchPostSortType.RELATIVE to ForumSearchPostFilterType.ALL,
            ), changes)
        }
    }

    @Test fun selectingThreadThenChangingSortKeepsTheThreadTabSelected() {
        val posts = resources.getString(R.string.title_search_thread)
        val oldest = resources.getString(R.string.title_search_order_old)
        compose.setContent {
            var tab by remember { mutableStateOf(SearchTab.Forum) }
            var sort by remember { mutableIntStateOf(SearchThreadSortType.SORT_TYPE_NEWEST) }
            MaterialTheme { SearchTabs(tab, sort, { tab = it }, { sort = it }) }
        }
        compose.onNodeWithText(posts).performClick().assertIsSelected().performClick()
        compose.onNodeWithText(resources.getString(R.string.title_search_order_new)).assertIsSelected()
        compose.onNodeWithText(oldest).performClick()
        compose.onNodeWithText(posts).assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, resources.getString(R.string.search_current_sort, oldest)))
    }
}

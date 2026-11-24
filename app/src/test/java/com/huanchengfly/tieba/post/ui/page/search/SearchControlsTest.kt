package com.huanchengfly.tieba.post.ui.page.search

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w320dp-h568dp")
class SearchControlsTest {
    @get:Rule val compose = createComposeRule()
    private val resources get() = RuntimeEnvironment.getApplication().resources

    @Test fun suggestionCanBeEditedBeforeSubmittingOrOpenedDirectly() {
        val submissions = mutableListOf<String>()
        val term = "安卓手机"
        compose.setContent {
            var draft by remember { mutableStateOf("") }
            MaterialTheme {
                Column {
                    Box(Modifier.height(48.dp)) {
                        SearchTopBar(draft, { draft = it }, { submissions.add(it) })
                    }
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

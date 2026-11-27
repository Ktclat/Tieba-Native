package com.huanchengfly.tieba.post.ui.page.threadstore

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w412dp-h915dp-mdpi")
class FavoriteSearchPaginationTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun failedSecondPageCanBeRetriedAndSearchHonorsLocalRulesOnLaterPages() {
        val first = (1..20).map { exampleFavorite.copy(threadId = it.toString(), title = "旧帖 $it") }
        val target = exampleFavorite.copy(threadId = "21", title = "稀有星图")
        val hidden = target.copy(threadId = "22", title = "稀有收藏", forumName = "屏蔽吧")
        val rules = LocalBlockRules.fromText(forum = "屏蔽")
        var state by mutableStateOf(ThreadStoreUiState().withPage(first, refresh = true))
        val offsets = mutableListOf<Int>()
        compose.setContent {
            TiebaLiteTheme {
                CompositionLocalProvider(LocalContentBlockRules provides rules) {
                    FavoriteListContent(
                        state = state, progress = emptyMap(),
                        onRefresh = { error("Expected a second-page retry") },
                        onLoadMore = {
                            offsets += state.nextOffset
                            state = when (offsets.size) {
                                1 -> state.withLoadError(IllegalStateException("连接中断"), refresh = false)
                                2 -> state.withPage(listOf(target, hidden), refresh = false)
                                else -> state.withPage(emptyList(), refresh = false)
                            }
                        },
                        onOpen = { _, _ -> }, onUserClick = {}, onDelete = {},
                    )
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("稀有")
        compose.onNodeWithText("连接中断").assertIsDisplayed()
        compose.onNodeWithText("没有符合条件的收藏").assertDoesNotExist()
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText(target.title, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(hidden.title, useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(20, 20, 22), offsets) }
    }
}

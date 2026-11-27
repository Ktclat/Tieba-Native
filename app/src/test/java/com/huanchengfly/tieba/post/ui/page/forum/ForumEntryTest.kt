package com.huanchengfly.tieba.post.ui.page.forum

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideShouldLoad
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import org.junit.Assert.assertEquals
import org.litepal.LitePal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ForumEntryTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun forumTabsFillEqualHalvesAndOnlyReselectingLatestOpensSorting() {
        val page = mutableIntStateOf(1)
        val sort = mutableIntStateOf(1)
        val changed = mutableListOf<Int>()
        compose.setContent {
            TiebaLiteTheme {
                ForumTabs(
                    selectedPage = page.intValue,
                    sortType = sort.intValue,
                    onPageSelected = { page.intValue = it },
                    onSortSelected = {
                        changed += it
                        sort.intValue = it
                    },
                )
            }
        }
        val latest = compose.onNodeWithText("最新")
        val featured = compose.onNodeWithText("精华")
        val left = latest.assertHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
        val right = featured.assertIsSelected().assertHeightIsEqualTo(48.dp).fetchSemanticsNode().boundsInRoot
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(screen.left, left.left, 1f)
        assertEquals(left.right, right.left, 1f)
        assertEquals(right.right, screen.right, 1f)
        assertEquals(left.width, right.width, 1f)

        latest.performClick().assertIsSelected()
        val byReply = compose.onNodeWithText("回复时间排序")
        val byPost = compose.onNodeWithText("发贴时间排序")
        byReply.assertDoesNotExist()
        latest.performClick()
        byPost.assertIsSelected().performClick()
        byPost.assertDoesNotExist()
        latest.performClick()
        byReply.performClick()
        latest.assertIsSelected().performClick()
        byReply.assertIsSelected()
        assertEquals(listOf(0), changed)
    }

    @Test fun coldEntryHasItsOwnNavigatorBeforeTheNetworkRequestStarts() {
        var backs = 0
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigateUp(): Boolean { backs++; return true }
        }
        compose.setContent {
            TiebaLiteTheme {
                ProvideShouldLoad(false) {
                    ForumPage("摄影", navigator = navigator)
                }
            }
        }
        compose.onNodeWithText("摄影吧").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        assertEquals(1, backs)
    }

    @Test fun failedLoadCanRetryAndRenderAForumWithoutOptionalMetadata() {
        LitePal.initialize(ApplicationProvider.getApplicationContext<Application>())
        val state = mutableStateOf(ForumPartialChange.Load.Failure(IllegalStateException()).reduce(ForumUiState()))
        val requests = mutableListOf<ForumUiIntent>()
        compose.setContent {
            TiebaLiteTheme {
                ProvideShouldLoad(false) {
                    ForumContent("摄影", 1, state.value, EmptyDestinationsNavigator, onIntent = {
                        requests += it
                        state.value = ForumPartialChange.Load.Start.reduce(state.value)
                    })
                }
            }
        }
        compose.onNodeWithText("摄影吧").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        assertEquals(listOf(ForumUiIntent.Load("摄影", 1)), requests)
        compose.onNodeWithText("重试").assertDoesNotExist()
        compose.runOnIdle {
            state.value = ForumPartialChange.Load.Success(ForumInfo(id = 120L, name = "摄影"), null)
                .reduce(state.value)
        }
        compose.onNodeWithText("摄影吧").assertIsDisplayed()
        compose.onNodeWithText("精华").performClick().assertIsSelected()
        compose.onRoot().performTouchInput { swipeRight() }
        compose.onNodeWithText("最新").assertIsSelected()
    }
}

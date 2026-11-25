package com.huanchengfly.tieba.post.ui.page.search.thread

import android.app.Application
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadList
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SearchThreadListLayoutTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun consecutiveCardsUseOnlyTheirOwnSpacing() {
        val first = SearchThreadBean.ThreadInfoBean(
            tid = "101", pid = "1", title = "老街夜景", content = "刚刚整理了周末拍的照片。",
            time = "1695372000", modifiedTime = 0, postNum = "6", likeNum = "12",
            shareNum = "1", forumId = "1", forumName = "摄影",
            user = SearchThreadBean.UserInfoBean("路过的摄影师", null, "7", null),
            type = 0, forumInfo = SearchThreadBean.ForumInfo("摄影", ""),
        )
        val second = first.copy(tid = "102", title = "河畔日落", content = "傍晚的光线让水面更柔和。")
        lateinit var view: View
        lateinit var list: LazyListState
        compose.setContent {
            val renderingView = LocalView.current
            val state = rememberLazyListState()
            SideEffect {
                view = renderingView
                list = state
            }
            TiebaLiteTheme {
                ProvideNavigator(EmptyDestinationsNavigator) {
                    SearchThreadList(
                        data = persistentListOf(first, second),
                        lazyListState = state,
                        onItemClick = {}, onItemUserClick = {}, onItemForumClick = {},
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
        compose.runOnIdle {
            val items = list.layoutInfo.visibleItemsInfo
            assertEquals(2, items.size)
            // These equal-height results have the same content structure. A rule before
            // the second card would increase its measured height by the rule's thickness.
            assertEquals(items[0].size, items[1].size)
        }
        compose.captureReviewImage(view, "search-thread-card-spacing.png")
    }
}

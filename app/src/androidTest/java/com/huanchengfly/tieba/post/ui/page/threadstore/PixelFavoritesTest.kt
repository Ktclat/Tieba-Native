package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.capturePhoneReview
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFromStoreExtra
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelFavoritesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()

    private fun favorite(id: Long, title: String, forum: String, name: String) =
        ThreadStoreBean.ThreadStoreInfo(
            threadId = id.toString(), title = title, forumName = forum,
            author = ThreadStoreBean.AuthorInfo(lzUid = id.toString(), name = name, nameShow = name),
            media = emptyList(), isDeleted = "0", lastTime = "0", type = "1", status = "0",
            maxPid = "${id}060", minPid = "${id}001", markPid = "0", markStatus = "0",
            postNo = "60", postNoMsg = "", count = "0",
        )

    @Composable
    private fun FavoritesScreen(
        state: ThreadStoreUiState,
        progress: Map<String, FavoriteReadingPosition>,
        navigator: NavHostController,
    ) {
        MyScaffold(backgroundColor = Color.Transparent, topBar = {
            TitleCentredToolbar(
                title = stringResource(R.string.title_my_collect),
                navigationIcon = { BackNavigationIcon(onBackPressed = {}) },
            )
        }) { padding ->
            FavoriteListContent(
                state = state, progress = progress, onRefresh = {}, onLoadMore = {},
                onOpen = { info, position ->
                    val route = checkNotNull(info.threadDestination(position, defaultOnlyAuthor = false, descending = false))
                    navigator.navigate(route.route)
                },
                onUserClick = {}, onDelete = {},
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    @Test fun searchSavedThreadsFilterProgressAndResumeAPartiallyReadThread() {
        val trail = favorite(11, "山脊徒步路线手记", "徒步", "青禾")
        val studio = favorite(22, "街头夜景拍摄笔记", "摄影", "远山")
        val lens = favorite(33, "山间晨雾的曝光记录", "摄影", "南星")
        val marker = FavoriteReadingPosition(11012L, 12L, true)
        val completed = FavoriteReadingPosition(lens.maxPid.toLong(), 60L, false)
        val state = ThreadStoreUiState(data = listOf(trail, studio, lens), hasMore = false)
        val progress = mapOf(trail.threadId to marker, lens.threadId to completed)
        lateinit var navigator: NavHostController
        val activity = compose.activity

        compose.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(activity)) {
                    TiebaLiteTheme {
                        navigator = rememberNavController()
                        NavHost(navigator, startDestination = "favorites") {
                            composable("favorites") { FavoritesScreen(state, progress, navigator) }
                            composable(ThreadPageDestination.route, ThreadPageDestination.arguments) {}
                        }
                    }
                }
            }
        }
        fun shown(item: ThreadStoreBean.ThreadStoreInfo) = compose.onNodeWithText(item.title, useUnmergedTree = true).assertIsDisplayed()
        fun absent(item: ThreadStoreBean.ThreadStoreInfo) = compose.onNodeWithText(item.title, useUnmergedTree = true).assertDoesNotExist()

        fun select(label: String) = compose.onNode(hasText(label) and hasClickAction()).performClick()
        val field = compose.onNode(hasSetTextAction())
        shown(trail)
        shown(studio)
        shown(lens)
        compose.capturePhoneReview("favorites-overview")
        field.performTextReplacement("夜景")
        shown(studio)
        absent(trail)
        absent(lens)
        field.performTextReplacement("青禾")
        shown(trail)
        absent(studio)
        field.performTextReplacement("摄影")
        shown(studio)
        shown(lens)
        select("无进度")
        shown(studio)
        absent(trail)
        absent(lens)
        compose.capturePhoneReview("favorites-search")
        field.performTextReplacement("")
        select("有进度")
        shown(trail)
        shown(lens)
        absent(studio)
        select("读到一半")
        shown(trail)
        absent(lens)
        compose.onNodeWithText("读到第 12 楼", useUnmergedTree = true).assertIsDisplayed()
        compose.capturePhoneReview("favorites-in-progress")
        compose.onNodeWithText(trail.title, useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle {
            val entry = checkNotNull(navigator.currentBackStackEntry)
            assertEquals(ThreadPageDestination.route, entry.destination.route)
            val args = ThreadPageDestination.argsFrom(entry.arguments)
            assertEquals(trail.threadId.toLong(), args.threadId)
            assertEquals(marker.postId, args.postId)
            assertEquals(marker.onlyAuthor, args.seeLz)
            assertEquals(ThreadPageFrom.FROM_STORE, args.from)
            assertEquals(ThreadPageFromStoreExtra(trail.maxPid.toLong(), 60), args.extra)
        }
    }
}

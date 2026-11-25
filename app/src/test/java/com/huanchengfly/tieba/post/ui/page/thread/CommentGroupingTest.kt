package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.subPosts
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowSizeClass
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItem as ReplyItem
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItemData as ReplyData
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsHeading
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommentGroupingTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var view: View
    private var opened by mutableStateOf<Pair<Long, Long>?>(null)
    private var replyTarget: Long? = null

    private val floors = listOf(
        floor(12, "沿溪寻找野花的人", "这一段山路适合清晨出发。", listOf(
            reply(1201, "春山", "六点的雾还没散。"),
            reply(1202, "听风", "可以从北门上山吗？", recipient = 1211 to "春山"),
        )),
        floor(13, "北窗", "我从南门走，沿溪会轻松些。", listOf(
            reply(1301, "林间月", "我也选南门。", recipient = 13 to "北窗"),
            reply(1302, "河岸", "昨天和 @小岚 一起走过。").copy(
                content = listOf(PbContent(text = "昨天和 "), PbContent(type = 4, uid = 88, text = "@小岚"), PbContent(text = " 一起走过。")),
            ),
        )),
        floor(14, "远舟", "午后水位涨了，过桥时留意。", emptyList()),
    )

    private fun reply(id: Long, name: String, text: String, recipient: Pair<Int, String>? = null) = SubPostList(
        id = id, author = User(id = id + 10, name = name, nameShow = name),
        time = 1695372600 + id.toInt(),
        content = buildList {
            if (recipient != null) {
                add(PbContent(text = "回复 "))
                add(PbContent(type = 4, uid = recipient.first.toLong(), text = "@${recipient.second}"))
                add(PbContent(text = "："))
            }
            add(PbContent(text = text))
        },
    )

    private fun floor(number: Int, name: String, text: String, replies: List<SubPostList>) = Post(
        id = number.toLong(), floor = number, time = 1695372600 + number * 60,
        author = User(id = number.toLong(), name = name, nameShow = name, ip_address = "浙江"),
        content = listOf(PbContent(text = text)), sub_post_number = replies.size,
        sub_post_list = SubPost(sub_post_list = replies),
    )

    @OptIn(ExperimentalFoundationApi::class)
    private fun show(width: Int, scale: Float) {
        ThemeUtils.init(App.ThemeDelegate)
        compose.setContent {
            val currentView = LocalView.current
            SideEffect { view = currentView }
            val window = WindowSizeClass.calculateFromSize(DpSize(width.dp, 800.dp))
            CompositionLocalProvider(LocalWindowSizeClass provides window) {
                TiebaLiteTheme {
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                        ProvideNavigator(EmptyDestinationsNavigator) {
                            val parent = floors.find { it.id == opened?.first }
                            key(parent?.id) {
                                MyLazyColumn(Modifier.fillMaxSize().background(ExtendedTheme.colors.background)
                                    .testTag("comment-list")) {
                                    if (parent == null) {
                                        items(floors, key = { it.id }) { post ->
                                            Container(Modifier.testTag("floor-${post.id}")) {
                                                PostCard(
                                                    postHolder = post.wrapImmutable(),
                                                    contentRenders = post.contentRenders,
                                                    subPosts = post.subPosts,
                                                    onOpenSubPosts = { child -> opened = post.id to child },
                                                )
                                            }
                                        }
                                    } else {
                                        val children = parent.sub_post_list!!.sub_post_list
                                        item {
                                            PostCard(
                                                parent.wrapImmutable(), parent.contentRenders,
                                                showSubPosts = false, showFloorDivider = false,
                                            )
                                        }
                                        stickyHeader(key = "SubPostsHeader") { SubPostsHeading(children.size) }
                                        items(children, key = { it.id }) { child ->
                                            Box(Modifier.testTag("detail-${child.id}")) {
                                                ReplyItem(ReplyData(child), onReplyClick = { replyTarget = it.id })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun adjacentFloorsAndTheirReplies() = review(360, 1f, "360")

    @Test
    @Config(qualifiers = "zh-rCN-w320dp-h800dp-mdpi")
    fun adjacentFloorsAndTheirRepliesAtLargeFont() = review(320, 1.3f, "320-large")

    private fun review(width: Int, scale: Float, suffix: String) {
        show(width, scale)
        val first = floors[0]
        val second = floors[1]
        text(first.content.single().text).assertIsDisplayed()
        compose.onNodeWithText(second.author!!.nameShow, useUnmergedTree = true).assertIsDisplayed()
        compose.captureReviewImage(view, "adjacent-floors-$suffix.png")

        val firstGroup = compose.onNodeWithTag("floor-${first.id}").fetchSemanticsNode().boundsInRoot
        val likes = compose.onNode(
            hasAnyAncestor(hasTestTag("floor-${first.id}")) and hasContentDescription(App.INSTANCE.getString(R.string.title_agree)),
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val firstPreview = compose.onNodeWithText("春山", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Parent action must precede the first reply", likes.bottom <= firstPreview.top)
        assertVisibleSeparator("adjacent-floors-$suffix.png", firstGroup.bottom)

        compose.onNodeWithTag("comment-list").performScrollToIndex(1)
        val children = second.sub_post_list!!.sub_post_list
        val target = children.last()
        val targetBody = target.content.joinToString("") { it.text }
        text(targetBody).assertIsDisplayed().performTouchInput { click(percentOffset(0.85f, 0.5f)) }
        compose.runOnIdle { assertEquals(second.id to target.id, opened) }
        text(first.author!!.nameShow).assertDoesNotExist()
        text(second.content.single().text).assertIsDisplayed()
        text(children.first().content.last().text).assertIsDisplayed()
        text(targetBody).assertIsDisplayed()
        compose.captureReviewImage(view, "adjacent-subreplies-$suffix.png")
        val recipient = compose.onNodeWithText(App.INSTANCE.getString(R.string.hint_reply, "@北窗"), useUnmergedTree = true)
        val recipientBounds = recipient.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val directedBody = text(children.first().content.last().text).fetchSemanticsNode().boundsInRoot
        val parentBody = text(second.content.single().text).fetchSemanticsNode().boundsInRoot
        val plainBody = text(targetBody).fetchSemanticsNode().boundsInRoot
        assertTrue("Directed reply label must precede its body", recipientBounds.bottom <= directedBody.top)
        assertTrue("Child replies need a distinct indentation from the parent", directedBody.left > parentBody.left)
        assertEquals("Sibling body alignment", directedBody.left, plainBody.left, 1f)
        compose.onNodeWithText(App.INSTANCE.getString(R.string.hint_reply, "@小岚")).assertDoesNotExist()
        val firstReply = compose.onNodeWithTag("detail-${children.first().id}").fetchSemanticsNode().boundsInRoot
        assertVisibleSeparator("adjacent-subreplies-$suffix.png", firstReply.bottom)
        text(targetBody).performTouchInput { click(percentOffset(0.85f, 0.5f)) }
        compose.runOnIdle { assertEquals(target.id, replyTarget) }
    }

    // At mdpi, the final pixel row crosses the inset divider and the outer gutter.
    private fun assertVisibleSeparator(fileName: String, bottom: Float) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            val y = bottom.roundToInt() - 1
            val line = image.getPixel(image.width / 2, y)
            assertNotEquals("No separator across the adjacent items", image.getPixel(0, y), line)
            assertNotEquals("Boundary color should differ from the surface above it", image.getPixel(image.width / 2, y - 1), line)
        } finally {
            image.recycle()
        }
    }

    private fun text(value: String) = compose.onNodeWithText(value, substring = true, useUnmergedTree = true)
}

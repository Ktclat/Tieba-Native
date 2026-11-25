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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Agree
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
import androidx.compose.material3.MaterialTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowSizeClass
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItem as ReplyItem
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItemData as ReplyData
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsHeading
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import java.io.File
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
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
    private var appSurface = 0
    private lateinit var view: View
    private var opened by mutableStateOf<Pair<Long, Long>?>(null)
    private var replyTarget: Long? = null
    private val liked = mutableListOf<Long>()
    private val profiles = mutableListOf<Long>()

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
        id = id, author = User(id = id + 10, name = name, nameShow = name), agree = Agree(),
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
                    val background = MaterialTheme.colorScheme.surface.toArgb()
                    SideEffect { appSurface = background }
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                        ProvideNavigator(EmptyDestinationsNavigator) {
                            val parent = floors.find { it.id == opened?.first }
                            key(parent?.id) {
                                MyLazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                                    .testTag("comment-list")) {
                                    if (parent == null) {
                                        items(floors, key = { it.id }) { post ->
                                            Container(Modifier.testTag("floor-${post.id}")) {
                                                PostCard(
                                                    postHolder = post.wrapImmutable(),
                                                    contentRenders = post.contentRenders,
                                                    subPosts = post.subPosts,
                                                    onAgree = { liked += post.id },
                                                    onSubPostAgree = { liked += it.id },
                                                    onUserClick = { profiles += it.id },
                                                    onOpenSubPosts = { child -> opened = post.id to child },
                                                )
                                            }
                                        }
                                    } else {
                                        val children = parent.sub_post_list!!.sub_post_list
                                        item {
                                            PostCard(
                                                parent.wrapImmutable(), parent.contentRenders,
                                                showSubPosts = false, discussionRole = DiscussionRole.ParentFloor,
                                            )
                                        }
                                        stickyHeader(key = "SubPostsHeader") {
                                            Box(Modifier.testTag("reply-heading")) { SubPostsHeading(children.size) }
                                        }
                                        items(children, key = { it.id }) { child ->
                                            Box(Modifier.testTag("detail-${child.id}")) {
                                                ReplyItem(
                                                    ReplyData(child), onAgree = { liked += it.id },
                                                    onUserClick = { profiles += it.id },
                                                    onReplyClick = { replyTarget = it.id },
                                                )
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

    @Test
    fun adjacentFloorsAndTheirRepliesInDarkTheme() {
        val theme = ThemeUtil.themeState
        val previous = theme.value
        try {
            theme.value = ThemeUtil.THEME_GREY_DARK
            review(360, 1f, "360-dark", checkDarkPanel = true)
        } finally {
            theme.value = previous
        }
    }

    private fun review(width: Int, scale: Float, suffix: String, checkDarkPanel: Boolean = false) {
        show(width, scale)
        val first = floors[0]
        val second = floors[1]
        text(first.content.single().text).assertIsDisplayed()
        compose.onNodeWithText(second.author!!.nameShow, useUnmergedTree = true).assertIsDisplayed()
        compose.captureReviewImage(view, "adjacent-floors-$suffix.png")

        val firstGroup = compose.onNodeWithTag("floor-${first.id}").fetchSemanticsNode().boundsInRoot
        val firstPreview = compose.onNodeWithText("春山", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val previewChildren = first.sub_post_list!!.sub_post_list
        val floorLike = tapHeaderLike(first.author!!, first.content.single().text)
        val previewLikes = previewChildren.map { child ->
            tapHeaderLike(child.author!!, child.content.last().text)
        }
        compose.runOnIdle {
            assertEquals(listOf(first.id) + previewChildren.map { it.id }, liked)
            assertEquals(listOf(first.author!!.id) + previewChildren.map { it.author!!.id }, profiles)
            assertNull(opened)
        }
        val threadImage = "adjacent-floors-$suffix.png"
        assertGlyphContrast(threadImage, "floor like" to floorLike, "preview like" to previewLikes.first(), minimum = 3.0)
        assertWhitespace(threadImage, firstGroup.bottom, clearance = 8)
        val lastPreviewName = compose.onNodeWithText("听风", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val firstPreviewBody = text(previewChildren.first().content.last().text).fetchSemanticsNode().boundsInRoot
        assertTrue("Preview replies must have breathing room", lastPreviewName.top - firstPreviewBody.bottom >= 12f)
        assertNoRule(threadImage, firstPreviewBody.bottom, lastPreviewName.top)
        val previewRecipient = App.INSTANCE.getString(R.string.hint_reply, "@春山")
        assertGlyphContrast(
            "adjacent-floors-$suffix.png",
            "floor body" to text(first.content.single().text).fetchSemanticsNode().boundsInRoot,
            "preview body" to text("六点的雾还没散。").fetchSemanticsNode().boundsInRoot,
            "preview recipient" to text(previewRecipient).fetchSemanticsNode().boundsInRoot,
        )
        if (checkDarkPanel) {
            assertDarkPanel("adjacent-floors-$suffix.png", firstPreview.center.y)
        }

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
        assertGlyphContrast(
            "adjacent-subreplies-$suffix.png",
            "detail parent" to parentBody,
            "detail reply" to directedBody,
            "detail recipient" to recipientBounds,
        )
        assertTrue("Directed reply label must precede its body", recipientBounds.bottom <= directedBody.top)
        val detailAuthor = compose.onNodeWithText(children.first().author!!.nameShow, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Short author and recipient should share a row", detailAuthor.top < recipientBounds.bottom &&
            recipientBounds.top < detailAuthor.bottom && detailAuthor.right <= recipientBounds.left)
        fun start(bounds: Rect) = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) -bounds.right else bounds.left
        val expectedIndent = with(compose.density) { 8.dp.toPx() }
        assertEquals("Expanded replies share an 8dp inset from the parent floor", expectedIndent, start(directedBody) - start(parentBody), 1f)
        assertEquals("Sibling body alignment", start(directedBody), start(plainBody), 1f)
        compose.onNodeWithText(App.INSTANCE.getString(R.string.hint_reply, "@小岚")).assertDoesNotExist()
        val firstReply = compose.onNodeWithTag("detail-${children.first().id}").fetchSemanticsNode().boundsInRoot
        val detailImage = "adjacent-subreplies-$suffix.png"
        val heading = compose.onNodeWithTag("reply-heading").fetchSemanticsNode().boundsInRoot
        assertNoRule(detailImage, heading.top - 2f, heading.top + 2f)
        assertWhitespace(detailImage, firstReply.bottom, clearance = 2)
        assertNoReplyRails(detailImage, directedBody, plainBody)
        val replyLikes = children.map { child -> tapHeaderLike(child.author!!, child.content.last().text) }
        assertGlyphContrast(detailImage, "detail like" to replyLikes.first(), minimum = 3.0)
        compose.runOnIdle {
            assertEquals(children.map { it.id }, liked.takeLast(children.size))
            assertEquals(children.map { it.author!!.id }, profiles.takeLast(children.size))
            assertNull(replyTarget)
        }
        text(targetBody).performTouchInput { click(percentOffset(0.85f, 0.5f)) }
        compose.runOnIdle { assertEquals(target.id, replyTarget) }
    }

    private fun tapHeaderLike(author: User, body: String): Rect {
        val name = compose.onNodeWithText(author.nameShow, useUnmergedTree = true)
        val nameBounds = name.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val bodyTop = text(body).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        val buttons = compose.onAllNodes(hasClickAction() and hasContentDescription(App.INSTANCE.getString(R.string.title_agree)))
        val matches = buttons.fetchSemanticsNodes().map { it.boundsInRoot }.filter { rect ->
            rect.left >= nameBounds.right && rect.top < nameBounds.bottom && rect.bottom > nameBounds.top
        }
        assertEquals("Expected one like action beside ${author.nameShow}", 1, matches.size)
        val target = matches.single()
        assertTrue("Action must precede its body", target.bottom <= bodyTop)
        assertTrue("Touch target must be at least 48dp at mdpi", target.width >= 48f && target.height >= 48f)
        name.performTouchInput { click() }
        compose.onRoot().performTouchInput { click(target.center) }
        return target
    }

    private fun assertGlyphContrast(fileName: String, vararg labels: Pair<String, Rect>, minimum: Double = 4.5) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            for ((name, bounds) in labels) {
                val left = bounds.left.roundToInt()
                val background = image.getPixel(left - 1, bounds.center.y.roundToInt())
                var legiblePixels = 0
                for (y in bounds.top.roundToInt() until bounds.bottom.roundToInt()) {
                    for (x in left until bounds.right.roundToInt()) {
                        if (ColorUtils.calculateContrast(image.getPixel(x, y), background) >= minimum) {
                            legiblePixels++
                        }
                    }
                }
                assertTrue("$name needs visible glyph strokes at $minimum:1 in $fileName", legiblePixels >= 8)
            }
        } finally {
            image.recycle()
        }
    }

    private fun assertDarkPanel(fileName: String, previewCenterY: Float) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            val y = previewCenterY.roundToInt()
            val surface = image.getPixel(0, y)
            assertEquals("The page draws the active Material surface", appSurface, surface)
            assertTrue("Night mode should draw a dark surface", ColorUtils.calculateLuminance(surface) < 0.18)
            val railX = (1 until image.width / 2).first { image.getPixel(it, y) != surface }
            val panel = image.getPixel(railX + 3, y)
            assertNotEquals("Preview panel must have its own surface", surface, panel)
            assertEquals("Preview edge must open directly onto the tonal panel", panel, image.getPixel(railX, y))
        } finally {
            image.recycle()
        }
    }

    private fun assertWhitespace(fileName: String, junction: Float, clearance: Int) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            val boundary = junction.roundToInt()
            for (y in boundary - clearance until boundary + clearance) {
                val surface = image.getPixel(0, y)
                assertTrue("Expected a clear row at y=$y in $fileName",
                    (0 until image.width).all { x -> image.getPixel(x, y) == surface })
            }
        } finally {
            image.recycle()
        }
    }

    private fun assertNoReplyRails(fileName: String, firstBody: Rect, secondBody: Rect) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            fun railAt(body: Rect): List<Int> {
                val y = body.center.y.roundToInt()
                val surface = image.getPixel(0, y)
                return (1 until body.left.roundToInt()).filter { x -> image.getPixel(x, y) != surface }
            }
            val firstRail = railAt(firstBody)
            assertTrue("No rail beside the first sibling", firstRail.isEmpty())
            assertTrue("No rail beside the next sibling", railAt(secondBody).isEmpty())
        } finally {
            image.recycle()
        }
    }

    private fun assertNoRule(fileName: String, top: Float, bottom: Float) {
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", fileName).absolutePath)
        try {
            for (y in top.roundToInt()..bottom.roundToInt()) {
                val line = image.getPixel(image.width / 2, y)
                val crossesRow = (image.width / 4 until image.width * 3 / 4 step 4).all { x ->
                    image.getPixel(x, y) == line && image.getPixel(x, y - 1) != line && image.getPixel(x, y + 1) != line
                }
                assertTrue("Unexpected horizontal divider at y=$y in $fileName", !crossesRow)
            }
        } finally {
            image.recycle()
        }
    }

    private fun text(value: String) = compose.onNodeWithText(value, substring = true, useUnmergedTree = true)
}

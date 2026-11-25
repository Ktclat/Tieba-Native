package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItem as DetailReplyItem
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItemData as DetailReply
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommentLayoutTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var view: View
    private var details by mutableStateOf(false)
    private val first = SubPostList(
        id = 110, floor = 1, time = 1695373200,
        author = User(id = 12, name = "在巷子尽头记录街头光影的摄影师", nameShow = "在巷子尽头记录街头光影的摄影师",
            level_id = 7, ip_address = "广东"),
        agree = Agree(diffAgreeNum = 12),
        content = listOf(PbContent(text = "我也喜欢这张照片，路灯下的阴影很有层次。")),
    )
    private val second = SubPostList(
        id = 111, floor = 2, time = 1695373500,
        author = User(id = 13, name = "午后散步", nameShow = "午后散步", level_id = 3),
        content = listOf(PbContent(text = "第二张是用多长的焦距拍的？")),
    )
    private val post = Post(
        id = 100, floor = 128, time = 1695372600,
        author = User(id = 11, name = "沿着海岸线慢慢走的风景摄影爱好者", nameShow = "沿着海岸线慢慢走的风景摄影爱好者",
            level_id = 12, ip_address = "上海"),
        agree = Agree(diffAgreeNum = 26),
        content = listOf(PbContent(text = "周末经过老街，等到傍晚才拍下这束光。调整构图时我把前景留给了走过的路人。")),
        sub_post_number = 3,
        sub_post_list = SubPost(sub_post_list = listOf(first, second)),
    )

    private fun show(scale: Float) {
        compose.setContent {
            val renderingView = LocalView.current
            SideEffect { view = renderingView }
            TiebaLiteTheme {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                    ProvideNavigator(EmptyDestinationsNavigator) {
                        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())) {
                            Box(Modifier.testTag("floor")) {
                                PostCard(post.wrapImmutable(), post.contentRenders, post.subPosts,
                                    threadAuthorId = 11, showSubPosts = !details, onSubPostAgree = {})
                            }
                            if (details) {
                                for (reply in listOf(first, second)) {
                                    Box(Modifier.testTag("reply-${reply.id}")) {
                                        DetailReplyItem(DetailReply(reply), threadAuthorId = 11)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun normalSizeThreadAndSubreplies() = review(1f, "360")

    @Test
    @Config(qualifiers = "zh-rCN-w320dp-h800dp-mdpi")
    fun largeFontsOnCompactThreadAndSubreplies() = review(1.3f, "320-large")

    private fun review(scale: Float, size: String) {
        show(scale)
        val context = App.INSTANCE
        val previewName = textIn("floor", first.author!!.name)
        val previewBody = textIn("floor", first.content.single().text)
        compose.captureReviewImage(view, "comment-thread-$size.png")
        val previewNameBounds = assertFullyVisible(previewName)
        val previewBodyBounds = assertFullyVisible(previewBody)
        assertTopAction("floor", "26", textIn("floor", post.author!!.name), textIn("floor", post.content.single().text))
        assertTopAction("floor", "12", previewName, previewBody)
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag("floor")) and hasClickAction() and
                hasContentDescription(context.getString(R.string.title_agree)),
        ).assertCountEquals(2) // The second preview has no vote state.
        for (metadata in listOf(
            DateTimeUtils.getRelativeTimeString(context, first.time.toLong()),
            context.getString(R.string.text_profile_ip_location, first.author!!.ip_address),
        )) {
            val bounds = assertFullyVisible(textIn("floor", metadata))
            assertTrue("Preview metadata overlaps author or body",
                previewNameBounds.bottom <= previewBodyBounds.top && bounds.top >= previewBodyBounds.bottom)
        }
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag("floor")) and
                hasText(context.getString(R.string.text_profile_ip_location, ""), substring = true),
            useUnmergedTree = true,
        ).assertCountEquals(2)

        compose.runOnIdle { details = true }
        compose.captureReviewImage(view, "comment-replies-$size.png")
        assertCommentOrder(
            "floor", post.author!!.name, post.content.single().text, listOf(
                DateTimeUtils.getRelativeTimeString(context, post.time.toLong()),
                context.getString(R.string.tip_post_floor, post.floor),
                context.getString(R.string.text_profile_ip_location, post.author!!.ip_address),
            ),
        )
        assertCommentOrder(
            "reply-${first.id}", first.author!!.name, first.content.single().text, listOf(
                DateTimeUtils.getRelativeTimeString(context, first.time.toLong()),
                context.getString(R.string.text_profile_ip_location, first.author!!.ip_address),
            ), metadataAfterBody = true,
        )
        assertTopAction("reply-${first.id}", "12", textIn("reply-${first.id}", first.author!!.name),
            textIn("reply-${first.id}", first.content.single().text))
        compose.onAllNodes(
            hasAnyAncestor(hasTestTag("reply-${second.id}")) and
                hasText(context.getString(R.string.text_profile_ip_location, ""), substring = true),
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    private fun assertTopAction(
        section: String, count: String, author: SemanticsNodeInteraction, body: SemanticsNodeInteraction,
    ) {
        val action = compose.onNode(
            hasAnyAncestor(hasTestTag(section)) and hasClickAction() and
                hasContentDescription(App.INSTANCE.getString(R.string.title_agree)) and hasText(count),
        ).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val name = assertFullyVisible(author)
        val bodyBounds = assertFullyVisible(body)
        assertTrue("Like must be alongside its own author", action.left >= name.right &&
            action.top < name.bottom && action.bottom > name.top)
        assertTrue("Like must stay above the body", action.bottom <= bodyBounds.top)
        assertTrue("Like needs a 48dp touch target at mdpi", action.width >= 48f && action.height >= 48f)
        val layouts = mutableListOf<TextLayoutResult>()
        body.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read -> check(read(layouts)) }
        val bodyRight = bodyBounds.left + layouts.single().layoutInput.constraints.maxWidth
        assertEquals("The body must extend to the header action’s trailing edge", bodyRight, action.right, 2f)
    }

    private fun textIn(section: String, value: String) = compose.onNode(
        hasAnyAncestor(hasTestTag(section)) and hasText(value),
        useUnmergedTree = true,
    )

    private fun assertCommentOrder(section: String, name: String, body: String, metadata: List<String>, metadataAfterBody: Boolean = false) {
        val nameBounds = assertFullyVisible(textIn(section, name))
        val bodyBounds = assertFullyVisible(textIn(section, body))
        assertTrue("Body remains indented below the avatar in $section", bodyBounds.left < nameBounds.left)
        val metadataBounds = metadata.map { assertFullyVisible(textIn(section, it)) }
        for ((index, bounds) in metadataBounds.withIndex()) {
            assertTrue("Metadata overlaps author or body in $section",
                if (metadataAfterBody) nameBounds.bottom <= bodyBounds.top && bounds.top >= bodyBounds.bottom
                else bounds.top >= nameBounds.bottom && bounds.bottom <= bodyBounds.top)
            for (other in metadataBounds.drop(index + 1)) {
                assertFalse("Metadata labels overlap in $section", bounds.overlaps(other))
            }
        }
    }

    private fun assertFullyVisible(node: SemanticsNodeInteraction): Rect {
        val bounds = node.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read -> check(read(layouts)) }
        val result = layouts.single()
        val lastLine = result.lineCount - 1
        assertEquals(result.layoutInput.text.length, result.getLineEnd(lastLine, visibleEnd = true))
        assertFalse(result.didOverflowHeight)
        for (line in 0..lastLine) {
            assertFalse("Unexpected ellipsis", result.isLineEllipsized(line))
            assertTrue("Line exceeds visible text bounds", result.getLineRight(line) - result.getLineLeft(line) <= bounds.width)
        }
        assertTrue("Last line is clipped", result.getLineBottom(lastLine) - result.getLineTop(0) <= bounds.height)
        return bounds
    }
}

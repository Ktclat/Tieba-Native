package com.huanchengfly.tieba.post.ui.page.thread

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.subPosts
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItem
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItemData
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsHeading
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsTopBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class PixelDiscussionReviewTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val target get() = instrumentation.targetContext

    private fun picture(name: String): PicContentRender {
        val file = File(target.cacheDir, "$name.png")
        instrumentation.context.assets.open(file.name).use { source ->
            file.outputStream().use { source.copyTo(it) }
        }
        return PicContentRender(Uri.fromFile(file).toString(), "", false, 0, 720, 284, name)
    }

    @Test fun reviewThreadFiltersAndOpenNestedReplies() {
        val topicPicture = picture("trail-map")
        val floorPicture = picture("trail-profile")
        val activity = compose.activity
        compose.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(activity)) {
                    TiebaLiteTheme {
                        ProvideNavigator(EmptyDestinationsNavigator) { Discussion(topicPicture, floorPicture) }
                    }
                }
            }
        }
        compose.onNodeWithText(DiscussionSample.topicTitle).assertIsDisplayed()
        capture("discussion-topic", Color.rgb(165, 83, 50))

        val onlyAuthor = compose.onNode(hasText(target.getString(R.string.title_see_lz)) and hasClickAction())
        onlyAuthor.performClick().assertIsSelected()
        compose.onNodeWithText(DiscussionSample.floor.content.single().text).assertDoesNotExist()
        compose.onNode(hasText(target.getString(R.string.text_all)) and hasClickAction()).performClick().assertIsSelected()
        compose.onNodeWithTag("discussion").performScrollToIndex(1)
        val firstReply = compose.onNodeWithText(DiscussionSample.children.first().content.last().text, useUnmergedTree = true)
        firstReply.assertIsDisplayed()
        capture("discussion-preview", Color.rgb(36, 75, 71))

        // A physical tap on the body bubbles to the production reply preview's clickable parent.
        firstReply.performTouchInput { click() }
        compose.onNodeWithText(target.getString(R.string.title_sub_posts_header, DiscussionSample.children.size)).assertIsDisplayed()
        capture("discussion-expanded", Color.rgb(36, 75, 71))
    }

    private fun capture(name: String, pictureColor: Int) {
        val directory = File(target.filesDir, "phone-review").apply { mkdirs() }
        compose.waitForIdle()
        compose.waitUntil(15_000) {
            val raw = instrumentation.uiAutomation.takeScreenshot() ?: return@waitUntil false
            val image = if (raw.config == Bitmap.Config.HARDWARE) {
                raw.copy(Bitmap.Config.ARGB_8888, false).also { raw.recycle() }
            } else raw
            try {
                // Wait for the actual decoded sample image, rather than accepting an image placeholder.
                var matches = 0
                for (y in 0 until image.height step 8) for (x in 0 until image.width step 8) {
                    val pixel = image.getPixel(x, y)
                    if (abs(Color.red(pixel) - Color.red(pictureColor)) <= 3 &&
                        abs(Color.green(pixel) - Color.green(pictureColor)) <= 3 &&
                        abs(Color.blue(pixel) - Color.blue(pictureColor)) <= 3) matches++
                }
                if (matches < 16) return@waitUntil false
                File(directory, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally {
                image.recycle()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun Discussion(topicPicture: PicContentRender, floorPicture: PicContentRender) {
        val topic = DiscussionSample.topic
        val floor = DiscussionSample.floor
        var onlyAuthor by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }
        val topicContent = remember { (topic.contentRenders + topicPicture).toImmutableList() }
        val floorContent = remember { (floor.contentRenders + floorPicture).toImmutableList() }
        MyScaffold(
            topBar = {
                if (expanded) SubPostsTopBar(floor.floor, { expanded = false }, { expanded = false })
                else ThreadTopBar(SimpleForum(name = "徒步").wrapImmutable(), {}, {})
            },
            bottomBar = {
                if (expanded) ReplyPrompt(false, null, {}, Modifier.navigationBarsPadding().padding(horizontal = 12.dp))
                else ThreadBottomBar(User().wrapImmutable(), {}, {}, {}, agreeNum = 18)
            },
        ) { padding ->
            key(expanded) {
                MyLazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(padding).testTag("discussion")) {
                    if (expanded) {
                        item {
                            PostCard(floor.wrapImmutable(), floorContent, showSubPosts = false, discussionRole = DiscussionRole.ParentFloor)
                        }
                        stickyHeader { SubPostsHeading(DiscussionSample.children.size) }
                        items(DiscussionSample.children, key = { it.id }) { child -> SubPostItem(SubPostItemData(child)) }
                    } else {
                        item {
                            TopicSurface {
                                PostCard(topic.wrapImmutable(), topicContent, showSubPosts = false, discussionRole = DiscussionRole.Topic,
                                    threadAuthorId = topic.author!!.id, threadTitle = DiscussionSample.topicTitle)
                            }
                        }
                        stickyHeader { ThreadRepliesHeader(269, onlyAuthor) { onlyAuthor = it } }
                        if (!onlyAuthor) item {
                            PostCard(floor.wrapImmutable(), floorContent, floor.subPosts, threadAuthorId = topic.author!!.id,
                                onOpenSubPosts = { expanded = true }, onSubPostAgree = {})
                        }
                    }
                }
            }
        }
    }
}

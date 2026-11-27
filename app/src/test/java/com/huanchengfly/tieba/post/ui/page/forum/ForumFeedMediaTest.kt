package com.huanchengfly.tieba.post.ui.page.forum

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideShouldLoad
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ForumFeedMediaTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    private fun show(thread: ThreadInfo) {
        compose.setContent {
            TiebaLiteTheme {
                ProvideShouldLoad(false) {
                    FeedCard(thread.wrapImmutable(), onClick = {}, onAgree = {})
                }
            }
        }
        compose.onNodeWithText(thread.title).assertIsDisplayed()
    }

    @Test fun rendersQuoteWhenDeletedSourceHasNoId() {
        show(ThreadInfo(
            title = "转发帖",
            is_share_thread = 1,
            origin_thread_info = OriginThreadInfo(is_deleted = 1, content = listOf(PbContent(text = "引文仍然可读"))),
        ))
        compose.onNodeWithText("引文仍然可读").assertIsDisplayed()
    }

    @Test fun rendersVideoWithoutThumbnailDimensions() {
        show(ThreadInfo(title = "视频帖", videoInfo = VideoInfo(videoUrl = "https://example.invalid/video.mp4")))
    }
}

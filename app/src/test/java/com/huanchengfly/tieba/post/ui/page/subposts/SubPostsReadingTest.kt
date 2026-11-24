package com.huanchengfly.tieba.post.ui.page.subposts

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.getContentText
import com.huanchengfly.tieba.post.api.models.protos.previewBody
import com.huanchengfly.tieba.post.arch.wrapImmutable
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SubPostsReadingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyAndMediaOnlyPreviewsAreReadableWithoutImageDimensions() {
        assertEquals("", SubPostList().previewBody.text)
        assertEquals("", SubPostList().getContentText().text)
        assertEquals("", SubPostList(content = listOf(PbContent(type = 99))).previewBody.text)
        val media = listOf(
            PbContent(type = 20, src = "https://example.org/image.jpg"),
            PbContent(type = 10, voiceMD5 = "sound"),
            PbContent(type = 5, src = "https://example.org/frame.jpg", bsize = "0,0"),
        )
        assertEquals("[图片][语音][视频]", SubPostList(content = media).previewBody.text)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun targetedReplyIsLocatedAfterItArrivesAndLaterPaginationPreservesPosition() {
        fun replies(count: Int) = (1..count).map {
            SubPostItemData(SubPostList(id = it.toLong()).wrapImmutable(), persistentListOf(), blocked = false)
        }
        val rows = mutableStateOf(replies(6))
        val target = mutableStateOf(0L)
        compose.setContent {
            val list = rememberLazyListState()
            PositionSubPostOnOpen(list, target.value, rows.value)
            LazyColumn(state = list, modifier = Modifier.height(160.dp).testTag("replies")) {
                item { Text("parent", Modifier.height(70.dp).testTag("parent")) }
                stickyHeader(key = "SubPostsHeader") { Text("replies", Modifier.height(30.dp)) }
                items(rows.value, key = { it.id }) {
                    Text("reply ${it.id}", Modifier.height(52.dp).testTag("reply-${it.id}"))
                }
            }
        }
        compose.onNodeWithTag("parent").assertIsDisplayed()
        compose.runOnIdle { target.value = 17 }
        compose.onNodeWithTag("parent").assertIsDisplayed()
        compose.runOnIdle { rows.value = replies(25) }
        compose.onNodeWithTag("reply-17").assertIsDisplayed()
        compose.onNodeWithTag("replies").performScrollToIndex(0)
        compose.runOnIdle { rows.value = replies(35) }
        compose.onNodeWithTag("parent").assertIsDisplayed()
    }
}

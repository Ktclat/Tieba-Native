package com.huanchengfly.tieba.post.ui.page.photoview

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoActionBarTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun compactControlsAlwaysActOnTheDisplayedPhoto() {
        val photos = listOf(
            PhotoViewItem("", "first.png", null, 1),
            PhotoViewItem("", "second.gif", null, 2),
        )
        var page by mutableIntStateOf(0)
        var shared = ""
        var saved = ""
        compose.setContent {
            TiebaLiteTheme {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.BottomCenter) {
                    PhotoActionBar(
                        photos[page], photos.size, true, true, { shared = it }, { saved = it },
                        modifier = Modifier.testTag("photo-actions"),
                    )
                }
            }
        }
        val bar = compose.onNodeWithTag("photo-actions")
        val share = compose.onNodeWithContentDescription("分享图片")
        val save = compose.onNodeWithContentDescription("下载图片")
        bar.assertHeightIsEqualTo(48.dp)
        val bounds = bar.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width <= 190f && bounds.width > 2 * bounds.height)
        share.assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
        save.assertHeightIsEqualTo(48.dp).assertWidthIsAtLeast(48.dp)
        assertTrue(share.fetchSemanticsNode().boundsInRoot.right <= save.fetchSemanticsNode().boundsInRoot.left)
        compose.onNodeWithText("1 / 2").assertIsDisplayed()
        share.performClick()
        assertEquals("first.png", shared)
        compose.runOnIdle { page = 1 }
        compose.onNodeWithText("2 / 2").assertIsDisplayed()
        share.performClick()
        save.performClick()
        assertEquals("second.gif", shared)
        assertEquals("second.gif", saved)
    }
}

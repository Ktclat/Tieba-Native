package com.huanchengfly.tieba.post.ui.widgets.compose.video

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class VideoSeekBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun seekingFromTheEndOfTheVideoPreviewsAndCommitsTheRequestedTime() {
        val preview = mutableListOf<Long>()
        val committed = mutableListOf<Long>()
        compose.setContent {
            MaterialTheme {
                SeekBar(
                    progress = 60_000, max = 60_000, enabled = true,
                    onSeek = preview::add, onSeekStopped = committed::add,
                )
            }
        }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        compose.runOnIdle {
            assertEquals(listOf(15_000L), preview)
            assertEquals(preview, committed)
        }
    }
}

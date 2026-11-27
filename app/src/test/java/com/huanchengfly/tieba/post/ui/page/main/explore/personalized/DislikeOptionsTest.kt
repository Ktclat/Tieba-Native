package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.wrapImmutable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DislikeOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun submitsSelectedReasonsOnlyAndDiscardsCancelledSelection() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val action = app.getString(R.string.button_submit_dislike)
        val reasons = listOf(
            DislikeReason(dislikeId = 7, dislikeReason = "内容重复", extra = "duplicate"),
            DislikeReason(dislikeId = 7, dislikeReason = "减少此类推荐", extra = "topic"),
        )
        val recommendation = ThreadPersonalized(tid = 123, dislikeResource = reasons).wrapImmutable()
        val submissions = mutableListOf<List<DislikeReason>>()
        compose.setContent {
            MaterialTheme {
                Dislike(recommendation) { openedAt, selected ->
                    assertTrue(openedAt > 0)
                    submissions += selected.map { it.get() }
                }
            }
        }

        compose.onNodeWithContentDescription(action).performClick()
        compose.onNodeWithText(reasons[0].dislikeReason).performClick()
        compose.onNodeWithText(app.getString(R.string.button_cancel)).performClick()
        compose.runOnIdle { assertTrue(submissions.isEmpty()) }

        compose.onNodeWithContentDescription(action).performClick()
        compose.onNodeWithText(reasons[1].dislikeReason).performClick()
        compose.onNodeWithText(action).performClick()
        compose.runOnIdle { assertEquals(listOf(listOf(reasons[1])), submissions) }
    }
}

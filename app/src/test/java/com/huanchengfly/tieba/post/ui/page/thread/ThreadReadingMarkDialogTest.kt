package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.R
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ThreadReadingMarkDialogTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val saveAndReturn get() = compose.onNodeWithText(context.getString(R.string.save_reading_mark_and_return))
    private val returnDirectly get() = compose.onNodeWithText(context.getString(R.string.return_without_saving_reading_mark))

    @Test fun savesTheCapturedFloorAndLeavesOnlyAfterAnAcknowledgedRetry() {
        val attempts = mutableListOf<Long>()
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        var exits = 0
        compose.setContent {
            MaterialTheme {
                ThreadReadingMarkDialog(
                    ThreadFloorKey(41, 7),
                    saveMark = { id ->
                        attempts += id
                        (if (attempts.size == 1) first else second).await()
                    },
                    onDismiss = {}, onLeave = { exits++ },
                )
            }
        }
        saveAndReturn.performClick()
        returnDirectly.assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(41L), attempts)
            assertEquals(0, exits)
            first.completeExceptionally(IOException("offline"))
        }
        compose.onNodeWithText(context.getString(R.string.message_update_collect_mark_failed, "offline")).assertExists()
        saveAndReturn.performClick()
        compose.runOnIdle {
            assertEquals(listOf(41L, 41L), attempts)
            assertEquals(0, exits)
            second.complete(Unit)
        }
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun returningDirectlyLeavesWithoutSendingARequest() {
        var exits = 0
        compose.setContent {
            ThreadReadingMarkDialog(
                ThreadFloorKey(42, 8),
                saveMark = { error("Unexpected bookmark save") },
                onDismiss = {}, onLeave = { exits++ },
            )
        }
        returnDirectly.performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun disposingTheScreenCancelsTheRequestWithoutNavigating() {
        var show by mutableStateOf(true)
        var cancelled = false
        var exits = 0
        compose.setContent {
            if (show) ThreadReadingMarkDialog(
                ThreadFloorKey(42, 8),
                saveMark = { try { awaitCancellation() } finally { cancelled = true } },
                onDismiss = {}, onLeave = { exits++ },
            )
        }
        saveAndReturn.performClick()
        returnDirectly.assertIsNotEnabled()
        compose.runOnIdle { show = false }
        compose.runOnIdle {
            assertTrue(cancelled)
            assertEquals(0, exits)
        }
    }
}

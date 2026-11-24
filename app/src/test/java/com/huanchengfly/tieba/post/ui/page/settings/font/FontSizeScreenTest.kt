package com.huanchengfly.tieba.post.ui.page.settings.font

import android.app.Application
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExpressiveShapes
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExpressiveTypography
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
@Config(sdk = [28], application = Application::class, qualifiers = "w412dp-h915dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontSizeScreenTest {
    @get:Rule val compose = createComposeRule()
    private var saves = 0
    private var savedDraft = -1
    private var backs = 0
    private var saveError by mutableStateOf<String?>(null)
    private lateinit var renderingView: View

    private fun show(initial: Int = 4, dark: Boolean = false, largeControls: Boolean = false) {
        compose.setContent {
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
                typography = ExpressiveTypography,
                shapes = ExpressiveShapes,
            ) {
                val density = LocalDensity.current
                val view = LocalView.current
                SideEffect { renderingView = view }
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeControls) 1.3f else 1f)) {
                    var draft by remember { mutableIntStateOf(initial) }
                    FontSizeScreen(
                        savedIndex = 4,
                        draftIndex = draft,
                        onDraftChange = { draft = it },
                        onSave = { saves++; savedDraft = draft },
                        onBack = { backs++ },
                        saveError = saveError,
                    )
                }
            }
        }
    }

    private fun scrollToControls() {
        compose.onNodeWithTag("font-content").performScrollToIndex(2)
    }

    @Test fun previewDoesNotSaveAndSaveCommitsOnlyChosenStep() {
        show()
        compose.onNodeWithTag("font-save").assertIsNotEnabled()
        scrollToControls()
        compose.onNodeWithTag("font-increase").performScrollTo().performClick()
        compose.onNodeWithTag("font-percentage").assertTextEquals("105%")
        compose.runOnIdle { assertEquals(0, saves) }
        compose.onNodeWithTag("font-save").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, saves); assertEquals(5, savedDraft) }
    }

    @Test fun backDiscardsAnUnsavedDraft() {
        show(initial = 8)
        compose.onNodeWithTag("font-back").performClick()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(0, saves) }
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun failedSaveShowsMessageAndAllowsRetryWithoutScrolling() {
        show(initial = 7, largeControls = true)
        val save = compose.onNodeWithTag("font-save")
        save.assertIsDisplayed().performClick()
        val message = compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(7, savedDraft)
            renderingView.context.getString(R.string.native_font_save_failed).also { saveError = it }
        }
        compose.onNodeWithTag("font-save-error")
            .assertIsDisplayed()
            .assertTextEquals(message)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        save.assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(2, saves)
            assertEquals(7, savedDraft)
        }
    }

    @Test fun resetRestoresDefaultAndDisablesUnnecessarySave() {
        show(initial = 10)
        scrollToControls()
        compose.onNodeWithTag("font-reset").performScrollTo().performClick()
        compose.onNodeWithTag("font-percentage").assertTextEquals("100%")
        compose.onNodeWithTag("font-save").assertIsNotEnabled()
    }

    @Test fun sliderSupportsAccessibilityProgressAction() {
        show()
        scrollToControls()
        compose.onNodeWithTag("font-slider").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        compose.onNodeWithTag("font-percentage").assertTextEquals("115%")
        compose.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun minimumStepCannotDecrease() {
        show(initial = 0)
        scrollToControls()
        compose.onNodeWithTag("font-decrease").performScrollTo().assertIsNotEnabled()
    }

    @Test fun lightLayoutKeepsSaveVisibleAndProducesReviewImage() {
        show()
        compose.onNodeWithTag("font-save").assertIsDisplayed()
        compose.captureReviewImage(renderingView, "font-light.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun compactDarkLayoutSupportsLargeFontsAndScroll() {
        show(initial = 10, dark = true, largeControls = true)
        compose.onNodeWithTag("font-save").assertIsDisplayed()
        scrollToControls()
        compose.onNodeWithTag("font-increase").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("font-reset").performScrollTo().assertIsDisplayed()
        // Preserve a review image even when a later layout assertion fails.
        compose.captureReviewImage(renderingView, "font-compact-dark-130.png")
        assertEquals("Reset label must stay on one line on a compact window", 1, assertTextFits("font-reset-label").lineCount)
    }

    @Test
    @Config(qualifiers = "w180dp-h568dp")
    fun narrowWindowKeepsPercentageAndAllFontControlsUsable() {
        show(initial = 10, dark = true, largeControls = true)
        scrollToControls()
        val percentage = compose.onNodeWithTag("font-percentage")
        val decrease = compose.onNodeWithTag("font-decrease")
        val increase = compose.onNodeWithTag("font-increase")
        percentage.assertIsDisplayed().assertTextEquals("130%")
        assertTextFits("font-percentage")
        val percentageBounds = percentage.fetchSemanticsNode().boundsInRoot
        assertFalse(percentageBounds.overlaps(decrease.fetchSemanticsNode().boundsInRoot))
        assertFalse(percentageBounds.overlaps(increase.fetchSemanticsNode().boundsInRoot))
        decrease.assertIsDisplayed().performClick()
        percentage.assertTextEquals("125%")
        increase.assertIsDisplayed().performClick()
        percentage.assertTextEquals("130%")
        val reset = compose.onNodeWithTag("font-reset")
        reset.performScrollTo().assertIsDisplayed()
        compose.captureReviewImage(renderingView, "font-narrow-dark-130.png")
        assertTrue("Reset label must wrap within the narrow window", assertTextFits("font-reset-label").lineCount > 1)
        reset.assertIsEnabled().performClick()
        compose.onNodeWithTag("font-percentage").assertTextEquals("100%")
        reset.assertIsNotEnabled()
    }

    private fun assertTextFits(tag: String): TextLayoutResult {
        val label = compose.onNodeWithTag(tag, useUnmergedTree = true)
        val textLayouts = mutableListOf<TextLayoutResult>()
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            check(action(textLayouts))
        }
        val result = textLayouts.single()
        val lastLine = result.lineCount - 1
        assertEquals(result.layoutInput.text.length, result.getLineEnd(lastLine, visibleEnd = true))
        assertFalse("$tag must fit vertically", result.didOverflowHeight)

        // Compose 1.7 rebuilds the semantics paragraph at the parent's maximum width.
        // Compare the drawn lines with the visible label, excluding unused paragraph space.
        val visibleBounds = label.fetchSemanticsNode().boundsInRoot
        for (line in 0..lastLine) {
            assertFalse("$tag must not be ellipsized", result.isLineEllipsized(line))
            val lineWidth = result.getLineRight(line) - result.getLineLeft(line)
            assertTrue("$tag line $line exceeds width $visibleBounds", lineWidth <= visibleBounds.width)
        }
        val height = result.getLineBottom(lastLine) - result.getLineTop(0)
        assertTrue("$tag exceeds height $visibleBounds", height <= visibleBounds.height)
        return result
    }

}

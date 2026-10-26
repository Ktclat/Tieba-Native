package com.huanchengfly.tieba.post.ui.page.settings.font

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
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
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExpressiveShapes
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExpressiveTypography
import java.io.File
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
                    )
                }
            }
        }
    }

    private fun scrollToControls() {
        // Off-screen LazyColumn items may not exist in the semantics tree yet.
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
        capture("font-light.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun compactDarkLayoutSupportsLargeFontsAndScroll() {
        show(initial = 10, dark = true, largeControls = true)
        compose.onNodeWithTag("font-save").assertIsDisplayed()
        scrollToControls()
        compose.onNodeWithTag("font-increase").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("font-reset").assertIsDisplayed()
        val textLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("font-reset-label", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                check(action(textLayouts))
            }
        assertEquals("Reset label must stay on one line on a compact window", 1, textLayouts.single().lineCount)
        assertFalse("Reset label must not clip at 130% scale", textLayouts.single().hasVisualOverflow)
        capture("font-compact-dark-130.png")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Robolectric has no real window compositor. Draw its actual native
        // Android Canvas instead of waiting for a device PixelCopy redraw fence.
        val image = compose.runOnIdle {
            check(renderingView.width > 0 && renderingView.height > 0)
            Bitmap.createBitmap(renderingView.width, renderingView.height, Bitmap.Config.ARGB_8888).also {
                renderingView.draw(Canvas(it))
            }
        }
        try {
            val samples = mutableSetOf<Int>()
            for (y in 0 until image.height step 7) {
                for (x in 0 until image.width step 7) samples.add(image.getPixel(x, y))
            }
            assertTrue("Review image must contain rendered content, not a blank canvas", samples.size > 4)
            val directory = File("build/reports/ui-screenshots").apply { mkdirs() }
            File(directory, name).outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            image.recycle()
        }
    }
}

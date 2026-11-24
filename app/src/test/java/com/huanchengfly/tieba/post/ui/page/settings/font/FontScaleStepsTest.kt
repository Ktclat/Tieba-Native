package com.huanchengfly.tieba.post.ui.page.settings.font

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontScaleStepsTest {
    @Test fun allElevenStepsRoundTripWithoutDrift() {
        for (index in 0..FontScaleSteps.LAST_INDEX) {
            assertEquals(index, FontScaleSteps.indexForScale(FontScaleSteps.scaleForIndex(index)))
            assertEquals(80 + index * 5, FontScaleSteps.percentForIndex(index))
        }
    }
    @Test fun defaultIsExactlyOneHundredPercent() {
        assertEquals(4, FontScaleSteps.indexForScale(1f))
        assertEquals(1f, FontScaleSteps.scaleForIndex(FontScaleSteps.DEFAULT_INDEX), 0f)
    }
    @Test fun clampsExtremeIndicesWithoutIntegerOverflow() {
        assertEquals(0.8f, FontScaleSteps.scaleForIndex(Int.MIN_VALUE), 0f)
        assertEquals(1.3f, FontScaleSteps.scaleForIndex(Int.MAX_VALUE), 0f)
    }
    @Test fun clampsPersistedOutOfRangeValues() {
        assertEquals(0, FontScaleSteps.indexForScale(-100f))
        assertEquals(10, FontScaleSteps.indexForScale(100f))
    }
    @Test fun invalidPersistedValuesUseDefault() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach {
            assertEquals(FontScaleSteps.DEFAULT_INDEX, FontScaleSteps.indexForScale(it))
        }
    }
    @Test fun normalizesToNearestSupportedStep() {
        assertEquals(1.05f, FontScaleSteps.normalized(1.06f), 0.0001f)
        assertEquals(0.95f, FontScaleSteps.normalized(0.94f), 0.0001f)
    }
    @Test fun stepsAreStrictlyIncreasing() {
        (0 until FontScaleSteps.LAST_INDEX).forEach {
            assertTrue(FontScaleSteps.scaleForIndex(it + 1) > FontScaleSteps.scaleForIndex(it))
        }
    }
}

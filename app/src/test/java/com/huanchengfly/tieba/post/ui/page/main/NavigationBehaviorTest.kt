package com.huanchengfly.tieba.post.ui.page.main

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationBehaviorTest {
    @Test fun dragCanSelectAdjacentOrDistantSegments() {
        assertEquals(2, resolveNavigationDragTarget(1, 105f, 100f, 4))
        assertEquals(3, resolveNavigationDragTarget(1, 250f, 100f, 4))
        assertEquals(0, resolveNavigationDragTarget(2, -210f, 100f, 4))
    }

    @Test fun tinyDragDoesNotChangeSelection() {
        assertEquals(2, resolveNavigationDragTarget(2, 34f, 100f, 4))
    }

    @Test fun dragClampsToExistingSegments() {
        assertEquals(0, resolveNavigationDragTarget(0, -500f, 100f, 4))
        assertEquals(3, resolveNavigationDragTarget(3, 500f, 100f, 4))
        assertEquals(0, resolveNavigationDragTarget(0, 100f, 0f, 0))
    }

    @Test fun negativeScrollHidesPositiveScrollRestoresNavigation() {
        assertEquals(0.5f, updateNavigationHideFraction(0f, -50f, 100f), 0.0001f)
        assertEquals(1f, updateNavigationHideFraction(0.8f, -50f, 100f), 0.0001f)
        assertEquals(0f, updateNavigationHideFraction(0.2f, 50f, 100f), 0.0001f)
        assertEquals(0.25f, updateNavigationHideFraction(0.5f, 25f, 100f), 0.0001f)
    }

    @Test fun invalidMeasurementsDoNotCorruptState() {
        assertEquals(0.4f, updateNavigationHideFraction(0.4f, Float.NaN, 100f), 0.0001f)
        assertEquals(0.4f, updateNavigationHideFraction(0.4f, -15f, 0f), 0.0001f)
        assertEquals(2, resolveNavigationDragTarget(2, Float.NaN, 100f, 4))
    }
}

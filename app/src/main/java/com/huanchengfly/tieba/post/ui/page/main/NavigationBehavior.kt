package com.huanchengfly.tieba.post.ui.page.main

import kotlin.math.roundToInt

/**
 * Pure navigation calculations, independent of Compose and Android views.
 * Kept small so drag and scroll behavior can be tested on the JVM.
 */
internal fun resolveNavigationDragTarget(
    selectedIndex: Int,
    dragDistancePx: Float,
    itemWidthPx: Float,
    itemCount: Int,
): Int {
    if (itemCount <= 0) return 0
    val selected = selectedIndex.coerceIn(0, itemCount - 1)
    if (!dragDistancePx.isFinite() || !itemWidthPx.isFinite() || itemWidthPx <= 0f) {
        return selected
    }
    return (selected + (dragDistancePx / itemWidthPx).roundToInt())
        .coerceIn(0, itemCount - 1)
}

/**
 * Positive dy means scrolling back toward the top: restore the navigation bar.
 * Negative dy means scrolling down the content: progressively hide it.
 */
internal fun updateNavigationHideFraction(
    current: Float,
    consumedDyPx: Float,
    distancePx: Float,
): Float {
    val safeCurrent = if (current.isFinite()) current.coerceIn(0f, 1f) else 0f
    if (!consumedDyPx.isFinite() || !distancePx.isFinite() || distancePx <= 0f) {
        return safeCurrent
    }
    return (safeCurrent - consumedDyPx / distancePx).coerceIn(0f, 1f)
}

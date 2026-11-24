package com.huanchengfly.tieba.post.ui.page.settings.font

import kotlin.math.roundToInt

/** Integer indices avoid floating point drift while retaining the upstream fontScale values. */
internal object FontScaleSteps {
    const val MIN = 0.8f
    const val MAX = 1.3f
    const val STEP = 0.05f
    const val LAST_INDEX = 10
    const val DEFAULT_INDEX = 4
    const val DEFAULT_SCALE = 1f

    fun indexForScale(scale: Float): Int {
        if (!scale.isFinite()) return DEFAULT_INDEX
        return ((scale.coerceIn(MIN, MAX) - MIN) / STEP).roundToInt()
            .coerceIn(0, LAST_INDEX)
    }

    fun scaleForIndex(index: Int): Float = percentForIndex(index) / 100f

    fun percentForIndex(index: Int): Int = 80 + index.coerceIn(0, LAST_INDEX) * 5

    fun normalized(scale: Float): Float = scaleForIndex(indexForScale(scale))
}
